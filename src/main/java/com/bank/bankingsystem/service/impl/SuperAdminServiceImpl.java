package com.bank.bankingsystem.service.impl;

import com.bank.bankingsystem.dto.request.AdminUpdateCustomerRequest;
import com.bank.bankingsystem.dto.request.RoleChangeRequest;
import com.bank.bankingsystem.dto.response.AccountResponse;
import com.bank.bankingsystem.dto.response.AuditLogResponse;
import com.bank.bankingsystem.dto.response.CustomerDetailResponse;
import com.bank.bankingsystem.dto.response.CustomerResponse;
import com.bank.bankingsystem.dto.response.TransactionResponse;
import com.bank.bankingsystem.dto.response.UserResponse;
import com.bank.bankingsystem.exception.BusinessRuleException;
import com.bank.bankingsystem.exception.ResourceNotFoundException;
import com.bank.bankingsystem.exception.UnauthorizedAccountAccessException;
import com.bank.bankingsystem.model.Account;
import com.bank.bankingsystem.model.Customer;
import com.bank.bankingsystem.model.Transaction;
import com.bank.bankingsystem.model.User;
import com.bank.bankingsystem.model.enums.AccountStatus;
import com.bank.bankingsystem.model.enums.Role;
import com.bank.bankingsystem.model.enums.TransactionType;
import com.bank.bankingsystem.model.enums.UserStatus;
import com.bank.bankingsystem.repository.AccountRepository;
import com.bank.bankingsystem.repository.AdminAuditLogRepository;
import com.bank.bankingsystem.repository.CustomerRepository;
import com.bank.bankingsystem.repository.TransactionRepository;
import com.bank.bankingsystem.repository.UserRepository;
import com.bank.bankingsystem.service.SuperAdminService;
import com.bank.bankingsystem.util.CurrentUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminServiceImpl.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AdminAuditLogRepository auditRepository;

    public SuperAdminServiceImpl(UserRepository userRepository,
                                 CustomerRepository customerRepository,
                                 AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 AdminAuditLogRepository auditRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditRepository = auditRepository;
    }

    // =========================================================
    // USERS
    // =========================================================
    @Override
    public List<UserResponse> listAllUsers() {
        requireSuperAdmin();
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse promoteToAdmin(Long userId, RoleChangeRequest req) {
        requireSuperAdmin();
        if (req.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Only promotion to ADMIN is allowed via this endpoint");
        }
        return changeRole(userId, Role.ADMIN);
    }

    @Override
    @Transactional
    public UserResponse demoteToCustomer(Long userId, RoleChangeRequest req) {
        requireSuperAdmin();
        if (req.getRole() != Role.CUSTOMER) {
            throw new BusinessRuleException("Only demotion to CUSTOMER is allowed via this endpoint");
        }
        User target = mustFind(userId);
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessRuleException("Cannot demote SUPER_ADMIN");
        }
        return changeRole(userId, Role.CUSTOMER);
    }

    @Override
    @Transactional
    public UserResponse changeUserStatus(Long userId, String status) {
        requireSuperAdmin();
        User target = mustFind(userId);
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessRuleException("Cannot change SUPER_ADMIN status");
        }
        UserStatus newStatus;
        try {
            newStatus = UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Invalid status. Allowed: ACTIVE, INACTIVE, LOCKED");
        }
        userRepository.updateStatus(userId, newStatus);
        auditRepository.log(CurrentUserUtil.currentUserId(),
                "USER_STATUS_CHANGE", "USER", userId,
                "set status to " + newStatus);
        log.info("SUPER_ADMIN changed status of userId={} to {}", userId, newStatus);
        return UserResponse.from(userRepository.findById(userId).orElseThrow());
    }

    // =========================================================
    // ADJUST CREDIT — audited deposit
    // =========================================================
    @Override
    @Transactional
    public TransactionResponse adjustCredit(Long accountId, BigDecimal amount, String reason) {
        requireSuperAdmin();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: id=" + accountId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Account is " + account.getStatus().name().toLowerCase() + " — cannot credit");
        }

        Account locked = accountRepository.findByIdForUpdate(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        BigDecimal before = locked.getBalance();
        int rows = accountRepository.credit(locked.getId(), amount);
        if (rows == 0) {
            throw new BusinessRuleException("Credit rejected: account not active");
        }

        Account after = accountRepository.findById(locked.getId()).orElseThrow();

        String ref = "ADJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        String desc = "ADMIN ADJUSTMENT: " + reason.trim();

        Long txId = transactionRepository.insert(new Transaction(
                null,
                ref,
                locked.getId(),
                null,
                TransactionType.DEPOSIT,
                amount,
                before,
                after.getBalance(),
                desc,
                null));

        auditRepository.log(CurrentUserUtil.currentUserId(),
                "ADJUST_CREDIT", "ACCOUNT", accountId,
                "credited " + amount + " reason=" + reason.trim());

        log.info("SUPER_ADMIN adjust credit: accountId={} amount={} reason={}",
                accountId, amount, reason.trim());

        return TransactionResponse.from(transactionRepository.findById(txId).orElseThrow());
    }

    // =========================================================
    // CUSTOMER DETAIL + EDIT
    // =========================================================
    @Override
    public CustomerDetailResponse getCustomerDetail(Long customerId) {
        requireSuperAdmin();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: id=" + customerId));

        User user = userRepository.findById(customer.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AccountResponse> accounts = accountRepository.findByCustomerId(customerId).stream()
                .map(AccountResponse::from)
                .toList();

        return new CustomerDetailResponse(
                UserResponse.from(user),
                CustomerResponse.from(customer),
                accounts);
    }

    @Override
    @Transactional
    public CustomerDetailResponse updateCustomer(Long customerId, AdminUpdateCustomerRequest req) {
        requireSuperAdmin();
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: id=" + customerId));

        customer.setFirstName(trimOrNull(req.getFirstName()));
        customer.setMiddleName(trimOrNull(req.getMiddleName()));
        customer.setLastName(trimOrNull(req.getLastName()));
        customer.setPhone(trimOrNull(req.getPhone()));
        customer.setAddress(trimOrNull(req.getAddress()));

        customerRepository.update(customer);
        auditRepository.log(CurrentUserUtil.currentUserId(),
                "CUSTOMER_UPDATE", "CUSTOMER", customerId,
                "profile edited");

        return getCustomerDetail(customerId);
    }

    // =========================================================
    // AUDIT LOG
    // =========================================================
    @Override
    public List<AuditLogResponse> listAuditLog(int page, int size) {
        requireSuperAdmin();
        int p = Math.max(0, page);
        int s = Math.min(Math.max(1, size), 200);
        return auditRepository.findRecent(s, p * s).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private UserResponse changeRole(Long userId, Role role) {
        User target = mustFind(userId);
        if (target.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessRuleException("Cannot modify SUPER_ADMIN role");
        }
        userRepository.updateRole(userId, role);
        auditRepository.log(CurrentUserUtil.currentUserId(),
                "ROLE_CHANGE", "USER", userId,
                "changed role to " + role);
        log.info("SUPER_ADMIN set role of userId={} to {}", userId, role);
        return UserResponse.from(userRepository.findById(userId).orElseThrow());
    }

    private User mustFind(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: id=" + userId));
    }

    private void requireSuperAdmin() {
        if (!CurrentUserUtil.isSuperAdmin()) {
            throw new UnauthorizedAccountAccessException("SUPER_ADMIN role required");
        }
    }

    private static String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}

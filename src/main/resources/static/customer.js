(function () {
  'use strict';

  var TOKEN_KEY = 'banking.customer.token';
  var NAME_KEY  = 'banking.customer.username';
  var THEME_KEY = 'banking.theme';

  function $(id) { return document.getElementById(id); }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function money(v) {
    if (v == null) return '—';
    var n = Number(v);
    return isNaN(n) ? String(v) : n.toLocaleString('en-PH', { style: 'currency', currency: 'PHP' });
  }
  function api(path, opts) {
    opts = opts || {};
    var headers = { 'Content-Type': 'application/json' };
    var t = localStorage.getItem(TOKEN_KEY);
    if (t) headers.Authorization = 'Bearer ' + t;
    return fetch(path, { method: opts.method || 'GET', headers: headers, body: opts.body })
      .then(function (res) {
        return res.json().catch(function () { return null; }).then(function (b) {
          if (res.status === 401) { logout(); throw new Error('Session expired'); }
          if (!res.ok) throw new Error((b && b.message) || (res.status + ' error'));
          return b;
        });
      });
  }
  var toastTimer;
  function toast(msg, kind) {
    var t = $('toast');
    if (!t) return;
    t.textContent = msg;
    t.className = 'toast ' + (kind || 'ok');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.classList.add('hidden'); }, 3200);
  }

  // ---------- THEME ----------
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    var btn = $('theme-toggle');
    if (btn) btn.textContent = theme === 'light' ? '\u263D' : '\u2600';
  }
  applyTheme(localStorage.getItem(THEME_KEY) || 'dark');

  // ---------- GLOBAL CLICKS ----------
  document.addEventListener('click', function (e) {
    if (e.target.closest && e.target.closest('#theme-toggle')) {
      var cur = document.documentElement.getAttribute('data-theme') || 'dark';
      var next = cur === 'light' ? 'dark' : 'light';
      localStorage.setItem(THEME_KEY, next);
      applyTheme(next); return;
    }
    var eye = e.target.closest && e.target.closest('.eye');
    if (eye) {
      e.preventDefault();
      var input = document.getElementById(eye.getAttribute('data-target'));
      if (input) {
        var show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        if (show) eye.classList.add('revealed'); else eye.classList.remove('revealed');
        input.focus();
      }
      return;
    }
    if (e.target.id === 'to-register') {
      e.preventDefault();
      $('login-view').classList.add('hidden');
      $('register-view').classList.remove('hidden');
      return;
    }
    if (e.target.id === 'to-login') {
      e.preventDefault();
      $('register-view').classList.add('hidden');
      $('login-view').classList.remove('hidden');
      return;
    }
    if (e.target.closest && e.target.closest('#logout-btn')) { logout(); return; }
    var tab = e.target.closest && e.target.closest('.tab');
    if (tab && tab.dataset.tab) {
      document.querySelectorAll('.tab').forEach(function (b) { b.classList.remove('active'); });
      tab.classList.add('active');
      document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.add('hidden'); });
      var panel = document.getElementById('tab-' + tab.dataset.tab);
      if (panel) panel.classList.remove('hidden');
      if (tab.dataset.tab === 'profile') Cust.loadProfile();
      if (tab.dataset.tab === 'transfer') Cust.loadTransferSource();
    }
  });

  // ---------- AUTH ----------
  function saveAuth(d) {
    localStorage.setItem(TOKEN_KEY, d.token);
    localStorage.setItem(NAME_KEY, d.username);
  }
  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    $('login-view').classList.remove('hidden');
    $('register-view').classList.add('hidden');
    $('app-view').classList.add('hidden');
    Cust.closeModal(); Cust.closeTx(); Cust.closeNewAcct();
  }
  function bootApp() {
    $('login-view').classList.add('hidden');
    $('register-view').classList.add('hidden');
    $('app-view').classList.remove('hidden');
    $('who-username').textContent = localStorage.getItem(NAME_KEY) || '—';
    Cust.loadAccounts();
  }

  document.addEventListener('submit', function (e) {

    // LOGIN
    if (e.target.id === 'login-form') {
      e.preventDefault();
      var btn = $('login-btn'), err = $('login-error');
      err.textContent = ''; btn.disabled = true; btn.textContent = 'Signing in…';
      fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          usernameOrEmail: $('login-identifier').value.trim(),
          password: $('login-password').value
        })
      }).then(function (r) { return r.json().then(function (b) { return { ok: r.ok, body: b }; }); })
        .then(function (r) {
          if (!r.ok || !r.body || !r.body.success) throw new Error((r.body && r.body.message) || 'Login failed');
          if (r.body.data.role !== 'CUSTOMER' && r.body.data.role !== 'ADMIN' && r.body.data.role !== 'SUPER_ADMIN') {
            throw new Error('Unsupported role: ' + r.body.data.role);
          }
          saveAuth(r.body.data);
          toast('Welcome, ' + r.body.data.username);
          bootApp();
        })
        .catch(function (ex) { err.textContent = ex.message; })
        .finally(function () { btn.disabled = false; btn.textContent = 'Sign in'; });
      return;
    }

    // REGISTER
    if (e.target.id === 'register-form') {
      e.preventDefault();
      var rbtn = $('register-btn'), rerr = $('register-error');
      rerr.textContent = ''; rbtn.disabled = true; rbtn.textContent = 'Creating…';

      var payload = {
        username: $('reg-username').value.trim(),
        email: $('reg-email').value.trim(),
        password: $('reg-password').value,
        firstName: $('reg-first').value.trim(),
        lastName: $('reg-last').value.trim(),
        phone: $('reg-phone').value.trim() || null,
        address: $('reg-address').value.trim() || null
      };
      fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      }).then(function (r) { return r.json().then(function (b) { return { ok: r.ok, body: b }; }); })
        .then(function (r) {
          if (!r.ok || !r.body || !r.body.success) throw new Error((r.body && r.body.message) || 'Registration failed');
          saveAuth(r.body.data);
          toast('Account created — welcome!');
          bootApp();
        })
        .catch(function (ex) { rerr.textContent = ex.message; })
        .finally(function () { rbtn.disabled = false; rbtn.textContent = 'Create account'; });
      return;
    }

    // DEPOSIT / WITHDRAW
    if (e.target.id === 'tx-form') {
      e.preventDefault();
      var kind = $('tx-kind').value;   // 'deposit' or 'withdraw'
      var accId = $('tx-account-id').value;
      var amount = Number($('tx-amount').value);
      var desc = $('tx-desc').value.trim();
      var tmsg = $('tx-msg');
      tmsg.textContent = 'Submitting…';

      api('/api/accounts/' + accId + '/' + kind, {
        method: 'POST',
        body: JSON.stringify({ amount: amount, description: desc || null })
      }).then(function () {
        toast(kind === 'deposit' ? 'Deposit successful' : 'Withdrawal successful');
        Cust.closeTx();
        Cust.loadAccounts();
      }).catch(function (ex) { tmsg.textContent = ex.message; });
      return;
    }

    // NEW ACCOUNT
    if (e.target.id === 'newacct-form') {
      e.preventDefault();
      var type = $('newacct-type').value;
      var nmsg = $('newacct-msg');
      nmsg.textContent = 'Creating…';
      api('/api/accounts', { method: 'POST', body: JSON.stringify({ accountType: type }) })
        .then(function (r) {
          toast('Account ' + r.data.accountNumber + ' created');
          Cust.closeNewAcct();
          Cust.loadAccounts();
        })
        .catch(function (ex) { nmsg.textContent = ex.message; });
      return;
    }

    // TRANSFER
    if (e.target.id === 'transfer-form') {
      e.preventDefault();
      var from = $('transfer-from').value;
      var to = $('transfer-to').value.trim();
      var amt = Number($('transfer-amount').value);
      var desc = $('transfer-desc').value.trim();
      var msg = $('transfer-msg');
      msg.textContent = 'Submitting…';

      api('/api/transfers', {
        method: 'POST',
        body: JSON.stringify({
          sourceAccountNumber: from,
          destinationAccountNumber: to,
          amount: amt,
          description: desc || null
        })
      }).then(function (r) {
        msg.textContent = 'Transfer complete — ref ' + r.data.referenceNumber;
        toast('Transfer successful');
        Cust.loadAccounts();
        Cust.loadTransferSource();
      }).catch(function (ex) { msg.textContent = ex.message; });
      return;
    }

    // PROFILE
    if (e.target.id === 'profile-form') {
      e.preventDefault();
      var pmsg = $('prof-msg');
      pmsg.textContent = 'Saving…';
      var payload = {};
      var f = $('prof-first').value.trim(); if (f) payload.firstName = f;
      var l = $('prof-last').value.trim();  if (l) payload.lastName  = l;
      var ph = $('prof-phone').value.trim(); if (ph) payload.phone    = ph;
      var ad = $('prof-address').value.trim(); if (ad) payload.address = ad;

      api('/api/customers/me', { method: 'PUT', body: JSON.stringify(payload) })
        .then(function () { pmsg.textContent = 'Saved ✓'; })
        .catch(function (ex) { pmsg.textContent = ex.message; });
      return;
    }
  });

  // ---------- ACCOUNTS LIST ----------
  function loadAccounts() {
    var box = $('accounts-list');
    if (!box) return;
    box.innerHTML = '<p class="empty">Loading…</p>';
    api('/api/accounts').then(function (r) {
      var list = (r && r.data) || [];
      if (!list.length) {
        box.innerHTML = '<p class="empty">No accounts yet. Click “New Account” to open one.</p>';
        return;
      }
      box.innerHTML = list.map(function (a) {
        return '<div class="acct-row">' +
          '<div>' +
            '<div><strong>' + esc(a.accountType) + '</strong> ' +
            '<span class="badge ' + esc(a.status) + '">' + esc(a.status) + '</span></div>' +
            '<div class="acct-num">' + esc(a.accountNumber) + '</div>' +
          '</div>' +
          '<div style="text-align:right">' +
            '<div class="acct-bal">' + esc(money(a.balance)) + '</div>' +
            '<div style="margin-top:6px">' +
              '<button class="btn small" data-acc-view="' + a.id + '" data-acc-num="' + esc(a.accountNumber) + '">History</button>' +
              '<button class="btn small primary" data-acc-tx="deposit"  data-acc-id="' + a.id + '" data-acc-num="' + esc(a.accountNumber) + '">Deposit</button>' +
              '<button class="btn small warn"    data-acc-tx="withdraw" data-acc-id="' + a.id + '" data-acc-num="' + esc(a.accountNumber) + '">Withdraw</button>' +
            '</div>' +
          '</div>' +
        '</div>';
      }).join('');
      Cust.loadTransferSource();
    }).catch(function (ex) {
      box.innerHTML = '<p class="error">' + esc(ex.message) + '</p>';
    });
  }

  // ---------- TRANSFER SOURCE SELECT ----------
  function loadTransferSource() {
    var sel = $('transfer-from');
    if (!sel) return;
    api('/api/accounts').then(function (r) {
      var list = (r && r.data) || [];
      var active = list.filter(function (a) { return a.status === 'ACTIVE'; });
      sel.innerHTML = active.length
        ? active.map(function (a) {
            return '<option value="' + esc(a.accountNumber) + '">' + esc(a.accountNumber) +
                   ' — ' + esc(money(a.balance)) + '</option>';
          }).join('')
        : '<option value="">No active accounts</option>';
    }).catch(function () {});
  }

  // ---------- HISTORY ----------
  function openModal(accountId, accountNumber) {
    $('acct-modal').classList.remove('hidden');
    $('acct-title').textContent = 'Account ' + accountNumber;
    $('acct-content').innerHTML = 'Loading…';
    api('/api/accounts/' + accountId + '/transactions?page=0&size=50').then(function (r) {
      var rows = (r && r.data) || [];
      if (!rows.length) { $('acct-content').innerHTML = '<p class="empty">No transactions yet.</p>'; return; }
      var html = '<table class="grid tx"><thead><tr><th>When</th><th>Type</th><th>Amount</th><th>Before</th><th>After</th><th>Reference</th><th>Description</th></tr></thead><tbody>';
      rows.forEach(function (t) {
        var cls = (t.transactionType === 'DEPOSIT' || t.transactionType === 'TRANSFER_IN') ? 'tx-in' : 'tx-out';
        html += '<tr>' +
          '<td>' + esc((t.createdAt || '').replace('T', ' ').slice(0, 19)) + '</td>' +
          '<td><span class="badge">' + esc(t.transactionType) + '</span></td>' +
          '<td class="' + cls + '">' + esc(money(t.amount)) + '</td>' +
          '<td>' + esc(money(t.balanceBefore)) + '</td>' +
          '<td>' + esc(money(t.balanceAfter)) + '</td>' +
          '<td><code>' + esc(t.referenceNumber) + '</code></td>' +
          '<td>' + esc(t.description || '') + '</td>' +
        '</tr>';
      });
      html += '</tbody></table>';
      $('acct-content').innerHTML = html;
    }).catch(function (ex) {
      $('acct-content').innerHTML = '<p class="error">' + esc(ex.message) + '</p>';
    });
  }
  function closeModal() { $('acct-modal').classList.add('hidden'); }

  // ---------- DEPOSIT / WITHDRAW MODAL ----------
  function openTx(kind, accountId, accountNumber) {
    $('tx-modal').classList.remove('hidden');
    $('tx-title').textContent = (kind === 'deposit' ? 'Deposit into ' : 'Withdraw from ') + accountNumber;
    $('tx-kind').value = kind;
    $('tx-account-id').value = accountId;
    $('tx-amount').value = '';
    $('tx-desc').value = '';
    $('tx-msg').textContent = '';
  }
  function closeTx() { $('tx-modal').classList.add('hidden'); }

  // ---------- CREATE ACCOUNT MODAL ----------
  function openCreateAccount() {
    $('newacct-modal').classList.remove('hidden');
    $('newacct-msg').textContent = '';
  }
  function closeNewAcct() { $('newacct-modal').classList.add('hidden'); }

  // ---------- PROFILE ----------
  function loadProfile() {
    api('/api/customers/me').then(function (r) {
      var c = r.data || {};
      $('prof-first').value = c.firstName || '';
      $('prof-last').value = c.lastName || '';
      $('prof-phone').value = c.phone || '';
      $('prof-address').value = c.address || '';
    }).catch(function (ex) { toast(ex.message, 'err'); });
  }

  // ---------- ACCOUNT CLICK DELEGATION ----------
  document.addEventListener('click', function (e) {
    var view = e.target.closest && e.target.closest('[data-acc-view]');
    if (view) { openModal(Number(view.dataset.accView), view.dataset.accNum); return; }
    var tx = e.target.closest && e.target.closest('[data-acc-tx]');
    if (tx) { openTx(tx.dataset.accTx, tx.dataset.accId, tx.dataset.accNum); }
  });

  // ---------- EXPORT ----------
  var Cust = {
    loadAccounts: loadAccounts,
    loadTransferSource: loadTransferSource,
    loadProfile: loadProfile,
    openCreateAccount: openCreateAccount,
    closeNewAcct: closeNewAcct,
    openTx: openTx,
    closeTx: closeTx,
    closeModal: closeModal
  };
  window.Cust = Cust;

  // ---------- BOOT ----------
  if (localStorage.getItem(TOKEN_KEY)) { bootApp(); }
})();

/* ============================================================
   WALLET & CARDS — appended
   ============================================================ */
(function () {
  'use strict';
  function $(id){ return document.getElementById(id); }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function money(v) {
    var n = Number(v);
    return isNaN(n) ? (v == null ? '—' : v) : n.toLocaleString('en-PH', { style: 'currency', currency: 'PHP' });
  }
  function api(path, opts) {
    opts = opts || {};
    var headers = { 'Content-Type': 'application/json' };
    var t = localStorage.getItem('banking.customer.token');
    if (t) headers.Authorization = 'Bearer ' + t;
    return fetch(path, { method: opts.method || 'GET', headers: headers, body: opts.body })
      .then(function (res) {
        return res.json().catch(function(){ return null; }).then(function (b) {
          if (!res.ok) throw new Error((b && b.message) || (res.status + ' error'));
          return b;
        });
      });
  }
  function toast(msg, kind) {
    var t = $('toast');
    if (!t) return;
    t.textContent = msg;
    t.className = 'toast ' + (kind || 'ok');
    setTimeout(function(){ t.classList.add('hidden'); }, 3200);
  }

  function loadAccountSelects() {
    api('/api/accounts').then(function (r) {
      var active = (r.data || []).filter(function (a) { return a.status === 'ACTIVE'; });
      var opts = active.map(function (a) {
        return '<option value="' + a.id + '">' + esc(a.accountNumber) +
               ' — ' + esc(money(a.balance)) + '</option>';
      }).join('');
      ['card-account','cashin-account','cashout-account'].forEach(function (id) {
        var el = $(id);
        if (el) el.innerHTML = opts || '<option value="">No active accounts</option>';
      });
    }).catch(function(){});
  }

  function loadWallet() {
    api('/api/wallet').then(function (r) {
      var w = r.data;
      var box = $('wallet-box');
      if (!box) return;
      box.innerHTML =
        '<div style="display:flex;justify-content:space-between;align-items:center;font-size:14px">' +
          '<div>' +
            '<div class="acct-num">' + esc(w.walletNumber) + '</div>' +
            '<div style="color:var(--muted);font-size:12px">GCash Wallet</div>' +
          '</div>' +
          '<div class="acct-bal" style="font-size:22px">' + esc(money(w.balance)) + '</div>' +
        '</div>';
    }).catch(function (ex) {
      var box = $('wallet-box');
      if (box) box.innerHTML = '<p class="error">' + esc(ex.message) + '</p>';
    });
  }

  function loadCards() {
    api('/api/cards').then(function (r) {
      var list = r.data || [];
      var box = $('cards-list');
      if (!box) return;
      if (!list.length) { box.innerHTML = '<p class="empty">No cards yet.</p>'; return; }
      box.innerHTML = list.map(function (c) {
        return '<div style="background:linear-gradient(135deg,#4c1d95,#0e7490);' +
               'border-radius:12px;padding:18px 20px;color:#fff;margin-bottom:12px;' +
               'box-shadow:0 6px 20px rgba(0,0,0,.35)">' +
          '<div style="display:flex;justify-content:space-between;align-items:center">' +
            '<div style="font-weight:700;letter-spacing:2px">' + esc(c.brand) + '</div>' +
            '<span class="badge ' + esc(c.status) + '" style="background:#0b1220;color:#fff">' + esc(c.status) + '</span>' +
          '</div>' +
          '<div style="font-family:monospace;font-size:19px;letter-spacing:3px;margin:16px 0 10px">' +
            esc(c.maskedNumber) + '</div>' +
          '<div style="display:flex;justify-content:space-between;font-size:12px;opacity:.85">' +
            '<span>' + esc(c.cardholderName) + '</span>' +
            '<span>' + esc(c.expiry) + '</span>' +
          '</div>' +
          '<div style="margin-top:14px;display:flex;gap:8px;flex-wrap:wrap">' +
            '<button class="btn small" data-card-cvv="' + c.id + '">Reveal CVV</button>' +
            (c.status === 'ACTIVE'
              ? '<button class="btn small warn" data-card-status="FROZEN" data-card-id="' + c.id + '">Freeze</button>'
              : c.status === 'FROZEN'
                ? '<button class="btn small ok" data-card-status="ACTIVE" data-card-id="' + c.id + '">Unfreeze</button>'
                : '') +
          '</div>' +
        '</div>';
      }).join('');
    }).catch(function(){});
  }

  function loadWalletTx() {
    api('/api/wallet/transactions?page=0&size=50').then(function (r) {
      var rows = r.data || [];
      var box = $('wallet-tx');
      if (!box) return;
      if (!rows.length) { box.innerHTML = '<p class="empty">No wallet transactions yet.</p>'; return; }
      box.innerHTML = '<table class="grid tx"><thead><tr><th>When</th><th>Type</th><th>Amount</th><th>Before</th><th>After</th><th>Ref</th></tr></thead><tbody>' +
        rows.map(function (t) {
          var cls = t.type === 'CASH_IN' ? 'tx-in' : 'tx-out';
          return '<tr>' +
            '<td>' + esc((t.createdAt || '').replace('T', ' ').slice(0, 19)) + '</td>' +
            '<td><span class="badge">' + esc(t.type) + '</span></td>' +
            '<td class="' + cls + '">' + esc(money(t.amount)) + '</td>' +
            '<td>' + esc(money(t.balanceBefore)) + '</td>' +
            '<td>' + esc(money(t.balanceAfter)) + '</td>' +
            '<td><code>' + esc(t.referenceNumber) + '</code></td>' +
          '</tr>';
        }).join('') + '</tbody></table>';
    }).catch(function (ex) { toast(ex.message, 'err'); });
  }

  function generateCard() {
    var accId = $('card-account') ? $('card-account').value : null;
    if (!accId) { toast('No active account to link', 'err'); return; }
    api('/api/cards', { method: 'POST', body: JSON.stringify({ accountId: Number(accId) }) })
      .then(function () { toast('Card generated'); loadCards(); })
      .catch(function (ex) { toast(ex.message, 'err'); });
  }

  function cashIn() {
    var accId = $('cashin-account').value;
    var amt = Number($('cashin-amount').value);
    if (!accId || !amt || amt <= 0) { toast('Pick account and amount', 'err'); return; }
    api('/api/wallet/cash-in', { method: 'POST', body: JSON.stringify({ accountId: Number(accId), amount: amt }) })
      .then(function () { toast('Cash-in successful'); $('cashin-amount').value=''; loadWallet(); if (window.Cust && window.Cust.loadAccounts) window.Cust.loadAccounts(); })
      .catch(function (ex) { toast(ex.message, 'err'); });
  }

  function cashOut() {
    var accId = $('cashout-account').value;
    var amt = Number($('cashout-amount').value);
    if (!accId || !amt || amt <= 0) { toast('Pick account and amount', 'err'); return; }
    api('/api/wallet/cash-out', { method: 'POST', body: JSON.stringify({ accountId: Number(accId), amount: amt }) })
      .then(function () { toast('Cash-out successful'); $('cashout-amount').value=''; loadWallet(); if (window.Cust && window.Cust.loadAccounts) window.Cust.loadAccounts(); })
      .catch(function (ex) { toast(ex.message, 'err'); });
  }

  document.addEventListener('click', function (e) {
    var cvv = e.target.closest && e.target.closest('[data-card-cvv]');
    if (cvv) {
      api('/api/cards/' + cvv.dataset.cardCvv + '/reveal-cvv', { method: 'POST' })
        .then(function (r) { alert('CVV: ' + r.data.cvv + '\n\n(Simulator only — this number will not work at any real terminal.)'); })
        .catch(function (ex) { toast(ex.message, 'err'); });
      return;
    }
    var st = e.target.closest && e.target.closest('[data-card-status]');
    if (st) {
      api('/api/cards/' + st.dataset.cardId + '/status?status=' + st.dataset.cardStatus, { method: 'PATCH' })
        .then(function () { toast('Card ' + st.dataset.cardStatus); loadCards(); })
        .catch(function (ex) { toast(ex.message, 'err'); });
    }
  });

  // Re-run on tab click
  document.querySelectorAll('.tab').forEach(function (btn) {
    btn.addEventListener('click', function () {
      if (btn.dataset.tab === 'wallet') {
        loadAccountSelects();
        loadWallet();
        loadCards();
      }
    });
  });

  window.Cust = window.Cust || {};
  window.Cust.generateCard = generateCard;
  window.Cust.cashIn = cashIn;
  window.Cust.cashOut = cashOut;
  window.Cust.loadWalletTx = loadWalletTx;
})();

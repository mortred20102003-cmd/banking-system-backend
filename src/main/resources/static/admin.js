(function () {
  'use strict';

  var TOKEN_KEY = 'banking.superadmin.token';
  var USER_KEY  = 'banking.superadmin.username';
  var ROLE_KEY  = 'banking.superadmin.role';
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
    if (isNaN(n)) return String(v);
    return n.toLocaleString('en-PH', { style: 'currency', currency: 'PHP' });
  }
  function api(path, opts) {
    opts = opts || {};
    var headers = { 'Content-Type': 'application/json' };
    var t = localStorage.getItem(TOKEN_KEY);
    if (t) headers['Authorization'] = 'Bearer ' + t;
    if (opts.headers) for (var k in opts.headers) headers[k] = opts.headers[k];
    return fetch(path, { method: opts.method || 'GET', headers: headers, body: opts.body })
      .then(function (res) {
        return res.json().catch(function () { return null; }).then(function (body) {
          if (res.status === 401) { logout(); throw new Error('Session expired'); }
          if (!res.ok) throw new Error((body && body.message) || (res.status + ' error'));
          return body;
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
    if (btn) btn.textContent = theme === 'light' ? '\u263D' : '\u2600'; // moon / sun
  }
  applyTheme(localStorage.getItem(THEME_KEY) || 'dark');

  // ---------- AUTH ----------
  function saveAuth(d) {
    localStorage.setItem(TOKEN_KEY, d.token);
    localStorage.setItem(USER_KEY, d.username);
    localStorage.setItem(ROLE_KEY, d.role);
  }
  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(ROLE_KEY);
    $('login-view').classList.remove('hidden');
    $('app-view').classList.add('hidden');
  }
  function bootApp() {
    $('login-view').classList.add('hidden');
    $('app-view').classList.remove('hidden');
    $('who-username').textContent = localStorage.getItem(USER_KEY) + ' (' + localStorage.getItem(ROLE_KEY) + ')';
    App.loadUsers();
    App.loadAccounts();
    App.loadStats();
  }

  // ---------- GLOBAL DELEGATED CLICKS ----------
  document.addEventListener('click', function (e) {
    // Theme toggle
    if (e.target.closest && e.target.closest('#theme-toggle')) {
      var cur = document.documentElement.getAttribute('data-theme') || 'dark';
      var next = cur === 'light' ? 'dark' : 'light';
      localStorage.setItem(THEME_KEY, next);
      applyTheme(next);
      return;
    }
    // Eye toggle
    var eye = e.target.closest && e.target.closest('.eye');
    if (eye) {
      e.preventDefault();
      e.stopPropagation();
      var input = document.getElementById(eye.getAttribute('data-target'));
      if (input) {
        var show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        if (show) eye.classList.add('revealed'); else eye.classList.remove('revealed');
        input.focus();
      }
      return;
    }
    // Forgot link
    if (e.target.id === 'forgot-link') {
      e.preventDefault();
      toast('Forgot password flow — see server console for token (feature scaffolded).');
      return;
    }
    // Logout
    if (e.target.closest && e.target.closest('#logout-btn')) {
      logout();
      return;
    }
    // Tab switch
    var tab = e.target.closest && e.target.closest('.tab');
    if (tab && tab.dataset.tab) {
      var name = tab.dataset.tab;
      document.querySelectorAll('.tab').forEach(function (b) { b.classList.remove('active'); });
      tab.classList.add('active');
      document.querySelectorAll('.tab-panel').forEach(function (p) { p.classList.add('hidden'); });
      var panel = document.getElementById('tab-' + name);
      if (panel) panel.classList.remove('hidden');
      if (name === 'audit') App.loadAudit();
      return;
    }
  });

  // ---------- LOGIN ----------
  document.addEventListener('submit', function (e) {
    if (e.target.id !== 'login-form') return;
    e.preventDefault();
    var btn = $('login-btn'), err = $('login-error');
    err.textContent = '';
    btn.disabled = true;
    btn.textContent = 'Signing in…';

    fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        usernameOrEmail: $('login-identifier').value.trim(),
        password: $('login-password').value
      })
    })
      .then(function (res) { return res.json().then(function (b) { return { ok: res.ok, body: b }; }); })
      .then(function (r) {
        if (!r.ok || !r.body || !r.body.success) {
          throw new Error((r.body && r.body.message) || 'Login failed');
        }
        if (r.body.data.role !== 'SUPER_ADMIN') {
          throw new Error('This account is not a SuperAdmin. Role: ' + r.body.data.role);
        }
        saveAuth(r.body.data);
        toast('Welcome, ' + r.body.data.username);
        bootApp();
      })
      .catch(function (ex) { err.textContent = ex.message; })
      .finally(function () { btn.disabled = false; btn.textContent = 'Sign in'; });
  });

  // ---------- USERS ----------
  function loadUsers() {
    var body = $('users-body');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="6" class="empty">Loading…</td></tr>';
    api('/api/super-admin/users').then(function (r) {
      var users = (r && r.data) || [];
      if (!users.length) { body.innerHTML = '<tr><td colspan="6" class="empty">No users.</td></tr>'; return; }
      body.innerHTML = users.map(function (u) {
        var promote = u.role === 'CUSTOMER'
          ? '<button class="btn small primary" data-action="promote" data-id="' + u.id + '" data-name="' + esc(u.username) + '">Promote</button>'
          : '';
        var demote = u.role === 'ADMIN'
          ? '<button class="btn small warn" data-action="demote" data-id="' + u.id + '" data-name="' + esc(u.username) + '">Demote</button>'
          : '';
        var toggle = u.role !== 'SUPER_ADMIN'
          ? '<button class="btn small" data-action="status" data-id="' + u.id + '" data-name="' + esc(u.username) + '" data-status="' + (u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE') + '">Set ' + (u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE') + '</button>'
          : '';
        return '<tr>' +
          '<td>' + esc(u.id) + '</td>' +
          '<td>' + esc(u.username) + '</td>' +
          '<td>' + esc(u.email) + '</td>' +
          '<td><span class="badge ' + esc(u.role) + '">' + esc(u.role) + '</span></td>' +
          '<td><span class="badge ' + esc(u.status) + '">' + esc(u.status) + '</span></td>' +
          '<td>' + promote + demote + toggle + '</td>' +
          '</tr>';
      }).join('');
    }).catch(function (ex) {
      body.innerHTML = '<tr><td colspan="6" class="empty">Error: ' + esc(ex.message) + '</td></tr>';
    });
  }

  // ---------- ACCOUNTS ----------
  function loadAccounts() {
    var body = $('accounts-body');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="7" class="empty">Loading…</td></tr>';
    api('/api/admin/accounts').then(function (r) {
      var accounts = (r && r.data) || [];
      if (!accounts.length) { body.innerHTML = '<tr><td colspan="7" class="empty">No accounts.</td></tr>'; return; }
      body.innerHTML = accounts.map(function (a) {
        var action = a.status === 'ACTIVE'
          ? '<button class="btn small warn" data-acc-action="FROZEN" data-acc-id="' + a.id + '">Freeze</button>'
          : a.status === 'FROZEN'
            ? '<button class="btn small ok" data-acc-action="ACTIVE" data-acc-id="' + a.id + '">Unfreeze</button>'
            : '';
        var close = a.status !== 'CLOSED'
          ? '<button class="btn small danger" data-acc-action="CLOSED" data-acc-id="' + a.id + '">Close</button>'
          : '';
        return '<tr>' +
          '<td>' + esc(a.id) + '</td>' +
          '<td><code>' + esc(a.accountNumber) + '</code></td>' +
          '<td>' + esc(a.customerName || ('#' + a.customerId)) + '</td>' +
          '<td><span class="badge ' + esc(a.accountType) + '">' + esc(a.accountType) + '</span></td>' +
          '<td>' + esc(money(a.balance)) + '</td>' +
          '<td><span class="badge ' + esc(a.status) + '">' + esc(a.status) + '</span></td>' +
          '<td>' + action + close + '</td>' +
          '</tr>';
      }).join('');
    }).catch(function (ex) {
      body.innerHTML = '<tr><td colspan="7" class="empty">Error: ' + esc(ex.message) + '</td></tr>';
    });
  }

  // ---------- AUDIT ----------
  function loadAudit() {
    var body = $('audit-body');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="5" class="empty">Loading…</td></tr>';
    api('/api/super-admin/audit?page=0&size=100').then(function (r) {
      var rows = (r && r.data) || [];
      if (!rows.length) { body.innerHTML = '<tr><td colspan="5" class="empty">No entries yet.</td></tr>'; return; }
      body.innerHTML = rows.map(function (a) {
        return '<tr>' +
          '<td>' + esc((a.createdAt || '').replace('T', ' ').slice(0, 19)) + '</td>' +
          '<td>' + esc(a.actorUsername) + ' (#' + esc(a.actorUserId) + ')</td>' +
          '<td><span class="badge">' + esc(a.action) + '</span></td>' +
          '<td>' + esc(a.targetType) + ' #' + esc(a.targetId == null ? '—' : a.targetId) + '</td>' +
          '<td>' + esc(a.details || '') + '</td>' +
          '</tr>';
      }).join('');
    }).catch(function (ex) {
      body.innerHTML = '<tr><td colspan="5" class="empty">Error: ' + esc(ex.message) + '</td></tr>';
    });
  }

  // ---------- OVERVIEW ----------
  function loadStats() {
    Promise.all([api('/api/super-admin/users'), api('/api/admin/accounts')]).then(function (rs) {
      var users = (rs[0] && rs[0].data) || [];
      var accounts = (rs[1] && rs[1].data) || [];
      var total = accounts.reduce(function (s, a) { return s + Number(a.balance || 0); }, 0);
      if ($('stat-users')) $('stat-users').textContent = users.length;
      if ($('stat-accounts')) $('stat-accounts').textContent = accounts.length;
      if ($('stat-balance')) $('stat-balance').textContent = money(total);
    }).catch(function () {});
  }

  // ---------- USER ACTIONS (event delegation) ----------
  document.addEventListener('click', function (e) {
    var btn = e.target.closest && e.target.closest('button[data-action]');
    if (btn) {
      var id = btn.dataset.id, name = btn.dataset.name;
      if (btn.dataset.action === 'promote') {
        if (!confirm('Promote ' + name + ' to ADMIN?')) return;
        api('/api/super-admin/users/' + id + '/promote', { method: 'POST', body: JSON.stringify({ role: 'ADMIN' }) })
          .then(function () { toast(name + ' is now ADMIN'); loadUsers(); })
          .catch(function (ex) { toast(ex.message, 'err'); });
      } else if (btn.dataset.action === 'demote') {
        if (!confirm('Demote ' + name + ' to CUSTOMER?')) return;
        api('/api/super-admin/users/' + id + '/demote', { method: 'POST', body: JSON.stringify({ role: 'CUSTOMER' }) })
          .then(function () { toast(name + ' is now CUSTOMER'); loadUsers(); })
          .catch(function (ex) { toast(ex.message, 'err'); });
      } else if (btn.dataset.action === 'status') {
        var st = btn.dataset.status;
        if (!confirm('Set ' + name + ' to ' + st + '?')) return;
        api('/api/super-admin/users/' + id + '/status?status=' + encodeURIComponent(st), { method: 'PATCH' })
          .then(function () { toast(name + ' → ' + st); loadUsers(); })
          .catch(function (ex) { toast(ex.message, 'err'); });
      }
      return;
    }

    var accBtn = e.target.closest && e.target.closest('button[data-acc-action]');
    if (accBtn) {
      var accId = accBtn.dataset.accId, status = accBtn.dataset.accAction;
      if (!confirm('Set account #' + accId + ' to ' + status + '?')) return;
      api('/api/admin/accounts/' + accId + '/status', { method: 'PATCH', body: JSON.stringify({ status: status }) })
        .then(function () { toast('Account #' + accId + ' → ' + status); loadAccounts(); })
        .catch(function (ex) { toast(ex.message, 'err'); });
    }
  });

  // ---------- EXPOSE ----------
  var App = {
    loadUsers: loadUsers,
    loadAccounts: loadAccounts,
    loadAudit: loadAudit,
    loadStats: loadStats,
    logout: logout
  };
  window.App = App;

  // ---------- BOOT ----------
  if (localStorage.getItem(TOKEN_KEY)) {
    bootApp();
  }
})();


/* ============================================================
   TIER-1 FEATURES — Credit button + Customer detail drawer
   ============================================================ */
(function () {
  'use strict';
  var TOKEN_KEY = 'banking.superadmin.token';
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
    var t = localStorage.getItem(TOKEN_KEY);
    if (t) headers.Authorization = 'Bearer ' + t;
    return fetch(path, { method: opts.method || 'GET', headers: headers, body: opts.body })
      .then(function (res) {
        return res.json().catch(function () { return null; }).then(function (b) {
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
    setTimeout(function () { t.classList.add('hidden'); }, 3200);
  }

  // ---------- CREDIT ----------
  function creditAccount(accountId, accountNumber) {
    var amountStr = prompt('Credit amount for account ' + accountNumber + ':', '100.00');
    if (!amountStr) return;
    var amount = Number(amountStr);
    if (isNaN(amount) || amount <= 0) { toast('Invalid amount', 'err'); return; }
    var reason = prompt('Reason (audit trail):', 'Customer compensation');
    if (!reason) return;

    api('/api/super-admin/accounts/' + accountId + '/adjust-credit', {
      method: 'POST',
      body: JSON.stringify({ amount: amount, reason: reason })
    }).then(function (r) {
      toast('Credited ' + money(amount) + ' — ref ' + r.data.referenceNumber);
      if (window.App && window.App.loadAccounts) window.App.loadAccounts();
    }).catch(function (ex) { toast(ex.message, 'err'); });
  }

  // ---------- CUSTOMER DETAIL ----------
  function openCustomer(customerId) {
    var modal = $('cust-modal');
    var title = $('cust-title');
    var content = $('cust-content');
    if (!modal) return;
    modal.classList.remove('hidden');
    title.textContent = 'Customer #' + customerId;
    content.innerHTML = 'Loading…';

    api('/api/super-admin/customers/' + customerId).then(function (r) {
      var d = r.data;
      var html = '';
      html += '<h4 style="margin:8px 0 6px">User Account</h4><div style="display:grid;grid-template-columns:1fr 1fr;gap:6px 20px;font-size:13.5px">' +
        '<div><b>Username:</b> ' + esc(d.user.username) + '</div>' +
        '<div><b>Email:</b> ' + esc(d.user.email) + '</div>' +
        '<div><b>Role:</b> <span class="badge ' + esc(d.user.role) + '">' + esc(d.user.role) + '</span></div>' +
        '<div><b>Status:</b> <span class="badge ' + esc(d.user.status) + '">' + esc(d.user.status) + '</span></div>' +
      '</div>';

      html += '<h4 style="margin:16px 0 6px">Profile</h4>' +
        '<form id="cust-edit-form" data-customer-id="' + customerId + '">' +
          '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:10px">' +
            '<label>First name<input name="firstName" value="' + esc(d.customer.firstName || '') + '" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--line);border-radius:8px;color:var(--text);margin-top:4px"></label>' +
            '<label>Middle name<input name="middleName" value="' + esc(d.customer.middleName || '') + '" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--line);border-radius:8px;color:var(--text);margin-top:4px"></label>' +
            '<label>Last name<input name="lastName" value="' + esc(d.customer.lastName || '') + '" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--line);border-radius:8px;color:var(--text);margin-top:4px"></label>' +
            '<label>Phone<input name="phone" value="' + esc(d.customer.phone || '') + '" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--line);border-radius:8px;color:var(--text);margin-top:4px"></label>' +
          '</div>' +
          '<label style="display:block;font-size:12px;color:var(--muted)">Address</label>' +
          '<input name="address" value="' + esc(d.customer.address || '') + '" style="width:100%;padding:8px;background:var(--bg);border:1px solid var(--line);border-radius:8px;color:var(--text);margin-top:4px">' +
          '<button type="submit" class="btn primary" style="margin-top:10px">Save profile</button>' +
          '<span id="cust-edit-msg" style="margin-left:10px;color:var(--muted);font-size:13px"></span>' +
        '</form>';

      html += '<h4 style="margin:16px 0 6px">Accounts</h4>';
      if (d.accounts && d.accounts.length) {
        html += '<table class="grid" style="width:100%"><thead><tr><th>#</th><th>Number</th><th>Type</th><th>Balance</th><th>Status</th></tr></thead><tbody>';
        d.accounts.forEach(function (a) {
          html += '<tr>' +
            '<td>' + esc(a.id) + '</td>' +
            '<td><code>' + esc(a.accountNumber) + '</code></td>' +
            '<td><span class="badge ' + esc(a.accountType) + '">' + esc(a.accountType) + '</span></td>' +
            '<td>' + esc(money(a.balance)) + '</td>' +
            '<td><span class="badge ' + esc(a.status) + '">' + esc(a.status) + '</span></td>' +
          '</tr>';
        });
        html += '</tbody></table>';
      } else {
        html += '<p style="color:var(--muted)">No accounts.</p>';
      }

      content.innerHTML = html;

      var form = $('cust-edit-form');
      if (form) form.addEventListener('submit', function (e) {
        e.preventDefault();
        var f = e.target;
        var payload = {};
        ['firstName','middleName','lastName','phone','address'].forEach(function (k) {
          var v = f.elements[k].value.trim();
          if (v) payload[k] = v;
        });
        api('/api/super-admin/customers/' + customerId, { method: 'PUT', body: JSON.stringify(payload) })
          .then(function () { $('cust-edit-msg').textContent = 'Saved ✓'; })
          .catch(function (ex) { $('cust-edit-msg').textContent = ex.message; });
      });
    }).catch(function (ex) {
      content.innerHTML = '<p style="color:var(--err)">' + esc(ex.message) + '</p>';
    });
  }
  function closeCustomer() { var m = $('cust-modal'); if (m) m.classList.add('hidden'); }

  // ---------- OBSERVER: inject Credit + Open buttons into the tables ----------
  function injectIntoAccounts() {
    document.querySelectorAll('#accounts-body tr').forEach(function (tr) {
      if (tr.dataset.enhanced) return;
      tr.dataset.enhanced = '1';
      var cells = tr.children;
      if (cells.length < 7) return;
      var actions = tr.querySelector('td:last-child');
      if (!actions) return;
      var accId = cells[0].textContent.trim();
      var accNum = cells[1].textContent.trim();
      var btn = document.createElement('button');
      btn.className = 'btn small primary';
      btn.textContent = 'Credit';
      btn.style.marginLeft = '6px';
      btn.onclick = function () { creditAccount(accId, accNum); };
      actions.appendChild(btn);
    });
  }
  function injectIntoUsers() {
    document.querySelectorAll('#users-body tr').forEach(function (tr) {
      if (tr.dataset.enhancedUser) return;
      tr.dataset.enhancedUser = '1';
      var cells = tr.children;
      if (cells.length < 6) return;
      var actions = tr.querySelector('td:last-child');
      if (!actions) return;
      var userId = cells[0].textContent.trim();
      var btn = document.createElement('button');
      btn.className = 'btn small';
      btn.textContent = 'Open';
      btn.style.marginLeft = '6px';
      btn.onclick = function () {
        // look up the customer record for this user
        api('/api/admin/customers').then(function (r) {
          var match = (r.data || []).find(function (c) { return String(c.userId) === String(userId); });
          if (!match) { toast('No customer profile for this user', 'err'); return; }
          openCustomer(match.id);
        }).catch(function (ex) { toast(ex.message, 'err'); });
      };
      actions.appendChild(btn);
    });
  }

  var mo = new MutationObserver(function () {
    injectIntoAccounts();
    injectIntoUsers();
  });
  document.addEventListener('DOMContentLoaded', function () {
    mo.observe(document.body, { childList: true, subtree: true });
    injectIntoAccounts();
    injectIntoUsers();
  });
  // also try immediately in case DOMContentLoaded already fired
  setTimeout(function () {
    mo.observe(document.body, { childList: true, subtree: true });
    injectIntoAccounts();
    injectIntoUsers();
  }, 300);

  window.App = window.App || {};
  window.App.openCustomer = openCustomer;
  window.App.closeCustomer = closeCustomer;
  window.App.creditAccount = creditAccount;
})();

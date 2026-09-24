(function () {
  'use strict';

  var TOKEN_KEY = 'banking.admin.token';
  var USER_KEY  = 'banking.admin.username';
  var ROLE_KEY  = 'banking.admin.role';
  var THEME_KEY = 'banking.theme';

  function $(id){ return document.getElementById(id); }
  function esc(s){
    return String(s == null ? '' : s).replace(/[&<>"']/g, function(c){
      return { '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;' }[c];
    });
  }
  function money(v){
    if (v == null) return '—';
    var n = Number(v);
    return isNaN(n) ? String(v) : n.toLocaleString('en-PH',{style:'currency',currency:'PHP'});
  }
  function api(path, opts){
    opts = opts || {};
    var headers = { 'Content-Type':'application/json' };
    var t = localStorage.getItem(TOKEN_KEY);
    if (t) headers.Authorization = 'Bearer ' + t;
    if (opts.headers) for (var k in opts.headers) headers[k] = opts.headers[k];
    return fetch(path, { method: opts.method || 'GET', headers: headers, body: opts.body })
      .then(function(res){
        return res.json().catch(function(){ return null; }).then(function(b){
          if (res.status === 401) { logout(); throw new Error('Session expired'); }
          if (!res.ok) throw new Error((b && b.message) || (res.status + ' error'));
          return b;
        });
      });
  }
  var toastTimer;
  function toast(msg, kind){
    var t = $('toast'); if (!t) return;
    t.textContent = msg;
    t.className = 'toast ' + (kind || 'ok');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function(){ t.classList.add('hidden'); }, 3200);
  }

  // ---------- THEME ----------
  function applyTheme(theme){
    document.documentElement.setAttribute('data-theme', theme);
    var btn = $('theme-toggle');
    if (btn) btn.textContent = theme === 'light' ? '\u263D' : '\u2600';
  }
  applyTheme(localStorage.getItem(THEME_KEY) || 'dark');

  // ---------- GLOBAL CLICKS ----------
  document.addEventListener('click', function(e){
    if (e.target.closest && e.target.closest('#theme-toggle')) {
      var cur = document.documentElement.getAttribute('data-theme') || 'dark';
      var next = cur === 'light' ? 'dark' : 'light';
      localStorage.setItem(THEME_KEY, next); applyTheme(next); return;
    }
    var eye = e.target.closest && e.target.closest('.eye');
    if (eye) {
      e.preventDefault(); e.stopPropagation();
      var input = document.getElementById(eye.getAttribute('data-target'));
      if (input) {
        var show = input.type === 'password';
        input.type = show ? 'text' : 'password';
        if (show) eye.classList.add('revealed'); else eye.classList.remove('revealed');
        input.focus();
      }
      return;
    }
    if (e.target.closest && e.target.closest('#logout-btn')) { logout(); return; }
    var tab = e.target.closest && e.target.closest('.tab');
    if (tab && tab.dataset.tab) {
      document.querySelectorAll('.tab').forEach(function(b){ b.classList.remove('active'); });
      tab.classList.add('active');
      document.querySelectorAll('.tab-panel').forEach(function(p){ p.classList.add('hidden'); });
      var panel = document.getElementById('tab-' + tab.dataset.tab);
      if (panel) panel.classList.remove('hidden');
      if (tab.dataset.tab === 'customers') App.loadCustomers();
      if (tab.dataset.tab === 'profile') App.loadProfile();
    }
  });

  // ---------- AUTH ----------
  function saveAuth(d){
    localStorage.setItem(TOKEN_KEY, d.token);
    localStorage.setItem(USER_KEY, d.username);
    localStorage.setItem(ROLE_KEY, d.role);
  }
  function logout(){
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(ROLE_KEY);
    $('login-view').classList.remove('hidden');
    $('app-view').classList.add('hidden');
    closeCustomer(); closeAcctModal();
  }
  function bootApp(){
    $('login-view').classList.add('hidden');
    $('app-view').classList.remove('hidden');
    $('who-username').textContent = localStorage.getItem(USER_KEY) +
      ' (' + localStorage.getItem(ROLE_KEY) + ')';
    App.loadAccounts();
    App.loadStats();
  }

  // ---------- LOGIN ----------
  document.addEventListener('submit', function(e){
    if (e.target.id !== 'login-form') return;
    e.preventDefault();
    var btn = $('login-btn'), err = $('login-error');
    err.textContent = ''; btn.disabled = true; btn.textContent = 'Signing in…';

    fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({
        usernameOrEmail: $('login-identifier').value.trim(),
        password: $('login-password').value
      })
    })
      .then(function(res){ return res.json().then(function(b){ return { ok:res.ok, body:b }; }); })
      .then(function(r){
        if (!r.ok || !r.body || !r.body.success) throw new Error((r.body && r.body.message) || 'Login failed');
        var role = r.body.data.role;
        if (role !== 'ADMIN' && role !== 'SUPER_ADMIN') {
          throw new Error('This console is for ADMIN users. Your role: ' + role);
        }
        saveAuth(r.body.data);
        toast('Welcome, ' + r.body.data.username);
        bootApp();
      })
      .catch(function(ex){ err.textContent = ex.message; })
      .finally(function(){ btn.disabled = false; btn.textContent = 'Sign in'; });
  });

  // ---------- STATS ----------
  function loadStats(){
    Promise.all([api('/api/admin/users'), api('/api/admin/accounts')]).then(function(rs){
      var users = (rs[0] && rs[0].data) || [];
      var accounts = (rs[1] && rs[1].data) || [];
      var total = accounts.reduce(function(s,a){ return s + Number(a.balance || 0); }, 0);
      if ($('stat-users')) $('stat-users').textContent = users.length;
      if ($('stat-accounts')) $('stat-accounts').textContent = accounts.length;
      if ($('stat-balance')) $('stat-balance').textContent = money(total);
    }).catch(function(){});
  }

  // ---------- ACCOUNTS ----------
  function loadAccounts(){
    var body = $('accounts-body');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="7" class="empty">Loading…</td></tr>';
    api('/api/admin/accounts').then(function(r){
      var accounts = (r && r.data) || [];
      if (!accounts.length) { body.innerHTML = '<tr><td colspan="7" class="empty">No accounts.</td></tr>'; return; }
      body.innerHTML = accounts.map(function(a){
        var freeze = a.status === 'ACTIVE'
          ? '<button class="btn small warn" data-acc-action="FROZEN" data-acc-id="' + a.id + '">Freeze</button>'
          : a.status === 'FROZEN'
            ? '<button class="btn small ok" data-acc-action="ACTIVE" data-acc-id="' + a.id + '">Unfreeze</button>'
            : '';
        var close = a.status !== 'CLOSED'
          ? '<button class="btn small danger" data-acc-action="CLOSED" data-acc-id="' + a.id + '">Close</button>'
          : '';
        var hist = '<button class="btn small" data-acc-history="' + a.id + '" data-acc-num="' + esc(a.accountNumber) + '">History</button>';
        return '<tr>' +
          '<td>' + esc(a.id) + '</td>' +
          '<td><code>' + esc(a.accountNumber) + '</code></td>' +
          '<td>' + esc(a.customerName || ('#' + a.customerId)) + '</td>' +
          '<td><span class="badge ' + esc(a.accountType) + '">' + esc(a.accountType) + '</span></td>' +
          '<td>' + esc(money(a.balance)) + '</td>' +
          '<td><span class="badge ' + esc(a.status) + '">' + esc(a.status) + '</span></td>' +
          '<td>' + hist + freeze + close + '</td>' +
        '</tr>';
      }).join('');
    }).catch(function(ex){
      body.innerHTML = '<tr><td colspan="7" class="empty">Error: ' + esc(ex.message) + '</td></tr>';
    });
  }

  // ---------- CUSTOMERS ----------
  function loadCustomers(){
    var body = $('customers-body');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="6" class="empty">Loading…</td></tr>';
    api('/api/admin/customers').then(function(r){
      var list = (r && r.data) || [];
      if (!list.length) { body.innerHTML = '<tr><td colspan="6" class="empty">No customers.</td></tr>'; return; }
      body.innerHTML = list.map(function(c){
        return '<tr>' +
          '<td>' + esc(c.id) + '</td>' +
          '<td>#' + esc(c.userId) + '</td>' +
          '<td>' + esc((c.firstName || '') + ' ' + (c.lastName || '')) + '</td>' +
          '<td>' + esc(c.phone || '—') + '</td>' +
          '<td>' + esc((c.createdAt || '').replace('T',' ').slice(0,19)) + '</td>' +
          '<td><button class="btn small" data-cust-open="' + c.id + '">Open</button></td>' +
        '</tr>';
      }).join('');
    }).catch(function(ex){
      body.innerHTML = '<tr><td colspan="6" class="empty">Error: ' + esc(ex.message) + '</td></tr>';
    });
  }

  // ---------- CUSTOMER DETAIL ----------
  function openCustomer(customerId){
    $('cust-modal').classList.remove('hidden');
    $('cust-title').textContent = 'Customer #' + customerId;
    $('cust-content').innerHTML = 'Loading…';
    api('/api/super-admin/customers/' + customerId).then(function(r){
      var d = r.data;
      var html = '';
      html += '<h4 style="margin:8px 0 6px">User Account</h4><div style="display:grid;grid-template-columns:1fr 1fr;gap:6px 20px;font-size:13.5px">' +
        '<div><b>Username:</b> ' + esc(d.user.username) + '</div>' +
        '<div><b>Email:</b> ' + esc(d.user.email) + '</div>' +
        '<div><b>Role:</b> <span class="badge ' + esc(d.user.role) + '">' + esc(d.user.role) + '</span></div>' +
        '<div><b>Status:</b> <span class="badge ' + esc(d.user.status) + '">' + esc(d.user.status) + '</span></div>' +
      '</div>';

      html += '<h4 style="margin:16px 0 6px">Profile</h4>' +
        '<form id="cust-edit-form">' +
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
        d.accounts.forEach(function(a){
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

      $('cust-content').innerHTML = html;

      var form = $('cust-edit-form');
      if (form) form.addEventListener('submit', function(e){
        e.preventDefault();
        var f = e.target;
        var payload = {};
        ['firstName','middleName','lastName','phone','address'].forEach(function(k){
          var v = f.elements[k].value.trim();
          if (v) payload[k] = v;
        });
        api('/api/super-admin/customers/' + customerId, { method:'PUT', body: JSON.stringify(payload) })
          .then(function(){ $('cust-edit-msg').textContent = 'Saved ✓'; })
          .catch(function(ex){ $('cust-edit-msg').textContent = ex.message; });
      });
    }).catch(function(ex){
      $('cust-content').innerHTML = '<p style="color:var(--err)">' + esc(ex.message) + '</p>';
    });
  }
  function closeCustomer(){ var m = $('cust-modal'); if (m) m.classList.add('hidden'); }

  // ---------- ACCOUNT HISTORY ----------
  function openAcctHistory(accountId, accountNumber){
    $('acct-modal').classList.remove('hidden');
    $('acct-title').textContent = 'Account ' + accountNumber + ' — History';
    $('acct-content').innerHTML = 'Loading…';
    api('/api/admin/accounts/' + accountId + '/transactions?page=0&size=50').then(function(r){
      var rows = (r && r.data) || [];
      if (!rows.length) { $('acct-content').innerHTML = '<p class="empty">No transactions yet.</p>'; return; }
      var html = '<table class="grid tx"><thead><tr><th>When</th><th>Type</th><th>Account</th><th>Owner</th><th>Amount</th><th>Before</th><th>After</th><th>Ref</th></tr></thead><tbody>';
      rows.forEach(function(t){
        var cls = (t.transactionType === 'DEPOSIT' || t.transactionType === 'TRANSFER_IN') ? 'tx-in' : 'tx-out';
        html += '<tr>' +
          '<td>' + esc((t.createdAt || '').replace('T',' ').slice(0,19)) + '</td>' +
          '<td><span class="badge">' + esc(t.transactionType) + '</span></td>' +
          '<td><code>' + esc(t.accountNumber || '') + '</code></td>' +
          '<td>' + esc(t.accountOwnerName || '') + '</td>' +
          '<td class="' + cls + '">' + esc(money(t.amount)) + '</td>' +
          '<td>' + esc(money(t.balanceBefore)) + '</td>' +
          '<td>' + esc(money(t.balanceAfter)) + '</td>' +
          '<td><code>' + esc(t.referenceNumber) + '</code></td>' +
        '</tr>';
      });
      html += '</tbody></table>';
      $('acct-content').innerHTML = html;
    }).catch(function(ex){
      $('acct-content').innerHTML = '<p style="color:var(--err)">' + esc(ex.message) + '</p>';
    });
  }
  function closeAcctModal(){ var m = $('acct-modal'); if (m) m.classList.add('hidden'); }

  // ---------- PROFILE ----------
  function loadProfile(){
    var box = $('profile-info');
    if (!box) return;
    api('/api/customers/me').then(function(r){
      var c = r.data || {};
      box.innerHTML =
        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px 24px;font-size:14px">' +
          '<div><b>First name:</b> ' + esc(c.firstName || '—') + '</div>' +
          '<div><b>Middle name:</b> ' + esc(c.middleName || '—') + '</div>' +
          '<div><b>Last name:</b> ' + esc(c.lastName || '—') + '</div>' +
          '<div><b>Phone:</b> ' + esc(c.phone || '—') + '</div>' +
          '<div style="grid-column:1/-1"><b>Address:</b> ' + esc(c.address || '—') + '</div>' +
        '</div>';
    }).catch(function(ex){
      box.innerHTML = '<p style="color:var(--err)">' + esc(ex.message) + '</p>';
    });
  }

  // ---------- EVENT DELEGATION ----------
  document.addEventListener('click', function(e){
    var open = e.target.closest && e.target.closest('[data-cust-open]');
    if (open) { openCustomer(Number(open.dataset.custOpen)); return; }

    var hist = e.target.closest && e.target.closest('[data-acc-history]');
    if (hist) { openAcctHistory(Number(hist.dataset.accHistory), hist.dataset.accNum); return; }

    var accBtn = e.target.closest && e.target.closest('button[data-acc-action]');
    if (accBtn) {
      var accId = accBtn.dataset.accId, status = accBtn.dataset.accAction;
      if (!confirm('Set account #' + accId + ' to ' + status + '?')) return;
      api('/api/admin/accounts/' + accId + '/status', {
        method: 'PATCH', body: JSON.stringify({ status: status })
      }).then(function(){ toast('Account #' + accId + ' → ' + status); loadAccounts(); })
        .catch(function(ex){ toast(ex.message, 'err'); });
    }
  });

  // ---------- EXPORT ----------
  var App = {
    loadAccounts: loadAccounts,
    loadCustomers: loadCustomers,
    loadStats: loadStats,
    loadProfile: loadProfile,
    openCustomer: openCustomer,
    closeCustomer: closeCustomer,
    closeAcctModal: closeAcctModal,
    logout: logout
  };
  window.App = App;

  // ---------- BOOT ----------
  if (localStorage.getItem(TOKEN_KEY)) bootApp();
})();

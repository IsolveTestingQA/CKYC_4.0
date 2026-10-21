/* CKYC Locator Recorder - injected into the application page by utils/capture/LocatorRecorder.
   It only OBSERVES what the tester does (clicks, fields, files, toasts, dialogs, module switches) and keeps the
   best locator candidates of every element. It never clicks or types, never stores typed values (only the length),
   and never records password fields. Java drains the events with window.__ckycRecDrain(). */
(function () {
  if (window.__ckycRecInstalled) { return; }
  window.__ckycRecInstalled = true;
  var init = window.__ckycRecInit || {};
  var S = window.__ckycRec = {
    q: [], paused: false, finish: false, module: init.module || '', modKey: init.modKey || '', lastNav: null,
    counts: init.counts || { click: 0, type: 0, toast: 0, file: 0, snap: 0, dialog: 0, note: 0 }, modCounts: init.modCounts || {}, lastToast: ''
  };
  var TEST = !!window.__ckycRecTest;
  var QUIET = !!init.quiet; // crawler mode: no listeners, no badge, no pop-ups - Java asks for inventories itself

  // ---------------------------------------------------------------- helpers
  function txt(el, n) {
    var t = (el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim();
    n = n || 80;
    return t.length > n ? t.slice(0, n) : t;
  }
  function q(s) {
    if (s.indexOf("'") < 0) { return "'" + s + "'"; }
    return 'concat(' + s.split("'").map(function (p) { return "'" + p + "'"; }).join(', "\'", ') + ')';
  }
  function count(xp) {
    try { return document.evaluate('count(' + xp + ')', document, null, XPathResult.NUMBER_TYPE, null).numberValue; } catch (e) { return -1; }
  }
  function stable(id) {
    return !!id && id.length < 60 && !/^(mui-|:r|radix-|react-|«)/i.test(id) && !/\d{4,}/.test(id);
  }
  function visible(el) {
    if (TEST) { return true; }
    return !!(el.offsetWidth || el.offsetHeight || (el.getClientRects && el.getClientRects().length));
  }
  function isRec(el) { return !!(el && el.closest && el.closest('[data-ckyc-rec]')); }
  function pretty(k) {
    var s = (k || '').replace(/[-_]/g, ' ').replace(/\//g, ' > ').trim();
    return s.replace(/\b\w/g, function (c) { return c.toUpperCase(); }) || 'Home';
  }
  function push(type, data) {
    if (S.paused && type !== 'note' && type !== 'module') { return; }
    var e = data || {};
    e.type = type; e.t = new Date().toISOString(); e.url = location.pathname + (location.search || '');
    e.module = S.module;
    S.q.push(e);
    if (S.counts[type] !== undefined) { S.counts[type]++; }
    var m = S.modCounts[S.modKey] = S.modCounts[S.modKey] || { click: 0, type: 0, toast: 0, file: 0, snap: 0, dialog: 0, note: 0 };
    if (m[type] !== undefined) { m[type]++; }
    refreshBadge();
  }

  function labelOf(el) {
    try {
      if (el.labels && el.labels.length) { return txt(el.labels[0], 60); }
      var by = el.getAttribute('aria-labelledby');
      if (by) { var l = document.getElementById(by.split(' ')[0]); if (l) { return txt(l, 60); } }
      var fc = el.closest && el.closest('.MuiFormControl-root, .MuiTextField-root');
      if (fc) { var lb = fc.querySelector('label'); if (lb) { return txt(lb, 60); } }
    } catch (e) { /* none */ }
    return '';
  }
  function section(el) {
    try {
      var d = el.closest && el.closest('[role=dialog], .MuiDialog-root, .MuiDrawer-root');
      if (d) {
        var h = d.querySelector('h1,h2,h3,[role=heading],.MuiDialogTitle-root,[id*=title]');
        return (h ? txt(h, 60) : 'dialog');
      }
      var p = el;
      for (var i = 0; i < 6 && p; i++, p = p.parentElement) {
        var hh = p.querySelector && p.querySelector('h1,h2,h3,h4,[role=heading]');
        if (hh && !hh.contains(el)) { return txt(hh, 60); }
      }
    } catch (e) { /* none */ }
    return '';
  }
  function positional(el) {
    var parts = [], p = el;
    while (p && p.nodeType === 1 && p !== document.body) {
      if (stable(p.id)) { parts.unshift('*[@id=' + q(p.id) + ']'); return '//' + parts.join('/'); }
      var tag = p.tagName.toLowerCase(), i = 1, s = p.previousElementSibling;
      while (s) { if (s.tagName === p.tagName) { i++; } s = s.previousElementSibling; }
      parts.unshift(tag + '[' + i + ']');
      p = p.parentElement;
    }
    return '//body/' + parts.join('/');
  }
  function candidates(el, full) {
    var tag = el.tagName.toLowerCase(), c = [], role = el.getAttribute('role'), t = txt(el, 60);
    function add(kind, xp) { c.push({ kind: kind, xpath: xp, count: count(xp) }); }
    var tid = el.getAttribute('data-testid') || el.getAttribute('data-test');
    if (tid) { add('data-testid', '//*[@data-testid=' + q(tid) + ' or @data-test=' + q(tid) + ']'); }
    var tour = el.getAttribute('data-tour-label');
    if (tour) { add('data-tour-label', '//*[@data-tour-label=' + q(tour) + ']'); }
    if (stable(el.id)) { add('id', '//*[@id=' + q(el.id) + ']'); }
    var al = el.getAttribute('aria-label');
    if (al) { add('aria-label', '//' + tag + '[@aria-label=' + q(al) + ']'); }
    if (tag === 'input' && el.type === 'file') { add('file-input', "//input[@type='file']"); }
    if (t && (tag === 'button' || tag === 'a' || role === 'tab' || role === 'button' || role === 'menuitem' || role === 'option' || tag === 'th' || tag === 'label')) {
      var base = (role && tag !== 'button' && tag !== 'a') ? '*[@role=' + q(role) + ']' : tag;
      add('text', '//' + base + '[normalize-space()=' + q(t) + ']');
    }
    var ph = el.getAttribute('placeholder');
    if (ph) { add('placeholder', '//' + tag + '[@placeholder=' + q(ph) + ']'); }
    var nm = el.getAttribute('name');
    if (nm) { add('name', '//' + tag + '[@name=' + q(nm) + ']'); }
    var lb = (/^(input|textarea|select)$/.test(tag) || role === 'combobox') ? labelOf(el) : '';
    if (lb) { add('label', '//label[normalize-space()=' + q(lb) + ']/following::' + (role === 'combobox' ? '*[@role=\'combobox\']' : tag) + '[1]'); }
    var ti = el.getAttribute('title');
    if (ti) { add('title', '//*[@title=' + q(ti) + ']'); }
    if (full !== false) { add('position', positional(el)); }
    return c;
  }
  function best(cs) {
    var i, pick = null;
    for (i = 0; i < cs.length; i++) { if (cs[i].count === 1 && cs[i].kind !== 'position') { return { x: cs[i], unique: true }; } }
    for (i = 0; i < cs.length; i++) { if (cs[i].kind !== 'position' && cs[i].count > 0 && (!pick || cs[i].count < pick.count)) { pick = cs[i]; } }
    if (pick) { return { x: pick, unique: false }; }
    for (i = 0; i < cs.length; i++) { if (cs[i].kind === 'position') { return { x: cs[i], unique: cs[i].count === 1 }; } }
    return { x: { kind: 'none', xpath: '', count: 0 }, unique: false };
  }
  function describe(el, light) {
    var cs = candidates(el, true), b = best(cs);
    var tag = el.tagName.toLowerCase();
    var d = {
      tag: tag, text: txt(el, 80), id: el.id || '', name: el.getAttribute('name') || '', inputType: el.type || '',
      role: el.getAttribute('role') || '', ariaLabel: el.getAttribute('aria-label') || '', placeholder: el.getAttribute('placeholder') || '',
      label: labelOf(el), section: section(el), best: b.x.xpath, bestKind: b.x.kind, unique: b.unique, matches: b.x.count
    };
    d.inNav = !!(el.closest && el.closest('nav,[id^="tour-nav-"],[data-tour-label],ul.MuiList-root'));
    if (!light) { d.candidates = cs; }
    if (/^(input|textarea|select)$/.test(tag) && el.type !== 'password' && el.type !== 'file') {
      var v = (el.value || '');
      d.valueLength = v.length;
      d.masked = /[X*•]{3,}/.test(v);
    } else if (/[X*•]{3,}/.test(d.text)) { d.masked = true; }
    return d;
  }
  function clickable(el) {
    var sel = 'button,a,[role=button],[role=tab],[role=menuitem],[role=option],[role=checkbox],[role=switch],input,label,li,[tabindex],td,tr,svg,[class*=Icon]';
    var c = el.closest ? el.closest(sel) : null;
    return c || el;
  }

  // ---------------------------------------------------------------- events
  document.addEventListener('click', function (e) {
    if (QUIET) { return; }
    var el = e.target;
    if (!el || isRec(el) || S.paused) { return; }
    var c = clickable(el);
    var nav = c.closest && c.closest('[id^="tour-nav-"],[id^="nav-nav-"],[id^="nav-group-"],[data-tour-label],[data-nav-label]');
    if (nav) {
      S.lastNav = { label: nav.getAttribute('data-tour-label') || nav.getAttribute('data-nav-label') || txt(nav, 40), t: Date.now() };
    }
    var d = describe(c);
    d.nav = !!nav;
    push('click', d);
    // a tab or a "view / eye" icon changes what is on screen: take a screen inventory (labels + shown values) of it
    var role = c.getAttribute ? c.getAttribute('role') : '';
    if (role === 'tab') {
      var tl = txt(c, 40);
      setTimeout(function () { snapshot('tab: ' + tl); }, 1200);
    }
    var hint = (c.getAttribute && (c.getAttribute('aria-label') || c.getAttribute('title'))) || '';
    var eye = c.querySelector ? c.querySelector('[data-testid*=Visib]') : null;
    if (eye || /view|show|reveal|unmask|eye/i.test(hint)) {
      setTimeout(function () { snapshot('reveal: ' + (hint || 'eye icon')); }, 800);
    }
  }, true);

  document.addEventListener('change', function (e) {
    if (QUIET) { return; }
    var el = e.target;
    if (!el || isRec(el) || S.paused || !el.tagName) { return; }
    if (el.type === 'password') { return; }
    if (el.type === 'file') {
      var names = [];
      for (var i = 0; el.files && i < el.files.length; i++) { names.push(el.files[i].name + ' (' + el.files[i].size + ' bytes)'); }
      var d = describe(el); d.files = names; d.via = 'file-input';
      push('file', d);
      return;
    }
    var dd = describe(el);
    if (el.type === 'checkbox' || el.type === 'radio') { dd.checked = !!el.checked; }
    if (el.tagName === 'SELECT' && el.selectedOptions && el.selectedOptions.length) { dd.selected = txt(el.selectedOptions[0], 40); }
    push('type', dd);
  }, true);

  document.addEventListener('drop', function (e) {
    if (QUIET) { return; }
    var el = e.target;
    if (!el || isRec(el) || S.paused) { return; }
    var names = [], f = e.dataTransfer && e.dataTransfer.files;
    for (var i = 0; f && i < f.length; i++) { names.push(f[i].name + ' (' + f[i].size + ' bytes)'); }
    var d = describe(clickable(el)); d.files = names; d.via = 'drop';
    push('file', d);
  }, true);

  // toasts / alerts / dialogs
  var TOAST = '[role=alert],[role=status],.Toastify__toast,.MuiSnackbar-root,.MuiAlert-root,[class*=toast i],[class*=Toast]';
  var DIALOG = '[role=dialog],.MuiDialog-root,.MuiDrawer-root,.MuiModal-root';
  function onAdded(n) {
    if (QUIET) { return; }
    if (n.nodeType !== 1 || isRec(n)) { return; }
    var t = n.matches && n.matches(TOAST) ? n : (n.querySelector ? n.querySelector(TOAST) : null);
    if (t) {
      var s = txt(t, 300);
      if (s && s !== S.lastToast) {
        S.lastToast = s;
        setTimeout(function () { S.lastToast = ''; }, 2000);
        var d = describe(t, true); d.message = s;
        push('toast', d);
      }
    }
    var dlg = n.matches && n.matches(DIALOG) ? n : (n.querySelector ? n.querySelector(DIALOG) : null);
    if (dlg) {
      var h = dlg.querySelector('h1,h2,h3,[role=heading],.MuiDialogTitle-root');
      push('dialog', { title: h ? txt(h, 80) : txt(dlg, 60), best: '' });
      setTimeout(function () { snapshot('dialog'); }, 1200);
    }
  }
  new MutationObserver(function (ms) {
    for (var i = 0; i < ms.length; i++) { for (var j = 0; j < ms[i].addedNodes.length; j++) { onAdded(ms[i].addedNodes[j]); } }
  }).observe(document.body || document.documentElement, { childList: true, subtree: true });

  // ---------------------------------------------------------------- screen inventory
  function snapshot(kind) {
    if (S.paused || QUIET) { return; }
    push('snap', buildSnap(kind));
  }
  function buildSnap(kind) {
    var sel = 'button,a[href],[role=tab],[role=button],[role=menuitem],input,textarea,select,[role=combobox],[role=checkbox],[role=switch],th,[role=columnheader],h1,h2,h3,h4,[role=heading],label,[data-tour-label]';
    var els = document.querySelectorAll(sel), items = [], i;
    for (i = 0; i < els.length && items.length < 350; i++) {
      var el = els[i];
      if (isRec(el) || (!visible(el) && !(el.tagName === 'INPUT' && el.type === 'file'))) { continue; }
      if (el.tagName === 'INPUT' && el.type === 'password') { continue; }
      items.push(describe(el, true));
    }
    // visible leaf texts (labels and shown values, e.g. Customer Information) with their locator
    var texts = [], all = document.querySelectorAll('main *, [role=dialog] *, [class*=Drawer] *, body *'), seen = {};
    for (i = 0; i < all.length && texts.length < 600; i++) {
      var n = all[i];
      if (n.children.length || /^(SCRIPT|STYLE|NOSCRIPT|TITLE|META|LINK|PATH|SVG)$/i.test(n.tagName) || isRec(n) || !visible(n)) { continue; }
      var s = (n.textContent || '').replace(/\s+/g, ' ').trim();
      if (!s || s.length > 80 || seen[s + '|' + (n.className || '')]) { continue; }
      seen[s + '|' + (n.className || '')] = 1;
      var cs = candidates(n, true), b = best(cs);
      texts.push({ text: s, tag: n.tagName.toLowerCase(), best: b.x.xpath, bestKind: b.x.kind, unique: b.unique,
        masked: /[X*•]{3,}/.test(s), section: section(n) });
    }
    var files = [];
    var fi = document.querySelectorAll("input[type=file]");
    for (i = 0; i < fi.length; i++) { var f = describe(fi[i]); f.accept = fi[i].getAttribute('accept') || ''; f.multiple = !!fi[i].multiple; files.push(f); }
    return { kind: kind || 'manual', items: items, texts: texts, fileInputs: files, title: document.title, path: location.pathname };
  }

  // ---------------------------------------------------------------- module switch + overlay
  function modKey() {
    var seg = location.pathname.replace(/\/+$/, '').split('/').filter(Boolean);
    return seg.slice(0, 2).join('/') || '/';
  }
  function modLabel() {
    return (S.lastNav && Date.now() - S.lastNav.t < 6000) ? S.lastNav.label : pretty(modKey());
  }
  function el(tag, css, text) { var e = document.createElement(tag); e.setAttribute('data-ckyc-rec', '1'); if (css) { e.style.cssText = css; } if (text != null) { e.textContent = text; } return e; }

  var badge, badgeText;
  function buildBadge() {
    badge = el('div', 'position:fixed;right:14px;bottom:14px;z-index:2147483646;background:#0b2a6f;color:#fff;border-radius:12px;padding:10px 12px;font:12.5px Segoe UI,Arial,sans-serif;box-shadow:0 8px 24px rgba(0,0,0,.35);width:250px;');
    var title = el('div', 'font-weight:700;font-size:13px;margin-bottom:4px;', '');
    var dot = el('span', 'display:inline-block;width:9px;height:9px;border-radius:50%;background:#ef4444;margin-right:7px;', '');
    title.appendChild(dot); title.appendChild(document.createTextNode('Locator capture ON'));
    badgeText = el('div', 'line-height:1.45;margin-bottom:8px;', '');
    var row = el('div', 'display:flex;gap:6px;flex-wrap:wrap;', '');
    function btn(label, fn, bg) {
      var b = el('button', 'border:0;border-radius:7px;padding:5px 9px;cursor:pointer;font-weight:600;background:' + (bg || '#e2e8f0') + ';color:#0f172a;', label);
      b.type = 'button'; b.onclick = fn; row.appendChild(b); return b;
    }
    btn('Snapshot screen', function () { snapshot('manual'); });
    btn('Add note', function () { var n = window.prompt('Note for this step (what you are doing / expected):'); if (n) { push('note', { note: n, best: '' }); } });
    var pb = btn('Pause', function () { S.paused = !S.paused; pb.textContent = S.paused ? 'Resume' : 'Pause'; refreshBadge(); });
    btn('Finish', function () { if (window.confirm('Finish the capture and save everything?')) { S.finish = true; refreshBadge(); } }, '#fecaca');
    badge.appendChild(title); badge.appendChild(badgeText); badge.appendChild(row);
    (document.body || document.documentElement).appendChild(badge);
  }
  function refreshBadge() {
    if (QUIET) { return; }
    if (!badge || !badge.isConnected) { if (document.body) { buildBadge(); } else { return; } }
    var c = S.counts;
    badgeText.textContent = (S.finish ? 'Finishing... ' : S.paused ? 'PAUSED. ' : '') + 'Module: ' + (S.module || '-') +
      '  |  clicks ' + c.click + ', fields ' + c.type + ', files ' + c.file + ', toasts ' + c.toast + ', snapshots ' + c.snap;
  }

  var popup;
  function showPopup(prev, now, prevKey) {
    if (popup && popup.isConnected) { popup.remove(); }
    var m = S.modCounts[prevKey] || { click: 0, type: 0, toast: 0, file: 0, snap: 0 };
    popup = el('div', 'position:fixed;top:16px;left:50%;transform:translateX(-50%);z-index:2147483647;width:min(560px,92vw);background:#fff;color:#0f172a;border-radius:14px;box-shadow:0 18px 50px rgba(0,0,0,.35);border:1px solid #cbd5e1;font:13px Segoe UI,Arial,sans-serif;overflow:hidden;');
    popup.appendChild(el('div', 'background:#1d4ed8;color:#fff;padding:11px 16px;font-weight:700;font-size:15px;', 'Module changed: ' + (prev || 'start') + '  →  ' + now));
    var body = el('div', 'padding:12px 16px;');
    body.appendChild(el('div', 'margin-bottom:8px;', 'Captured in "' + (prev || 'start') + '": ' + m.click + ' clicks, ' + m.type + ' fields, ' + m.file +
      ' files, ' + m.toast + ' toasts, ' + m.snap + ' screen snapshots. A snapshot of the new screen is taken automatically in a moment.'));
    var ta = el('textarea', 'width:100%;height:54px;border:1px solid #94a3b8;border-radius:8px;padding:6px;font:inherit;box-sizing:border-box;', '');
    ta.placeholder = 'What did you do in "' + (prev || 'start') + '" and what did you expect? (optional note)';
    body.appendChild(ta);
    var row = el('div', 'display:flex;gap:8px;justify-content:flex-end;margin-top:10px;', '');
    function b(label, bg, color, fn) { var x = el('button', 'border:0;border-radius:8px;padding:7px 14px;font-weight:600;cursor:pointer;background:' + bg + ';color:' + color + ';', label); x.type = 'button'; x.onclick = fn; row.appendChild(x); }
    b('Snapshot now', '#e2e8f0', '#0f172a', function () { snapshot('manual'); });
    b('Save note and continue', '#16a34a', '#fff', function () { if (ta.value.trim()) { push('note', { note: ta.value.trim(), best: '', about: prev }); } popup.remove(); });
    body.appendChild(row); popup.appendChild(body);
    (document.body || document.documentElement).appendChild(popup);
  }

  var first = !S.modKey;
  function checkModule() {
    var k = modKey();
    if (k === S.modKey) { return; }
    var prevKey = S.modKey, prev = S.module;
    S.modKey = k; S.module = modLabel();
    push('module', { from: prev, to: S.module, key: k, best: '' });
    if (first || !prevKey) { first = false; } else if (!QUIET) { showPopup(prev, S.module, prevKey); }
    if (!QUIET) { setTimeout(function () { snapshot('auto'); }, 2500); }
    refreshBadge();
  }
  ['pushState', 'replaceState'].forEach(function (fn) {
    var orig = history[fn];
    history[fn] = function () { var r = orig.apply(this, arguments); setTimeout(checkModule, 50); return r; };
  });
  window.addEventListener('popstate', function () { setTimeout(checkModule, 50); });
  setInterval(checkModule, 500);

  window.__ckycRecDrain = function () {
    return JSON.stringify({ events: S.q.splice(0, S.q.length), module: S.module, modKey: S.modKey, finish: S.finish, paused: S.paused,
      counts: S.counts, modCounts: S.modCounts });
  };
  window.__ckycRecSnapshot = snapshot;
  window.__ckycRecSnapshotNow = function (kind) { return JSON.stringify(buildSnap(kind)); };
  checkModule();
  refreshBadge();
})();

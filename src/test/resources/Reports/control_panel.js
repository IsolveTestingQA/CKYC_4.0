/* Test Control Panel - runs inside the application page right after the browser opens.
   arguments[0] = JSON: { tester, url, modules:[{key,title,subs:[{key,title,pos,neg}],groups:[{key,label,options,defaults}],texts:[{key,label,def}]}],
                          last, presets:[{name,sel}] }
   The answer is written to window.__ckycPanel as a JSON string:
   { action:'start'|'stop', mode, modules:{key:{subs:[],groups:{},texts:{}}}, opts:{advanced,extentCucumber,stopFirst}, preset } */
var cfg = JSON.parse(arguments[0]);
var st = { mode: 'POSITIVE', mods: {}, opts: { advanced: true, extentCucumber: true, stopFirst: false, record: !!cfg.record } };

function modDef(key) { for (var i = 0; i < cfg.modules.length; i++) { if (cfg.modules[i].key === key) { return cfg.modules[i]; } } return null; }

function defaults() {
  st.mode = 'POSITIVE';
  st.opts = { advanced: true, extentCucumber: true, stopFirst: false, record: !!cfg.record };
  st.mods = {};
  cfg.modules.forEach(function (m) {
    var s = { on: m.key === 'login' || m.key === 'dashboard', subs: {}, groups: {}, texts: {} };
    m.subs.forEach(function (x) { s.subs[x.key] = true; });
    m.groups.forEach(function (g) { s.groups[g.key] = {}; g.options.forEach(function (o) { s.groups[g.key][o] = g.defaults.indexOf(o) >= 0; }); });
    m.texts.forEach(function (t) { s.texts[t.key] = t.def; });
    st.mods[m.key] = s;
  });
}

function fromAnswer(sel) {
  defaults();
  if (!sel) { return; }
  st.mode = sel.mode || 'POSITIVE';
  if (sel.opts) { st.opts = { advanced: sel.opts.advanced !== false, extentCucumber: sel.opts.extentCucumber !== false, stopFirst: !!sel.opts.stopFirst, record: !!cfg.record }; }
  cfg.modules.forEach(function (m) {
    var a = sel.modules && sel.modules[m.key];
    var s = st.mods[m.key];
    s.on = !!a;
    if (a) {
      m.subs.forEach(function (x) { s.subs[x.key] = (a.subs || []).indexOf(x.key) >= 0; });
      m.groups.forEach(function (g) {
        var picked = (a.groups && a.groups[g.key]) || [];
        g.options.forEach(function (o) { s.groups[g.key][o] = picked.length ? picked.indexOf(o) >= 0 : g.defaults.indexOf(o) >= 0; });
      });
      m.texts.forEach(function (t) { if (a.texts && a.texts[t.key]) { s.texts[t.key] = a.texts[t.key]; } });
    }
  });
}

function supported(sub) { return st.mode === 'POSITIVE' ? sub.pos : st.mode === 'NEGATIVE' ? sub.neg : (sub.pos || sub.neg); }

// ---- DOM helpers
function el(tag, css, text) { var e = document.createElement(tag); if (css) { e.style.cssText = css; } if (text != null) { e.textContent = text; } return e; }
function checkbox(label, checked, disabled, onchange, bold) {
  var l = el('label', 'display:inline-flex;align-items:center;gap:7px;margin-right:16px;cursor:' + (disabled ? 'not-allowed' : 'pointer') +
    ';opacity:' + (disabled ? '.5' : '1') + ';font-weight:' + (bold ? '700' : '400') + ';');
  var c = document.createElement('input'); c.type = 'checkbox'; c.checked = checked; c.disabled = disabled;
  c.style.cssText = 'width:16px;height:16px;accent-color:#1d4ed8;';
  c.onchange = function () { onchange(c.checked); };
  l.appendChild(c); l.appendChild(document.createTextNode(label));
  return l;
}

var wrap = el('div', 'position:fixed;inset:0;z-index:2147483647;display:flex;align-items:flex-start;justify-content:center;' +
  'background:rgba(15,23,42,.6);font-family:Segoe UI,Arial,sans-serif;overflow:auto;padding:24px 0;');
wrap.id = 'ckyc-control-panel';
var box = el('div', 'width:min(1040px,95vw);background:#fff;border-radius:14px;box-shadow:0 24px 60px rgba(0,0,0,.4);overflow:hidden;color:#14213d;');
var head = el('div', 'background:#1d4ed8;color:#fff;padding:14px 22px;');
head.appendChild(el('div', 'font-size:19px;font-weight:700;', 'iFlowCKYC 4.0 - Test Control Panel'));
head.appendChild(el('div', 'font-size:13px;opacity:.9;', 'Browser is open and waiting. Choose what to test, then press Start Test - the run continues in this same browser.'));
var body = el('div', 'padding:16px 22px 8px;');
box.appendChild(head); box.appendChild(body);
wrap.appendChild(box);

var left, right, summary, startBtn;
function render() {
  body.innerHTML = '';
  var grid = el('div', 'display:grid;grid-template-columns:1fr 1.7fr;gap:16px;');
  left = el('div'); right = el('div');
  grid.appendChild(left); grid.appendChild(right); body.appendChild(grid);

  function card(parent, title) {
    var c = el('div', 'border:1px solid #d8e0ec;border-radius:11px;padding:11px 14px;margin-bottom:12px;');
    c.appendChild(el('div', 'font-size:11.5px;text-transform:uppercase;letter-spacing:.08em;color:#5b6b85;margin-bottom:6px;', title));
    parent.appendChild(c); return c;
  }
  // this run
  var c1 = card(left, 'This run');
  c1.appendChild(el('div', 'font-size:13px;', 'Environment: ' + cfg.url));
  c1.appendChild(el('div', 'font-size:13px;', 'Tested by: ' + cfg.tester));
  if (cfg.runner) { c1.appendChild(el('div', 'font-size:13px;', 'Started from: ' + cfg.runner)); }
  c1.appendChild(el('div', 'font-size:12px;color:#5b6b85;margin-top:4px;', 'Login comes from config - no password is shown here.'));
  // presets
  var cp = card(left, 'Preset');
  var sel = el('select', 'width:100%;padding:6px;border:1px solid #94a3b8;border-radius:6px;');
  var o0 = document.createElement('option'); o0.value = ''; o0.textContent = '(choose a saved preset)'; sel.appendChild(o0);
  if (cfg.last) { var ol = document.createElement('option'); ol.value = '__last'; ol.textContent = 'Last run'; sel.appendChild(ol); }
  cfg.presets.forEach(function (p, i) { var o = document.createElement('option'); o.value = String(i); o.textContent = p.name; sel.appendChild(o); });
  sel.onchange = function () {
    if (sel.value === '__last') { fromAnswer(cfg.last); }
    else if (sel.value !== '') { fromAnswer(cfg.presets[parseInt(sel.value, 10)].sel); }
    else { return; }
    render();
  };
  cp.appendChild(sel);
  var nameBox = document.createElement('input'); nameBox.type = 'text'; nameBox.id = 'ckyc-preset-name'; nameBox.placeholder = 'Save these choices as preset (name)';
  nameBox.style.cssText = 'width:100%;margin-top:6px;padding:6px;border:1px solid #94a3b8;border-radius:6px;box-sizing:border-box;';
  cp.appendChild(nameBox);
  // type of testing
  var c2 = card(left, 'Type of testing');
  [['POSITIVE', 'Positive'], ['NEGATIVE', 'Negative'], ['BOTH', 'Both (positive and negative)']].forEach(function (m) {
    var l = el('label', 'display:block;margin:3px 0;cursor:pointer;');
    var r = document.createElement('input'); r.type = 'radio'; r.name = 'ckyc-mode'; r.checked = st.mode === m[0];
    r.style.cssText = 'accent-color:#1d4ed8;margin-right:7px;';
    r.onchange = function () { st.mode = m[0]; render(); };
    l.appendChild(r); l.appendChild(document.createTextNode(m[1])); c2.appendChild(l);
  });
  c2.appendChild(el('div', 'font-size:12px;color:#5b6b85;margin-top:4px;', 'Parts without that kind of test are greyed out.'));
  // when it ends
  var c3 = card(left, 'When the run ends');
  var o = st.opts;
  c3.appendChild(checkbox('Open the Advanced report', o.advanced, false, function (v) { o.advanced = v; }));
  c3.appendChild(document.createElement('br'));
  c3.appendChild(checkbox('Open Extent and Cucumber reports', o.extentCucumber, false, function (v) { o.extentCucumber = v; }));
  c3.appendChild(document.createElement('br'));
  c3.appendChild(checkbox('Stop at the first failed scenario', o.stopFirst, false, function (v) { o.stopFirst = v; }));
  // screen recording: off by default, decided here BEFORE Start Test (it starts with the first step)
  var c4 = card(left, 'Screen recording');
  c4.appendChild(checkbox('Record the browser window (video with timer)', o.record, false, function (v) { o.record = v; update(); }, true));
  c4.appendChild(el('div', 'font-size:12px;color:#5b6b85;margin-top:4px;', 'Off by default. On = the video runs through the whole run (a runner with RECORD_SCREEN = true has already started it when the browser opened) and stops after the reports are generated, with a completion message and a final screenshot. Saved in Report Output/Screen Recordings. Other windows you open on top do not appear in the video.'));

  // modules
  var cm = card(right, 'Modules (tick one or many)');
  cfg.modules.forEach(function (m) {
    var s = st.mods[m.key];
    var row = el('div', 'border:1px solid ' + (s.on ? '#1d4ed8' : '#d8e0ec') + ';background:' + (s.on ? '#f5f8ff' : '#fff') + ';border-radius:9px;padding:7px 11px;margin:6px 0;');
    row.appendChild(checkbox(m.title, s.on, false, function (v) { s.on = v; render(); }, true));
    if (!m.subs.some(supported)) {
      row.appendChild(el('span', 'color:#b45309;font-size:12.5px;', '(no ' + (st.mode === 'NEGATIVE' ? 'negative' : 'positive') + ' tests for this module - nothing would run)'));
    }
    if (s.on) {
      var sub = el('div', 'margin:6px 0 0 22px;font-size:13px;');
      if (m.subs.length > 1 || m.subs[0].title !== m.title) {
        m.subs.forEach(function (x) {
          var ok = supported(x);
          var lab = x.title + (ok ? '' : ' (no ' + (st.mode === 'NEGATIVE' ? 'negative' : 'positive') + ' tests)');
          sub.appendChild(checkbox(lab, ok && s.subs[x.key], !ok, function (v) { s.subs[x.key] = v; update(); }));
        });
        sub.appendChild(document.createElement('br'));
      }
      m.groups.forEach(function (g) {
        sub.appendChild(el('span', 'color:#5b6b85;margin-right:8px;', g.label + ':'));
        g.options.forEach(function (op) { sub.appendChild(checkbox(op, s.groups[g.key][op], false, function (v) { s.groups[g.key][op] = v; update(); })); });
        sub.appendChild(document.createElement('br'));
        if (g.quick && g.quick.length) {
          sub.appendChild(el('span', 'color:#5b6b85;margin-right:8px;font-size:12px;', 'Quick pick:'));
          g.quick.forEach(function (q) {
            var b = el('button', 'margin:2px 6px 2px 0;padding:3px 10px;border-radius:14px;border:1px solid #94a3b8;background:#fff;'
              + 'color:#0f172a;font-size:12px;cursor:pointer;', q.label);
            b.type = 'button';
            b.onclick = function () {
              g.options.forEach(function (op) { s.groups[g.key][op] = q.options.indexOf(op) >= 0; });
              render();
            };
            sub.appendChild(b);
          });
          sub.appendChild(document.createElement('br'));
        }
      });
      m.texts.forEach(function (t) {
        sub.appendChild(el('span', 'color:#5b6b85;margin-right:8px;', t.label + ':'));
        var inp = document.createElement('input'); inp.type = 'text'; inp.value = s.texts[t.key] || '';
        inp.style.cssText = 'padding:3px 8px;border:1px solid #94a3b8;border-radius:6px;margin-right:16px;width:150px;';
        inp.oninput = function () { s.texts[t.key] = inp.value; };
        sub.appendChild(inp);
      });
      row.appendChild(sub);
    }
    cm.appendChild(row);
  });

  // summary + buttons
  summary = el('div', 'background:#f0f9ff;border:1px solid #bae6fd;border-radius:11px;padding:10px 14px;margin:2px 0 12px;font-size:13px;');
  body.appendChild(summary);
  var foot = el('div', 'display:flex;justify-content:flex-end;gap:10px;padding:0 0 16px;');
  var stopBtn = el('button', 'padding:9px 18px;border-radius:9px;border:1px solid #cbd5e1;background:#f8fafc;font-weight:700;cursor:pointer;', 'Stop - do not run');
  stopBtn.onclick = function () { window.__ckycPanel = JSON.stringify({ action: 'stop' }); };
  startBtn = el('button', 'padding:9px 22px;border-radius:9px;border:none;background:#16a34a;color:#fff;font-weight:700;font-size:15px;cursor:pointer;', '▶  Start Test');
  startBtn.onclick = function () { if (!startBtn.disabled) { window.__ckycPanel = JSON.stringify(answer()); } };
  foot.appendChild(stopBtn); foot.appendChild(startBtn); body.appendChild(foot);
  update();
}

function selectedModules() {
  var out = [];
  cfg.modules.forEach(function (m) {
    var s = st.mods[m.key];
    if (!s.on) { return; }
    var subs = m.subs.filter(function (x) { return s.subs[x.key] && supported(x); });
    if (subs.length) { out.push({ m: m, s: s, subs: subs }); }
  });
  return out;
}

function update() {
  var mods = selectedModules();
  var modeName = st.mode === 'NEGATIVE' ? 'Negative' : st.mode === 'BOTH' ? 'Positive + Negative' : 'Positive';
  if (!mods.length) {
    summary.innerHTML = '<b>Nothing selected yet.</b> Tick at least one module that has ' + modeName.toLowerCase() + ' tests.';
    startBtn.disabled = true; startBtn.style.opacity = '.5'; return;
  }
  var txt = mods.map(function (x) {
    var d = x.subs.length < x.m.subs.length || x.m.subs.length > 1 ? ' (' + x.subs.map(function (y) { return y.title; }).join(', ') + ')' : '';
    return x.m.title + d;
  }).join(' → ');
  summary.innerHTML = '<b>This run will do (' + modeName + '):</b> ' + txt.replace(/&/g, '&amp;').replace(/</g, '&lt;') + (st.opts.record ? ' &nbsp;|&nbsp; <b>screen recording ON</b>' : '');
  startBtn.disabled = false; startBtn.style.opacity = '1';
}

function answer() {
  var res = { action: 'start', mode: st.mode, modules: {}, opts: st.opts, preset: (document.getElementById('ckyc-preset-name') || {}).value || '' };
  selectedModules().forEach(function (x) {
    var groups = {};
    x.m.groups.forEach(function (g) { groups[g.key] = g.options.filter(function (o) { return x.s.groups[g.key][o]; }); });
    res.modules[x.m.key] = { subs: x.subs.map(function (y) { return y.key; }), groups: groups, texts: x.s.texts };
  });
  return res;
}

fromAnswer(cfg.defaults || cfg.last);
render();
document.documentElement.appendChild(wrap);

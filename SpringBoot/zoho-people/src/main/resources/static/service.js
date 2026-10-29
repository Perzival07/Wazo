// Generic service page: lists the records of each Zoho form mapped to a service.
const SERVICES = [
 {
  "slug": "onboarding",
  "glyph": "idcard",
  "color": ["#f59e0b", "#f97316"],
  "title": "Onboarding",
  "icon": "🪪",
  "desc": "New joiners and recently added employees.",
  "forms": [
   [
    "employee",
    "Employees (newest have the latest Added time)"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "employee-information",
  "glyph": "building",
  "color": ["#f59e0b", "#eab308"],
  "title": "Employee Information",
  "icon": "🏢",
  "desc": "Employee records, departments and designations.",
  "forms": [
   [
    "employee",
    "Employees"
   ],
   [
    "department",
    "Departments"
   ],
   [
    "designation",
    "Designations"
   ]
  ],
  "links": [
   [
    "employees.html",
    "Open the employee directory"
   ]
  ],
  "note": null
 },
 {
  "slug": "leave-tracker",
  "glyph": "umbrella",
  "color": ["#3b82f6", "#06b6d4"],
  "title": "Leave Tracker",
  "icon": "🏖️",
  "desc": "Leave requests, balances and applying for leave.",
  "forms": [
   [
    "leave",
    "Leave requests"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "attendance",
  "glyph": "calendar",
  "color": ["#ef4444", "#f97316"],
  "title": "Attendance",
  "icon": "📅",
  "desc": "Check in and check out.",
  "forms": [],
  "links": [
   [
    "attendance.html",
    "Check in / check out"
   ]
  ],
  "note": null
 },
 {
  "slug": "shift",
  "glyph": "clock",
  "color": ["#06b6d4", "#3b82f6"],
  "title": "Shift",
  "icon": "🕒",
  "desc": "Work shifts and schedules.",
  "forms": [],
  "links": [],
  "note": "Zoho People's forms API has no form for this module in your organisation, and this integration only requests the forms, leave, attendance and employee scopes."
 },
 {
  "slug": "time-tracker",
  "glyph": "timer",
  "color": ["#f59e0b", "#f97316"],
  "title": "Time Tracker",
  "icon": "⏱️",
  "desc": "Clients, projects and jobs used for timesheets.",
  "forms": [
   [
    "P_ClientDetails",
    "Clients"
   ],
   [
    "P_TimesheetJobsList",
    "Projects"
   ],
   [
    "P_TimesheetJob",
    "Jobs"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "performance",
  "glyph": "trophy",
  "color": ["#ec4899", "#f43f5e"],
  "title": "Performance",
  "icon": "🏆",
  "desc": "Appraisals and goals.",
  "forms": [],
  "links": [],
  "note": "Zoho People's forms API has no form for this module in your organisation, and this integration only requests the forms, leave, attendance and employee scopes."
 },
 {
  "slug": "files",
  "glyph": "folder",
  "color": ["#3b82f6", "#6366f1"],
  "title": "Files",
  "icon": "📁",
  "desc": "Company and employee documents.",
  "forms": [],
  "links": [],
  "note": "Zoho People's forms API has no form for this module in your organisation, and this integration only requests the forms, leave, attendance and employee scopes."
 },
 {
  "slug": "employee-engagement",
  "glyph": "heart",
  "color": ["#ec4899", "#a855f7"],
  "title": "Employee Engagement",
  "icon": "🤝",
  "desc": "Surveys and engagement.",
  "forms": [],
  "links": [],
  "note": "Zoho People's forms API has no form for this module in your organisation, and this integration only requests the forms, leave, attendance and employee scopes."
 },
 {
  "slug": "hr-letters",
  "glyph": "file",
  "color": ["#8b5cf6", "#6366f1"],
  "title": "HR Letters",
  "icon": "⭐",
  "desc": "Address proof, bonafide and experience letter requests.",
  "forms": [
   [
    "AddressProof",
    "Address proof"
   ],
   [
    "BonafideLetter",
    "Bonafide letters"
   ],
   [
    "ExperienceLetter",
    "Experience letters"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "travel",
  "glyph": "send",
  "color": ["#0ea5e9", "#6366f1"],
  "title": "Travel",
  "icon": "✈️",
  "desc": "Travel requests and expenses.",
  "forms": [
   [
    "travelrequest",
    "Travel requests"
   ],
   [
    "travelexpenses",
    "Travel expenses"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "tasks",
  "glyph": "check",
  "color": ["#10b981", "#14b8a6"],
  "title": "Tasks",
  "icon": "✅",
  "desc": "Tasks assigned in Zoho People.",
  "forms": [
   [
    "P_Task",
    "Tasks"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "general",
  "glyph": "building",
  "color": ["#f59e0b", "#f97316"],
  "title": "General",
  "icon": "🏛️",
  "desc": "Organisation structure: departments and designations.",
  "forms": [
   [
    "department",
    "Departments"
   ],
   [
    "designation",
    "Designations"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "offboarding",
  "glyph": "logout",
  "color": ["#ef4444", "#f43f5e"],
  "title": "Offboarding",
  "icon": "🚪",
  "desc": "Resignations, terminations and exit details.",
  "forms": [
   [
    "zp_resignation",
    "Resignations"
   ],
   [
    "zp_termination",
    "Terminations"
   ],
   [
    "exitinterview",
    "Exit details"
   ],
   [
    "zp_deceased",
    "Deceased"
   ]
  ],
  "links": [],
  "note": null
 },
 {
  "slug": "okr",
  "glyph": "target",
  "color": ["#eab308", "#f59e0b"],
  "title": "OKR",
  "icon": "🎯",
  "desc": "Objectives and key results.",
  "forms": [],
  "links": [],
  "note": "Zoho People's forms API has no form for this module in your organisation, and this integration only requests the forms, leave, attendance and employee scopes."
 },
 {
  "slug": "approvals",
  "glyph": "filecheck",
  "color": ["#14b8a6", "#10b981"],
  "title": "Approvals",
  "icon": "📝",
  "desc": "Requests and their approval status. Leave requests are shown here; travel requests are on the Travel page.",
  "forms": [
   [
    "leave",
    "Leave requests (see ApprovalStatus)"
   ],
   [
    "travelrequest",
    "Travel requests (see ApprovalStatus)"
   ]
  ],
  "links": [
   [
    "svc-travel.html",
    "Travel"
   ],
   [
    "svc-leave-tracker.html",
    "Leave Tracker"
   ]
  ],
  "note": null
 },
 {
  "slug": "data-administration",
  "glyph": "database",
  "color": ["#a855f7", "#ec4899"],
  "title": "Data Administration",
  "icon": "🗂️",
  "desc": "Every form in your Zoho People org. Open any form to view its records.",
  "forms": [],
  "links": [
   [
    "forms.html",
    "Browse all forms"
   ]
  ],
  "note": null
 }
];

function servicePage(slug) {
  const svc = SERVICES.find(s => s.slug === slug);
  const page = layout(svc.title, svc.desc);
  page.insertAdjacentHTML('beforeend', `<a class="back" href="services.html">${icon('back')} All services</a>`);
  if (svc.note) page.insertAdjacentHTML('beforeend', `<section class="card"><div class="empty"><div class="ico">${icon(svc.glyph)}</div><b>Not available through the API</b><p>${esc(svc.note)}</p></div></section>`);
  if (svc.links && svc.links.length) page.insertAdjacentHTML('beforeend',
    `<div class="actions" style="margin:0 0 20px">${svc.links.map(([h, l]) => `<a href="${h}"><button class="primary">${esc(l)}</button></a>`).join('')}</div>`);
  if (slug === 'leave-tracker') leaveActions(page);
  (svc.forms || []).forEach(([form, label]) => section(page, form, label));
}

function leaveActions(page) {
  page.insertAdjacentHTML('beforeend', `
    <section class="card"><div class="row"><h2 style="margin:0">Leave types &amp; balances</h2><button id="lv-load">Load</button></div>
      <pre id="lv-types" hidden style="margin-top:12px"></pre></section>
    <section class="card"><h2>Apply for leave</h2><div class="grid">
      <div><label for="lv-type">Leave type ID</label><input id="lv-type" placeholder="from the list above"></div>
      <div><label for="lv-from">From</label><input id="lv-from" type="date"></div>
      <div><label for="lv-to">To</label><input id="lv-to" type="date"></div>
      <div><label for="lv-count">Day length</label><select id="lv-count"><option value="1">Full day</option><option value="0.5">Half day</option><option value="0.25">Quarter day</option></select></div>
    </div><div class="actions"><button class="primary" id="lv-apply">Apply</button></div>
    <p class="hint">Half and quarter days need From and To to be the same date.</p></section>`);
  $('lv-load').addEventListener('click', e => run(e.target, async () => {
    $('lv-types').textContent = JSON.stringify(await api('GET', DATA + '/leave/types'), null, 2); $('lv-types').hidden = false; }));
  $('lv-apply').addEventListener('click', e => run(e.target, async () => {
    await api('POST', DATA + '/leave', { leaveTypeId: $('lv-type').value.trim(), from: $('lv-from').value,
      to: $('lv-to').value || $('lv-from').value, leaveCount: Number($('lv-count').value) });
    banner('ok', 'Leave request sent to Zoho.'); }));
}

const SKIP = /(^|\.)(id|ZUID)$|photo|tabularSections|\.ID$|_downloadUrl/i;
const PRIORITY = /name|employee|status|type|from|to$|date|title|subject|department|designation|project|client|job|reason|amount|days/i;

function section(page, form, label) {
  const el = document.createElement('section');
  el.className = 'card';
  el.className = 'card flush';
  el.innerHTML = `<div class="row" style="padding:20px 22px 0"><h2 style="margin:0">${esc(label)}</h2><span class="hint" data-count></span></div>
    <div class="toolbar" style="padding:14px 22px 18px"><div class="field">${icon('search')}<input data-q placeholder="Search ${esc(label.toLowerCase())}..."></div>
      <label style="display:inline-flex;gap:6px;align-items:center;margin:0"><input type="checkbox" data-all> All fields</label>
      <button class="sm" data-load>Reload</button><button class="sm" data-csv>Download CSV</button></div>
    <div class="tablewrap"><table><thead></thead><tbody></tbody></table></div>
    <div data-detail hidden style="padding:22px;border-top:1px solid var(--line)"><div class="row"><h2 data-dname style="margin:0"></h2><button class="sm" data-close>Close</button></div><dl data-dl style="margin-top:12px"></dl></div>`;
  page.append(el);
  const q = s => el.querySelector(s);
  let rows = [];

  const keysOf = list => [...new Set(list.flatMap(r => Object.keys(r)))];
  function columns(list) {
    const keys = keysOf(list).filter(k => !SKIP.test(k) && list.some(r => r[k] !== '' && r[k] != null && typeof r[k] !== 'object'));
    if (q('[data-all]').checked) return keys;
    return [...keys.filter(k => PRIORITY.test(k)), ...keys.filter(k => !PRIORITY.test(k))].slice(0, 7);
  }
  function render() {
    const term = q('[data-q]').value.trim().toLowerCase();
    const list = rows.filter(r => !term || Object.values(r).some(v => cell(v).toLowerCase().includes(term)));
    const cols = columns(rows);
    q('[data-count]').textContent = list.length + ' of ' + rows.length + ' records';
    q('thead').innerHTML = '<tr>' + cols.map(c => `<th>${esc(c.replace(/_/g, ' '))}</th>`).join('') + '</tr>';
    const tb = q('tbody'); tb.replaceChildren();
    if (!list.length) { const c = tb.insertRow().insertCell(); c.colSpan = Math.max(1, cols.length); c.innerHTML = `<div class="empty"><div class="ico">${icon('inbox')}</div><b>No ${esc(label.toLowerCase())} yet</b><p>Nothing has been recorded in Zoho People for this form.</p></div>`; return; }
    for (const r of list) {
      const tr = tb.insertRow(); tr.className = 'click';
      for (const c of cols) { const td = tr.insertCell(); if (/status/i.test(c)) td.innerHTML = statusPill(cell(r[c])); else td.textContent = cell(r[c]); }
      tr.addEventListener('click', () => {
        q('[data-dname]').textContent = label;
        q('[data-dl]').innerHTML = Object.entries(r).filter(([k, v]) => k !== 'tabularSections' && v !== '' && v != null)
          .map(([k, v]) => `<dt>${esc(k)}</dt><dd>${esc(cell(v))}</dd>`).join('');
        q('[data-detail]').hidden = false;
      });
    }
  }
  async function load() {
    q('[data-count]').textContent = 'Loading…';
    try { rows = normalizeEmployees(await api('GET', DATA + '/forms/' + form + '/records')); }
    catch (e) { rows = []; q('[data-count]').textContent = ''; banner('err', label + ': ' + e.message); return; }
    render();
  }
  q('[data-load]').addEventListener('click', e => run(e.target, load));
  q('[data-q]').addEventListener('input', render);
  q('[data-all]').addEventListener('change', render);
  q('[data-close]').addEventListener('click', () => { q('[data-detail]').hidden = true; });
  q('[data-csv]').addEventListener('click', () => {
    const keys = keysOf(rows), qt = v => '"' + cell(v).replace(/"/g, '""') + '"';
    const csv = [keys.map(qt).join(',')].concat(rows.map(r => keys.map(k => qt(r[k])).join(','))).join('\n');
    const a = Object.assign(document.createElement('a'), { href: URL.createObjectURL(new Blob([csv], { type: 'text/csv' })), download: form + '.csv' });
    a.click(); URL.revokeObjectURL(a.href);
  });
  load();
}

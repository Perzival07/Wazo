// Shared bits for every page: nav, dev identity, API helper, banner.
// Theme: saved choice wins, otherwise follow the system. Applied immediately to avoid a flash.
const THEME_KEY = 'zp_theme';
const systemDark = () => matchMedia('(prefers-color-scheme: dark)').matches;
function currentTheme() { let t = ''; try { t = localStorage.getItem(THEME_KEY) || ''; } catch { /* ignore */ } return t || (systemDark() ? 'dark' : 'light'); }
{ const t = new URLSearchParams(location.search).get('theme');   // ?theme=light|dark picks and remembers a theme
  if (t === 'light' || t === 'dark') { try { localStorage.setItem(THEME_KEY, t); } catch { /* ignore */ } } }
document.documentElement.dataset.theme = currentTheme();
function toggleTheme() {
  const next = currentTheme() === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = next;
  try { localStorage.setItem(THEME_KEY, next); } catch { /* ignore */ }
  const b = document.getElementById('theme'); if (b) b.innerHTML = themeIcon();
}
const themeIcon = () => currentTheme() === 'dark' ? icon('sun') : icon('moon');

const API = '/api/integrations/zoho-people';
const DATA = '/api/zoho-people';
const $ = id => document.getElementById(id);

const store = {
  get(k) { try { return localStorage.getItem(k) || ''; } catch { return ''; } },
  set(k, v) { try { localStorage.setItem(k, v); } catch { /* storage unavailable */ } },
};

// Line icons (24px grid, stroke only).
const ICONS = {
  home: '<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.5-3.5 3.200-5.500 6.500-5.500s6 2 6.500 5.500M16 4.600a3.500 3.500 0 0 1 0 6.800M18 14.800c2 .7 3.200 2.400 3.500 5.200"/>',
  grid: '<rect x="3" y="3" width="7.500" height="7.500" rx="2"/><rect x="13.500" y="3" width="7.500" height="7.500" rx="2"/><rect x="3" y="13.500" width="7.500" height="7.500" rx="2"/><rect x="13.500" y="13.500" width="7.500" height="7.500" rx="2"/>',
  calendar: '<rect x="3" y="4.500" width="18" height="16" rx="3"/><path d="M8 2.500v4M16 2.500v4M3 10h18M9 15l2 2 4-4"/>',
  plug: '<path d="M9 2v5M15 2v5M6 7h12v3a6 6 0 0 1-12 0zM12 16v6"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c.7-4 4-6 8-6s7.300 2 8 6"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.500-3.500"/>',
  chevron: '<path d="m9 6 6 6-6 6"/>',
  back: '<path d="m15 6-6 6 6 6"/>',
  idcard: '<rect x="3" y="5" width="18" height="14" rx="3"/><circle cx="9" cy="11" r="2"/><path d="M5.500 16.500c.5-1.700 1.700-2.500 3.500-2.500s3 .8 3.500 2.500M14.500 10h3.500M14.500 14h2.500"/>',
  building: '<path d="M5 21V4a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v17M15 9h3a1 1 0 0 1 1 1v11M3 21h18M9 7h2M9 11h2M9 15h2"/>',
  umbrella: '<path d="M3 12a9 9 0 0 1 18 0zM12 12v6.500a2 2 0 0 0 4 0"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  timer: '<circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.500 1.500M9.500 2h5"/>',
  trophy: '<path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0zM17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3"/>',
  folder: '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
  heart: '<path d="M12 20s-7-4.400-7-10a4 4 0 0 1 7-2.600A4 4 0 0 1 19 10c0 5.600-7 10-7 10z"/>',
  file: '<path d="M6 3h8l4 4v14H6zM14 3v4h4M9 12h6M9 16h6"/>',
  send: '<path d="M21 3 10 14M21 3l-7 18-4-7-7-4z"/>',
  check: '<rect x="4" y="4" width="16" height="16" rx="3"/><path d="m8.500 12 2.500 2.500 4.500-5"/>',
  logout: '<path d="M14 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M10 8l-4 4 4 4M6 12h10"/>',
  target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1.500"/>',
  filecheck: '<path d="M6 3h8l4 4v14H6zM14 3v4h4M9 14l2 2 4-4"/>',
  database: '<ellipse cx="12" cy="6" rx="8" ry="3"/><path d="M4 6v6c0 1.700 3.600 3 8 3s8-1.300 8-3V6M4 12v6c0 1.700 3.600 3 8 3s8-1.300 8-3v-6"/>',
  shield: '<path d="M12 3 5 6v5c0 4.500 3 8 7 10 4-2 7-5.500 7-10V6z"/><path d="m9 12 2 2 4-4"/>',
  doc: '<path d="M6 3h9l3 3v15H6zM9 11h6M9 15h6"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.900 4.900l1.400 1.400M17.700 17.700l1.400 1.400M2 12h2M20 12h2M4.900 19.100l1.400-1.400M17.700 6.300l1.400-1.400"/>',
  moon: '<path d="M20 14.500A8 8 0 0 1 9.500 4a8 8 0 1 0 10.500 10.500z"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  inbox: '<path d="M3 13h5l1 3h6l1-3h5M5 5h14l2 8v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-6z"/>',
};
const icon = (n, cls = '') => `<svg class="i ${cls}" viewBox="0 0 24 24" aria-hidden="true">${ICONS[n] || ''}</svg>`;

const PAGES = [['index.html', 'Home', 'home'], ['employees.html', 'Employees', 'users'], ['services.html', 'Services', 'grid'],
               ['attendance.html', 'Attendance', 'calendar'], ['settings.html', 'Settings', 'plug']];

const hue = s => [...String(s)].reduce((a, c) => (a * 31 + c.charCodeAt(0)) % 360, 7);
const initials = n => (n || '?').split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('');
const avatar = (name, cls = '') => `<span class="avatar ${cls}" style="--h:${hue(name)}">${esc(initials(name))}</span>`;

/** Page chrome. opts.actions = html shown at the right of the title; opts.bare = skip the title block. */
function layout(title, subtitle, opts = {}) {
  let here = location.pathname.split('/').pop() || 'index.html';
  if (here.startsWith('svc-') || here === 'forms.html') here = 'services.html';
  if (here === 'profile.html') here = 'settings.html';
  const email = store.get('zp_email');
  const meName = email ? email.split('@')[0].replace(/[._]/g, ' ') : 'Me';
  document.body.insertAdjacentHTML('afterbegin', `
    <header class="top"><div class="in">
      <a href="index.html" class="logo" aria-label="Home">W</a>
      <form class="search" id="gsearch" role="search">${icon('search')}<input id="gq" placeholder="Search people..." autocomplete="off"></form>
      <nav>${PAGES.map(([h, n, ic]) => `<a href="${h}"${h === here ? ' aria-current="page"' : ''}>${icon(ic)}<span>${n}</span></a>`).join('')}</nav>
      <button class="theme" id="theme" title="Switch light / dark" aria-label="Switch light or dark theme">${themeIcon()}</button>
      <a class="me" href="profile.html">${avatar(meName)}<span>Me</span></a>
    </div></header>
    <main>
      ${opts.bare ? '' : `<div class="pagehead"><div><h1>${title}</h1><p class="sub">${subtitle || ''}</p></div>${opts.actions || ''}</div>`}
      <div id="banner" class="banner" hidden role="status"></div>
      <div id="page"></div>
    </main>`);
  $('theme').addEventListener('click', toggleTheme);
  $('gsearch').addEventListener('submit', e => { e.preventDefault(); location.href = 'employees.html?q=' + encodeURIComponent($('gq').value.trim()); });
  if (!store.get('zp_email') && !location.pathname.endsWith('settings.html'))
    banner('err', 'Set your dev identity first: open Settings → Developer.');
  return $('page');
}

function headers(json) {
  const h = { 'X-Tenant-Id': store.get('zp_tenant') || 't1', 'X-User-Id': store.get('zp_user') || 'u1', 'X-User-Email': store.get('zp_email') };
  if (json) h['Content-Type'] = 'application/json';
  return h;
}

async function api(method, url, body) {
  const res = await fetch(url, { method, credentials: 'include', headers: headers(body !== undefined),
    body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    const err = new Error((data && data.message) || (typeof data === 'string' && data) || 'HTTP ' + res.status);
    err.data = data; err.status = res.status;
    throw err;
  }
  return data;
}

function banner(kind, msg) {
  const b = $('banner');
  b.className = 'banner ' + kind; b.textContent = msg; b.hidden = !msg;
}

async function run(btn, fn) {
  btn.disabled = true;
  try { await fn(); } catch (e) { banner('err', e.message); } finally { btn.disabled = false; }
}

const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const cell = v => v == null ? '' : typeof v === 'object' ? JSON.stringify(v) : String(v);

const RID = Symbol('recordId');   // Zoho record id, kept off the record's visible fields
// Zoho returns {recordId: [ {fields} ]} per employee.
function normalizeEmployees(data) {
  const rows = Array.isArray(data) ? data : (data && data.response && data.response.result) || [];
  return rows.map(r => {
    const v = Object.values(r || {});
    if (v.length === 1 && Array.isArray(v[0])) { const rec = v[0][0]; if (rec) rec[RID] = Object.keys(r)[0]; return rec; }
    return r;
  }).filter(r => r && typeof r === 'object');
}
function pick(rec, names) {
  const norm = k => k.toLowerCase().replace(/[^a-z0-9]/g, '');
  const map = Object.fromEntries(Object.keys(rec).map(k => [norm(k), rec[k]]));
  for (const n of names) if (map[norm(n)] != null && map[norm(n)] !== '') return String(map[norm(n)]);
  return '';
}
const emp = {
  id: r => pick(r, ['EmployeeID']), first: r => pick(r, ['FirstName']), last: r => pick(r, ['LastName']),
  name: r => (pick(r, ['FirstName']) + ' ' + pick(r, ['LastName'])).trim(),
  email: r => pick(r, ['EmailID', 'Email']), dept: r => pick(r, ['Department']),
  title: r => pick(r, ['Designation']), status: r => pick(r, ['Employeestatus']),
  photo: r => pick(r, ['Photo_downloadUrl', 'Photo']),
};

function statusPill(s) {
  if (!s) return '';
  const k = /approved|active|complete/i.test(s) ? 'ok' : /notice|probation|pending|progress/i.test(s) ? 'info' : /terminat|resign|inactive|exit|reject|cancel/i.test(s) ? 'err' : 'off';
  return `<span class="pill ${k}">${esc(s)}</span>`;
}

/** Left sidebar used by Settings and My profile. */
function settingsShell(active, title, subtitle) {
  const item = (href, ic, t, s, key, ext) => `<a href="${href}" class="${key === active ? 'on' : ''}"${ext ? ' target="_blank" rel="noopener"' : ''}>
    <span class="ico-sq">${icon(ic)}</span><span style="flex:1"><b>${t}</b><small>${s}</small></span>${key === active ? icon('chevron') : ''}</a>`;
  const page = layout('', '', { bare: true });
  page.innerHTML = `<div class="settings"><aside class="side">
      <h6>Account</h6>${item('profile.html', 'user', 'My profile', 'Your Zoho People record', 'profile')}
      <h6>Connected services</h6>${item('settings.html', 'plug', 'Integrations', 'Zoho People', 'integrations')}
      <h6>Developer</h6>${item('settings.html#dev', 'shield', 'Dev identity', 'Local testing headers', 'dev')}
      ${item('/swagger-ui.html', 'doc', 'API docs', 'Swagger UI', 'api', true)}
    </aside><section id="content"><div class="eyebrow">${icon('shield')} Account</div><h1>${title}</h1><p class="sub" style="margin-bottom:26px">${subtitle}</p><div id="inner"></div></section></div>`;
  return $('inner');
}

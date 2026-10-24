import { useEffect, useMemo, useState } from 'react';
import {
  Activity, ChevronDown, Clock3, Download, FileCheck2, FileText, Filter,
  FolderOpen, LockKeyhole, LogOut, MoreHorizontal, Plus, RefreshCw, Search,
  Settings, ShieldAlert, ShieldCheck, Upload, Users, X,
} from 'lucide-react';
import { request } from './api/client.js';

export default function App() {
  const [auth, setAuth] = useState(() => JSON.parse(sessionStorage.getItem('tv-auth') || 'null'));
  const [me, setMe] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [events, setEvents] = useState([]);
  const [tab, setTab] = useState('Documents');
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All categories');
  const [modal, setModal] = useState('');
  const [permissions, setPermissions] = useState([]);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const flash = (message) => { setToast(message); setTimeout(() => setToast(''), 2600); };
  async function refresh(credentials = auth) {
    if (!credentials) return;
    try {
      const user = await request('/me', {}, credentials);
      setMe(user);
      const docs = await request(user.role === 'ADMIN' && tab === 'Activity' ? '/admin/documents' : '/documents', {}, credentials);
      setDocuments(docs);
      if (user.role === 'ADMIN') setEvents(await request('/admin/audit', {}, credentials));
      setError('');
    } catch (e) { setError(e.message); }
  }
  useEffect(() => { if (auth) refresh(auth); }, [auth, tab]);
  useEffect(() => {
    if (!auth || !modal.startsWith('permissions:')) return;
    request(`/documents/${modal.split(':')[1]}/permissions`, {}, auth).then(setPermissions).catch((e) => setError(e.message));
  }, [auth, modal]);

  const visible = useMemo(() => documents.filter((doc) =>
    (doc.name.toLowerCase().includes(query.toLowerCase()) || doc.owner.toLowerCase().includes(query.toLowerCase())) &&
    (category === 'All categories' || doc.category === category)), [documents, query, category]);

  async function login(event) {
    event.preventDefault();
    const credentials = { username: event.target.username.value, password: event.target.password.value };
    try {
      const user = await request('/me', {}, credentials);
      sessionStorage.setItem('tv-auth', JSON.stringify(credentials));
      setAuth(credentials); setMe(user); setTab(user.role === 'ADMIN' ? 'Activity' : 'Documents');
      flash(`Welcome back, ${user.username}`);
    } catch { setError('We could not sign you in. Check the demo credentials and try again.'); }
  }
  function logout() {
    sessionStorage.removeItem('tv-auth'); setAuth(null); setMe(null); setDocuments([]); setEvents([]); setTab('Documents');
  }
  async function upload(event) {
    event.preventDefault();
    const form = new FormData(); form.append('file', event.target.file.files[0]);
    try {
      await request(`/documents?category=${encodeURIComponent(event.target.category.value || 'General')}`, { method: 'POST', body: form }, auth);
      setModal(''); await refresh(); flash('Document safely added');
    } catch (e) { setError(e.message); }
  }
  async function download(doc) {
    try {
      const response = await request(`/documents/${doc.id}/download`, {}, auth);
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement('a'); link.href = url; link.download = doc.name; link.click(); URL.revokeObjectURL(url);
      flash('Secure download started');
    } catch (e) { setError(e.message); }
  }
  async function grant(event) {
    event.preventDefault();
    try {
      await request(`/documents/${event.target.doc.value}/permissions`, {
        method: 'PUT', body: JSON.stringify({ username: event.target.username.value, read: true, edit: event.target.edit.checked }),
      }, auth);
      setModal(''); await refresh(); flash(`Access granted to ${event.target.username.value}`);
    } catch (e) { setError(e.message); }
  }
  async function revoke(username) {
    const id = modal.split(':')[1];
    try {
      await request(`/documents/${id}/permissions/${encodeURIComponent(username)}`, { method: 'DELETE' }, auth);
      setPermissions((items) => items.filter((item) => item.username !== username));
      flash(`Access revoked for ${username}`);
    } catch (e) { setError(e.message); }
  }
  async function renameDocument(event) {
    event.preventDefault();
    const id = modal.split(':')[1];
    try {
      await request(`/documents/${id}`, { method: 'PUT', body: JSON.stringify({ name: event.target.name.value }) }, auth);
      setModal(''); await refresh(); flash('Document name updated');
    } catch (e) { setError(e.message); }
  }
  async function deleteDocument() {
    const id = modal.split(':')[1];
    try {
      await request(`/documents/${id}`, { method: 'DELETE' }, auth);
      setModal(''); await refresh(); flash('Document deleted');
    } catch (e) { setError(e.message); }
  }

  if (!auth) return <Login onLogin={login} error={error} />;

  const admin = me?.role === 'ADMIN';
  const isDocuments = tab === 'Documents' || tab === 'Shared with me';
  return <div className="app-shell">
    <Sidebar me={me} tab={tab} setTab={setTab} logout={logout} />
    <main className="main">
      <header className="topbar"><div className="crumb">Workspace <span>/</span> <b>{tab}</b></div>
        <div className="top-right"><div className="protected"><span /> <LockKeyhole size={13} /> Protected session</div>
          <div className="avatar top-avatar">{me?.username?.[0]?.toUpperCase()}</div></div>
      </header>
      <section className="page">
        <div className="page-head"><div><div className="eyebrow">{tab === 'Activity' ? 'OVERSIGHT' : 'YOUR SECURE SPACE'}</div>
          <h1>{tab === 'Activity' ? 'Activity trail' : tab === 'Shared with me' ? 'Shared with me' : tab === 'Settings' ? 'Workspace settings' : 'Documents'}</h1>
          <p>{tab === 'Activity' ? 'A transparent record of access and changes across your workspace.' : 'Keep important files close, and decide exactly who can access them.'}</p>
        </div>{me?.role !== 'VIEWER' && tab === 'Documents' && <button className="primary" onClick={() => setModal('upload')}><Upload size={16} /> Upload document</button>}</div>
        {error && <div className="error-banner"><ShieldAlert size={17} />{error}<button onClick={() => setError('')}><X size={15} /></button></div>}
        {isDocuments && <>
          <div className="stat-row">
            <Stat icon={<FolderOpen size={18} />} tone="blue" label="ACCESSIBLE FILES" value={visible.length} suffix="documents" />
            <Stat icon={<ShieldCheck size={18} />} tone="green" label="ACCESS MODEL" value="Named people" suffix="only" />
            <Stat icon={<Clock3 size={18} />} tone="violet" label="YOUR ROLE" value={me?.role} suffix="account" />
          </div>
          <div className="toolbar"><div className="search"><Search size={17} /><input placeholder="Search documents or owners…" value={query} onChange={(e) => setQuery(e.target.value)} /><kbd>⌘ K</kbd></div>
            <div className="filter"><Filter size={15} /><select value={category} onChange={(e) => setCategory(e.target.value)}><option>All categories</option>{[...new Set(documents.map((d) => d.category))].map((x) => <option key={x}>{x}</option>)}</select></div>
            <button className="icon-button refresh" onClick={() => refresh()} title="Refresh"><RefreshCw size={16} /></button>
          </div>
          <DocumentTable docs={visible} total={documents.length} me={me} download={download} share={(id) => setModal(`permissions:${id}`)} manage={(id) => setModal(`manage:${id}`)} />
        </>}
        {tab === 'Activity' && <ActivityPanel events={events} />}
        {tab === 'Settings' && <SettingsPanel me={me} logout={logout} />}
      </section>
      <footer className="footer"><span>© 2026 TrustVault</span><span><ShieldCheck size={13} /> Every document. Every access. Accounted for.</span><span>SECURE WORKSPACE</span></footer>
    </main>
    {toast && <div className="toast"><ShieldCheck size={17} />{toast}</div>}
    {modal && <Dialog modal={modal} docs={documents} permissions={permissions} revoke={revoke} renameDocument={renameDocument} deleteDocument={deleteDocument} close={() => setModal('')} upload={upload} grant={grant} />}
  </div>;
}

function Login({ onLogin, error }) {
  return <main className="login"><div className="login-card"><Brand /><div className="login-mark"><LockKeyhole size={25} /></div>
    <h1>Your documents,<br />under your control.</h1><p className="muted">A private workspace with clear access and a record of every touch.</p>
    <form onSubmit={onLogin}><label>Username<input name="username" placeholder="Enter your username" required autoComplete="username" /></label>
      <label>Password<input name="password" type="password" placeholder="Enter your password" required autoComplete="current-password" /></label>
      {error && <p className="error">{error}</p>}<button className="primary wide">Sign in securely <span>→</span></button></form>
    <div className="demo-hint"><ShieldCheck size={14} /><span>Demo accounts available in the README</span></div>
  </div><div className="login-foot">ENCRYPTED WORKSPACE <i /> PRIVATE BY DESIGN <i /> AUDIT READY</div></main>;
}

function Brand() { return <div className="brand"><span className="brand-icon"><ShieldCheck /></span><span>trustvault<span className="brand-dot">.</span></span></div>; }
function Stat({ icon, tone, label, value, suffix }) { return <div className="stat-card"><div className={`stat-icon ${tone}`}>{icon}</div><div><small>{label}</small><b>{value} <span>{suffix}</span></b></div></div>; }

function Sidebar({ me, tab, setTab, logout }) {
  const links = me?.role === 'ADMIN' ? [['Activity', <Activity size={17} />], ['Documents', <FileText size={17} />]] : [['Documents', <FileText size={17} />], ['Shared with me', <Users size={17} />]];
  return <aside className="sidebar"><Brand /><div className="workspace-label">WORKSPACE</div>
    <div className="workspace"><div className="workspace-avatar">N</div><span><b>Northstar Studio</b><small>Private workspace</small></span><ChevronDown size={15} /></div>
    <div className="nav-label">MENU</div><nav>{links.map(([label, icon]) => <button key={label} className={`nav-item ${tab === label ? 'active' : ''}`} onClick={() => setTab(label)}>{icon}{label}{label === 'Activity' && <span className="nav-pip" />}</button>)}</nav>
    <div className="side-bottom"><div className="secure-card"><div className="secure-icon"><ShieldCheck size={16} /></div><b>Private by default</b><p>Only you and people you choose can open your files.</p><div className="secure-status"><i /> All systems protected</div></div>
      <button className={`nav-item ${tab === 'Settings' ? 'active' : ''}`} onClick={() => setTab('Settings')}><Settings size={17} />Settings</button>
      <div className="profile"><div className="avatar">{me?.username?.[0]?.toUpperCase()}</div><span><b>{me?.username}</b><small>{me?.role?.toLowerCase()}</small></span><button className="icon-button" title="Sign out" onClick={logout}><LogOut size={16} /></button></div>
    </div>
  </aside>;
}

function DocumentTable({ docs, total, me, download, share, manage }) {
  return <div className="table-card"><div className="table-head"><div>DOCUMENT <span>{docs.length}</span></div><div>CATEGORY</div><div>OWNER</div><div>ADDED</div><div>ACCESS</div><div /></div>
    {docs.length ? docs.map((doc) => <div className="doc-row" key={doc.id}>
      <div className="doc-title"><div className={`file-icon ${doc.contentType === 'application/pdf' ? 'pdf' : ''}`}><FileText size={17} /></div><span><b>{doc.name}</b><small>{(doc.size / 1024).toFixed(1)} KB</small></span></div>
      <div><span className="category-tag">{doc.category}</span></div><div className="owner-cell"><div className="mini-avatar">{doc.owner[0].toUpperCase()}</div>{doc.owner}</div>
      <div className="date-cell">{new Date(doc.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</div>
      <div><span className="access-tag"><LockKeyhole size={12} /> Restricted</span></div>
      <div className="row-actions"><button onClick={() => download(doc)} title="Download"><Download size={16} /></button>{me?.role === 'OWNER' && doc.owner === me.username && <><button onClick={() => share(doc.id)} title="Manage access"><Users size={16} /></button><button onClick={() => manage(doc.id)} title="Rename or delete"><MoreHorizontal size={17} /></button></>}</div>
    </div>) : <div className="empty"><div className="empty-icon"><FileCheck2 size={23} /></div><b>{total ? 'No matching documents' : 'Nothing here yet'}</b><span>{total ? 'Try another name or category.' : 'Upload a document to get your secure workspace started.'}</span></div>}
    <div className="table-foot"><span>Showing <b>{docs.length}</b> of {total} documents</span><span className="safe-note"><ShieldCheck size={14} /> Access checked on every request</span></div>
  </div>;
}

function ActivityPanel({ events }) {
  return <div className="activity-card"><div className="activity-header"><div><h2>Recent events</h2><p>Recorded with a linked integrity hash.</p></div><span className="live"><i /> LIVE RECORD</span></div>
    {events.length ? events.map((event) => <div className="event-row" key={event.eventHash}>
      <div className={`event-icon ${event.action === 'ACCESS_DENIED' ? 'denied' : ''}`}>{event.action === 'DOWNLOAD' ? <Download size={15} /> : event.action === 'UPLOAD' ? <Upload size={15} /> : event.action === 'ACCESS_DENIED' ? <ShieldAlert size={15} /> : <Activity size={15} />}</div>
      <div className="event-text"><b>{event.action.replaceAll('_', ' ').toLowerCase()}</b><span>{event.actor}{event.details ? ` · ${event.details}` : ''}</span></div>
      <time>{new Date(event.occurredAt).toLocaleString()}</time><code title={event.eventHash}>{event.eventHash.slice(0, 10)}…</code>
    </div>) : <div className="empty"><div className="empty-icon"><Activity size={23} /></div><b>No activity yet</b><span>Document actions will appear here as they happen.</span></div>}
  </div>;
}

function SettingsPanel({ me, logout }) {
  return <div className="activity-card settings-card"><div className="settings-icon"><ShieldCheck size={20} /></div><h2>Security at a glance</h2>
    <p>Your workspace checks permissions at the server for every document request. Access grants are attached to individual files. When a grant is removed, the next request is denied.</p>
    <div className="setting-line"><span><b>Signed in as</b><small>{me?.username}</small></span><span className="access-tag">{me?.role}</span></div>
    <div className="setting-line"><span><b>Session protection</b><small>HTTP Basic over your active connection</small></span><span className="live"><i /> ACTIVE</span></div>
    <button className="secondary" onClick={logout}><LogOut size={15} /> Sign out</button>
  </div>;
}

function Dialog({ modal, docs, permissions, revoke, renameDocument, deleteDocument, close, upload, grant }) {
  const isUpload = modal === 'upload';
  const isPermissions = modal.startsWith('permissions:');
  const isManage = modal.startsWith('manage:');
  const document = isManage ? docs.find((item) => item.id === modal.split(':')[1]) : null;
  return <div className="overlay" onClick={close}><div className="modal" onClick={(e) => e.stopPropagation()}>
    <button className="close" onClick={close}><X size={18} /></button><div className="modal-icon">{isUpload ? <Upload size={19} /> : <Users size={19} />}</div>
    <h2>{isUpload ? 'Add a document' : isPermissions ? 'Manage access' : isManage ? 'Manage document' : 'Grant access'}</h2>
    <p>{isUpload ? 'Your file is private by default. You can share it with named people later.' : isPermissions ? 'Access changes take effect on the next request.' : isManage ? 'Update its name or remove this file and its grants.' : 'Choose the person who should have access to this document.'}</p>
    {isUpload ? <form onSubmit={upload}><label>File<input type="file" name="file" required accept=".pdf,.png,.jpg,.jpeg,.txt,.docx" /></label>
      <label>Category<input name="category" placeholder="e.g. Contracts, Finance" defaultValue="General" maxLength="80" required /></label><button className="primary wide">Upload securely <Upload size={16} /></button></form>
      : isManage ? <><form onSubmit={renameDocument}><label>Document name<input name="name" defaultValue={document?.name} maxLength="200" required /></label><button className="primary wide">Save name</button></form><button type="button" className="danger wide" onClick={deleteDocument}>Delete document and revoke access</button></>
      : isPermissions ? <><form onSubmit={grant}><input type="hidden" name="doc" value={modal.split(':')[1]} /><label>Username<input name="username" placeholder="e.g. bob" required /></label>
        <label className="checkbox"><input type="checkbox" name="edit" /> Allow editing</label><div className="grant-note"><Users size={14} /> Viewers always have read-only access. Grant only to people who need this file.</div>
        <button className="primary wide">Grant access <Users size={16} /></button></form><div className="permission-list"><b>Current access</b>{permissions.length ? permissions.map((item) => <div className="permission-item" key={item.username}><span><b>{item.username}</b><small>{item.edit ? 'Read and edit' : 'Read only'}</small></span><button type="button" className="secondary" onClick={() => revoke(item.username)}>Revoke</button></div>) : <span className="permission-empty">No one else has access yet.</span>}</div></> : <form onSubmit={grant}><input type="hidden" name="doc" value={modal.split(':')[1]} /><label>Username<input name="username" placeholder="e.g. bob" required /></label><label className="checkbox"><input type="checkbox" name="edit" /> Allow editing</label><button className="primary wide">Grant access <Users size={16} /></button></form>}
  </div></div>;
}

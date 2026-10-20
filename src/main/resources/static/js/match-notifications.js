(() => {
  const link = document.createElement('a');
  link.href = '/customer/community/mine'; link.textContent = 'Kèo của tôi';
  Object.assign(link.style,{position:'fixed',bottom:'22px',left:'22px',zIndex:'900',padding:'12px 18px',background:'#047857',color:'white',borderRadius:'14px',boxShadow:'0 4px 20px #0002'});
  document.body.append(link);
  async function refresh() {
    try {
      const res = await fetch('/customer/api/match-notifications'); if (!res.ok) return;
      const notices = await res.json(); const unread = notices.filter(n => !n.read);
      link.textContent = unread.length ? `Kèo của tôi • ${unread.length} thông báo mới` : 'Kèo của tôi';
      if (location.pathname.endsWith('/mine')) {
        let panel = document.getElementById('matchNotices');
        if (!panel) { panel = document.createElement('section'); panel.id = 'matchNotices'; document.querySelector('main').prepend(panel); }
        panel.replaceChildren();
        for (const n of unread) {
          const row = document.createElement('p'), title = document.createElement('strong'), message = document.createElement('span'), button = document.createElement('button');
          title.textContent = n.title + ': '; message.textContent = n.message; button.textContent = 'Đã đọc'; button.className = 'btn';
          button.onclick = async () => { const r = await fetch('/customer/api/match-notifications/' + encodeURIComponent(n.id) + '/read',{method:'POST'}); if(r.ok) refresh(); };
          row.append(title,message,' ',button); panel.append(row);
        }
      }
    } catch (_) { /* Existing feed remains available while offline. */ }
  }
  refresh(); setInterval(refresh,30000);
})();
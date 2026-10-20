(() => {
  if (window.sporthubDialogsInstalled) return;
  window.sporthubDialogsInstalled = true;
  window.appNotice = message => {
    const notice = document.createElement('div'); notice.setAttribute('role','status');
    notice.className = 'app-notice'; const text = document.createElement('span'); text.textContent = message;
    const close = document.createElement('button'); close.type = 'button'; close.textContent = 'Đóng';
    close.onclick = () => notice.remove(); notice.append(text,close); document.body.append(notice);
    setTimeout(() => notice.remove(),7000);
  };
  let active = false;
  window.appConfirm = (message, title = 'Xác nhận thao tác') => new Promise(resolve => {
    if (active) { resolve(false); return; } active = true;
    const previous = document.activeElement, dialog = document.createElement('dialog');
    dialog.className = 'app-confirm';
    const heading = document.createElement('h3'); heading.id = 'app-confirm-title'; heading.textContent = title;
    dialog.setAttribute('aria-labelledby',heading.id);
    const text = document.createElement('p'); text.textContent = message;
    const actions = document.createElement('div'), cancel = document.createElement('button'), yes = document.createElement('button');
    actions.className = 'app-confirm-actions'; cancel.textContent = 'Hủy'; yes.textContent = 'Xác nhận';
    cancel.className = 'btn'; yes.className = 'btn primary'; cancel.type = yes.type = 'button';
    let done = false;
    function finish(value) { if(done)return; done=true; dialog.close(); dialog.remove(); active=false; previous?.focus(); resolve(value); }
    cancel.onclick = () => finish(false); yes.onclick = () => finish(true);
    dialog.addEventListener('cancel', e => { e.preventDefault(); finish(false); });
    dialog.addEventListener('click', e => { if(e.target === dialog) { const r=dialog.getBoundingClientRect(); if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom) finish(false); } });
    actions.append(cancel,yes); dialog.append(heading,text,actions); document.body.append(dialog); dialog.showModal(); cancel.focus();
  });
  document.addEventListener('submit', async e => {
    const form=e.target; if(!form.matches('form[data-confirm]') || form.dataset.confirmed === 'yes') return;
    e.preventDefault(); const button=e.submitter;
    const item=form.dataset.confirmItem || '';
    if(await appConfirm(form.dataset.confirm + (item ? ' (' + item + ')' : ''))) {
      form.dataset.confirmed='yes'; form.requestSubmit(button); delete form.dataset.confirmed;
    }
  });
  const style=document.createElement('style'); style.textContent='.app-notice{position:fixed;right:20px;top:90px;z-index:10000;display:flex;align-items:center;gap:16px;max-width:440px;padding:18px;background:#fff;border:1px solid #bddbd0;border-radius:12px;box-shadow:0 10px 40px #0002;color:#164e3b}.app-notice button{border:0;background:#e8f5ef;border-radius:8px;padding:8px;cursor:pointer}.app-confirm{position:fixed;inset:0;margin:auto;border:0;border-radius:18px;padding:28px;max-width:440px;width:calc(100% - 40px);box-shadow:0 20px 80px #0004;color:#18352d}.app-confirm::backdrop{background:#10231f88}.app-confirm p{margin:20px 0;line-height:1.6}.app-confirm-actions{display:flex;justify-content:flex-end;gap:10px}.app-confirm button{padding:10px 20px;border-radius:10px;border:1px solid #cbd5e1;cursor:pointer}.app-confirm .primary{background:#047857;color:white}';document.head.append(style);
})();

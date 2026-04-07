// Sidebar mobile toggle
document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.querySelector('.menu-toggle');
  const sidebar = document.querySelector('.sidebar');
  const overlay = document.querySelector('.sidebar-overlay');

  if (toggle) {
    toggle.addEventListener('click', () => {
      sidebar.classList.toggle('open');
      overlay.classList.toggle('open');
    });
  }
  if (overlay) {
    overlay.addEventListener('click', () => {
      sidebar.classList.remove('open');
      overlay.classList.remove('open');
    });
  }

  // Tabs
  document.querySelectorAll('.tabs').forEach(tabGroup => {
    const btns = tabGroup.querySelectorAll('.tab-btn');
    const container = tabGroup.parentElement;
    btns.forEach(btn => {
      btn.addEventListener('click', () => {
        const target = btn.dataset.tab;
        btns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        container.querySelectorAll('.tab-panel').forEach(p => {
          p.classList.toggle('active', p.id === target);
        });
      });
    });
  });

  // Install tabs
  document.querySelectorAll('.install-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      const group = tab.closest('.install-group');
      group.querySelectorAll('.install-tab').forEach(t => t.classList.remove('active'));
      group.querySelectorAll('.install-panel').forEach(p => p.classList.remove('active'));
      tab.classList.add('active');
      group.querySelector('#' + tab.dataset.panel).classList.add('active');
    });
  });
});

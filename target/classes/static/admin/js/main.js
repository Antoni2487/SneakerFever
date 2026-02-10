document.addEventListener('DOMContentLoaded', function () {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebar-overlay');
    const openBtn = document.getElementById('open-sidebar');
    const closeBtn = document.getElementById('close-sidebar');

    if (!sidebar || !overlay) return;

    const openSidebar = () => {
        sidebar.classList.add('active');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
        if (openBtn) openBtn.style.display = 'none';
    };

    const closeSidebar = () => {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
        document.body.style.overflow = '';
        if (openBtn) openBtn.style.display = 'flex';
    };

    openBtn?.addEventListener('click', openSidebar);
    closeBtn?.addEventListener('click', closeSidebar);
    overlay.addEventListener('click', closeSidebar);

    document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && sidebar.classList.contains('active')) closeSidebar();
    });

    window.addEventListener('resize', () => {
        if (window.innerWidth >= 768) closeSidebar();
    });

    sidebar.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', () => {
            if (window.innerWidth < 768) setTimeout(closeSidebar, 100);
        });
    });

    // ✅ LÓGICA MEJORADA para marcar enlace activo
    const normalize = (p) => {
        if (!p) return '/';
        p = p.split('?')[0].split('#')[0];
        p = p.replace(/\/+$/, '');
        return p === '' ? '/' : p;
    };

    const currentPath = normalize(window.location.pathname);

    // Primero remover todas las clases 'active'
    sidebar.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });

    // Buscar la mejor coincidencia (la más específica)
    let bestMatch = null;
    let bestMatchLength = 0;

    sidebar.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('javascript:')) return;

        const linkPath = normalize(new URL(href, window.location.origin).pathname);

        // Coincidencia exacta o por prefijo
        if (currentPath === linkPath) {
            // Coincidencia exacta tiene prioridad
            if (!bestMatch || linkPath.length > bestMatchLength) {
                bestMatch = link;
                bestMatchLength = linkPath.length;
            }
        } else if (linkPath !== '/' && currentPath.startsWith(linkPath + '/')) {
            // Coincidencia por prefijo
            if (linkPath.length > bestMatchLength) {
                bestMatch = link;
                bestMatchLength = linkPath.length;
            }
        }
    });

    // Marcar solo la mejor coincidencia
    if (bestMatch) {
        bestMatch.classList.add('active');
    }
});
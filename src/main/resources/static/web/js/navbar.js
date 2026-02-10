document.addEventListener('DOMContentLoaded', function() {
    // Elementos principales
    const hamburger = document.getElementById('open-navbar-mobile');
    const mobileMenu = document.getElementById('mobile-menu');
    const closeMenu = document.getElementById('close-menu');
    const overlay = document.getElementById('navbar-overlay');
    const navbar = document.getElementById('navbar');
    const searchToggle = document.getElementById('search-toggle');
    const searchForm = document.getElementById('search-form');

    // Mobile menu submenu elements
    const mobileNavLinks = document.querySelectorAll('.mobile-nav-link[data-target]');
    const submenuBacks = document.querySelectorAll('.submenu-back');

    // ============================================
    // SCROLL EFFECT - Navbar backdrop effect
    // ============================================
    let lastScroll = 0;
    window.addEventListener('scroll', function() {
        const currentScroll = window.pageYOffset;

        if (currentScroll > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }

        lastScroll = currentScroll;
    });

    // ============================================
    // MOBILE MENU - Open/Close
    // ============================================
    function openMobileMenu() {
        mobileMenu.classList.add('active');
        overlay.classList.add('active');
        hamburger.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeMobileMenu() {
        mobileMenu.classList.remove('active');
        overlay.classList.remove('active');
        hamburger.classList.remove('active');
        document.body.style.overflow = '';

        // Cerrar todos los submenús al cerrar el menú principal
        document.querySelectorAll('.mobile-submenu').forEach(submenu => {
            submenu.classList.remove('active');
        });
    }

    // Event listeners para abrir/cerrar menú móvil
    if (hamburger) {
        hamburger.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            if (mobileMenu.classList.contains('active')) {
                closeMobileMenu();
            } else {
                openMobileMenu();
            }
        });
    }

    if (closeMenu) {
        closeMenu.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            closeMobileMenu();
        });
    }

    if (overlay) {
        overlay.addEventListener('click', closeMobileMenu);
    }

    // Cerrar con tecla ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            closeMobileMenu();
            closeSearch();
        }
    });

    // ============================================
    // MOBILE SUBMENU - Navigation
    // ============================================
    mobileNavLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const targetId = this.getAttribute('data-target');
            const targetSubmenu = document.getElementById(targetId);

            if (targetSubmenu) {
                targetSubmenu.classList.add('active');
            }
        });
    });

    submenuBacks.forEach(back => {
        back.addEventListener('click', function() {
            this.closest('.mobile-submenu').classList.remove('active');
        });
    });

    // ============================================
    // SEARCH BAR - Toggle functionality
    // ============================================
    function openSearch() {
        searchForm.classList.add('active');
        searchForm.querySelector('input').focus();
    }

    function closeSearch() {
        searchForm.classList.remove('active');
    }

    if (searchToggle) {
        searchToggle.addEventListener('click', function(e) {
            e.stopPropagation();
            if (searchForm.classList.contains('active')) {
                closeSearch();
            } else {
                openSearch();
            }
        });
    }

    // Cerrar búsqueda al hacer clic fuera
    document.addEventListener('click', function(e) {
        if (!searchForm.contains(e.target) && !searchToggle.contains(e.target)) {
            closeSearch();
        }
    });

    // Prevenir que el clic en el formulario cierre la búsqueda
    if (searchForm) {
        searchForm.addEventListener('click', function(e) {
            e.stopPropagation();
        });

        // Handle search form submission
        searchForm.addEventListener('submit', function(e) {
            e.preventDefault();
            const searchValue = this.querySelector('input').value.trim();
            if (searchValue) {
                console.log('Buscando:', searchValue);
                // Aquí puedes agregar la lógica de búsqueda
                // window.location.href = `/buscar?q=${encodeURIComponent(searchValue)}`;
            }
        });
    }

    // ============================================
    // CART COUNT - Update functionality
    // ============================================
    function updateCartCount(count) {
        const cartBadge = document.getElementById('cart-count');
        const cartBadgeMobile = document.getElementById('cart-count-mobile');

        if (cartBadge) {
            cartBadge.textContent = count;
            cartBadge.style.display = count > 0 ? 'flex' : 'none';
        }

        if (cartBadgeMobile) {
            cartBadgeMobile.textContent = count;
            cartBadgeMobile.style.display = count > 0 ? 'flex' : 'none';
        }
    }

    // Ejemplo: actualizar el contador del carrito
    // Puedes llamar esta función desde tu lógica de carrito
    // updateCartCount(3);

    // Simular contador inicial (opcional - eliminar en producción)
    // Si tienes localStorage o una API, obtén el valor real aquí
    const storedCartCount = 0; // Reemplazar con: localStorage.getItem('cartCount') || 0
    updateCartCount(parseInt(storedCartCount));

    // ============================================
    // DROPDOWN MENUS - Close on click outside
    // ============================================
    const dropdowns = document.querySelectorAll('.nav-dropdown');

    dropdowns.forEach(dropdown => {
        dropdown.addEventListener('mouseenter', function() {
            // Cerrar otros dropdowns
            dropdowns.forEach(d => {
                if (d !== dropdown) {
                    d.classList.remove('active');
                }
            });
        });
    });

    // ============================================
    // ANIMATIONS - Smooth interactions
    // ============================================
    // Animación de entrada para los items del menú móvil
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry, index) => {
            if (entry.isIntersecting) {
                setTimeout(() => {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateX(0)';
                }, index * 50);
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.mobile-nav-item, .mobile-action-btn').forEach(item => {
        item.style.opacity = '0';
        item.style.transform = 'translateX(-20px)';
        item.style.transition = 'all 0.4s ease';
        observer.observe(item);
    });

    // ============================================
    // PREVENT BODY SCROLL when menu is open
    // ============================================
    function preventBodyScroll(prevent) {
        if (prevent) {
            document.body.style.overflow = 'hidden';
            document.body.style.paddingRight = getScrollbarWidth() + 'px';
        } else {
            document.body.style.overflow = '';
            document.body.style.paddingRight = '';
        }
    }

    function getScrollbarWidth() {
        return window.innerWidth - document.documentElement.clientWidth;
    }

    // ============================================
    // ACTIVE LINK HIGHLIGHTING (opcional)
    // ============================================
    // Resaltar el link activo según la URL actual
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-link, .mobile-nav-link').forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.style.color = 'var(--accent-color)';
        }
    });

    // ============================================
    // PERFORMANCE OPTIMIZATION
    // ============================================
    // Debounce para el scroll
    let scrollTimeout;
    window.addEventListener('scroll', function() {
        if (scrollTimeout) {
            window.cancelAnimationFrame(scrollTimeout);
        }
        scrollTimeout = window.requestAnimationFrame(function() {
            // El código de scroll ya está arriba
        });
    }, { passive: true });
    // NAVIGATION FILTERS - Handle catalog filtering
    document.querySelectorAll('.filter-nav').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            const categoria = this.dataset.category;
            const subcategoria = this.dataset.subcategory;
            const marca = this.dataset.brand;
            const sale = this.dataset.sale;
            
            // 🔍 DEBUG: Ver qué capturamos
            console.log('🔍 NAVBAR CLICK:', {
                categoria,
                subcategoria,
                marca,
                sale,
                elemento: this
            });
            
            // Construir URL con parámetros
            const params = [];
            
            if (categoria) params.push(`genero=${categoria.toUpperCase()}`);
            if (subcategoria) params.push(`subcategoria=${subcategoria}`);
            if (marca) params.push(`marca=${encodeURIComponent(marca)}`);
            if (sale) params.push(`sale=true`);
            
            const url = '/catalogo' + (params.length ? '?' + params.join('&') : '');
            
            // 🔍 DEBUG: Ver URL construida
            console.log('🔍 URL CONSTRUIDA:', url);
            
            // Cerrar menú móvil si está abierto
            if (mobileMenu && mobileMenu.classList.contains('active')) {
                closeMobileMenu();
            }
            
            // Redirigir
            window.location.href = url;
        });
    });

    // ============================================
    // UTILITY FUNCTIONS
    // ============================================
    // Función global para actualizar el carrito desde otros scripts
    window.updateNavbarCart = updateCartCount;

    // Función para cerrar todos los menús
    window.closeAllMenus = function() {
        closeMobileMenu();
        closeSearch();
    };

    console.log('✅ Dynamic Island Navbar initialized');
});

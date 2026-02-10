// ==================== VARIABLES GLOBALES ====================
let todosLosProductos = [];
let productosFiltrados = [];
let categorias = [];
let marcas = [];

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', async function() {
     await Promise.all([
        cargarCategorias(),
        cargarMarcas()
    ]);
    detectarFiltrosURL();
    configurarEventListeners();
    configurarNavbarFilters(); // 🆕 NUEVO
});

// ==================== CARGAR DATOS INICIALES ====================
async function cargarCategorias() {
    try {
        const response = await fetch('/categorias/api/listar');
        if (response.ok) {
            const data = await response.json();
            categorias = data.success ? data.data : data;
            llenarSelectCategorias();
        }
    } catch (error) {
        console.error('Error al cargar categorías:', error);
    }
}

async function cargarMarcas() {
    try {
        const response = await fetch('/marcas/api/listar');
        if (response.ok) {
            const data = await response.json();
            marcas = data.success ? data.data : data;
            llenarSelectMarcas();
        }
    } catch (error) {
        console.error('Error al cargar marcas:', error);
    }
}

function llenarSelectCategorias() {
    const select = document.getElementById('filtroCategoria');
    categorias.forEach(cat => {
        const option = document.createElement('option');
        option.value = cat.id;
        option.textContent = cat.nombre;
        select.appendChild(option);
    });
}

function llenarSelectMarcas() {
    const select = document.getElementById('filtroMarca');
    marcas.forEach(marca => {
        const option = document.createElement('option');
        option.value = marca.id;
        option.textContent = marca.nombre;
        select.appendChild(option);
    });
}

// ==================== 🆕 CONFIGURAR FILTROS DEL NAVBAR ====================
function configurarNavbarFilters() {
    document.querySelectorAll('.filter-nav').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            const categoria = this.dataset.category;      // "hombre" o "mujer"
            const subcategoria = this.dataset.subcategory; // "zapatillas", "ropa", "accesorios"
            const marca = this.dataset.brand;              // nombre de marca
            const sale = this.dataset.sale;                // "true"
            
            // Construir URL con parámetros correctos
            const params = [];
            
            if (categoria) params.push(`genero=${categoria.toUpperCase()}`);
            if (subcategoria) params.push(`subcategoria=${subcategoria}`);
            if (marca) params.push(`marca=${encodeURIComponent(marca)}`);
            if (sale) params.push(`sale=true`);
            
            const url = '/catalogo' + (params.length ? '?' + params.join('&') : '');
            
            // Redirigir
            window.location.href = url;
        });
    });
}

// ==================== DETECTAR FILTROS DE LA URL ====================
function detectarFiltrosURL() {
    const params = new URLSearchParams(window.location.search);

     console.log('🔍 URL PARAMS:', {
        genero: params.get('genero'),
        subcategoria: params.get('subcategoria'),
        marca: params.get('marca'),
        sale: params.get('sale'),
        urlCompleta: window.location.href
    });
    console.log('🔍 CATEGORIAS CARGADAS:', categorias);
    console.log('🔍 MARCAS CARGADAS:', marcas);
    
    // Resetear filtros
    document.getElementById('filtroGenero').value = '';
    document.getElementById('filtroCategoria').value = '';
    document.getElementById('filtroMarca').value = '';
    document.getElementById('filtroSale').checked = false;
    document.getElementById('filtroDestacados').checked = false;
    
    // 🔥 FILTRO POR GÉNERO
    const generoParam = params.get('genero');
    if (generoParam) {
        document.getElementById('filtroGenero').value = generoParam.toUpperCase();
        document.getElementById('catalogTitle').textContent = `Catálogo ${generoParam.toUpperCase()}`;
        actualizarBreadcrumb(generoParam.toUpperCase());
    }
    
    // 🔥 FILTRO POR SUBCATEGORÍA (nombre, no ID)
    const subcategoriaParam = params.get('subcategoria');
    if (subcategoriaParam) {
        const categoria = categorias.find(c => 
            c.nombre.toLowerCase() === subcategoriaParam.toLowerCase()
        );
        if (categoria) {
            document.getElementById('filtroCategoria').value = categoria.id;
            if (generoParam) {
                document.getElementById('catalogTitle').textContent = 
                    `${generoParam.toUpperCase()} - ${categoria.nombre}`;
                actualizarBreadcrumb(`${generoParam} / ${categoria.nombre}`);
            }
        }
    }
    
    // 🔥 FILTRO POR MARCA (nombre, no ID)
    const marcaParam = params.get('marca');
    if (marcaParam) {
        const marca = marcas.find(m => m.nombre === marcaParam);
        if (marca) {
            document.getElementById('filtroMarca').value = marca.id;
            document.getElementById('catalogTitle').textContent = marca.nombre;
            actualizarBreadcrumb(marca.nombre);
        }
    }
    
    // 🔥 FILTRO SALE
    const saleParam = params.get('sale');
    if (saleParam === 'true') {
        document.getElementById('filtroSale').checked = true;
        document.getElementById('catalogTitle').textContent = 'SALE - Ofertas';
        actualizarBreadcrumb('SALE');
    }
    
    // Cargar productos
    cargarProductos();
}

async function cargarProductos() {
    const container = document.getElementById('catalogoContainer');
    container.innerHTML = `
        <div class="col-12">
            <div class="loading-container">
                <div class="spinner-border text-dark" role="status">
                    <span class="visually-hidden">Cargando...</span>
                </div>
                <p class="mt-3">Cargando productos...</p>
            </div>
        </div>
    `;
    
    try {
        const params = new URLSearchParams(window.location.search);
        
        // 🔥 Siempre cargar TODOS los productos y filtrar localmente
        // Esto permite combinar múltiples filtros (género + categoría + marca)
        const endpoint = '/productos/api/listar';
        
        console.log('Cargando desde:', endpoint);
        const response = await fetch(endpoint);
        
        if (response.ok) {
            const result = await response.json();
            
            if (result.success && result.data) {
                todosLosProductos = result.data;
            } else if (Array.isArray(result)) {
                todosLosProductos = result;
            } else {
                todosLosProductos = [];
            }
            
            console.log('Productos cargados:', todosLosProductos.length);
            productosFiltrados = [...todosLosProductos];
            aplicarFiltrosLocales();
            renderizarProductos();
        } else {
            console.error('Error en respuesta:', response.status);
            mostrarError('Error al cargar los productos');
        }
    } catch (error) {
        console.error('Error en fetch:', error);
        mostrarError('Error al conectar con el servidor: ' + error.message);
    }
}

// ==================== APLICAR FILTROS LOCALES ====================
function aplicarFiltrosLocales() {
    productosFiltrados = [...todosLosProductos];
    console.log('🔍 FILTROS - Productos iniciales:', productosFiltrados.length);
    
    // Filtro de género
    const genero = document.getElementById('filtroGenero').value;
    if (genero) {
        productosFiltrados = productosFiltrados.filter(p => 
            p.genero && p.genero.toUpperCase() === genero.toUpperCase()
        );
         console.log('🔍 Después de filtro género:', productosFiltrados.length);
    }
    
    // Filtro de categoría (adaptado para nombre y ID)
    const categoriaId = document.getElementById('filtroCategoria').value;
    const params = new URLSearchParams(window.location.search);
    const subcategoriaParam = params.get('subcategoria');

    if (categoriaId) {
        productosFiltrados = productosFiltrados.filter(p => 
            (p.categoryId && p.categoryId == categoriaId) || 
            (p.category && p.category.id == categoriaId)
        );
    } else if (subcategoriaParam) {
        // Filtrar por nombre de categoría desde URL
        productosFiltrados = productosFiltrados.filter(p => 
            (p.categoryNombre && p.categoryNombre.toLowerCase() === subcategoriaParam.toLowerCase()) ||
            (p.category && p.category.nombre.toLowerCase() === subcategoriaParam.toLowerCase())
        );
         console.log('🔍 Después de filtro categoría:', productosFiltrados.length);
    }
    
    // Filtro de marca (adaptado para DTO)
    const marcaId = document.getElementById('filtroMarca').value;
    if (marcaId) {
        productosFiltrados = productosFiltrados.filter(p => 
            (p.brandId && p.brandId == marcaId) || 
            (p.brand && p.brand.id == marcaId)
        );
        console.log('🔍 Después de filtro marca:', productosFiltrados.length);
    }
    
    // Filtro de precio
    const precioMin = parseFloat(document.getElementById('precioMin').value);
    const precioMax = parseFloat(document.getElementById('precioMax').value);
    if (!isNaN(precioMin)) {
        productosFiltrados = productosFiltrados.filter(p => p.precio >= precioMin);
    }
    if (!isNaN(precioMax)) {
        productosFiltrados = productosFiltrados.filter(p => p.precio <= precioMax);
    }
    
    // Filtro SALE
    if (document.getElementById('filtroSale').checked) {
        productosFiltrados = productosFiltrados.filter(p => p.descuento && p.descuento > 0);
    }
    
    // Filtro de stock
    if (document.getElementById('filtroStock').checked) {
        productosFiltrados = productosFiltrados.filter(p => p.stock > 0);
    }
    
    // Filtro destacados
    if (document.getElementById('filtroDestacados').checked) {
        productosFiltrados = productosFiltrados.filter(p => p.destacado === true);
    }
    
    console.log('🔍 FILTROS - Productos finales:', productosFiltrados.length);
}

// ==================== RENDERIZAR PRODUCTOS ====================
function renderizarProductos() {
    const container = document.getElementById('catalogoContainer');
    
    if (productosFiltrados.length === 0) {
        container.innerHTML = `
            <div class="col-12">
                <div class="empty-state">
                    <i class="fas fa-box-open"></i>
                    <h4>No se encontraron productos</h4>
                    <p>Intenta ajustar los filtros de búsqueda</p>
                </div>
            </div>
        `;
        actualizarContador(0);
        return;
    }
    
    container.innerHTML = '';
    
    productosFiltrados.forEach(producto => {
        const col = document.createElement('div');
        col.className = 'col-md-4 col-sm-6';
        col.innerHTML = crearCardProducto(producto);
        container.appendChild(col);
    });
    
    actualizarContador(productosFiltrados.length);
}

// ==================== CREAR CARD DE PRODUCTO ====================
function crearCardProducto(producto) {
    // Manejo de imagen
    const imagen = producto.imagen || (producto.imagenes && producto.imagenes.length > 0 ? producto.imagenes[0] : '/images/no-image.png');
    
    // Cálculo de precio original
    const precioOriginal = producto.descuento && producto.descuento > 0 
        ? (producto.precio / (1 - producto.descuento / 100)).toFixed(2)
        : null;
    
    // Estado de stock
    const stockClass = producto.stock === 0 ? 'stock-out' 
        : (producto.stockMinimo && producto.stock < producto.stockMinimo ? 'stock-low' : 'stock-available');
    const stockTexto = producto.stock === 0 ? 'Agotado' : `${producto.stock} disponibles`;
    
    // Nombre de categoría y marca (con fallback para DTO)
    const categoriaNombre = producto.categoryNombre || (producto.category ? producto.category.nombre : 'Sin categoría');
    const marcaNombre = producto.brandNombre || (producto.brand ? producto.brand.nombre : 'Sin marca');
    
    return `
        <div class="product-card">
            <div class="product-image-wrapper">
                <img src="${imagen}" 
                     alt="${producto.nombre}" 
                     class="product-image"
                     onerror="this.src='/images/no-image.png'">
                <div class="product-badges">
                    <div class="badge-left">
                        ${producto.descuento && producto.descuento > 0 ? `<span class="badge badge-sale">-${Math.round(producto.descuento)}%</span>` : ''}
                        ${producto.destacado ? '<span class="badge badge-destacado">★ Destacado</span>' : ''}
                        ${producto.stock === 0 ? '<span class="badge badge-out">Agotado</span>' : ''}
                    </div>
                    <button class="product-favorite">
                        <i class="far fa-heart"></i>
                    </button>
                </div>
            </div>
            <div class="product-content">
                <p class="product-category">${categoriaNombre}</p>
                <h5 class="product-name">${producto.nombre}</h5>
                <p class="product-brand">${marcaNombre}</p>
                <div class="product-price-box">
                    <span class="product-price">S/ ${parseFloat(producto.precio).toFixed(2)}</span>
                    ${precioOriginal ? `<span class="product-price-old">S/ ${precioOriginal}</span>` : ''}
                </div>
                <p class="product-stock ${stockClass}">
                    <i class="fas fa-box-open"></i>
                    ${stockTexto}
                </p>
                <div class="product-actions">
                    <button class="btn-add-cart" 
                            data-producto-id="${producto.id}"
                            ${producto.stock === 0 ? 'disabled' : ''}>
                        <i class="fas fa-shopping-bag"></i>
                        ${producto.stock === 0 ? 'Agotado' : 'Agregar'}
                    </button>
                    <button class="btn-quick-view" 
                            data-bs-toggle="modal"
                            data-bs-target="#productModal"
                            data-producto='${JSON.stringify(producto).replace(/'/g, "&apos;")}'>
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </div>
        </div>
    `;
}

// ==================== CONFIGURAR EVENT LISTENERS ====================
function configurarEventListeners() {
    // Filtros
    document.getElementById('filtroGenero').addEventListener('change', aplicarFiltrosYRenderizar);
    document.getElementById('filtroCategoria').addEventListener('change', aplicarFiltrosYRenderizar);
    document.getElementById('filtroMarca').addEventListener('change', aplicarFiltrosYRenderizar);
    document.getElementById('precioMin').addEventListener('input', debounce(aplicarFiltrosYRenderizar, 500));
    document.getElementById('precioMax').addEventListener('input', debounce(aplicarFiltrosYRenderizar, 500));
    document.getElementById('filtroSale').addEventListener('change', aplicarFiltrosYRenderizar);
    document.getElementById('filtroStock').addEventListener('change', aplicarFiltrosYRenderizar);
    document.getElementById('filtroDestacados').addEventListener('change', aplicarFiltrosYRenderizar);
    
    // Resetear filtros
    document.getElementById('resetFilters').addEventListener('click', resetearFiltros);
    
    // Vistas
    document.getElementById('gridView').addEventListener('click', () => cambiarVista('grid'));
    document.getElementById('listView').addEventListener('click', () => cambiarVista('list'));
}

// ==================== FUNCIONES AUXILIARES ====================
function aplicarFiltrosYRenderizar() {
    aplicarFiltrosLocales();
    renderizarProductos();
}

function resetearFiltros() {
    document.getElementById('filtroGenero').value = '';
    document.getElementById('filtroCategoria').value = '';
    document.getElementById('filtroMarca').value = '';
    document.getElementById('precioMin').value = '';
    document.getElementById('precioMax').value = '';
    document.getElementById('filtroSale').checked = false;
    document.getElementById('filtroStock').checked = true;
    document.getElementById('filtroDestacados').checked = false;
    
    aplicarFiltrosYRenderizar();
}

function actualizarContador(cantidad) {
    const contador = document.getElementById('productCount');
    contador.textContent = `${cantidad} producto${cantidad !== 1 ? 's' : ''} encontrado${cantidad !== 1 ? 's' : ''}`;
}

function actualizarBreadcrumb(seccion) {
    const breadcrumb = document.getElementById('currentSection');
    breadcrumb.textContent = seccion;
}

function cambiarVista(vista) {
    const container = document.getElementById('catalogoContainer');
    const gridBtn = document.getElementById('gridView');
    const listBtn = document.getElementById('listView');
    
    if (vista === 'grid') {
        container.className = 'row g-4';
        gridBtn.classList.add('active');
        listBtn.classList.remove('active');
    } else {
        container.className = 'row g-3';
        gridBtn.classList.remove('active');
        listBtn.classList.add('active');
    }
}

function mostrarModalProducto(producto) {
    const imagen = producto.imagen || '/web/images/placeholder.png';
    const precio = parseFloat(producto.precio).toFixed(2);
    
    // 1. Llenar textos
    document.getElementById('modalProductName').innerText = producto.nombre;
    document.getElementById('modalProductCategory').innerText = producto.category ? producto.category.nombre : '';
    document.getElementById('modalProductBrand').innerText = producto.brand ? producto.brand.nombre : '';
    document.getElementById('modalProductImage').src = imagen;
    
    // 2. Precio
    const precioContainer = document.getElementById('modalProductPriceContainer');
    precioContainer.innerHTML = `<h3 class="fw-bold">S/ ${precio}</h3>`;

    // 3. Stock y Botón
    const btnAgregar = document.getElementById('modalAddToCart');
    const stockMsg = document.getElementById('modalProductStock');
    
    if(producto.stock > 0) {
        stockMsg.innerHTML = `<span class="text-success"><i class="fas fa-check-circle"></i> Stock disponible: ${producto.stock}</span>`;
        btnAgregar.disabled = false;
        btnAgregar.innerHTML = 'AGREGAR AL CARRITO';
        btnAgregar.onclick = () => window.agregarAlCarrito(producto.id);
    } else {
        stockMsg.innerHTML = `<span class="text-danger"><i class="fas fa-times-circle"></i> Agotado</span>`;
        btnAgregar.disabled = true;
        btnAgregar.innerHTML = 'AGOTADO';
    }

    // 4. (TRUCO) Agregar selector de tallas visual al modal
    // Buscamos si ya existe el contenedor, si no lo creamos
    let tallasDiv = document.getElementById('modalTallas');
    if(!tallasDiv) {
        tallasDiv = document.createElement('div');
        tallasDiv.id = 'modalTallas';
        tallasDiv.className = 'mb-3';
        // Insertamos antes del botón de agregar
        btnAgregar.parentNode.insertBefore(tallasDiv, btnAgregar);
    }

    tallasDiv.innerHTML = `
        <label class="fw-bold mb-2">Talla:</label>
        <div class="d-flex gap-2">
            <button class="btn btn-outline-dark btn-sm" onclick="this.classList.toggle('active')">US 7</button>
            <button class="btn btn-outline-dark btn-sm" onclick="this.classList.toggle('active')">US 8</button>
            <button class="btn btn-outline-dark btn-sm" onclick="this.classList.toggle('active')">US 9</button>
            <button class="btn btn-outline-dark btn-sm" onclick="this.classList.toggle('active')">US 10</button>
        </div>
        <small class="text-muted d-block mt-1">* Selección visual</small>
    `;
}
 function crearCardProducto(producto) {
    // ... (tu lógica de imagen, precio, stock sigue igual) ...
    const imagen = producto.imagen || (producto.imagenes && producto.imagenes.length > 0 ? producto.imagenes[0] : '/images/no-image.png');
    
    // Cálculo de precio original
    const precioOriginal = producto.descuento && producto.descuento > 0 
        ? (producto.precio / (1 - producto.descuento / 100)).toFixed(2)
        : null;
    
    // Estado de stock
    const stockClass = producto.stock === 0 ? 'stock-out' 
        : (producto.stockMinimo && producto.stock < producto.stockMinimo ? 'stock-low' : 'stock-available');
    const stockTexto = producto.stock === 0 ? 'Agotado' : `${producto.stock} disponibles`;
    
    // Nombre de categoría y marca
    const categoriaNombre = producto.categoryNombre || (producto.category ? producto.category.nombre : 'Sin categoría');
    const marcaNombre = producto.brandNombre || (producto.brand ? producto.brand.nombre : 'Sin marca');
    
    return `
        <div class="product-card">
            <div class="product-image-wrapper">
                <img src="${imagen}" alt="${producto.nombre}" class="product-image" onerror="this.src='/images/no-image.png'">
                <div class="product-badges">
                    <div class="badge-left">
                        ${producto.descuento && producto.descuento > 0 ? `<span class="badge badge-sale">-${Math.round(producto.descuento)}%</span>` : ''}
                        ${producto.destacado ? '<span class="badge badge-destacado">★ Destacado</span>' : ''}
                        ${producto.stock === 0 ? '<span class="badge badge-out">Agotado</span>' : ''}
                    </div>
                </div>
            </div>
            <div class="product-content">
                <p class="product-category">${categoriaNombre}</p>
                <h5 class="product-name">${producto.nombre}</h5>
                <p class="product-brand">${marcaNombre}</p>
                <div class="product-price-box">
                    <span class="product-price">S/ ${parseFloat(producto.precio).toFixed(2)}</span>
                    ${precioOriginal ? `<span class="product-price-old">S/ ${precioOriginal}</span>` : ''}
                </div>
                <p class="product-stock ${stockClass}">
                    <i class="fas fa-box-open"></i> ${stockTexto}
                </p>
                <div class="product-actions">
                    <button class="btn-add-cart" 
                            onclick="agregarAlCarrito(${producto.id})"
                            ${producto.stock === 0 ? 'disabled' : ''}>
                        <i class="fas fa-shopping-bag"></i>
                        ${producto.stock === 0 ? 'Agotado' : 'Agregar'}
                    </button>
                    
                    <button class="btn-quick-view" 
                            data-bs-toggle="modal"
                            data-bs-target="#productModal"
                            data-producto='${JSON.stringify(producto).replace(/'/g, "&apos;")}'>
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </div>
        </div>
    `;
}


function mostrarError(mensaje) {
    const container = document.getElementById('catalogoContainer');
    container.innerHTML = `
        <div class="col-12">
            <div class="empty-state">
                <i class="fas fa-exclamation-triangle"></i>
                <h4>Error</h4>
                <p>${mensaje}</p>
            </div>
        </div>
    `;
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
'use strict';
$(window).on('load', function() {
    // Dar tiempo adicional para que se carguen completamente los CDN
    setTimeout(initializeApp, 500);
});

function initializeApp() {
    console.log('=== INICIALIZANDO APLICACIÓN PRODUCTOS ===');
    setTipoEntidad('productos');

    // Verificación robusta de dependencias
    console.log('Verificando dependencias...');
    console.log('- jQuery:', typeof $ !== 'undefined' ? '✅' : '❌');
    console.log('- DataTables:', typeof $.fn.DataTable !== 'undefined' ? '✅' : '❌');
    console.log('- Bootstrap:', typeof bootstrap !== 'undefined' ? '✅' : '❌');
    console.log('- SweetAlert2:', typeof Swal !== 'undefined' ? '✅' : '❌');

    // Si DataTables no está disponible, usar datos estáticos
    if (typeof $.fn.DataTable === 'undefined') {
        console.warn('⚠️ DataTables no disponible, usando tabla básica');
        showBasicTable();
        return;
    }

    // Variables globales
    let dataTable;
    let productoModal;
    let detalleModal;
    let isEditMode = false;
    let currentProductoId = null;
    let categoriasData = [];
    let marcasData = [];

    // Configuración de API
    const API_BASE = '/productos/api';
    const ENDPOINTS = {
        list: `${API_BASE}/datatables`,
        create: `${API_BASE}/crear`,
        update: (id) => `${API_BASE}/actualizar/${id}`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        toggleDestacado: (id) => `${API_BASE}/destacado/${id}`,
        stockBajo: `${API_BASE}/stock-bajo`,
        categorias: '/categorias/api/listar',
        marcas: '/marcas/api/listar',
        uploadImagen: '/api/upload/productos/imagen',
        uploadImagenes: '/api/upload/productos/imagenes'
    };

    // Inicializar aplicación
    try {
        initializeModals();
        loadCategoriasYMarcas();
        initializeDataTable();
        setupEventListeners();
        setupPrecioPreview();
        console.log('✅ Aplicación inicializada exitosamente');
    } catch (error) {
        console.error('❌ Error en inicialización:', error);
        showError('Error al inicializar la aplicación: ' + error.message);
    }

    /**
     * Utilidad para parsear números decimales de forma segura
     */
    function parseDecimal(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }
        const parsed = parseFloat(value);
        return isNaN(parsed) ? null : parsed;
    }

    /**
     * Utilidad para formatear precio
     */
    function formatPrice(price) {
        const num = parseDecimal(price);
        return num !== null ? num.toFixed(2) : '0.00';
    }

    /**
     * Calcular precio con descuento
     */
    function calcularPrecioFinal(precio, descuento) {
        const p = parseDecimal(precio);
        const d = parseDecimal(descuento);

        if (p === null) return null;
        if (d === null || d === 0) return p;

        return p - (p * d / 100);
    }

    /**
     * Inicializar modales de Bootstrap
     */
    function initializeModals() {
        const productoModalEl = document.getElementById('productoModal');
        const detalleModalEl = document.getElementById('detalleModal');
        const stockModalEl = document.getElementById('stockModal');

        if (productoModalEl && typeof bootstrap !== 'undefined') {
            productoModal = new bootstrap.Modal(productoModalEl);
        }

        if (detalleModalEl && typeof bootstrap !== 'undefined') {
            detalleModal = new bootstrap.Modal(detalleModalEl);
        }

        if (stockModalEl && typeof bootstrap !== 'undefined') {
            stockModal = new bootstrap.Modal(stockModalEl);
        }

        console.log('✅ Modales inicializados');
    }

    /**
     * Cargar categorías y marcas para los selectores
     */
    function loadCategoriasYMarcas() {
        // Cargar categorías
        $.ajax({
            url: ENDPOINTS.categorias,
            method: 'GET',
            success: function(response) {
                if (response.success && Array.isArray(response.data)) {
                    categoriasData = response.data;
                    populateCategoriaSelect();
                    console.log('✅ Categorías cargadas:', categoriasData.length);
                }
            },
            error: function() {
                console.error('❌ Error al cargar categorías');
            }
        });

        // Cargar marcas
        $.ajax({
            url: ENDPOINTS.marcas,
            method: 'GET',
            success: function(response) {
                if (response.success && Array.isArray(response.data)) {
                    marcasData = response.data;
                    populateMarcaSelect();
                    console.log('✅ Marcas cargadas:', marcasData.length);
                }
            },
            error: function() {
                console.error('❌ Error al cargar marcas');
            }
        });
    }

    /**
     * Poblar select de categorías
     */
    function populateCategoriaSelect() {
        const select = $('#categoryId');
        select.empty().append('<option value="">Seleccione una categoría</option>');

        categoriasData.forEach(cat => {
            if (cat.estado) {
                select.append(`<option value="${cat.id}">${cat.nombre}</option>`);
            }
        });
    }

    /**
     * Poblar select de marcas
     */
    function populateMarcaSelect() {
        const select = $('#brandId');
        select.empty().append('<option value="">Seleccione una marca</option>');

        marcasData.forEach(marca => {
            if (marca.estado) {
                select.append(`<option value="${marca.id}">${marca.nombre}</option>`);
            }
        });
    }

    /**
     * Inicializar DataTable
     */
    function initializeDataTable() {
        const tableElement = $('#tablaProductos');

        if (tableElement.length === 0) {
            throw new Error('Elemento tabla #tablaProductos no encontrado');
        }

        console.log('📄 Inicializando DataTable...');

        dataTable = tableElement.DataTable({
            responsive: true,
            processing: true,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(json) {
                    console.log('📊 Datos recibidos:', json);
                     console.log('📊 JSON completo:', JSON.stringify(json, null, 2)); // ✅ VER TODO
        
                    if (json && Array.isArray(json.data)) {
                        console.log('📊 Total productos recibidos del servidor:', json.data.length); // ✅ NUEVO
                        console.log('📊 Todos los productos:', json.data); // ✅ NUEVO
                        
                        const productosActivos = json.data.filter(p => p.estado !== 2);
                        console.log(`✅ ${productosActivos.length} productos activos de ${json.data.length} totales`);
                        console.log('📊 IDs de productos activos:', productosActivos.map(p => p.id)); // ✅ NUEVO

                        updateContadores(productosActivos);
                        return productosActivos;
                    } else {
                        console.error('❌ Formato de respuesta inválido:', json);
                        showNotification('Error: Datos no válidos recibidos del servidor', 'error');
                        return [];
                    }
                },
                error: function(xhr, error, thrown) {
                    console.error('❌ Error AJAX:', {
                        status: xhr.status,
                        error: error,
                        thrown: thrown,
                        responseText: xhr.responseText
                    });

                    let message = 'Error al cargar datos';
                    if (xhr.status === 0) {
                        message = 'Sin conexión al servidor';
                    } else if (xhr.status === 404) {
                        message = 'Servicio no encontrado';
                    } else if (xhr.status >= 500) {
                        message = 'Error interno del servidor';
                    }

                    showNotification(message, 'error');
                }
            },
            columns: [
                { data: 'id', title: 'ID', width: '5%' },
                {
                    data: 'imagen',
                    title: 'Imagen',
                    width: '10%',
                    orderable: false,
                    searchable: false,
                    render: function(data, type, row) {
                        // ✅ NUEVO: Mostrar primera imagen del array
                        let imagenUrl = '/admin/images/no-image.png';

                        if (row.imagenes && Array.isArray(row.imagenes) && row.imagenes.length > 0) {
                            imagenUrl = row.imagenes[0];
                        } else if (row.imagen) {
                            imagenUrl = row.imagen;
                        }

                        const imageCount = row.imagenes?.length || (row.imagen ? 1 : 0);
                        const badge = imageCount > 1 ? `<span class="badge bg-primary position-absolute top-0 end-0">${imageCount}</span>` : '';

                        return `
                            <div class="position-relative d-inline-block">
                                <img src="${imagenUrl}" class="product-image-preview" alt="Producto"
                                     onerror="this.src='/admin/images/no-image.png'">
                                ${badge}
                            </div>
                        `;
                    }
                },
                { data: 'nombre', title: 'Nombre', width: '20%' },
                {
                    data: null,
                    title: 'Precio',
                    width: '12%',
                    render: function(data, type, row) {
                        if (type === 'display') {
                            const precio = parseDecimal(row.precio);
                            const descuento = parseDecimal(row.descuento);
                            if (precio === null) return '<span class="text-muted">N/A</span>';

                            if (descuento !== null && descuento > 0) {
                                const precioFinal = calcularPrecioFinal(precio, descuento);
                                return `
                                    <div style="font-size: 0.85rem;">
                                        <span class="price-original">S/. ${formatPrice(precio)}</span><br>
                                        <span class="price-final">S/. ${formatPrice(precioFinal)}</span>
                                        <span class="badge bg-danger ms-1" style="font-size: 0.7rem;">${formatPrice(descuento)}% OFF</span>
                                    </div>
                                `;
                            }
                            return `<span style="font-size: 0.9rem;">S/. ${formatPrice(precio)}</span>`;
                        }
                        return row.precio;
                    }
                },
                {
                    data: 'stock',
                    title: 'Stock',
                    width: '8%',
                    render: function(data, type, row) {
                        if (type === 'display') {
                            const stock = parseInt(data) || 0;
                            const stock_minimo = parseInt(row.stock_minimo || row.stockMinimo) || 0;
                            let badgeClass = 'bg-success';
                            if (stock === 0) badgeClass = 'bg-danger';
                            else if (stock <= stock_minimo) badgeClass = 'bg-warning';
                            return `<span class="badge ${badgeClass}">${stock}</span>`;
                        }
                        return data;
                    }
                },
                {
                    data: null,
                    title: 'Stock.Mín',
                    width: '5%',
                    render: function(data, type, row) {
                        const stockMin = row.stock_minimo || row.stockMinimo || 0;
                        if (type === 'display') {
                            return `<span class="badge bg-info text-dark" style="font-size: 0.85rem;">${parseInt(stockMin)}</span>`;
                        }
                        return stockMin;
                    }
                },
                { data: 'category.nombre', title: 'Categoría', width: '10%', defaultContent: '-' },
                { data: 'brand.nombre', title: 'Marca', width: '10%', defaultContent: '-' },
                {
                    data: 'genero',
                    title: 'Género',
                    width: '8%',
                    render: function(data, type) {
                        if (type === 'display') {
                            const generos = {
                                'HOMBRE': '<span class="badge bg-primary">Hombre</span>',
                                'MUJER': '<span class="badge bg-danger">Mujer</span>'
                            };
                            return generos[data] || data;
                        }
                        return data;
                    }
                },
                {
                    data: 'estado',
                    title: 'Estado',
                    width: '8%',
                    render: function(data, type) {
                        if (type === 'display') {
                            const isActive = data === true || data === 1 || data === 'true';
                            return isActive
                                ? '<span class="badge bg-success">Activo</span>'
                                : '<span class="badge bg-danger">Inactivo</span>';
                        }
                        return data;
                    }
                },
                {
                    data: 'destacado',
                    title: 'Destacado',
                    width: '8%',
                    render: function(data, type) {
                        if (type === 'display') {
                            const isDestacado = data === true || data === 1 || data === 'true';
                            return isDestacado
                                ? '<span class="badge badge-destacado"><i class="bi bi-star-fill"></i> Destacado</span>'
                                : '<span class="badge bg-secondary">Normal</span>';
                        }
                        return data;
                    }
                },
                {
                    data: null,
                    title: 'Acciones',
                    width: '13%',
                    orderable: false,
                    searchable: false,
                    render: function(data, type, row) {
                        return createActionButtons(row);
                    }
                }
            ],
            columnDefs: [
                { responsivePriority: 1, targets: 2 },
                { responsivePriority: 2, targets: 10 }
            ],
            language: {
                processing: "Procesando...",
                lengthMenu: "Mostrar _MENU_ registros",
                zeroRecords: "No se encontraron resultados",
                emptyTable: "Ningún dato disponible en esta tabla",
                info: "Mostrando registros del _START_ al _END_ de un total de _TOTAL_ registros",
                infoEmpty: "Mostrando registros del 0 al 0 de un total de 0 registros",
                infoFiltered: "(filtrado de un total de _MAX_ registros)",
                search: "Buscar:",
                paginate: {
                    first: "Primero",
                    last: "Último",
                    next: "Siguiente",
                    previous: "Anterior"
                }
            },
            pageLength: 10,
            order: [[0, 'desc']],
            rowCallback: function(row, data) {
                const activo = data.estado === 1 || data.estado === true || data.estado === '1';
                if (!activo) {
                    $(row).addClass('table-secondary');
                } else {
                    $(row).removeClass('table-secondary');
                }
            }
        });

        // ✅ Actualiza los contadores automáticamente cada vez que la tabla se redibuja
        dataTable.on('draw', function() {
            const productos = dataTable.rows().data().toArray();
            updateContadores(productos);
        });

    }


    /**
     * Crear botones de acción para cada fila
     */
    function createActionButtons(row) {
        const isActive = row.estado === true || row.estado ===1 || row.estado === 'true';
        const isDestacado = row.destacado === true || row.destacado === 1 || row.destacado === 'true';
        const statusIcon = isActive ? 'bi-eye-slash' : 'bi-eye';
        const statusTitle = isActive ? 'Desactivar' : 'Activar';
        const statusClass = isActive ? 'btn-warning' : 'btn-success';
        const destacadoIcon = isDestacado ? 'bi-star-fill' : 'bi-star';
        const destacadoTitle = isDestacado ? 'Quitar destacado' : 'Marcar destacado';

        return `
            <div class="btn-group btn-group-sm" role="group">
                <button class="btn btn-info action-detail" data-id="${row.id}" title="Ver detalles">
                    <i class="bi bi-eye"></i>
                </button>
                <button class="btn btn-primary action-edit" data-id="${row.id}" title="Editar">
                    <i class="bi bi-pencil-square"></i>
                </button>
                <button class="btn btn-secondary action-destacado" data-id="${row.id}" title="${destacadoTitle}">
                    <i class="bi ${destacadoIcon}"></i>
                </button>
                <div class="form-check form-switch text-center">
                <input
                    class="form-check-input toggle-status"
                    type="checkbox"
                    data-id="${row.id}"
                    ${row.estado ===1 ? 'checked' : ''}>
                </div>
                <button class="btn btn-danger action-delete" data-id="${row.id}" title="Eliminar">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
    }

    /**
     * Actualizar contadores de la interfaz
     */
    function updateContadores(productos) {
        fetch('/productos/api/estadisticas')
            .then(res => res.json())
            .then(stats => {
                if (stats.success) {
                    // Actualizar contadores con datos del servidor
                    updateContadorElement('contador-total-activos', stats.data.activos);
                    updateContadorElement('contador-total-inactivos', stats.data.inactivos);
                    updateContadorElement('contador-total-eliminados', stats.data.eliminados);
                    updateContadorElement('contador-total-todos', stats.data.total);

                    console.log('📊 Contadores actualizados desde servidor:', stats.data);
                }
            })
            .catch(error => {
                console.error('❌ Error al obtener estadísticas:', error);
                // Fallback: usar contadores locales
                updateContadoresLocales(productos);
            });
    }

    /**
     * ✅ FUNCIÓN AUXILIAR: Actualizar elemento de contador si existe
     */
    function updateContadorElement(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    /**
     * ✅ FALLBACK: Actualizar contadores con datos locales (solo productos activos visibles)
     */
    function updateContadoresLocales(productos) {
        // Solo productos activos (estado !== 2)
        const totalActivos = productos.length;
        updateContadorElement('contador-total-activos', totalActivos);

        console.log('📊 Contadores actualizados localmente: Total Activos =', totalActivos);
    }

    /**
     * Configurar event listeners
     */
    function setupEventListeners() {
        // Botón nuevo registro
        $('#btnNuevoRegistro').on('click', openModalForNew);

        // Submit formulario
        $('#formProducto').on('submit', handleFormSubmit);

        // Eventos de tabla (delegación)
        $('#tablaProductos tbody')
            .on('click', '.action-detail', handleViewDetails)
            .on('click', '.action-edit', handleEdit)
            .on('change', '.toggle-status', handleToggleStatus)
            .on('click', '.action-destacado', handleToggleDestacado)
            .on('click', '.action-delete', handleDelete);

        // Editar desde modal de detalles
        $('#btnEditarDesdeDetalle').on('click', handleEditFromDetail);
        console.log('✅ Event listeners configurados');
    }

   /**
    * Formatear porcentaje de descuento (elimina decimales .00)
    */
   function formatDiscount(discount) {
       const num = parseDecimal(discount);
       if (num === null) return '0';

       return num % 1 === 0 ? num.toString() : num.toFixed(2);
   }

   /**
    * Configurar preview de precio con descuento
    */
   function setupPrecioPreview() {
       $('#precio, #descuento').on('input', function() {
           const precio = parseDecimal($('#precio').val());
           const descuento = parseDecimal($('#descuento').val());

           if (precio !== null && precio > 0 && descuento !== null && descuento > 0) {
               const precioFinal = calcularPrecioFinal(precio, descuento);
               $('#precioPreviewContent').html(`
                   Precio Original: <span class="price-original">S/. ${formatPrice(precio)}</span> |
                   Descuento: <span class="badge bg-danger">${formatDiscount(descuento)}%</span> |
                   Precio Final: <span class="price-final">S/. ${formatPrice(precioFinal)}</span>
               `);
               $('#precioPreview').show();
           } else {
               $('#precioPreview').hide();
           }
       });
   }

    /**
     * FUNCIONES CRUD REALES
     */

    /**
     * Manejar envío del formulario (Crear/Actualizar)
     */

    /**
     * Manejar envío del formulario (Crear/Actualizar)
     */
    let isSubmitting = false;

    async function handleFormSubmit(e) {
        e.preventDefault();

        // ✅ Prevenir envíos concurrentes
        if (isSubmitting) {
            console.warn('⚠️ Ya hay un envío en proceso');
            return;
        }

        isSubmitting = true;

        try {
            showLoading('Procesando...');

            const precio = parseDecimal($('#precio').val());
            const descuento = parseDecimal($('#descuento').val());
            const categoryId = parseInt($('#categoryId').val());
            const brandId = parseInt($('#brandId').val());
            const stockMinimo = parseInt($('#stockMinimo').val()) || 0;

            // Validaciones...
            if (!$('#nombre').val().trim()) throw new Error('El nombre del producto es requerido');
            if (!$('#genero').val()) throw new Error('El género es requerido');
            if (isNaN(categoryId) || categoryId <= 0) throw new Error('La categoría es requerida');
            if (isNaN(brandId) || brandId <= 0) throw new Error('La marca es requerida');
            if (precio === null || precio <= 0) throw new Error('El precio debe ser mayor a 0');

            // 🚀 AQUI ESTÁ EL CAMBIO CLAVE ==========================
            // Borramos toda la lógica antigua y usamos la función de ordenamiento
            
            console.log('🔄 Procesando imágenes...');
            // Esta función (del nuevo imagenes.js) sube las fotos nuevas 
            // y devuelve la lista de URLs en el orden visual exacto
            const imageUrls = await obtenerImagenesOrdenadas(); 
            
            console.log('✅ URLs finales ordenadas:', imageUrls);
            // =======================================================

            // Preparar datos
            const formData = {
                nombre: $('#nombre').val().trim(),
                descripcion: $('#descripcion').val().trim() || null,
                genero: $('#genero').val(),
                precio: precio,
                descuento: descuento,
                stock: parseInt($('#stock').val()) || 0,
                stockMinimo: stockMinimo,
                // Asignamos la lista ordenada que nos devolvió la función
                imagenes: imageUrls.length > 0 ? imageUrls : null,
                imagen: imageUrls.length > 0 ? imageUrls[0] : null, // La primera es la principal
                destacado: $('#destacado').is(':checked'),
                estado: $('#estado').is(':checked') ? 1 : 0,
                categoryId: categoryId,
                brandId: brandId
            };

            console.log('📦 Datos a enviar:', formData);

            // Crear o actualizar
            if (isEditMode && currentProductoId) {
                await updateProducto(currentProductoId, formData);
            } else {
                await createProducto(formData);
            }

        } catch (error) {
            hideLoading();
            console.error('Error:', error);
            showNotification(error.message || 'Error desconocido', 'error');
        } finally {
            isSubmitting = false; // ✅ Liberar flag
        }
    }

    /**
     * Crear nuevo producto
     */
    function createProducto(data) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: ENDPOINTS.create,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                hideLoading();
                if (response.success) {
                    console.log('✅ Producto creado:', response.data); // ✅ NUEVO
                    showNotification('Producto creado exitosamente', 'success');
                    productoModal.hide();
                    clearForm();
                    
                    // ✅ Agregar un pequeño delay antes de recargar
                    setTimeout(() => {
                        console.log('🔄 Recargando tabla...'); // ✅ NUEVO
                        dataTable.ajax.reload(function(json) {
                            console.log('✅ Tabla recargada. Productos visibles:', json.data.length); // ✅ NUEVO
                        }, false);
                    }, 500);
                    
                    resolve(response);
                } else {
                    reject(new Error(response.message || 'Error al crear el producto'));
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al crear el producto';
                reject(new Error(errorMsg));
            }
        });
    });
}


    /**
     * Actualizar producto existente
     */
    function updateProducto(id, data) {
    return new Promise((resolve, reject) => {
        $.ajax({
            url: ENDPOINTS.update(id),
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showNotification('Producto actualizado exitosamente', 'success');
                    productoModal.hide();
                    clearForm();
                    
                    // ✅ FIX: Recargar sin resetear paginación
                    dataTable.ajax.reload(null, false);
                    
                    resolve(response);
                } else {
                    reject(new Error(response.message || 'Error al actualizar el producto'));
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al actualizar el producto';
                reject(new Error(errorMsg));
            }
        });
    });
}

    /**
     * Ver detalles de producto
     */
    function handleViewDetails() {
        const id = $(this).data('id');

        $.ajax({
            url: ENDPOINTS.get(id),
            method: 'GET',
            beforeSend: function() {
                showLoading('Cargando detalles...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showDetalleModal(response.data);
                } else {
                    showNotification('Error al cargar los detalles', 'error');
                }
            },
            error: function() {
                hideLoading();
                showNotification('Error al cargar los detalles', 'error');
            }
        });
    }

    /**
     * Editar producto
     */
    function handleEdit() {
        const id = $(this).data('id');

        $.ajax({
            url: ENDPOINTS.get(id),
            method: 'GET',
            beforeSend: function() {
                showLoading('Cargando datos...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    openModalForEdit(response.data);
                } else {
                    showNotification('Error al cargar los datos', 'error');
                }
            },
            error: function() {
                hideLoading();
                showNotification('Error al cargar los datos', 'error');
            }
        });
    }

   /**
    * Maneja el cambio de estado de un producto (activar/desactivar)
    */
   function handleToggleStatus(e) {
       e.preventDefault();

       const checkbox = $(this);
       const id = checkbox.data('id');
       const newState = checkbox.is(':checked');
       const prevState = !newState;
       const rowNode = checkbox.closest('tr');
       const rowDT = dataTable.row(rowNode);
       const rowData = rowDT.data();

       // Preguntar solo si se está desactivando
       if (!newState) {
           Swal.fire({
               title: '¿Desactivar producto?',
               text: `El producto "${rowData.nombre}" dejará de estar disponible.`,
               icon: 'warning',
               showCancelButton: true,
               confirmButtonColor: '#dc3545',
               cancelButtonColor: '#6c757d',
               confirmButtonText: 'Sí, desactivar',
               cancelButtonText: 'Cancelar'
           }).then((result) => {
               if (result.isConfirmed) {
                   toggleProductoStatus(id, checkbox, rowDT);
               } else {
                   checkbox.prop('checked', prevState);
               }
           });
       } else {
           toggleProductoStatus(id, checkbox, rowDT);
       }
   }

   /**
    * Ejecuta el cambio de estado y actualiza la fila localmente
    */
   function toggleProductoStatus(id, checkbox, rowDT) {
       const newState = checkbox.is(':checked');
       showLoading('Cambiando estado...');

       fetch(ENDPOINTS.toggleStatus(id), {
           method: 'PUT',
           headers: { 'Content-Type': 'application/json' },
           body: JSON.stringify({ estado: newState ? 1 : 0 })
       })
       .then(res => res.json())
       .then(data => {
           if (data.success) {
               showNotification(data.message || 'Estado actualizado', 'success');

               let rowData = rowDT.data() || {};
               rowData.estado = newState ? 1 : 0;

               rowDT.data(rowData).invalidate().draw(false);

               const node = $(rowDT.node());
               if (newState) node.removeClass('table-secondary');
               else node.addClass('table-secondary');

               $(`input[type="checkbox"][data-id="${id}"]`).prop('checked', newState);
           } else {
               showNotification(data.message || 'Error al cambiar el estado', 'error');
               checkbox.prop('checked', !newState);
           }
       })
       .catch(() => {
           showNotification('Error de conexión con el servidor', 'error');
           checkbox.prop('checked', !newState);
       })
       .finally(() => hideLoading());
   }

    /**
     * Cambiar estado destacado
     */
    function handleToggleDestacado() {
        const id = $(this).data('id');
        const row = dataTable.row($(this).closest('tr')).data();
        const isDestacado = row.destacado === true || row.destacado === 1 || row.destacado === 'true';
        const action = isDestacado ? 'quitar destacado de' : 'destacar';

        Swal.fire({
            title: `¿${action.charAt(0).toUpperCase() + action.slice(1)} producto?`,
            text: `¿Está seguro de ${action} el producto "${row.nombre}"?`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#764ba2',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, continuar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                toggleProductoDestacado(id);
            }
        });
    }

    /**
     * Ejecutar cambio de destacado
     */
    function toggleProductoDestacado(id) {
        $.ajax({
            url: ENDPOINTS.toggleDestacado(id),
            method: 'PUT',
            beforeSend: function() {
                showLoading('Cambiando destacado...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showNotification('Estado destacado cambiado exitosamente', 'success');
                    dataTable.ajax.reload();
                } else {
                    showNotification(response.message || 'Error al cambiar el destacado', 'error');
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al cambiar el destacado';
                showNotification(errorMsg, 'error');
            }
        });
    }

   /**
    * Maneja la eliminación lógica de un producto
    */
   function handleDelete() {
       const id = $(this).data('id');
       const rowNode = $(this).closest('tr');
       const rowDT = dataTable.row(rowNode);
       const rowData = rowDT.data();

       Swal.fire({
           title: '¿Eliminar producto?',
           text: `¿Está seguro de eliminar el producto "${rowData.nombre}"? Esta acción no se puede deshacer.`,
           icon: 'warning',
           showCancelButton: true,
           confirmButtonColor: '#dc3545',
           cancelButtonColor: '#6c757d',
           confirmButtonText: 'Sí, eliminar',
           cancelButtonText: 'Cancelar'
       }).then((result) => {
           if (result.isConfirmed) {
               deleteProducto(id, rowDT);
           }
       });
   }

   /**
    * Ejecuta la eliminación lógica (estado = 2)
    */
   function deleteProducto(id, rowDT) {
       showLoading('Eliminando producto...');

       $.ajax({
           url: ENDPOINTS.delete(id),
           method: 'DELETE',
           success: function(response) {
               hideLoading();
               if (response.success) {
                   showNotification(response.message || 'Producto eliminado exitosamente', 'success');


                   dataTable.ajax.reload(null, false);


               } else {
                   showNotification(response.message || 'Error al eliminar el producto', 'error');
               }
           },
           error: function(xhr) {
               hideLoading();
               const errorMsg = xhr.responseJSON?.message || 'Error al eliminar el producto';
               showNotification(errorMsg, 'error');
           }
       });
   }

    /**
     * Editar desde modal de detalles
     */
    function handleEditFromDetail() {
        const id = $('#detalle-id').text();
        if (id && id !== '-') {
            detalleModal.hide();

            $.ajax({
                url: ENDPOINTS.get(id),
                method: 'GET',
                success: function(response) {
                    if (response.success) {
                        openModalForEdit(response.data);
                    }
                }
            });
        }
    }

    /**
     * FUNCIONES DE MODALES
     */


    /**
     * Abrir modal para nuevo producto
     */
    function openModalForNew() {
        isEditMode = false;
        currentProductoId = null;
        
        // 1. Primero limpiamos el formulario básico
        clearForm(); // Esta función ya tiene protecciones internas

        $('#modalTitle').text('Agregar Producto');
        $('#estado').prop('checked', true);
        $('#destacado').prop('checked', false);
        $('#stock').val(0);
        $('#stockMinimo').val(0);

        // 2. PROTECCIÓN: Verificar si existen las utilidades de imagen antes de usarlas
        // Esto evita el error "ReferenceError" si imagenes.js no cargó bien o por orden de carga
        if (typeof window.clearAllImagePreviews === 'function') {
            window.clearAllImagePreviews();
        } else if (typeof clearAllImagePreviews === 'function') {
            clearAllImagePreviews();
        }

        // 3. Resetear array de archivos seleccionado de forma segura
        if (typeof window.selectedFiles !== 'undefined') {
            window.selectedFiles = [];
        } else if (typeof selectedFiles !== 'undefined') {
            selectedFiles = [];
        }

        // 4. Activar pestaña de archivos
        const fileTab = document.querySelector('[data-tab="file-tab"]');
        if (fileTab) {
            fileTab.click();
        }

        if (productoModal) {
            productoModal.show();
        } else {
            showNotification('Modal no disponible', 'error');
        }
    }

    /**
     * Abrir modal para editar producto
     */
    function openModalForEdit(producto) {
        isEditMode = true;
        currentProductoId = producto.id;

        // Llenar formulario con datos
        $('#id').val(producto.id);
        $('#nombre').val(producto.nombre);
        $('#descripcion').val(producto.descripcion || '');
        $('#genero').val(producto.genero);
        $('#precio').val(formatPrice(producto.precio));
        $('#descuento').val(producto.descuento ? formatPrice(producto.descuento) : '');
        $('#stock').val(producto.stock || 0);

        const stockMin = producto.stock_minimo || producto.stockMinimo || 0;
        $('#stockMinimo').val(stockMin);

        $('#categoryId').val(producto.category?.id || '');
        $('#brandId').val(producto.brand?.id || '');
        $('#destacado').prop('checked', producto.destacado === true || producto.destacado === 1);
        $('#estado').prop('checked', producto.estado === true || producto.estado === 1);

        // ✅ MANEJO DE IMÁGENES EN EDICIÓN
        const imagenes = producto.imagenes || (producto.imagen ? [producto.imagen] : []);

        if (imagenes.length > 0) {
            // Activar tab de archivos (para que funcione con el sistema de múltiples imágenes)
            const fileTab = document.querySelector('[data-tab="file-tab"]');
            if (fileTab) {
                fileTab.click();
            }

            // Mostrar todas las imágenes existentes (una o múltiples)
            mostrarImagenesExistentes(imagenes);

            console.log('✅ Producto cargado con', imagenes.length, 'imagen(es)');
        } else {
            clearAllImagePreviews();
        }

        $('#modalTitle').text('Editar Producto');

        // Trigger precio preview
        $('#precio').trigger('input');

        if (productoModal) {
            productoModal.show();
        }
    }
    /**
     * ✅ FUNCIÓN GLOBAL: Inicializar galería de imágenes en modal de detalles
     *
     */
    function initDetalleGaleria(imagenes) {
        const imagenPrincipal = document.getElementById('detalle-imagen-principal');
        const thumbnailsContainer = document.getElementById('detalle-galeria-thumbnails');
        const imageCount = document.getElementById('detalle-image-count');
        const imageTotal = document.getElementById('detalle-image-total');

        console.log('🖼️ Inicializando galería con', imagenes?.length || 0, 'imágenes');

        if (!imagenPrincipal || !thumbnailsContainer) {
            console.error('❌ Elementos de galería no encontrados');
            return;
        }

        // Limpiar galería anterior
        thumbnailsContainer.innerHTML = '';

        // Si no hay imágenes, mostrar imagen por defecto
        if (!imagenes || imagenes.length === 0) {
            imagenPrincipal.src = '/admin/images/no-image.png';
            imageCount.textContent = '0';
            imageTotal.textContent = '0';
            console.log('⚠️ No hay imágenes para mostrar');
            return;
        }

        // Actualizar contador
        imageCount.textContent = '1';
        imageTotal.textContent = imagenes.length;

        // Mostrar primera imagen
        imagenPrincipal.src = imagenes[0];
        imagenPrincipal.onerror = function() {
            console.error('❌ Error al cargar imagen:', this.src);
            this.src = '/admin/images/no-image.png';
        };

        console.log('✅ Imagen principal cargada:', imagenes[0]);

        // Crear thumbnails
        imagenes.forEach((url, index) => {
            const thumbnail = document.createElement('div');
            thumbnail.className = 'thumbnail-item' + (index === 0 ? ' active' : '');
            thumbnail.innerHTML = `
                <img src="${url}"
                     alt="Imagen ${index + 1}"
                     onerror="this.src='/admin/images/no-image.png'">
                <span class="thumbnail-number">${index + 1}</span>
            `;

            // Evento click para cambiar imagen principal
            thumbnail.addEventListener('click', function() {
                console.log('🖱️ Click en thumbnail', index + 1);

                // Remover clase active de todos
                document.querySelectorAll('.thumbnail-item').forEach(t =>
                    t.classList.remove('active')
                );

                // Agregar active al clickeado
                this.classList.add('active');

                // Cambiar imagen principal con efecto fade
                imagenPrincipal.style.opacity = '0.5';
                setTimeout(() => {
                    imagenPrincipal.src = url;
                    imagenPrincipal.style.opacity = '1';
                    imageCount.textContent = index + 1;
                }, 150);
            });

            thumbnailsContainer.appendChild(thumbnail);
        });

        console.log('✅ Galería inicializada con', imagenes.length, 'thumbnails');
    }

    /**
     * ✅ MEJORADO: Mostrar modal de detalles con galería de imágenes
     * REEMPLAZAR LA FUNCIÓN showDetalleModal EXISTENTE
     */
    function showDetalleModal(producto) {
        console.log('📋 Mostrando detalles del producto:', producto);

        // Información básica
        $('#detalle-id').text(producto.id);
        $('#detalle-nombre').text(producto.nombre);
        $('#detalle-descripcion').text(producto.descripcion || 'Sin descripción');

        // Género
        let generoValor = producto.genero?.toUpperCase();
        if (generoValor === 'HOMBRE') generoValor = 'MASCULINO';
        if (generoValor === 'MUJER') generoValor = 'FEMENINO';
        const generos = {
            'MASCULINO': 'Masculino',
            'FEMENINO': 'Femenino'
        };
        $('#detalle-genero').text(generos[generoValor] || generoValor || '-');

        // Categoría y Marca
        $('#detalle-categoria').text(producto.category?.nombre || '-');
        $('#detalle-marca').text(producto.brand?.nombre || '-');

        // Estado
        const isActive = producto.estado === true || producto.estado === 1 || producto.estado === 'true';
        const estadoBadge = $('#detalle-estado-badge');
        estadoBadge.text(isActive ? 'Activo' : 'Inactivo');
        estadoBadge.removeClass('bg-success bg-danger').addClass(isActive ? 'bg-success' : 'bg-danger');

        // Destacado
        const isDestacado = producto.destacado === true || producto.destacado === 1 || producto.destacado === 'true';
        const destacadoBadge = $('#detalle-destacado-badge');
        if (isDestacado) {
            destacadoBadge.html('<i class="bi bi-star-fill"></i> Destacado');
            destacadoBadge.removeClass('bg-secondary').addClass('badge-destacado');
        } else {
            destacadoBadge.text('Normal');
            destacadoBadge.removeClass('badge-destacado').addClass('bg-secondary');
        }

        // Precios
        const precio = parseDecimal(producto.precio);
        const descuento = parseDecimal(producto.descuento);

        $('#detalle-precio').text(`S/. ${formatPrice(precio)}`);

        if (descuento !== null && descuento > 0) {
            $('#detalle-descuento').text(`${formatPrice(descuento)}%`);
            const precioFinal = calcularPrecioFinal(precio, descuento);
            $('#detalle-precio-final').html(`
                <span class="price-original">S/. ${formatPrice(precio)}</span><br>
                <span class="price-final">S/. ${formatPrice(precioFinal)}</span>
            `);
        } else {
            $('#detalle-descuento').text('Sin descuento');
            $('#detalle-precio-final').text(`S/. ${formatPrice(precio)}`);
        }

        // Stock
        const stock = parseInt(producto.stock) || 0;
        const stockMinimo = parseInt(producto.stock_minimo || producto.stockMinimo) || 0;
        const stockBadge = $('#detalle-stock');
        let stockClass = 'bg-success';

        if (stock === 0) {
            stockClass = 'bg-danger';
        } else if (stock <= stockMinimo) {
            stockClass = 'bg-warning';
        }

        stockBadge.text(`${stock} unidades (mín: ${stockMinimo})`);
        stockBadge.removeClass('bg-success bg-warning bg-danger').addClass(stockClass);

        // ✅ GALERÍA DE IMÁGENES
        const imagenes = producto.imagenes || (producto.imagen ? [producto.imagen] : []);
        console.log('🖼️ Imágenes del producto:', imagenes);

        // Llamar a la función de galería
        initDetalleGaleria(imagenes);

        // Fechas
        $('#detalle-fecha-creacion').text(producto.fechaCreacion || 'No disponible');
        $('#detalle-fecha-actualizacion').text(producto.fechaActualizacion || 'No disponible');

        // Mostrar modal
        if (detalleModal) {
            detalleModal.show();
        }
    }


    /**
     * Limpiar formulario
     */
    function clearForm() {
        $('#formProducto')[0].reset();
        $('#formProducto .is-invalid').removeClass('is-invalid');
        $('.invalid-feedback').hide();
        $('#precioPreview').hide();
        $('#id').val('');

        // ✅ Verificar que la función existe
        if (typeof clearAllImagePreviews === 'function') {
            clearAllImagePreviews();
        }

        // ✅ Verificar que selectedFiles existe
        if (typeof selectedFiles !== 'undefined') {
            selectedFiles = [];
        }

        // ✅ Limpiar inputs de imagen
        const fileInput = document.getElementById('imagenFile');
        const urlInput = document.getElementById('imagenUrl');
        if (fileInput) fileInput.value = '';
        if (urlInput) urlInput.value = '';
    }


    /**
     * FUNCIONES DE UTILIDAD
     */

    /**
     * Mostrar notificación
     */
    function showNotification(message, type = 'info') {
        if (typeof Swal !== 'undefined') {
            const icon = type === 'success' ? 'success' : type === 'error' ? 'error' : 'info';
            Swal.fire({
                title: message,
                icon: icon,
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });
        } else {
            alert(message);
        }
    }

    /**
     * Mostrar loading
     */
    function showLoading(message = 'Procesando...') {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: message,
                allowOutsideClick: false,
                allowEscapeKey: false,
                showConfirmButton: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });
        }
    }

    /**
     * Ocultar loading
     */
    function hideLoading() {
        if (typeof Swal !== 'undefined') {
            Swal.close();
        }
    }

    /**
     * Mostrar error crítico
     */
    function showError(message) {
        console.error('💥 Error crítico:', message);
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: 'Error',
                text: message,
                icon: 'error',
                confirmButtonText: 'Entendido'
            });
        } else {
            alert('Error: ' + message);
        }
    }
}

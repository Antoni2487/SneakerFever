'use strict';
$(window).on('load', function() {
    setTimeout(initializeInventarioApp, 500);
});

function initializeInventarioApp() {
    console.log('=== INICIALIZANDO APLICACIÓN INVENTARIO ===');

    // Variables globales
    let dataTableMovimientos;
    let dataTableKardex;
    let dataTableStockBajo;
    let movimientoModal;
    let detalleMovimientoModal;
    let productosData = [];
    let stockActualProducto = 0;

    // Configuración de API
    const API_BASE = '/inventario/api';
    const API_PRODUCTOS = '/productos/api';

    const ENDPOINTS = {
        registrar: `${API_BASE}/registrar`,
        ultimos: `${API_BASE}/ultimos`,
        kardex: (id) => `${API_BASE}/kardex/${id}`,
        porTipo: (tipo) => `${API_BASE}/tipo/${tipo}`,
        productos: `${API_PRODUCTOS}/listar`,
        stockBajo: `${API_PRODUCTOS}/stock-bajo`
    };

    // Mapeos de tipos y motivos
    const TIPOS_MOVIMIENTO = {
        'ENTRADA': { badge: 'bg-success', icon: 'bi-arrow-up-circle', text: 'Entrada' },
        'SALIDA': { badge: 'bg-danger', icon: 'bi-arrow-down-circle', text: 'Salida' },
        'DEVOLUCION': { badge: 'bg-info', icon: 'bi-arrow-return-left', text: 'Devolución' },
        'MERMA': { badge: 'bg-warning', icon: 'bi-exclamation-triangle', text: 'Merma' }
    };

    const MOTIVOS_POR_TIPO = {
        'ENTRADA': [
            { value: 'COMPRA', text: 'Compra de mercadería' },
            { value: 'DEVOLUCION_CLIENTE', text: 'Devolución de cliente' },
            { value: 'AJUSTE_POSITIVO', text: 'Ajuste positivo' }
        ],
        'SALIDA': [
            { value: 'VENTA', text: 'Venta al cliente' },
            { value: 'AJUSTE_NEGATIVO', text: 'Ajuste negativo' }
        ],
        'DEVOLUCION': [
            { value: 'DEVOLUCION_CLIENTE', text: 'Devolución de cliente' }
        ],
        'MERMA': [
            { value: 'MERMA', text: 'Producto vencido/dañado' }
        ]
    };

    try {
        initializeModals();
        loadProductos();
        initializeDataTables();
        setupEventListeners();
        cargarEstadisticas();
        console.log('✅ Aplicación de inventario inicializada');
    } catch (error) {
        console.error('❌ Error inicializando inventario:', error);
        showError('Error al inicializar la aplicación', error.message);
    }

    // ===================== INICIALIZACIÓN =====================

    function initializeModals() {
        const movimientoModalEl = document.getElementById('movimientoModal');
        const detalleModalEl = document.getElementById('detalleMovimientoModal');

        if (!movimientoModalEl || !detalleModalEl) {
            throw new Error('Modales no encontrados');
        }

        if (typeof bootstrap === 'undefined') {
            throw new Error('Bootstrap no está cargado');
        }

        movimientoModal = new bootstrap.Modal(movimientoModalEl);
        detalleMovimientoModal = new bootstrap.Modal(detalleModalEl);

        // Limpiar formulario al cerrar modal
        movimientoModalEl.addEventListener('hidden.bs.modal', function() {
            resetFormMovimiento();
        });

        console.log('✅ Modales inicializados');
    }

    function initializeDataTables() {
        const tableMovimientos = $('#tablaMovimientos');
        const tableKardex = $('#tablaKardex');
        const tableStockBajo = $('#tablaStockBajo');

        if (tableMovimientos.length === 0) {
            throw new Error('Tabla #tablaMovimientos no encontrada');
        }

        // DataTable: Movimientos
        dataTableMovimientos = tableMovimientos.DataTable({
            responsive: true,
            processing: false,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.ultimos,
                dataSrc: function(json) {
                    console.log('📊 MOVIMIENTOS:', json.data);
                    return json.data || [];
                },
                error: function(xhr, error, thrown) {
                    console.error('Error en DataTables:', error, thrown);
                    showError('Error al cargar movimientos', 'No se pudieron cargar los datos.');
                }
            },
            columns: [
                { data: 'id', title: 'ID', width: '5%', className: 'text-center' },
                {
                    data: 'fechaMovimiento',
                    title: 'Fecha',
                    width: '12%',
                    render: function(data) {
                        return formatDateTime(data);
                    }
                },
                { data: 'productoNombre', title: 'Producto', width: '20%' },
                {
                    data: 'tipoMovimiento',
                    title: 'Tipo',
                    width: '10%',
                    className: 'text-center',
                    render: function(data) {
                        const tipo = TIPOS_MOVIMIENTO[data];
                        return `<span class="badge ${tipo.badge}">
                            <i class="bi ${tipo.icon} me-1"></i>${tipo.text}
                        </span>`;
                    }
                },
                {
                    data: 'motivo',
                    title: 'Motivo',
                    width: '15%',
                    render: function(data) {
                        return formatMotivo(data);
                    }
                },
                {
                    data: 'cantidad',
                    title: 'Cantidad',
                    width: '8%',
                    className: 'text-center',
                    render: function(data, type, row) {
                        const color = ['ENTRADA', 'DEVOLUCION'].includes(row.tipoMovimiento)
                            ? 'text-success' : 'text-danger';
                        const signo = ['ENTRADA', 'DEVOLUCION'].includes(row.tipoMovimiento)
                            ? '+' : '-';
                        return `<strong class="${color}">${signo}${data}</strong>`;
                    }
                },
                {
                    data: 'stockAnterior',
                    title: 'Stock Ant.',
                    width: '8%',
                    className: 'text-center'
                },
                {
                    data: 'stockNuevo',
                    title: 'Stock Nuevo',
                    width: '8%',
                    className: 'text-center',
                    render: function(data) {
                        return `<strong>${data}</strong>`;
                    }
                },
                { data: 'usuarioNombre', title: 'Usuario', width: '10%' },
                {
                    data: null,
                    title: 'Acciones',
                    width: '8%',
                    orderable: false,
                    className: 'text-center',
                    render: function(data, type, row) {
                        return `
                            <button class="btn btn-info btn-sm action-view-movimiento"
                                    data-id="${row.id}" title="Ver detalles">
                                <i class="bi bi-eye"></i>
                            </button>
                        `;
                    }
                }
            ],
            language: {
                processing: "Procesando...",
                lengthMenu: "Mostrar _MENU_ registros",
                zeroRecords: "No se encontraron movimientos",
                emptyTable: "No hay movimientos registrados",
                info: "Mostrando _START_ a _END_ de _TOTAL_ registros",
                infoEmpty: "Mostrando 0 a 0 de 0 registros",
                infoFiltered: "(filtrado de _MAX_ registros)",
                search: "Buscar:",
                paginate: {
                    first: "Primero",
                    last: "Último",
                    next: "Siguiente",
                    previous: "Anterior"
                }
            },
            pageLength: 10,
            order: [[0, 'desc']]
        });

        // DataTable: Kardex (se inicializa vacío)
        dataTableKardex = tableKardex.DataTable({
            responsive: true,
            paging: false,
            searching: false,
            info: false,
            columns: [
                {
                    data: 'fechaMovimiento',
                    title: 'Fecha',
                    render: function(data) {
                        return formatDateTime(data);
                    }
                },
                {
                    data: 'tipoMovimiento',
                    title: 'Tipo',
                    render: function(data) {
                        const tipo = TIPOS_MOVIMIENTO[data];
                        return `<span class="badge ${tipo.badge}">${tipo.text}</span>`;
                    }
                },
                {
                    data: 'motivo',
                    title: 'Motivo',
                    render: function(data) {
                        return formatMotivo(data);
                    }
                },
                {
                    data: 'cantidad',
                    className: 'text-center',
                    render: function(data, type, row) {
                        const color = ['ENTRADA', 'DEVOLUCION'].includes(row.tipoMovimiento)
                            ? 'text-success' : 'text-danger';
                        const signo = ['ENTRADA', 'DEVOLUCION'].includes(row.tipoMovimiento)
                            ? '+' : '-';
                        return `<strong class="${color}">${signo}${data}</strong>`;
                    }
                },
                { data: 'stockAnterior', className: 'text-center' },
                { data: 'stockNuevo', className: 'text-center' },
                {
                    data: 'referenciaTipo',
                    render: function(data, type, row) {
                        if (data === 'VENTA' && row.referenciaId) {
                            return `Venta #${row.referenciaId}`;
                        }
                        return data || '-';
                    }
                },
                { data: 'usuarioNombre' }
            ],
            language: {
                emptyTable: "Seleccione un producto para ver su kardex"
            }
        });

        // DataTable: Stock Bajo
        dataTableStockBajo = tableStockBajo.DataTable({
            responsive: true,
            processing: false,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.stockBajo,
                dataSrc: function(json) {
                    console.log('📊 STOCK BAJO:', json.data);
                    return json.data || [];
                },
                error: function(xhr, error, thrown) {
                    console.error('Error cargando stock bajo:', error);
                }
            },
            columns: [
                { data: 'id', width: '5%', className: 'text-center' },
                { data: 'nombre', width: '35%' },
                {
                    data: 'stock',
                    width: '15%',
                    className: 'text-center',
                    render: function(data) {
                        const badge = data === 0 ? 'bg-danger' : 'bg-warning';
                        return `<span class="badge ${badge}">${data}</span>`;
                    }
                },
                {
                    data: 'stockMinimo',
                    width: '15%',
                    className: 'text-center',
                    render: function(data) {
                        return `<span class="badge bg-info">${data || 0}</span>`;
                    }
                },
                {
                    data: 'estado',
                    width: '15%',
                    className: 'text-center',
                    render: function(data) {
                        return data === 1
                            ? '<span class="badge bg-success">Activo</span>'
                            : '<span class="badge bg-secondary">Inactivo</span>';
                    }
                },
                {
                    data: null,
                    width: '15%',
                    orderable: false,
                    className: 'text-center',
                    render: function(data, type, row) {
                        return `
                            <button class="btn btn-success btn-sm action-reponer"
                                    data-id="${row.id}"
                                    data-nombre="${escapeHtml(row.nombre)}"
                                    title="Registrar entrada">
                                <i class="bi bi-plus-circle me-1"></i>Reponer
                            </button>
                        `;
                    }
                }
            ],
            language: {
                zeroRecords: "No hay productos con stock bajo",
                emptyTable: "Todos los productos tienen stock adecuado"
            },
            order: [[2, 'asc']]
        });

        console.log('✅ DataTables inicializadas');
    }

    function setupEventListeners() {
        // Botón registrar movimiento
        $('#btnRegistrarMovimiento').on('click', openModalRegistrarMovimiento);

        // Formulario registrar movimiento
        $('#formMovimiento').on('submit', handleFormMovimientoSubmit);

        // Cambio de producto en formulario
        $('#productoId').on('change', handleProductoChange);

        // Cambio de tipo de movimiento
        $('#tipoMovimiento').on('change', handleTipoMovimientoChange);

        // Cambio de cantidad
        $('#cantidad').on('input', updatePreviewStock);

        // Filtros de tipo de movimiento
        $('.filter-tipo').on('click', handleFilterTipo);

        // Acciones de tabla movimientos
        $('#tablaMovimientos tbody').on('click', '.action-view-movimiento', handleViewMovimiento);

        // Buscar kardex
        $('#btnBuscarKardex').on('click', handleBuscarKardex);

        // Acción reponer stock
        $('#tablaStockBajo tbody').on('click', '.action-reponer', handleReponerStock);

        // Al activar tab de stock bajo, recargar datos
        $('#tab-stock').on('shown.bs.tab', function() {
            dataTableStockBajo.ajax.reload();
        });

        console.log('✅ Event listeners configurados');
    }

    // ===================== CARGA DE DATOS =====================

    function loadProductos() {
        console.log('🔄 Iniciando carga de productos...');

        $.ajax({
            url: ENDPOINTS.productos,
            method: 'GET',
            dataType: 'json',
            success: function(response) {
                console.log('📦 Respuesta del servidor:', response);

                // ✅ Manejar ambos formatos de respuesta
                let datos = null;
                
                if (response && response.success && Array.isArray(response.data)) {
                    // Formato: {success: true, data: [...]}
                    datos = response.data;
                } else if (response && Array.isArray(response.data)) {
                    // Formato: {data: [...], total: X}
                    datos = response.data;
                } else if (Array.isArray(response)) {
                    // Formato: [...]
                    datos = response;
                }

                if (datos) {
                    productosData = datos.filter(p => p.estado === 1);

                    console.log('✅ Productos activos cargados:', productosData.length);
                    console.log('📋 Productos:', productosData);

                    populateProductoSelects();

                    if (productosData.length === 0) {
                        console.warn('⚠️ No hay productos activos disponibles');
                        showNotification('No hay productos activos disponibles', 'warning');
                    }
                } else {
                    console.error('❌ Formato de respuesta inválido:', response);
                    showNotification('Error al cargar productos - formato inválido', 'error');
                    productosData = [];
                }
            },
            error: function(xhr, status, error) {
                console.error('❌ Error al cargar productos:', {
                    status: status,
                    error: error,
                    response: xhr.responseText
                });
                showNotification('Error al cargar productos: ' + error, 'error');
                productosData = [];
            }
        });
    }


    function populateProductoSelects() {
        const selectMovimiento = $('#productoId');
        const selectKardex = $('#buscarProductoKardex');

        console.log('🔧 Poblando selects con', productosData.length, 'productos');

        // Limpiar selects
        selectMovimiento.empty().append('<option value="">Seleccione un producto</option>');
        selectKardex.empty().append('<option value="">-- Seleccione un producto --</option>');

        if (!productosData || productosData.length === 0) {
            console.warn('⚠️ No hay productos para mostrar');
            selectMovimiento.append('<option value="" disabled>No hay productos disponibles</option>');
            selectKardex.append('<option value="" disabled>No hay productos disponibles</option>');
            return;
        }

        // Agregar opciones de productos
        productosData.forEach(producto => {
            try {
                // Validar que el producto tenga los campos necesarios
                if (!producto.id || !producto.nombre) {
                    console.warn('⚠️ Producto sin ID o nombre:', producto);
                    return;
                }

                // Asegurar que stock sea un número
                const stock = parseInt(producto.stock) || 0;

                // Escapar caracteres especiales en el nombre
                const nombreEscapado = escapeHtml(producto.nombre);

                // Crear opción con información completa
                const option = `<option value="${producto.id}"
                                       data-stock="${stock}"
                                       data-nombre="${nombreEscapado}"
                                       data-precio="${producto.precio || 0}"
                                       data-stock-minimo="${producto.stockMinimo || 0}">
                    ${nombreEscapado} (Stock: ${stock})
                </option>`;

                selectMovimiento.append(option);
                selectKardex.append(option);

                console.log('✅ Producto agregado:', producto.id, '-', producto.nombre);
            } catch (error) {
                console.error('❌ Error al procesar producto:', producto, error);
            }
        });

        console.log('✅ Selects poblados exitosamente');
    }

    function cargarEstadisticas() {
        // Cargar últimos movimientos para estadísticas del día
        $.ajax({
            url: ENDPOINTS.ultimos,
            method: 'GET',
            success: function(response) {
                if (response.success && response.data) {
                    calcularEstadisticas(response.data);
                }
            },
            error: function() {
                console.warn('No se pudieron cargar estadísticas');
            }
        });

        // Cargar productos con stock bajo
        $.ajax({
            url: ENDPOINTS.stockBajo,
            method: 'GET',
            success: function(response) {
                if (response.success && response.data) {
                    $('#stat-stock-bajo').text(response.data.length);
                }
            }
        });
    }

    function calcularEstadisticas(movimientos) {
        const hoy = new Date().toISOString().split('T')[0];

        const movimientosHoy = movimientos.filter(m => {
            const fechaMov = new Date(m.fechaMovimiento).toISOString().split('T')[0];
            return fechaMov === hoy;
        });

        const entradas = movimientosHoy.filter(m =>
            m.tipoMovimiento === 'ENTRADA' || m.tipoMovimiento === 'DEVOLUCION'
        ).length;

        const salidas = movimientosHoy.filter(m =>
            m.tipoMovimiento === 'SALIDA' || m.tipoMovimiento === 'MERMA'
        ).length;

        $('#stat-entradas').text(entradas);
        $('#stat-salidas').text(salidas);
        $('#stat-total').text(movimientosHoy.length);
    }

    // ===================== MODAL REGISTRAR MOVIMIENTO =====================

    function openModalRegistrarMovimiento() {
        resetFormMovimiento();
        if (movimientoModal) {
            movimientoModal.show();
        }
    }

    function resetFormMovimiento() {
        $('#formMovimiento')[0].reset();
        $('#formMovimiento .is-invalid').removeClass('is-invalid');
        $('#infoProducto').hide();
        $('#previewStock').hide();
        $('#motivo').empty().append('<option value="">Seleccione motivo</option>');
        stockActualProducto = 0;
    }

    function handleProductoChange() {
        const selectedOption = $(this).find('option:selected');
        const productoId = $(this).val();

        console.log('📦 Producto seleccionado:', productoId);

        if (!productoId) {
            $('#infoProducto').hide();
            $('#previewStock').hide();
            stockActualProducto = 0;
            return;
        }

        // Obtener stock desde los data attributes
        const stock = parseInt(selectedOption.data('stock')) || 0;
        const stockMinimo = parseInt(selectedOption.data('stock-minimo')) || 0;
        const nombre = selectedOption.data('nombre') || selectedOption.text();

        console.log('📊 Stock actual:', stock, '| Stock mínimo:', stockMinimo);

        stockActualProducto = stock;

        // Mostrar información del producto
        $('#stockActual').text(stock);

        // Agregar badge de alerta si el stock está bajo
        let badgeHtml = '';
        if (stock === 0) {
            badgeHtml = '<span class="badge bg-danger ms-2">Sin stock</span>';
        } else if (stock <= stockMinimo) {
            badgeHtml = '<span class="badge bg-warning ms-2">Stock bajo</span>';
        }

        $('#stockActual').html(`${stock} ${badgeHtml}`);

        $('#infoProducto').show();
        updatePreviewStock();

        console.log('✅ Información del producto actualizada');
    }
    // ===================== DEBUG Y VERIFICACIÓN =====================

    // Función para verificar el estado de los productos
    function debugProductos() {
        console.log('=== DEBUG PRODUCTOS ===');
        console.log('Total productos cargados:', productosData.length);
        console.log('Productos:', productosData);

        const selectMovimiento = $('#productoId');
        const selectKardex = $('#buscarProductoKardex');

        console.log('Opciones en select movimiento:', selectMovimiento.find('option').length);
        console.log('Opciones en select kardex:', selectKardex.find('option').length);

        // Verificar si los selects están en el DOM
        console.log('Select movimiento existe:', selectMovimiento.length > 0);
        console.log('Select kardex existe:', selectKardex.length > 0);

        return {
            productosData,
            selectsExisten: selectMovimiento.length > 0 && selectKardex.length > 0,
            opcionesMovimiento: selectMovimiento.find('option').length,
            opcionesKardex: selectKardex.find('option').length
        };
    }

    // Exponer función de debug
    window.debugInventarioProductos = debugProductos;

    function handleTipoMovimientoChange() {
        const tipo = $(this).val();
        const selectMotivo = $('#motivo');

        selectMotivo.empty().append('<option value="">Seleccione motivo</option>');

        if (tipo && MOTIVOS_POR_TIPO[tipo]) {
            MOTIVOS_POR_TIPO[tipo].forEach(motivo => {
                selectMotivo.append(`<option value="${motivo.value}">${motivo.text}</option>`);
            });
        }

        updatePreviewStock();
    }

    function updatePreviewStock() {
        const tipo = $('#tipoMovimiento').val();
        const cantidad = parseInt($('#cantidad').val()) || 0;

        if (tipo && cantidad > 0 && stockActualProducto >= 0) {
            let nuevoStock = stockActualProducto;

            if (tipo === 'ENTRADA' || tipo === 'DEVOLUCION') {
                nuevoStock += cantidad;
            } else if (tipo === 'SALIDA' || tipo === 'MERMA') {
                nuevoStock -= cantidad;
            }

            $('#nuevoStock').text(nuevoStock);
            $('#previewStock').show();

            // Advertencia si el stock será negativo
            if (nuevoStock < 0) {
                $('#previewStock').removeClass('alert-warning alert-success').addClass('alert-danger');
                $('#previewStock strong').text('⚠️ Stock insuficiente. Nuevo stock: ');
            } else if (nuevoStock === 0) {
                $('#previewStock').removeClass('alert-success alert-danger').addClass('alert-warning');
                $('#previewStock strong').text('⚠️ Stock quedará en cero. Nuevo stock: ');
            } else {
                $('#previewStock').removeClass('alert-warning alert-danger').addClass('alert-success');
                $('#previewStock strong').text('✓ Nuevo stock: ');
            }
        } else {
            $('#previewStock').hide();
        }
    }

    async function handleFormMovimientoSubmit(e) {
        e.preventDefault();

        const form = $('#formMovimiento')[0];

        if (!form.checkValidity()) {
            e.stopPropagation();
            form.classList.add('was-validated');
            return;
        }

        const formData = {
            productoId: parseInt($('#productoId').val()),
            tipoMovimiento: $('#tipoMovimiento').val(),
            cantidad: parseInt($('#cantidad').val()),
            motivo: $('#motivo').val(),
            observaciones: $('#observaciones').val() || null,
            referenciaId: null,
            referenciaTipo: 'NINGUNO'
        };

        console.log('📦 Datos a enviar:', formData);

        showLoading('Registrando movimiento...');

        try {
            const response = await fetch(ENDPOINTS.registrar, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            const data = await response.json();

            if (data.success) {
                hideLoading();
                showNotification(data.message || 'Movimiento registrado exitosamente', 'success');
                movimientoModal.hide();
                dataTableMovimientos.ajax.reload();
                loadProductos(); // Recargar productos para actualizar stocks
                cargarEstadisticas();
            } else {
                throw new Error(data.message || 'Error al registrar movimiento');
            }
        } catch (error) {
            hideLoading();
            console.error('Error:', error);
            showNotification(error.message, 'error');
        }
    }

    // ===================== FILTROS =====================

    function handleFilterTipo(e) {
        e.preventDefault();

        $('.filter-tipo').removeClass('active');
        $(this).addClass('active');

        const tipo = $(this).data('tipo');

        if (tipo === 'TODOS') {
            dataTableMovimientos.ajax.url(ENDPOINTS.ultimos).load();
        } else {
            dataTableMovimientos.ajax.url(ENDPOINTS.porTipo(tipo)).load();
        }
    }

    // ===================== VER DETALLE MOVIMIENTO =====================

    function handleViewMovimiento() {
        const id = $(this).data('id');
        const rowData = dataTableMovimientos.row($(this).closest('tr')).data();

        if (rowData) {
            mostrarDetalleMovimiento(rowData);
        }
    }

    function mostrarDetalleMovimiento(movimiento) {
        $('#detalle-id').text(movimiento.id);
        $('#detalle-fecha').text(formatDateTime(movimiento.fechaMovimiento));
        $('#detalle-producto').text(movimiento.productoNombre);

        const tipo = TIPOS_MOVIMIENTO[movimiento.tipoMovimiento];
        $('#detalle-tipo').html(`<span class="badge ${tipo.badge}">${tipo.text}</span>`);

        $('#detalle-motivo').text(formatMotivo(movimiento.motivo));

        const color = ['ENTRADA', 'DEVOLUCION'].includes(movimiento.tipoMovimiento)
            ? 'text-success' : 'text-danger';
        const signo = ['ENTRADA', 'DEVOLUCION'].includes(movimiento.tipoMovimiento)
            ? '+' : '-';
        $('#detalle-cantidad').html(`<span class="badge ${color === 'text-success' ? 'bg-success' : 'bg-danger'}">${signo}${movimiento.cantidad}</span>`);

        $('#detalle-stock-anterior').text(movimiento.stockAnterior);
        $('#detalle-stock-nuevo').text(movimiento.stockNuevo);
        $('#detalle-usuario').text(movimiento.usuarioNombre);

        let referenciaText = '-';
        if (movimiento.referenciaTipo === 'VENTA' && movimiento.referenciaId) {
            referenciaText = `Venta #${movimiento.referenciaId}`;
        } else if (movimiento.referenciaTipo !== 'NINGUNO') {
            referenciaText = movimiento.referenciaTipo;
        }
        $('#detalle-referencia').text(referenciaText);

        $('#detalle-observaciones').text(movimiento.observaciones || 'Sin observaciones');

        detalleMovimientoModal.show();
    }

    // ===================== KARDEX =====================

    function handleBuscarKardex() {
        const productoId = $('#buscarProductoKardex').val();

        if (!productoId) {
            showNotification('Debe seleccionar un producto', 'warning');
            return;
        }

        showLoading('Cargando kardex...');

        $.ajax({
            url: ENDPOINTS.kardex(productoId),
            method: 'GET',
            success: function(response) {
                hideLoading();

                if (response.success && response.data) {
                    const producto = productosData.find(p => p.id == productoId);

                    if (producto) {
                        $('#kardexProductoInfo').html(`
                            <strong>Producto:</strong> ${producto.nombre} |
                            <strong>Stock Actual:</strong> ${producto.stock} unidades |
                            <strong>Total de movimientos:</strong> ${response.data.length}
                        `);
                    }

                    dataTableKardex.clear();
                    dataTableKardex.rows.add(response.data);
                    dataTableKardex.draw();

                    $('#kardexResults').show();
                } else {
                    showNotification('No se encontraron movimientos para este producto', 'info');
                }
            },
            error: function() {
                hideLoading();
                showNotification('Error al cargar kardex', 'error');
            }
        });
    }

    // ===================== REPONER STOCK =====================

    function handleReponerStock() {
        const productoId = $(this).data('id');
        const productoNombre = $(this).data('nombre');

        // Abrir modal con datos precargados
        resetFormMovimiento();
        $('#productoId').val(productoId).trigger('change');
        $('#tipoMovimiento').val('ENTRADA').trigger('change');

        movimientoModal.show();

        showNotification(`Reponiendo stock de: ${productoNombre}`, 'info');
    }

    // ===================== UTILIDADES =====================

    function formatDateTime(dateTime) {
        if (!dateTime) return '-';
        const date = new Date(dateTime);
        return date.toLocaleString('es-PE', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function formatMotivo(motivo) {
        const motivos = {
            'COMPRA': 'Compra',
            'VENTA': 'Venta',
            'AJUSTE_FISICO': 'Ajuste físico',
            'AJUSTE_POSITIVO': 'Ajuste positivo',
            'AJUSTE_NEGATIVO': 'Ajuste negativo',
            'MERMA': 'Merma',
            'DEVOLUCION_CLIENTE': 'Devolución cliente'
        };
        return motivos[motivo] || motivo;
    }

    function showNotification(message, type = 'info') {
        if (typeof Swal !== 'undefined') {
            const icon = type === 'success' ? 'success' : type === 'error' ? 'error' :
                         type === 'warning' ? 'warning' : 'info';
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

    function hideLoading() {
        if (typeof Swal !== 'undefined') {
            Swal.close();
        }
    }

    function showError(title, message) {
        if (typeof Swal !== 'undefined') {
            Swal.fire({
                title: title,
                text: message,
                icon: 'error',
                confirmButtonText: 'Entendido'
            });
        } else {
            alert(title + ': ' + message);
        }
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    // Exponer API pública
    window.InventarioApp = {
        reload: () => {
            dataTableMovimientos.ajax.reload();
            cargarEstadisticas();
        },
        getDataTable: () => dataTableMovimientos
    };

    console.log('✅ inventario.js inicializado correctamente');
}
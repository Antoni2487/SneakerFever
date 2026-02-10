'use strict';
$(window).on('load', function() {
    setTimeout(initializeVentasApp, 500);
});

function initializeVentasApp() {
    console.log('=== INICIALIZANDO APLICACIÓN VENTAS ===');

    let dataTable;
    let ventaModal;
    let detallesVenta = [];
    let clienteSeleccionado = null;
    let subtotalVenta = 0;
    let productosSeleccionadosEnModal = [];
    let totalVentaSinInteres = 0;

    // Constantes de validación
    const VALIDATION = {
        CANTIDAD_MIN: 1,
        PRECIO_MIN: 0.01,
        DESCUENTO_MAX: 100
    };

    // ===================== CONFIGURACIÓN DE ENDPOINTS =====================
    const API_BASE = '/ventas/api';

    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        create: `${API_BASE}/crear`,
        get: (id) => `${API_BASE}/${id}`,
        anular: (id) => `${API_BASE}/anular/${id}`,
        estado: (id) => `${API_BASE}/estado/${id}`,
        cliente: (id) => `${API_BASE}/cliente/${id}`,
        reporte: `${API_BASE}/reporte`,
        clientes: '/clientes/api/listar',
        productosDisponibles: '/productos/api/listarDisponibles'
    };

    try {
        initializeModals();
        initializeDataTable();
        setupEventListeners();
        actualizarEstadisticasVentas();
        console.log('✅ Aplicación de ventas inicializada');
    } catch (error) {
        console.error('❌ Error inicializando ventas:', error);
        showError('Error al inicializar la aplicación', error.message);
    }

    // ===================== INICIALIZACIÓN =====================

    

    function initializeModals() {
        const ventaModalEl = document.getElementById('ventaModal');
        if (!ventaModalEl) {
            throw new Error('No se encontró el modal #ventaModal');
        }
        if (typeof bootstrap === 'undefined') {
            throw new Error('Bootstrap no está cargado');
        }
        ventaModal = new bootstrap.Modal(ventaModalEl);

        ventaModalEl.addEventListener('hidden.bs.modal', function() {
            resetForm();
        });
    }

    function initializeDataTable() {
        const tableElement = $('#tablaVentas');
        if (tableElement.length === 0) {
            throw new Error('No se encontró #tablaVentas');
        }

        dataTable = tableElement.DataTable({
            responsive: true,
            processing: false,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(json) {
                    console.log('📊 DATOS VENTAS:', json.data);
                    return json.data || [];
                },
                error: function(xhr, error, thrown) {
                    console.error('Error en DataTables:', error, thrown);
                    showError('Error al cargar las ventas',
                        'No se pudieron cargar los datos. Por favor, recargue la página.');
                }
            },
            columns: [
                {
                    data: 'id',
                    title: 'ID',
                    width: '5%',
                    className: 'text-center'
                },
                {
                    data: 'comprobante_completo',
                    title: 'Comprobante',
                    width: '12%',
                    className: 'text-center',
                    render: function(data) {
                        return escapeHtml(data);
                    }
                },
                {
                    data: 'cliente_nombre',
                    title: 'Cliente',
                    render: function(data, type, row) {
                        return `${escapeHtml(data)}<br><small class="text-muted">${escapeHtml(row.cliente_documento)}</small>`;
                    }
                },
                {
                    data: 'forma_pago',
                    title: 'Forma Pago',
                    width: '10%',
                    className: 'text-center',
                    render: function(data) {
                        return data === 'CONTADO'
                            ? '<span class="badge bg-success">Contado</span>'
                            : '<span class="badge bg-warning">Crédito</span>';
                    }
                },
                {
                    data: 'total',
                    title: 'Total',
                    width: '10%',
                    className: 'text-end',
                    render: function(data) {
                        return `S/ ${parseFloat(data).toFixed(2)}`;
                    }
                },
                {
                    data: 'estado',
                    title: 'Estado',
                    width: '10%',
                    className: 'text-center',
                    render: function(data) {
                        const badges = {
                            'PENDIENTE': '<span class="badge bg-warning">Pendiente</span>',
                            'CONFIRMADA': '<span class="badge bg-info">Confirmada</span>',
                            'EN_PROCESO': '<span class="badge bg-primary">En Proceso</span>',
                            'ENTREGADA': '<span class="badge bg-success">Entregada</span>',
                            'ANULADA': '<span class="badge bg-danger">Anulada</span>'
                        };
                        return badges[data] || data;
                    }
                },
                {
                    data: 'fecha_creacion',
                    title: 'Fecha',
                    width: '12%',
                    className: 'text-center',
                    render: function(data) {
                        return formatDateTime(data);
                    }
                },
                {
                    data: null,
                    title: 'Acciones',
                    width: '12%',
                    className: 'text-center',
                    orderable: false,
                    render: function(data, type, row) {
                        return createActionButtons(row);
                    }
                }
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
            order: [[0, 'desc']]
        });
    }

    function createActionButtons(row) {
        const isAnulada = row.estado === 'ANULADA';

        let buttons = `
            <div class="d-flex justify-content-center align-items-center gap-1">
                <button class="btn btn-info btn-sm action-view"
                        data-id="${row.id}"
                        title="Ver detalles">
                    <i class="bi bi-eye"></i>
                </button>`;

        if (!isAnulada) {
            buttons += `
                <button class="btn btn-danger btn-sm action-anular"
                        data-id="${row.id}"
                        title="Anular venta">
                    <i class="bi bi-x-circle"></i>
                </button>`;
        }

        buttons += `</div>`;
        return buttons;
    }

    function setupEventListeners() {
        // Botón nueva venta
        $('#btnNuevaVenta').on('click', openModalForNew);

        // Formulario
        $('#formVenta').on('submit', handleFormSubmit);

        // Buscar cliente
        $('#buscarCliente').on('click', buscarCliente);

        // Buscar y agregar productos - abre modal
        $('#btnBuscarProductos').on('click', function() {
            productosSeleccionadosEnModal = []; // Limpiar lista temporal
            $('#modalProductos').modal('show');
        });

        // Calcular totales al cambiar descuento general
        $('#descuento_general').on('input', calcularTotales);

        // Recalcular cronograma al cambiar campos de crédito
        $('#numero_cuotas, #intervalo_cuotas, #interes_porcentaje, #monto_inicial').on('input change', function() {
            if ($('#forma_pago').val() === 'CREDITO') {
                calcularTotales(); // Esto llamará a generarCronogramaPagos()
            }
        });

        // Acciones de la tabla
        $('#tablaVentas tbody')
            .on('click', '.action-view', handleView)
            .on('click', '.action-anular', handleAnular)
            .on('click', '.action-credito', handleVerCredito);

        // Eliminar detalle
        $(document).on('click', '.btn-eliminar-detalle', eliminarDetalleProducto);

        // Editar cantidad en tabla de detalles
        $(document).on('change', '.input-cantidad-detalle', actualizarCantidadDetalle);

        // Mostrar/ocultar sección de crédito
        $('#forma_pago').on('change', function() {
            if ($(this).val() === 'CREDITO') {
                $('.seccion-credito').show();
            } else {
                $('.seccion-credito').hide();
            }
        });

        // Modal de productos - cargar al abrir
        $('#modalProductos').on('shown.bs.modal', cargarProductosDisponibles);

        // Seleccionar producto del modal (MÚLTIPLES)
        $(document).on('click', '.seleccionar-producto', agregarAListaTemporal);

        // Confirmar productos seleccionados en el modal
        $('#btnConfirmarProductos').on('click', confirmarAgregarProductos);
        $('#tipo_comprobante').on('change', function() {
            const tipo = $(this).val();
            if (!tipo) {
                $('#serie').val('');
                $('#numero_venta').val('');
                return;
            }

            $.ajax({
                url: `/ventas/api/series/activas?tipo_comprobante=${tipo}`,
                method: 'GET',
                success: function(response) {
                    if (response.success && response.data) {
                        $('#serie').val(response.data.serie);
                        $('#numero_venta').val(response.data.numero_actual + 1); // muestra el siguiente correlativo
                    } else {
                        $('#serie').val('');
                        $('#numero_venta').val('');
                        Swal.fire('Sin series activas', response.message || 'No se encontró serie activa', 'warning');
                    }
                },
                error: function(xhr) {
                    console.error('Error al cargar serie activa:', xhr);
                    $('#serie').val('');
                    $('#numero_venta').val('');
                    Swal.fire('Error', 'No se pudo obtener la serie activa', 'error');
                }
            });
        });
    }

    // ===================== MANEJO DE MODAL =====================

    function openModalForNew() {
        resetForm();
        $('#modalTitle').text('Nueva Venta');
        ventaModal.show();
    }

    function resetForm() {
        $('#formVenta')[0].reset();
        detallesVenta = [];
        clienteSeleccionado = null;
        subtotalVenta = 0;
        productosSeleccionadosEnModal = [];
        renderizarDetalles();
        calcularTotales();
        $('#clienteSeleccionado').html('');
        $('#numeroProductosSeleccionados').text('0'); // ✅ Resetear contador
        $('#cronogramaPagos').html('<p class="text-muted text-center">Configure el crédito para ver el cronograma</p>'); // ✅ Limpiar cronograma
        $('.seccion-credito').hide();
    }

    // ===================== BUSCAR CLIENTE =====================

    function buscarCliente() {
        const documento = $('#documento_cliente').val().trim();

        if (!documento) {
            showNotification('Ingrese un documento', 'warning');
            return;
        }

        // Validar formato
        if (!/^[0-9]{8}$|^[0-9]{11}$/.test(documento)) {
            showNotification('El documento debe ser DNI (8 dígitos) o RUC (11 dígitos)', 'warning');
            return;
        }

        // Primero buscar en BD local
        $.ajax({
            url: `/clientes/api/documento/${documento}`,
            method: 'GET',
            success: function(response) {
                if (response.success && response.data) {
                    // Cliente encontrado en BD
                    clienteSeleccionado = response.data;
                    mostrarClienteSeleccionado(response.data, false);
                } else {
                    // No existe en BD, consultar API externa
                    consultarClienteEnAPI(documento);
                }
            },
            error: function() {
                // Error en búsqueda local, intentar con API externa
                consultarClienteEnAPI(documento);
            }
        });
    }

    // Nueva función: Consultar en API externa
    function consultarClienteEnAPI(documento) {
        showNotification('Consultando en RENIEC/SUNAT...', 'info');

        $.ajax({
            url: `/clientes/api/consultar-documento/${documento}`,
            method: 'GET',
            success: function(response) {
                if (response.success && response.nombre) {
                    // Datos encontrados en API, crear objeto temporal
                    clienteSeleccionado = {
                        id: null, // No tiene ID aún (se creará al guardar la venta)
                        documento: documento,
                        nombre: response.nombre,
                        telefono: '000000000',
                        correo: null
                    };
                    mostrarClienteSeleccionado(clienteSeleccionado, true);
                } else {
                    showNotification(response.message || 'No se encontraron datos del documento', 'error');
                    $('#clienteSeleccionado').html(`
                        <div class="alert alert-warning">
                            <i class="bi bi-exclamation-triangle me-2"></i>
                            No se encontró información. Puede registrar el cliente manualmente en <a href="/clientes">Gestión de Clientes</a>
                        </div>
                    `);
                }
            },
            error: function() {
                showNotification('Error al consultar la API externa', 'error');
            }
        });
    }

    // Nueva función: Mostrar cliente seleccionado
    function mostrarClienteSeleccionado(cliente, esNuevo) {
        const badge = esNuevo
            ? '<span class="badge bg-success ms-2">NUEVO</span>'
            : '<span class="badge bg-info ms-2">EXISTENTE</span>';

        $('#clienteSeleccionado').html(`
            <div class="alert alert-success">
                <strong>${escapeHtml(cliente.nombre)}</strong> ${badge}<br>
                <small>
                    Doc: ${escapeHtml(cliente.documento)} |
                    Tel: ${escapeHtml(cliente.telefono || 'N/A')}
                    ${esNuevo ? ' | <em>Se creará automáticamente al guardar la venta</em>' : ''}
                </small>
            </div>
        `);

        // 🚀 VALIDACIÓN SUNAT: Bloquear Factura si es DNI
        const documento = cliente.documento.trim();
        const selectTipo = $('#tipo_comprobante');
        
        // Resetear estados primero (habilitar todo)
        selectTipo.find('option').prop('disabled', false);

        if (documento.length === 8) {
            // Es DNI -> Bloquear FACTURA y forzar BOLETA
            selectTipo.find('option[value="FACTURA"]').prop('disabled', true);
            selectTipo.val('BOLETA').trigger('change');
            
            showNotification('ℹ️ Clientes con DNI solo pueden recibir BOLETA', 'info');
        } else if (documento.length === 11) {
            // Es RUC -> Permitir FACTURA (y sugerirla por defecto)
            selectTipo.val('FACTURA').trigger('change');
        }
    }

    // ===================== MANEJO DE PRODUCTOS =====================

    function cargarProductosDisponibles() {
        $.ajax({
            url: ENDPOINTS.productosDisponibles,
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                const tbody = $('#tablaProductosModal tbody');
                tbody.empty();

                if (!data || data.length === 0) {
                    tbody.append(`
                        <tr>
                            <td colspan="6" class="text-center">No hay productos disponibles</td>
                        </tr>
                    `);
                    return;
                }

                data.forEach(prod => {
                    // Construir URL completa de la imagen
                    let imagenUrl = prod.imagen || '';

                    // Si la imagen existe y no es una URL completa, agregar el contexto
                    if (imagenUrl && !imagenUrl.startsWith('http')) {
                        imagenUrl = imagenUrl; // Ya viene con /uploads/productos/
                    }

                    // Fallback si no hay imagen
                    if (!imagenUrl) {
                        imagenUrl = '/admin/img/no-image.png'; // Ajusta esta ruta según tu proyecto
                    }

                    const codigo = escapeHtml(prod.codigo || '');
                    const nombre = escapeHtml(prod.nombre || '');
                    const stock = parseInt(prod.stock) || 0;
                    const precio = parseFloat(prod.precio) || 0;

                    tbody.append(`
                        <tr>
                            <td>
                                <img src="${imagenUrl}" alt="${nombre}"
                                     class="img-thumbnail"
                                     style="width:60px;height:60px;object-fit:cover;"
                                     onerror="this.src='/admin/img/placeholder.png'; this.onerror=null;">
                            </td>
                            <td>${codigo}</td>
                            <td>${nombre}</td>
                            <td class="text-center">${stock}</td>
                            <td class="text-end">S/ ${precio.toFixed(2)}</td>
                            <td class="text-center">
                                <button class="btn btn-sm btn-success seleccionar-producto"
                                        data-id="${prod.id}"
                                        data-nombre="${nombre}"
                                        data-precio="${precio}"
                                        data-stock="${stock}"
                                        data-codigo="${codigo}"
                                        data-descuento="${prod.descuento || 0}">
                                    <i class="bi bi-check-circle"></i> Seleccionar
                                </button>
                            </td>
                        </tr>
                    `);
                });
            },
            error: function(xhr, status, error) {
                console.error('Error al cargar productos:', error);
                showNotification('Error al cargar productos disponibles', 'error');
            }
        });
    }
    // ===================== SELECCIÓN MÚLTIPLE DE PRODUCTOS =====================

    function agregarAListaTemporal() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        const codigo = $(this).data('codigo');
        const precio = $(this).data('precio');
        const stock = $(this).data('stock');
        const descuento = $(this).data('descuento') || 0;

        // Validar si ya está en la lista temporal
        const yaExiste = productosSeleccionadosEnModal.some(p => p.producto_id === id);
        if (yaExiste) {
            showNotification('Este producto ya está seleccionado', 'warning');
            return;
        }

        // Agregar al array temporal (SIN cantidad)
        productosSeleccionadosEnModal.push({
            producto_id: id,
            producto_nombre: nombre,
            codigo: codigo,
            precio: precio,
            descuento: descuento,
            stock_disponible: stock
        });

        // Renderizar lista temporal
        renderizarListaTemporal();
        showNotification(`✓ ${nombre} agregado`, 'success');
    }

    function renderizarListaTemporal() {
        const contador = productosSeleccionadosEnModal.length;
        $('#numeroProductosSeleccionados').text(contador);

        if (contador > 0) {
            $('#contadorProductosSeleccionados').removeClass('alert-info').addClass('alert-success');
        } else {
            $('#contadorProductosSeleccionados').removeClass('alert-success').addClass('alert-info');
        }
    }

    function confirmarAgregarProductos() {
        if (productosSeleccionadosEnModal.length === 0) {
            showNotification('No hay productos seleccionados', 'warning');
            return;
        }

        // Agregar todos los productos a la tabla con cantidad 1
        productosSeleccionadosEnModal.forEach(prod => {
            // Validar si ya existe en detallesVenta
            const yaEnDetalle = detallesVenta.some(d => d.producto_id === prod.producto_id);
            if (yaEnDetalle) {
                showNotification(`${prod.producto_nombre} ya está en la venta`, 'warning');
                return;
            }

            // Calcular subtotal (con descuento si existe)
            const montoDescuento = prod.precio * (prod.descuento / 100);
            const precioConDescuento = prod.precio - montoDescuento;
            const subtotal = precioConDescuento * 1; // cantidad = 1

            detallesVenta.push({
                producto_id: prod.producto_id,
                producto_nombre: prod.producto_nombre,
                cantidad: 1, // ✅ Cantidad inicial = 1
                precio_unitario: prod.precio,
                descuento_porcentaje: prod.descuento,
                subtotal: subtotal,
                stock_disponible: prod.stock_disponible // Para validar después
            });
        });

        // Limpiar y cerrar modal
        productosSeleccionadosEnModal = [];
        $('#modalProductos').modal('hide');

        // Actualizar tabla
        renderizarDetalles();
        calcularTotales();

        showNotification('Productos agregados correctamente', 'success');
    }

    // Event listener para quitar producto de lista temporal
    $(document).on('click', '.btn-quitar-temporal', function() {
        const index = $(this).data('index');
        const nombre = productosSeleccionadosEnModal[index].producto_nombre;
        productosSeleccionadosEnModal.splice(index, 1);
        renderizarListaTemporal();
        showNotification(`${nombre} quitado de la selección`, 'info');
    });
    function actualizarEstadisticasVentas() {
            $.ajax({
                url: '/ventas/api/estadisticas',
                method: 'GET',
                success: function(response) {
                    if (response.success && response.data) {
                        $('#ventas-hoy').text('S/ ' + parseFloat(response.data.ventasHoy || 0).toFixed(2));
                        $('#ventas-semana').text('S/ ' + parseFloat(response.data.ventasSemana || 0).toFixed(2));
                        $('#ventas-mes').text('S/ ' + parseFloat(response.data.ventasMes || 0).toFixed(2));
                        $('#ventas-total').text(response.data.totalVentas || 0);
                    }
                },
                error: function(xhr, status, error) {
                    console.error('Error al cargar estadísticas de ventas:', error);
                    // Mantener valores en 0 si hay error
                    $('#ventas-hoy').text('S/ 0.00');
                    $('#ventas-semana').text('S/ 0.00');
                    $('#ventas-mes').text('S/ 0.00');
                    $('#ventas-total').text('0');
                }
            });
        }

    // ===================== MANEJO DE DETALLES =====================


    function eliminarDetalleProducto() {
        const index = $(this).data('index');
        detallesVenta.splice(index, 1);
        renderizarDetalles();
        calcularTotales();
        showNotification('Producto eliminado', 'info');
    }

    function actualizarCantidadDetalle() {
        const index = $(this).data('index');
        const nuevaCantidad = parseInt($(this).val()) || 1;
        const detalle = detallesVenta[index];

        // Validar stock
        if (nuevaCantidad > detalle.stock_disponible) {
            showNotification(`Stock insuficiente. Máximo: ${detalle.stock_disponible}`, 'error');
            $(this).val(detalle.cantidad); // Restaurar valor anterior
            return;
        }

        if (nuevaCantidad < 1) {
            $(this).val(1);
            return;
        }

        // Actualizar cantidad y recalcular subtotal
        detalle.cantidad = nuevaCantidad;

        const montoDescuento = detalle.precio_unitario * (detalle.descuento_porcentaje / 100);
        const precioConDescuento = detalle.precio_unitario - montoDescuento;
        detalle.subtotal = precioConDescuento * nuevaCantidad;

        // Re-renderizar y recalcular
        renderizarDetalles();
        calcularTotales();
    }

    function renderizarDetalles() {
        const tbody = $('#detallesVentaBody');
        tbody.empty();

        if (detallesVenta.length === 0) {
            tbody.html('<tr><td colspan="6" class="text-center">No hay productos agregados</td></tr>');
            return;
        }

        detallesVenta.forEach((detalle, index) => {
            // Mostrar descuento como "-" si es 0
            const descuentoTexto = detalle.descuento_porcentaje > 0
                ? `${detalle.descuento_porcentaje.toFixed(2)}%`
                : '-';

            tbody.append(`
                <tr>
                    <td>${index + 1}</td>
                    <td>${escapeHtml(detalle.producto_nombre)}</td>
                    <td class="text-center">
                        <input type="number"
                               class="form-control form-control-sm text-center input-cantidad-detalle"
                               value="${detalle.cantidad}"
                               min="1"
                               max="${detalle.stock_disponible || 999}"
                               data-index="${index}"
                               style="width: 80px; display: inline-block;">
                    </td>
                    <td class="text-end">S/ ${detalle.precio_unitario.toFixed(2)}</td>
                    <td class="text-center">${descuentoTexto}</td>
                    <td class="text-end">S/ ${detalle.subtotal.toFixed(2)}</td>
                    <td class="text-center">
                        <button type="button" class="btn btn-danger btn-sm btn-eliminar-detalle" data-index="${index}">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                </tr>
            `);
        });
    }
    // ✅ 1. NUEVA FUNCIÓN UTILITARIA (Pégala al inicio de initializeVentasApp)
    function roundMoney(amount) {
        return Math.round((amount + Number.EPSILON) * 100) / 100;
    }

    function calcularTotales() {
        // Calcular subtotal
        subtotalVenta = detallesVenta.reduce((sum, detalle) => sum + detalle.subtotal, 0);

        // Descuento general
        const descuentoGeneral = parseFloat($('#descuento_general').val()) || 0;
        const montoDescuentoGeneral = subtotalVenta * (descuentoGeneral / 100);

        // ✅ Total SIN IGV y SIN INTERÉS (este es el total real de la venta)
        totalVentaSinInteres = subtotalVenta - montoDescuentoGeneral;

        // Mostrar subtotal y descuento
        $('#subtotalDisplay').text(`S/ ${subtotalVenta.toFixed(2)}`);
        $('#descuentoDisplay').text(`S/ ${montoDescuentoGeneral.toFixed(2)}`);

        // Si es crédito, calcular con interés
        if ($('#forma_pago').val() === 'CREDITO') {
            const montoInicial = parseFloat($('#monto_inicial').val()) || 0;
            const interesPorcentaje = parseFloat($('#interes_porcentaje').val()) || 0;

            // Calcular saldo a financiar
            const saldoAFinanciar = totalVentaSinInteres - montoInicial;

            // Calcular interés sobre el saldo
            const montoInteres = saldoAFinanciar * (interesPorcentaje / 100);
            const saldoConInteres = saldoAFinanciar + montoInteres;

            // Total real = monto inicial + saldo con interés
            const totalConInteres = montoInicial + saldoConInteres;

            // Mostrar el total con información detallada y mejorada visualmente
            $('#totalDisplay').html(`
                <div class="card border-0 bg-light">
                    <div class="card-body p-3">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <span class="text-muted">Total venta:</span>
                            <span class="fw-bold fs-6">S/ ${totalVentaSinInteres.toFixed(2)}</span>
                        </div>

                        ${montoInicial > 0 ? `
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted">
                                    <i class="bi bi-cash-coin me-1"></i>Monto inicial:
                                </span>
                                <span class="text-primary fw-semibold">S/ ${montoInicial.toFixed(2)}</span>
                            </div>
                        ` : ''}

                        ${interesPorcentaje > 0 ? `
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted">
                                    <i class="bi bi-percent me-1"></i>Interés (${interesPorcentaje}%):
                                </span>
                                <span class="text-warning fw-semibold">+ S/ ${montoInteres.toFixed(2)}</span>
                            </div>
                        ` : ''}

                        ${(montoInicial > 0 || interesPorcentaje > 0) ? `
                            <div class="d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom">
                                <span class="text-muted">
                                    <i class="bi bi-calendar-check me-1"></i>Saldo a financiar:
                                </span>
                                <span class="text-info fw-semibold">S/ ${saldoConInteres.toFixed(2)}</span>
                            </div>
                        ` : ''}

                        <div class="d-flex justify-content-between align-items-center mt-3 pt-2 border-top border-2">
                            <span class="text-dark fw-bold fs-5">
                                <i class="bi bi-currency-dollar me-1"></i>TOTAL A PAGAR:
                            </span>
                            <span class="text-success fw-bold fs-4">S/ ${totalConInteres.toFixed(2)}</span>
                        </div>
                    </div>
                </div>
            `);

            generarCronogramaPagos(totalVentaSinInteres); // ✅ CORREGIDO
        } else {
            // Si es contado, mostrar solo el total con estilo mejorado
            $('#totalDisplay').html(`
                <div class="text-end">
                    <span class="fs-4 fw-bold text-success">S/ ${totalVentaSinInteres.toFixed(2)}</span>
                </div>
            `);
        }
    }

    function generarCronogramaPagos(totalVenta) {
        const numeroCuotas = parseInt($('#numero_cuotas').val()) || 1;
        const intervalo = $('#intervalo_cuotas').val();
        const interesPorcentaje = parseFloat($('#interes_porcentaje').val()) || 0;
        const montoInicial = parseFloat($('#monto_inicial').val()) || 0;

        // Validar que haya detalles
        if (detallesVenta.length === 0) {
            $('#cronogramaPagos').html('<p class="text-muted text-center">Agregue productos para calcular el cronograma</p>');
            return;
        }

        // Saldo a financiar
        const saldoFinanciar = totalVenta - montoInicial;

        if (saldoFinanciar <= 0) {
            $('#cronogramaPagos').html('<p class="text-success text-center"><i class="bi bi-check-circle"></i> El monto inicial cubre el total</p>');
            return;
        }

        // Calcular monto con interés
        const montoConInteres = saldoFinanciar * (1 + (interesPorcentaje / 100));
        const montoPorCuota = montoConInteres / numeroCuotas;

        // Generar fechas
        const hoy = new Date();
        let html = `
            <table class="table table-sm table-bordered mb-0">
                <thead class="table-warning">
                    <tr>
                        <th class="text-center">Cuota</th>
                        <th class="text-center">Fecha Vencimiento</th>
                        <th class="text-end">Monto</th>
                    </tr>
                </thead>
                <tbody>
        `;

        // Calcular días según intervalo
        let diasIntervalo = 0;
        switch(intervalo) {
            case 'SEMANAL':
                diasIntervalo = 7;
                break;
            case 'QUINCENAL':
                diasIntervalo = 15;
                break;
            case 'MENSUAL':
                diasIntervalo = 30;
                break;
        }

        // Generar cuotas
        for (let i = 1; i <= numeroCuotas; i++) {
            const fechaVencimiento = new Date(hoy);
            fechaVencimiento.setDate(hoy.getDate() + (diasIntervalo * i));

            const fechaFormateada = fechaVencimiento.toLocaleDateString('es-PE', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric'
            });

            html += `
                <tr>
                    <td class="text-center"><strong>Cuota ${i}</strong></td>
                    <td class="text-center">${fechaFormateada}</td>
                    <td class="text-end">S/ ${montoPorCuota.toFixed(2)}</td>
                </tr>
            `;
        }

        html += `
                </tbody>
                <tfoot class="table-light">
                    <tr>
                        <td colspan="2" class="text-end"><strong>TOTAL A PAGAR:</strong></td>
                        <td class="text-end"><strong>S/ ${montoConInteres.toFixed(2)}</strong></td>
                    </tr>
                </tfoot>
            </table>
            <div class="mt-2">
                <small class="text-muted">
                    <i class="bi bi-info-circle me-1"></i>
                    Monto inicial: S/ ${montoInicial.toFixed(2)} |
                    Saldo financiado: S/ ${saldoFinanciar.toFixed(2)} |
                    Interés: ${interesPorcentaje}%
                </small>
            </div>
        `;

        $('#cronogramaPagos').html(html);
    }


    // ===================== MANEJO DE FORMULARIO =====================

    async function handleFormSubmit(e) {
        e.preventDefault();

        if (typeof Swal !== 'undefined' && Swal.isVisible()) {
            Swal.close();
        }

        // Validar cliente
        if (!clienteSeleccionado) {
            showNotification('Debe seleccionar un cliente', 'error');
            return;
        }

        // Validar detalles
        if (detallesVenta.length === 0) {
            showNotification('Debe agregar al menos un producto', 'error');
            return;
        }

        const formaPago = $('#forma_pago').val();
        const tipoComprobante = $('#tipo_comprobante').val();
        const observaciones = $('#observaciones').val();
        const descuentoGeneral = parseFloat($('#descuento_general').val()) || 0;

        // Preparar datos base
        const ventaData = {
            cliente_id: clienteSeleccionado.id || null,
            documento: clienteSeleccionado.documento,
            tipo_comprobante: tipoComprobante,
            serie: $('#serie').val(),
            forma_pago: formaPago,
            forma_pago_detalle: $('#forma_pago_detalle').val() || null,
            billetera: $('#billetera').val() || null,
            descuento_general: descuentoGeneral,
            observaciones: observaciones,
            detalles: detallesVenta.map(d => ({
                producto_id: d.producto_id,
                cantidad: d.cantidad,
                precio_unitario: d.precio_unitario,
                descuento_porcentaje: d.descuento_porcentaje
            }))
        };

        // Si es crédito, agregar datos
        if (formaPago === 'CREDITO') {
            const numeroCuotas = parseInt($('#numero_cuotas').val());
            const intervaloCuotas = $('#intervalo_cuotas').val();
            const interesPorcentaje = parseFloat($('#interes_porcentaje').val()) || 0;

            if (!numeroCuotas || numeroCuotas < 1) {
                showNotification('El número de cuotas debe ser mayor a 0', 'error');
                return;
            }

            ventaData.credito = {
                numero_cuotas: numeroCuotas,
                intervalo_cuotas: intervaloCuotas,
                interes_porcentaje: interesPorcentaje,
                monto_inicial: parseFloat($('#monto_inicial').val()) || 0
            };

        }

        const $submitBtn = $('#formVenta button[type="submit"]');
        const originalText = $submitBtn.html();
        $submitBtn.prop('disabled', true)
            .html('<i class="bi bi-hourglass-split"></i> Guardando...');

        try {
            $.ajax({
                url: ENDPOINTS.create,
                method: 'POST',
                data: JSON.stringify(ventaData),
                contentType: 'application/json',
                success: function(response) {
                    handleSubmitSuccess(response);
                },
                error: function(xhr) {
                    handleSubmitError(xhr);
                },
                complete: function() {
                    $submitBtn.prop('disabled', false).html(originalText);
                }
            });

        } catch (error) {
            console.error('Error en handleFormSubmit:', error);
            showNotification('Error inesperado: ' + error.message, 'error');
            $submitBtn.prop('disabled', false).html(originalText);
        }
    }

    function handleSubmitSuccess(response) {
        if (response.success) {
            ventaModal.hide();
            resetForm();

            Swal.fire({
                icon: 'success',
                title: 'Venta creada correctamente',
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });

            setTimeout(() => {
                dataTable.ajax.reload(null, false);
                if (typeof actualizarEstadisticasVentas === 'function') {
                                actualizarEstadisticasVentas();
                }
            }, 100);
        } else {
            showNotification(response.message || 'Error al crear la venta', 'error');
        }
    }

    function handleSubmitError(xhr) {
        let message = 'Error al crear la venta';

        if (xhr.responseJSON) {
            message = xhr.responseJSON.message || message;
        }

        showNotification(message, 'error');



    }

    // ===================== ACCIONES DE TABLA =====================

    function handleView(e) {
        e.preventDefault();
        const id = $(this).data('id');

        Swal.fire({
            title: 'Cargando...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        $.ajax({
            url: ENDPOINTS.get(id),
            method: 'GET',
            success: function(response) {
                Swal.close();

                if (response.success && response.data) {
                    mostrarDetalleVenta(response.data);
                } else {
                    showNotification('No se pudo cargar la venta', 'error');
                }
            },
            error: function() {
                Swal.close();
                showNotification('Error al cargar la venta', 'error');
            }
        });
    }

    function mostrarDetalleVenta(venta) {
        // Generar detalles de productos con mejor diseño
        let detallesHTML = venta.detalles.map((d, i) => `
            <tr class="align-middle">
                <td class="text-center">
                    <span class="badge bg-secondary">${i + 1}</span>
                </td>
                <td>
                    <div class="fw-semibold text-dark">${escapeHtml(d.producto_nombre)}</div>
                </td>
                <td class="text-center">
                    <span class="badge bg-info text-dark">${d.cantidad}</span>
                </td>
                <td class="text-end text-muted">S/ ${parseFloat(d.precio_unitario).toFixed(2)}</td>
                <td class="text-center">
                    ${parseFloat(d.descuento_porcentaje) > 0
                        ? `<span class="badge bg-warning text-dark">${parseFloat(d.descuento_porcentaje).toFixed(2)}%</span>`
                        : '<span class="text-muted">-</span>'}
                </td>
                <td class="text-end">
                    <strong class="text-primary">S/ ${parseFloat(d.subtotal).toFixed(2)}</strong>
                </td>
            </tr>
        `).join('');

        // Badge para el estado
        const estadoBadge = {
            'COMPLETADA': 'success',
            'PENDIENTE': 'warning',
            'ANULADA': 'danger',
            'CANCELADA': 'danger'
        };

        const estadoClass = estadoBadge[venta.estado] || 'secondary';

        // Badge para forma de pago
        const esCredito = venta.forma_pago === 'CREDITO';
        const formaPagoIcon = esCredito
            ? '<i class="bi bi-calendar-check"></i>'
            : '<i class="bi bi-cash-coin"></i>';

        // ✅ Calcular deuda si es crédito
        let seccionCredito = '';
        if (esCredito && venta.credito) {
            const montoInicial = parseFloat(venta.credito.monto_inicial || 0);
            const saldoPendiente = parseFloat(venta.credito.saldo_pendiente || 0);

            seccionCredito = `
                <div class="card border-warning mb-3">
                    <div class="card-header bg-warning text-dark">
                        <i class="bi bi-credit-card"></i> Información de Crédito
                    </div>
                    <div class="card-body">
                        <div class="row g-2">
                            <div class="col-md-4">
                                <small class="text-muted d-block">Monto Inicial</small>
                                <strong class="text-primary">S/ ${montoInicial.toFixed(2)}</strong>
                            </div>
                            <div class="col-md-4">
                                <small class="text-muted d-block">Cuotas</small>
                                <strong>${venta.credito.numero_cuotas} cuotas</strong>
                            </div>
                            <div class="col-md-4">
                                <small class="text-muted d-block">Interés</small>
                                <strong class="text-warning">${parseFloat(venta.credito.interes_porcentaje || 0).toFixed(2)}%</strong>
                            </div>
                            <div class="col-12 mt-3">
                                <div class="alert alert-danger mb-0">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <span><i class="bi bi-exclamation-circle"></i> <strong>Deuda Pendiente:</strong></span>
                                        <span class="fs-5 fw-bold">S/ ${saldoPendiente.toFixed(2)}</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        }

        Swal.fire({
            title: `<i class="bi bi-receipt-cutoff"></i> Venta #${venta.id}`,
            html: `
                <div class="text-start">
                    <!-- Información del comprobante -->
                    <div class="card border-primary mb-3">
                        <div class="card-header bg-primary text-white">
                            <i class="bi bi-file-earmark-text"></i> Información del Comprobante
                        </div>
                        <div class="card-body">
                            <div class="row g-2">
                                <div class="col-md-6">
                                    <small class="text-muted d-block">Tipo de Comprobante</small>
                                    <strong>${escapeHtml(venta.comprobante_completo)}</strong>
                                </div>
                                <div class="col-md-6">
                                    <small class="text-muted d-block">Serie - Número</small>
                                    <strong>${escapeHtml(venta.serie)} - ${escapeHtml(venta.numero)}</strong>
                                </div>
                                <div class="col-md-6 mt-2">
                                    <small class="text-muted d-block">Cliente</small>
                                    <strong class="text-dark">${escapeHtml(venta.cliente_nombre)}</strong>
                                </div>
                                <div class="col-md-3 mt-2">
                                    <small class="text-muted d-block">Forma de Pago</small>
                                    <span class="badge bg-info text-dark">
                                        ${formaPagoIcon} ${venta.forma_pago}
                                    </span>
                                </div>
                                <div class="col-md-3 mt-2">
                                    <small class="text-muted d-block">Estado</small>
                                    <span class="badge bg-${estadoClass}">${venta.estado}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    ${seccionCredito}

                    <!-- Tabla de productos -->
                    <div class="card border-0 shadow-sm mb-3">
                        <div class="card-header bg-light">
                            <i class="bi bi-box-seam"></i> <strong>Detalle de Productos</strong>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive">
                                <table class="table table-hover table-sm mb-0">
                                    <thead class="table-light">
                                        <tr>
                                            <th class="text-center" width="50">#</th>
                                            <th>Producto</th>
                                            <th class="text-center" width="80">Cant.</th>
                                            <th class="text-end" width="100">Precio</th>
                                            <th class="text-center" width="80">Desc.</th>
                                            <th class="text-end" width="120">Subtotal</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${detallesHTML}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                    <!-- Resumen de totales -->
                    <div class="card border-0 bg-light">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="text-muted">
                                    <i class="bi bi-calculator"></i> Subtotal:
                                </span>
                                <span class="fw-semibold">S/ ${parseFloat(venta.subtotal).toFixed(2)}</span>
                            </div>

                            ${parseFloat(venta.descuento_general) > 0 ? `
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="text-muted">
                                        <i class="bi bi-percent"></i> Descuento General:
                                    </span>
                                    <span class="text-warning fw-semibold">
                                        ${parseFloat(venta.descuento_general).toFixed(2)}%
                                    </span>
                                </div>
                            ` : ''}

                            <hr class="my-2">

                            <div class="d-flex justify-content-between align-items-center pt-2">
                                <span class="fw-bold fs-5 text-dark">
                                    <i class="bi bi-currency-dollar"></i> TOTAL:
                                </span>
                                <span class="fw-bold fs-4 text-success">
                                    S/ ${parseFloat(venta.total).toFixed(2)}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            `,
            width: '900px',
            confirmButtonText: '<i class="bi bi-x-circle"></i> Cerrar',
            confirmButtonColor: '#6c757d',
            customClass: {
                popup: 'swal-custom-popup',
                confirmButton: 'btn btn-lg'
            }
        });
    }

    function handleAnular(e) {
        e.preventDefault();
        const id = $(this).data('id');

        Swal.fire({
            title: '¿Anular venta?',
            text: 'Esta acción devolverá el stock de los productos. ¿Está seguro?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, anular',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                executeAnular(id);
            }
        });
    }

    function executeAnular(id) {
        Swal.fire({
            title: 'Anulando...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        $.ajax({
            url: ENDPOINTS.anular(id),
            method: 'PUT',
            success: function(response) {
                Swal.close();

                if (response.success) {
                    Swal.fire({
                        title: '¡Anulada!',
                        text: 'La venta ha sido anulada',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    });

                    setTimeout(() => {
                        dataTable.ajax.reload(null, false);
                    }, 2100);
                } else {
                    showNotification(response.message || 'Error al anular', 'error');
                }
            },
            error: function() {
                Swal.close();
                showNotification('Error al anular la venta', 'error');
            }
        });
    }

    function handleVerCredito(e) {
        e.preventDefault();
        const ventaId = $(this).data('id');
        window.location.href = `/creditos?venta_id=${ventaId}`;
    }

    // ===================== UTILIDADES =====================

    function showNotification(message, type = 'info') {
        Swal.close();

        const iconMap = {
            success: 'success',
            error: 'error',
            warning: 'warning',
            info: 'info'
        };

        Swal.fire({
            title: message,
            icon: iconMap[type] || 'info',
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true
        });
    }

    function showError(title, message) {
        Swal.close();

        Swal.fire({
            title: title,
            text: message,
            icon: 'error',
            confirmButtonText: 'Entendido'
        });
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

    function formatDateTime(datetime) {
        if (!datetime) return '-';
        const date = new Date(datetime);
        return date.toLocaleString('es-PE', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    // ===================== API PÚBLICA =====================

    window.VentasApp = {
        reload: () => dataTable.ajax.reload(),
        getDataTable: () => dataTable,
        openNew: openModalForNew
    };

    console.log('✅ ventas.js inicializado correctamente');
}
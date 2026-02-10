'use strict';
$(window).on('load', function() {
    setTimeout(initializeCreditosApp, 500);
});

function initializeCreditosApp() {
    console.log('=== INICIALIZANDO APLICACIÓN CRÉDITOS ===');

    let dataTable;
    let pagoModal;
    let creditoActual = null;

    // ===================== CONFIGURACIÓN DE ENDPOINTS =====================
    const API_BASE = '/creditos/api';

    const ENDPOINTS = {
        list: `${API_BASE}/activos`,
        get: (id) => `${API_BASE}/${id}`,
        cuotas: (id) => `${API_BASE}/${id}/cuotas`,
        pagos: (id) => `${API_BASE}/${id}/pagos`,
        registrarPago: `${API_BASE}/pagos/registrar`,
        vencidos: `${API_BASE}/vencidos`,
        proximosVencer: `${API_BASE}/proximos-vencer`,
        reporte: `${API_BASE}/reporte`
    };

    try {
        initializeModals();
        initializeDataTable();
        cargarEstadisticas();
        setupEventListeners();
        console.log('✅ Aplicación de créditos inicializada');
    } catch (error) {
        console.error('❌ Error inicializando créditos:', error);
        showError('Error al inicializar la aplicación', error.message);
    }

    // ===================== INICIALIZACIÓN =====================

    function initializeModals() {
        const pagoModalEl = document.getElementById('pagoModal');
        if (!pagoModalEl) {
            throw new Error('No se encontró el modal #pagoModal');
        }
        if (typeof bootstrap === 'undefined') {
            throw new Error('Bootstrap no está cargado');
        }
        pagoModal = new bootstrap.Modal(pagoModalEl);

        pagoModalEl.addEventListener('hidden.bs.modal', function() {
            resetFormPago();
        });
    }

    function initializeDataTable() {
        const tableElement = $('#tablaCreditosActivos');
        if (tableElement.length === 0) {
            throw new Error('No se encontró #tablaCreditosActivos');
        }

        dataTable = tableElement.DataTable({
            responsive: true,
            processing: false,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(json) {
                    console.log('📊 DATOS CRÉDITOS:', json.data);
                    return json.data || [];
                },
                error: function(xhr, error, thrown) {
                    console.error('Error en DataTables:', error, thrown);
                    showError('Error al cargar los créditos',
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
                    width: '10%',
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
                    data: 'monto_con_interes',
                    title: 'Monto Total',
                    width: '10%',
                    className: 'text-end',
                    render: function(data) {
                        return `S/ ${parseFloat(data).toFixed(2)}`;
                    }
                },
                {
                    data: 'monto_pagado',
                    title: 'Pagado',
                    width: '10%',
                    className: 'text-end',
                    render: function(data) {
                        return `S/ ${parseFloat(data).toFixed(2)}`;
                    }
                },
                {
                    data: 'saldo_pendiente',
                    title: 'Saldo',
                    width: '10%',
                    className: 'text-end',
                    render: function(data) {
                        return `<strong>S/ ${parseFloat(data).toFixed(2)}</strong>`;
                    }
                },
                {
                    data: 'porcentaje_pagado',
                    title: 'Progreso',
                    width: '10%',
                    className: 'text-center',
                    render: function(data, type, row) {
                        const porcentaje = parseFloat(data || 0).toFixed(0);
                        let colorClass = 'bg-danger';
                        if (porcentaje >= 75) colorClass = 'bg-success';
                        else if (porcentaje >= 50) colorClass = 'bg-warning';

                        return `
                            <div class="progress" style="height: 20px;">
                                <div class="progress-bar ${colorClass}" role="progressbar"
                                     style="width: ${porcentaje}%"
                                     aria-valuenow="${porcentaje}" aria-valuemin="0" aria-valuemax="100">
                                    ${porcentaje}%
                                </div>
                            </div>`;
                    }
                },
                {
                    data: 'estado',
                    title: 'Estado',
                    width: '10%',
                    className: 'text-center',
                    render: function(data) {
                        const badges = {
                            'ACTIVO': '<span class="badge bg-success">Activo</span>',
                            'PAGADO': '<span class="badge bg-info">Pagado</span>',
                            'VENCIDO': '<span class="badge bg-danger">Vencido</span>',
                            'CANCELADO': '<span class="badge bg-secondary">Cancelado</span>'
                        };
                        return badges[data] || data;
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
                zeroRecords: "No se encontraron créditos activos",
                emptyTable: "No hay créditos registrados",
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
        // ✅ Determinar si tiene cuotas pendientes de pago (secuencial)
        const tieneCuotasPendientes = row.estado === 'ACTIVO' && parseFloat(row.saldo_pendiente) > 0;
        const botonPagarDisabled = !tieneCuotasPendientes ? 'disabled' : '';
        const titlePagar = tieneCuotasPendientes
            ? 'Registrar pago'
            : 'No hay cuotas pendientes';

        return `
            <div class="d-flex justify-content-center align-items-center gap-1">
                <button class="btn btn-info btn-sm action-view"
                        data-id="${row.id}"
                        title="Ver detalles">
                    <i class="bi bi-eye"></i>
                </button>
                <button class="btn btn-success btn-sm action-pagar ${botonPagarDisabled}"
                        data-id="${row.id}"
                        title="${titlePagar}"
                        ${botonPagarDisabled}>
                    <i class="bi bi-cash"></i>
                </button>
                <button class="btn btn-primary btn-sm action-cuotas"
                        data-id="${row.id}"
                        title="Ver cuotas">
                    <i class="bi bi-calendar-check"></i>
                </button>
            </div>
        `;
    }

    function setupEventListeners() {
        // Tabs de navegación
        $('#tabActivos').on('click', () => cargarCreditos('activos'));
        $('#tabVencidos').on('click', () => cargarCreditos('vencidos'));
        $('#tabProximosVencer').on('click', () => cargarCreditos('proximos'));

        // Formulario de pago
        $('#formPago').on('submit', handleFormPagoSubmit);

        // Acciones de la tabla
        $('#tablaCreditosActivos tbody')
            .on('click', '.action-view', handleView)
            .on('click', '.action-pagar', handlePagar)
            .on('click', '.action-cuotas', handleVerCuotas);
    }

    // ===================== CARGA DE DATOS =====================

    function cargarCreditos(tipo) {
        let endpoint;

        switch(tipo) {
            case 'vencidos':
                endpoint = ENDPOINTS.vencidos;
                break;
            case 'proximos':
                endpoint = ENDPOINTS.proximosVencer + '?dias=7';
                break;
            default:
                endpoint = ENDPOINTS.list;
        }

        dataTable.ajax.url(endpoint).load();
    }

    // ===================== REPORTE DE ESTADÍSTICAS =====================
    function cargarEstadisticas() {
        $.ajax({
            url: ENDPOINTS.reporte,
            method: 'GET',
            success: function(response) {
                if (response.success && response.data) {
                    $('#monto-activos').text(`S/ ${parseFloat(response.data.totalDeuda || 0).toFixed(2)}`);
                    $('#count-activos').text(response.data.activos || 0);
                    $('#count-vencidos').text(response.data.vencidos || 0);
                    $('#count-proximos').text(response.data.proximosAVencer || 0);
                }
            },
            error: function(xhr) {
                console.warn('No se pudo cargar las estadísticas:', xhr);
            }
        });
    }



    // ===================== MANEJO DE MODAL DE PAGO =====================

    function resetFormPago() {
        $('#formPago')[0].reset();
        creditoActual = null;
        $('.grupo-billetera').hide();
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
                    mostrarDetalleCredito(response.data);
                } else {
                    showNotification('No se pudo cargar el crédito', 'error');
                }
            },
            error: function() {
                Swal.close();
                showNotification('Error al cargar el crédito', 'error');
            }
        });
    }

    function mostrarDetalleCredito(credito) {
       const cuotasHTML = credito.cuotas.map((c, i) => {
           const estadoBadge = {
               'PENDIENTE': '<span class="badge bg-warning">Pendiente</span>',
               'PAGADA': '<span class="badge bg-success">Pagada</span>',
               'PARCIAL': '<span class="badge bg-info">Parcial</span>',
               'VENCIDA': '<span class="badge bg-danger">Vencida</span>'
           };

           // ✅ Determinar si la cuota está bloqueada (solo la primera pendiente/vencida está habilitada)
           const primerasPagadas = credito.cuotas
               .slice(0, i)
               .every(cuota => cuota.estado === 'PAGADA');

           const esProximaPagar = (c.estado === 'PENDIENTE' || c.estado === 'VENCIDA') && primerasPagadas;
           const rowClass = esProximaPagar ? 'table-success' : c.estado === 'VENCIDA' ? 'table-danger' : '';

           return `
               <tr class="${rowClass}">
                   <td class="text-center">
                       ${esProximaPagar ? '<i class="bi bi-arrow-right-circle-fill text-success"></i> ' : ''}
                       ${c.numero_cuota}
                   </td>
                   <td>${formatDate(c.fecha_vencimiento)}</td>
                   <td class="text-end">S/ ${parseFloat(c.monto_cuota).toFixed(2)}</td>
                   <td class="text-end">S/ ${parseFloat(c.monto_pagado).toFixed(2)}</td>
                   <td class="text-end">S/ ${parseFloat(c.saldo_pendiente).toFixed(2)}</td>
                   <td class="text-center">${estadoBadge[c.estado] || c.estado}</td>
               </tr>
           `;
       }).join('');

        Swal.fire({
            title: `Crédito #${credito.id}`,
            html: `
                <div class="text-start">
                    <p><strong>Cliente:</strong> ${escapeHtml(credito.cliente_nombre)}</p>
                    <p><strong>Comprobante:</strong> ${escapeHtml(credito.comprobante_completo)}</p>
                    <p><strong>Monto Total:</strong> S/ ${parseFloat(credito.monto_total).toFixed(2)}</p>
                    <p><strong>Interés:</strong> ${parseFloat(credito.interes_porcentaje).toFixed(2)}%</p>
                    <p><strong>Monto con Interés:</strong> S/ ${parseFloat(credito.monto_con_interes).toFixed(2)}</p>
                    <hr>
                    <p><strong>Monto Pagado:</strong> <span class="text-success">S/ ${parseFloat(credito.monto_pagado).toFixed(2)}</span></p>
                    <p><strong>Saldo Pendiente:</strong> <span class="text-danger">S/ ${parseFloat(credito.saldo_pendiente).toFixed(2)}</span></p>
                    <p><strong>Progreso:</strong> ${parseFloat(credito.porcentaje_pagado || 0).toFixed(0)}%</p>
                    <hr>
                    <h6>Cuotas (${credito.numero_cuotas})</h6>
                    <table class="table table-sm table-bordered">
                        <thead>
                            <tr>
                                <th>Cuota</th>
                                <th>Vencimiento</th>
                                <th class="text-end">Monto</th>
                                <th class="text-end">Pagado</th>
                                <th class="text-end">Saldo</th>
                                <th class="text-center">Estado</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${cuotasHTML}
                        </tbody>
                    </table>
                </div>
            `,
            width: '900px',
            confirmButtonText: 'Cerrar'
        });
    }

    function handlePagar(e) {
        e.preventDefault();
        const id = $(this).data('id');

        // Cargar datos del crédito
        $.ajax({
            url: ENDPOINTS.get(id),
            method: 'GET',
            success: function(response) {
                if (response.success && response.data) {
                    creditoActual = response.data;

                    // ✅ Buscar la primera cuota pendiente (secuencial)
                    const cuotaPendiente = creditoActual.cuotas
                        .filter(c => c.estado === 'PENDIENTE' || c.estado === 'VENCIDA')
                        .sort((a, b) => a.numero_cuota - b.numero_cuota)[0];

                    if (!cuotaPendiente) {
                        showNotification('No hay cuotas pendientes de pago', 'warning');
                        return;
                    }

                    // Mostrar info en el modal
                    $('#infoCredito').html(`
                        <div class="alert alert-info">
                            <div class="row">
                                <div class="col-md-6">
                                    <strong>Cliente:</strong><br>
                                    <span class="text-dark">${escapeHtml(creditoActual.cliente_nombre)}</span>
                                </div>
                                <div class="col-md-6">
                                    <strong>Saldo Total Pendiente:</strong><br>
                                    <span class="text-danger fs-5">S/ ${parseFloat(creditoActual.saldo_pendiente).toFixed(2)}</span>
                                </div>
                            </div>
                            <hr>
                            <div class="alert alert-warning mb-0 mt-2">
                                <i class="bi bi-calendar-check"></i>
                                <strong>Cuota a Pagar: #${cuotaPendiente.numero_cuota}</strong><br>
                                <small>Vencimiento: ${formatDate(cuotaPendiente.fecha_vencimiento)}</small><br>
                                <strong>Monto cuota: S/ ${parseFloat(cuotaPendiente.saldo_pendiente).toFixed(2)}</strong>
                            </div>
                        </div>
                    `);

                    $('#credito_id').val(id);
                    $('#cuota_id').val(cuotaPendiente.id); // ✅ Asignar cuota obligatoria
                    $('#monto_pagado').attr('max', parseFloat(cuotaPendiente.saldo_pendiente).toFixed(2));
                    $('#monto_pagado').val(''); // Limpiar campo

                    pagoModal.show();
                } else {
                    showNotification('No se pudo cargar el crédito', 'error');
                }
            },
            error: function() {
                showNotification('Error al cargar el crédito', 'error');
            }
        });
    }

    function handleVerCuotas(e) {
        e.preventDefault();
        const id = $(this).data('id');

        Swal.fire({
            title: 'Cargando cuotas...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        $.ajax({
            url: ENDPOINTS.cuotas(id),
            method: 'GET',
            success: function(response) {
                Swal.close();

                if (response.success && response.data) {
                    mostrarCuotas(response.data);
                } else {
                    showNotification('No se pudieron cargar las cuotas', 'error');
                }
            },
            error: function() {
                Swal.close();
                showNotification('Error al cargar las cuotas', 'error');
            }
        });
    }

    function mostrarCuotas(cuotas) {
        const cuotasHTML = cuotas.map((c, i) => {
            const estadoBadge = {
                'PENDIENTE': '<span class="badge bg-warning">Pendiente</span>',
                'PAGADA': '<span class="badge bg-success">Pagada</span>',
                'PARCIAL': '<span class="badge bg-info">Parcial</span>',
                'VENCIDA': '<span class="badge bg-danger">Vencida</span>'
            };

            const diasInfo = c.estado === 'PENDIENTE' && c.dias_para_vencer !== undefined
                ? `<small class="text-muted">(${c.dias_para_vencer} días)</small>`
                : c.estado === 'VENCIDA' && c.dias_vencida !== undefined
                ? `<small class="text-danger">(+${c.dias_vencida} días)</small>`
                : '';

            return `
                <tr class="${c.estado === 'VENCIDA' ? 'table-danger' : ''}">
                    <td class="text-center">${c.numero_cuota}</td>
                    <td class="text-center">${formatDate(c.fecha_vencimiento)} ${diasInfo}</td>
                    <td class="text-end">S/ ${parseFloat(c.monto_cuota).toFixed(2)}</td>
                    <td class="text-end">S/ ${parseFloat(c.monto_pagado).toFixed(2)}</td>
                    <td class="text-end"><strong>S/ ${parseFloat(c.saldo_pendiente).toFixed(2)}</strong></td>
                    <td class="text-center">${estadoBadge[c.estado] || c.estado}</td>
                    <td class="text-center">${c.fecha_pago ? formatDate(c.fecha_pago) : '-'}</td>
                </tr>
            `;
        }).join('');

        Swal.fire({
            title: 'Cronograma de Cuotas',
            html: `
                <div class="table-responsive">
                    <table class="table table-sm table-bordered">
                        <thead>
                            <tr>
                                <th class="text-center">Cuota</th>
                                <th class="text-center">Vencimiento</th>
                                <th class="text-end">Monto</th>
                                <th class="text-end">Pagado</th>
                                <th class="text-end">Saldo</th>
                                <th class="text-center">Estado</th>
                                <th class="text-center">Fecha Pago</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${cuotasHTML}
                        </tbody>
                    </table>
                </div>
            `,
            width: '1000px',
            confirmButtonText: 'Cerrar'
        });
    }

    // ===================== MANEJO DE FORMULARIO PAGO =====================

    async function handleFormPagoSubmit(e) {
        e.preventDefault();

        if (typeof Swal !== 'undefined' && Swal.isVisible()) {
            Swal.close();
        }

        const montoPagado = parseFloat($('#monto_pagado').val());
        const saldoPendiente = parseFloat(creditoActual.saldo_pendiente);

        // Validar monto
        if (montoPagado <= 0) {
            showNotification('El monto debe ser mayor a 0', 'error');
            return;
        }

        if (montoPagado > saldoPendiente) {
            showNotification(`El monto no puede exceder el saldo pendiente (S/ ${saldoPendiente.toFixed(2)})`, 'error');
            return;
        }

        // Validar billetera si es necesario
        const formaPagoDetalle = $('#forma_pago_detalle').val();
        if (['YAPE', 'PLIN'].includes(formaPagoDetalle) && !$('#billetera').val()) {
            showNotification('Debe seleccionar una billetera', 'error');
            return;
        }
        // Validar que haya cuota seleccionada
        if (!$('#cuota_id').val()) {
            showNotification('Debe seleccionar una cuota para pagar', 'error');
            return;
        }

        // Preparar datos
        const pagoData = {
          credito_id: parseInt($('#credito_id').val()),
          cuota_id: parseInt($('#cuota_id').val()),
          monto_pagado: montoPagado,
          metodo_pago: formaPagoDetalle,
          numero_operacion: $('#numero_operacion').val() || null,
          observaciones: $('#observaciones_pago').val() || null
        };

        const $submitBtn = $('#formPago button[type="submit"]');
        const originalText = $submitBtn.html();
        $submitBtn.prop('disabled', true)
            .html('<i class="bi bi-hourglass-split"></i> Registrando...');

        try {
            $.ajax({
                url: ENDPOINTS.registrarPago,
                method: 'POST',
                data: JSON.stringify(pagoData),
                contentType: 'application/json',
                success: function(response) {
                    handlePagoSuccess(response);
                },
                error: function(xhr) {
                    handlePagoError(xhr);
                },
                complete: function() {
                    $submitBtn.prop('disabled', false).html(originalText);
                }
            });

        } catch (error) {
            console.error('Error en handleFormPagoSubmit:', error);
            showNotification('Error inesperado: ' + error.message, 'error');
            $submitBtn.prop('disabled', false).html(originalText);
        }
    }

    function handlePagoSuccess(response) {
        if (response.success) {
            pagoModal.hide();
            resetFormPago();

            Swal.fire({
                icon: 'success',
                title: 'Pago registrado correctamente',
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });

            setTimeout(() => {
                dataTable.ajax.reload(null, false);
                cargarEstadisticas();

            }, 100);
        } else {
            showNotification(response.message || 'Error al registrar el pago', 'error');
        }
    }

    function handlePagoError(xhr) {
        let message = 'Error al registrar el pago';

        if (xhr.responseJSON) {
            message = xhr.responseJSON.message || message;
        }

        showNotification(message, 'error');
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

    function formatDate(date) {
        if (!date) return '-';
        const d = new Date(date);
        return d.toLocaleDateString('es-PE');
    }

    window.CreditosApp = {
        reload: () => dataTable.ajax.reload(),
        getDataTable: () => dataTable
    };

    console.log('✅ creditos.js inicializado correctamente');
}
'use strict';
$(window).on('load', function() {
    setTimeout(initializeClientesApp, 500);
});

function initializeClientesApp() {
    console.log('=== INICIALIZANDO APLICACIÓN CLIENTES ===');

    let dataTable;
    let clienteModal;
    let isEditMode = false;
    let currentClienteId = null;

    // Constantes de validación
    const VALIDATION = {
        NOMBRE_MIN_LENGTH: 2,
        NOMBRE_MAX_LENGTH: 150,
        DNI_LENGTH: 8,
        RUC_LENGTH: 11,
        TELEFONO_LENGTH: 9,
        EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    };

    // ===================== CONFIGURACIÓN DE ENDPOINTS =====================
    const API_BASE = '/clientes/api';

    const ENDPOINTS = {
        list: `${API_BASE}/datatables`,
        create: `${API_BASE}/crear`,
        update: (id) => `${API_BASE}/actualizar/${id}`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        consultarDocumento: (doc) => `${API_BASE}/consultar-documento/${doc}`
    };

    try {
        initializeModals();
        initializeDataTable();
        setupEventListeners();
        console.log('✅ Aplicación de clientes inicializada');
    } catch (error) {
        console.error('❌ Error inicializando clientes:', error);
        showError('Error al inicializar la aplicación', error.message);
    }

    // ===================== INICIALIZACIÓN =====================

    function initializeModals() {
        const clienteModalEl = document.getElementById('clienteModal');
        if (!clienteModalEl) {
            throw new Error('No se encontró el modal #clienteModal');
        }
        if (typeof bootstrap === 'undefined') {
            throw new Error('Bootstrap no está cargado');
        }
        clienteModal = new bootstrap.Modal(clienteModalEl);

        // Limpiar formulario al cerrar modal
        clienteModalEl.addEventListener('hidden.bs.modal', function() {
            resetForm();
        });
    }

    function initializeDataTable() {
        const tableElement = $('#tablaClientes');
        if (tableElement.length === 0) {
            throw new Error('No se encontró #tablaClientes');
        }

        dataTable = tableElement.DataTable({
            responsive: true,
            processing: false,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(json) {
                    console.log('📊 DATOS CRUDOS DEL SERVIDOR:', json.data);

                    // Ver estados de todos los registros
                    const conteoEstados = {
                        activos: json.data.filter(c => c.estado === 1).length,
                        inactivos: json.data.filter(c => c.estado === 0).length,
                        eliminados: json.data.filter(c => c.estado === 2).length,
                        otros: json.data.filter(c => ![0,1,2].includes(c.estado)).length
                    };
                    console.log('📊 Conteo por estado:', conteoEstados);

                    // 🔥 FILTRAR registros eliminados (estado = 2) en el frontend
                    const dataFiltrada = json.data.filter(item => item.estado !== 2);
                    console.log(`📊 Total registros: ${json.data.length}, Mostrados: ${dataFiltrada.length}`);

                    // Mostrar los últimos 3 registros para debugging
                    console.log('🆕 Últimos 3 clientes:', dataFiltrada.slice(-3).map(c => ({
                        id: c.id,
                        nombre: c.nombre,
                        documento: c.documento,
                        estado: c.estado
                    })));

                    return dataFiltrada;
                },
                error: function(xhr, error, thrown) {
                    console.error('Error en DataTables:', error, thrown);
                    showError('Error al cargar los clientes',
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
                    data: 'nombre',
                    title: 'Nombre',
                    render: function(data) {
                        return escapeHtml(data);
                    }
                },
                {
                    data: 'documento',
                    title: 'Documento',
                    width: '12%',
                    className: 'text-center',
                    render: function(data) {
                        return escapeHtml(data);
                    }
                },
                {
                    data: 'telefono',
                    title: 'Teléfono',
                    width: '10%',
                    className: 'text-center',
                    render: function(data) {
                        return escapeHtml(data || '-');
                    }
                },
                {
                    data: 'correo',
                    title: 'Correo',
                    width: '15%',
                    render: function(data) {
                        return data ? escapeHtml(data) : '-';
                    }
                },
                {
                    data: 'estado',
                    title: 'Estado',
                    width: '10%',
                    className: 'text-center',
                    render: function(data) {
                        return data === 1
                            ? '<span class="badge bg-success">Activo</span>'
                            : '<span class="badge bg-danger">Inactivo</span>';
                    }
                },
                {
                    data: null,
                    title: 'Acciones',
                    width: '15%',
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
    }

    function createActionButtons(row) {
        const isActive = row.estado === 1;

        return `
            <div class="d-flex justify-content-center align-items-center gap-1">
                <button class="btn btn-primary btn-sm action-edit"
                        data-id="${row.id}"
                        title="Editar">
                    <i class="bi bi-pencil-square"></i>
                </button>
                <div class="form-check form-switch">
                    <input
                        class="form-check-input toggle-status"
                        type="checkbox"
                        data-id="${row.id}"
                        ${isActive ? 'checked' : ''}>
                </div>
                <button class="btn btn-danger btn-sm action-delete"
                        data-id="${row.id}"
                        title="Eliminar"
                        aria-label="Eliminar cliente ${escapeHtml(row.nombre)}">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
    }

    function setupEventListeners() {
        // Botón nuevo registro
        $('#btnNuevoRegistro').on('click', openModalForNew);

        // Formulario
        $('#formCliente').on('submit', handleFormSubmit);

        // Validación en tiempo real
        $('#nombre').on('input', function() {
            validateNombreField($(this));
        });

        $('#documento').on('input', function() {
            validateDocumentoField($(this));
        });

        $('#telefono').on('input', function() {
            validateTelefonoField($(this));
        });

        $('#correo').on('input', function() {
            validateCorreoField($(this));
        });

        // Botón consultar documento
        $('#btnConsultarDocumento').on('click', handleConsultarDocumento);

        // Enter en campo de consulta
        $('#documentoConsulta').on('keypress', function(e) {
            if (e.which === 13) {
                e.preventDefault();
                handleConsultarDocumento();
            }
        });

        // Acciones de la tabla (event delegation)
        $('#tablaClientes tbody')
            .on('click', '.action-edit', handleEdit)
            .on('change', '.toggle-status', handleToggleStatus)
            .on('click', '.action-delete', handleDelete);
    }

    // ===================== MANEJO DE MODAL =====================

    function openModalForNew() {
        isEditMode = false;
        currentClienteId = null;
        resetForm();

        $('#modalTitle').text('Agregar Nuevo Cliente');
        clienteModal.show();
    }

    function resetForm() {
        $('#formCliente')[0].reset();
        clearValidationErrors();
        clearConsultaMessage();

        isEditMode = false;
        currentClienteId = null;
    }

    function clearValidationErrors() {
        $('.is-invalid').removeClass('is-invalid');
        $('.is-valid').removeClass('is-valid');
        $('.invalid-feedback').remove();
    }

    function clearConsultaMessage() {
        $('#consulta-mensaje').html('');
    }

    // ===================== CONSULTA DE DOCUMENTO =====================

    function handleConsultarDocumento() {
        const documento = $('#documentoConsulta').val().trim();

        clearConsultaMessage();

        if (!documento) {
            showConsultaMessage('Por favor ingrese un documento', 'warning');
            return;
        }

        if (documento.length !== VALIDATION.DNI_LENGTH && documento.length !== VALIDATION.RUC_LENGTH) {
            showConsultaMessage('El documento debe tener 8 dígitos (DNI) o 11 dígitos (RUC)', 'danger');
            return;
        }

        // Deshabilitar botón
        const $btn = $('#btnConsultarDocumento');
        const originalHtml = $btn.html();
        $btn.prop('disabled', true).html('<i class="bi bi-hourglass-split"></i> Consultando...');

        $.ajax({
            url: ENDPOINTS.consultarDocumento(documento),
            method: 'GET',
            success: function(response) {
                console.log('📄 Respuesta consulta:', response);

                // 🔥 CORRECCIÓN: La API devuelve el nombre directamente en response
                if (response.success) {
                    // Autocompletar NOMBRE desde response.nombre (no response.data)
                    const nombreEncontrado = response.nombre || response.razonSocial || '';

                    if (nombreEncontrado) {
                        $('#nombre').val(nombreEncontrado);
                        validateNombreField($('#nombre'));

                        // 🔥 IMPORTANTE: Copiar documento al campo del formulario
                        $('#documento').val(documento);
                        validateDocumentoField($('#documento'));

                        showConsultaMessage('✅ Datos encontrados y autocompletados. Complete teléfono y correo.', 'success');
                    } else {
                        showConsultaMessage('⚠️ No se encontró información para este documento', 'warning');
                    }
                } else {
                    showConsultaMessage(response.message || 'No se encontraron datos', 'warning');
                }
            },
            error: function(xhr) {
                console.error('Error en consulta:', xhr);
                let message = 'Error al consultar el documento';

                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }

                showConsultaMessage(message, 'danger');
            },
            complete: function() {
                $btn.prop('disabled', false).html(originalHtml);
            }
        });
    }

    function showConsultaMessage(message, type) {
        const alertClass = `alert alert-${type} alert-dismissible fade show`;
        const html = `
            <div class="${alertClass}" role="alert">
                <small>${message}</small>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;
        $('#consulta-mensaje').html(html);
    }

    // ===================== VALIDACIONES =====================

    function validateNombreField($input) {
        const nombre = $input.val().trim();
        const $formGroup = $input.closest('.col-md-12');

        // Limpiar errores previos
        $input.removeClass('is-invalid is-valid');
        $formGroup.find('.invalid-feedback').remove();

        if (nombre.length === 0) {
            showFieldError($input, 'El nombre es requerido');
            return false;
        }

        if (nombre.length < VALIDATION.NOMBRE_MIN_LENGTH) {
            showFieldError($input, `El nombre debe tener al menos ${VALIDATION.NOMBRE_MIN_LENGTH} caracteres`);
            return false;
        }

        if (nombre.length > VALIDATION.NOMBRE_MAX_LENGTH) {
            showFieldError($input, `El nombre no puede exceder ${VALIDATION.NOMBRE_MAX_LENGTH} caracteres`);
            return false;
        }

        $input.removeClass('is-invalid').addClass('is-valid');
        return true;
    }

    function validateDocumentoField($input) {
        const documento = $input.val().trim();
        const $formGroup = $input.closest('.col-md-6');

        $input.removeClass('is-invalid is-valid');
        $formGroup.find('.invalid-feedback').remove();

        if (documento.length === 0) {
            showFieldError($input, 'El documento es requerido');
            return false;
        }

        if (!/^\d+$/.test(documento)) {
            showFieldError($input, 'El documento solo debe contener números');
            return false;
        }

        if (documento.length !== VALIDATION.DNI_LENGTH && documento.length !== VALIDATION.RUC_LENGTH) {
            showFieldError($input, 'El documento debe tener 8 dígitos (DNI) o 11 dígitos (RUC)');
            return false;
        }

        $input.removeClass('is-invalid').addClass('is-valid');
        return true;
    }

    function validateTelefonoField($input) {
        const telefono = $input.val().trim();
        const $formGroup = $input.closest('.col-md-6');

        $input.removeClass('is-invalid is-valid');
        $formGroup.find('.invalid-feedback').remove();

        if (telefono.length === 0) {
            showFieldError($input, 'El teléfono es requerido');
            return false;
        }

        if (!/^\d+$/.test(telefono)) {
            showFieldError($input, 'El teléfono solo debe contener números');
            return false;
        }

        if (telefono.length !== VALIDATION.TELEFONO_LENGTH) {
            showFieldError($input, `El teléfono debe tener ${VALIDATION.TELEFONO_LENGTH} dígitos`);
            return false;
        }

        $input.removeClass('is-invalid').addClass('is-valid');
        return true;
    }

    function validateCorreoField($input) {
        const correo = $input.val().trim();
        const $formGroup = $input.closest('.col-md-12');

        $input.removeClass('is-invalid is-valid');
        $formGroup.find('.invalid-feedback').remove();

        // El correo es opcional
        if (correo.length === 0) {
            return true;
        }

        if (!VALIDATION.EMAIL_PATTERN.test(correo)) {
            showFieldError($input, 'Ingrese un correo electrónico válido');
            return false;
        }

        $input.removeClass('is-invalid').addClass('is-valid');
        return true;
    }

    function showFieldError($input, message) {
        $input.addClass('is-invalid');
        const $feedback = $('<div class="invalid-feedback"></div>').text(message);
        $input.after($feedback);
    }

    // ===================== MANEJO DE FORMULARIO =====================

    async function handleFormSubmit(e) {
        e.preventDefault();
        console.log('📄 Iniciando handleFormSubmit...');

        // 🔥 FORZAR CIERRE de cualquier Swal abierto
        if (typeof Swal !== 'undefined' && Swal.isVisible()) {
            console.log('⚠️ Cerrando Swal previo...');
            Swal.close();
        }

        // Obtener valores
        const nombre = $('#nombre').val().trim();
        const documento = $('#documento').val().trim();
        const telefono = $('#telefono').val().trim();
        const correo = $('#correo').val().trim();

        clearValidationErrors();

        // Validar todos los campos
        const nombreValido = validateNombreField($('#nombre'));
        const documentoValido = validateDocumentoField($('#documento'));
        const telefonoValido = validateTelefonoField($('#telefono'));
        const correoValido = validateCorreoField($('#correo'));

        if (!nombreValido || !documentoValido || !telefonoValido || !correoValido) {
            showNotification('Por favor corrija los errores en el formulario', 'error');
            return;
        }

        // Deshabilitar botón de envío
        const $submitBtn = $('#formCliente button[type="submit"]');
        const originalText = $submitBtn.html();
        $submitBtn.prop('disabled', true)
            .html('<i class="bi bi-hourglass-split"></i> Guardando...');

        try {
            // Preparar datos
            const clienteData = {
                nombre: nombre,
                documento: documento,
                telefono: telefono,
                correo: correo || null
            };

            console.log('📤 Datos a enviar:', clienteData);

            // Determinar endpoint y método
            const url = isEditMode && currentClienteId
                ? ENDPOINTS.update(currentClienteId)
                : ENDPOINTS.create;
            const method = isEditMode ? 'PUT' : 'POST';

            // Enviar datos
            $.ajax({
                url: url,
                method: method,
                data: JSON.stringify(clienteData),
                contentType: 'application/json',
                success: function(response) {
                    console.log('✅ Respuesta exitosa:', response);
                    handleSubmitSuccess(response);
                },
                error: function(xhr) {
                    console.error('❌ Error en petición:', xhr);
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
            const message = isEditMode
                ? 'Cliente actualizado correctamente'
                : 'Cliente creado correctamente';

            // 🔥 IMPORTANTE: Cerrar modal Y resetear ANTES de recargar
            clienteModal.hide();
            resetForm();

            // 🔥 Mostrar notificación ANTES de recargar la tabla
            Swal.fire({
                icon: 'success',
                title: message,
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                timerProgressBar: true
            });

            // 🔥 Recargar tabla DESPUÉS de mostrar notificación (con delay mínimo)
            setTimeout(() => {
                dataTable.ajax.reload(null, false);
            }, 100);

        } else {
            showNotification(response.message || 'Error al guardar el cliente', 'error');
        }
    }

    function handleSubmitError(xhr) {
        console.error('Error en submit:', xhr);

        let message = 'Error al guardar el cliente';

        if (xhr.responseJSON) {
            message = xhr.responseJSON.message || message;

            // Manejar errores de validación específicos
            if (xhr.responseJSON.errors) {
                const errors = xhr.responseJSON.errors;
                Object.keys(errors).forEach(field => {
                    const $input = $(`#${field}`);
                    if ($input.length) {
                        showFieldError($input, errors[field]);
                    }
                });
            }
        } else if (xhr.status === 0) {
            message = 'No se pudo conectar con el servidor';
        } else if (xhr.status === 404) {
            message = 'Recurso no encontrado';
        } else if (xhr.status === 500) {
            message = 'Error interno del servidor';
        }

        showNotification(message, 'error');
    }

    // ===================== ACCIONES DE TABLA =====================

    function handleEdit(e) {
        e.preventDefault();
        const id = $(this).data('id');

        // Mostrar loading
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
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();

                if (response.success && response.data) {
                    const cliente = response.data;

                    // Llenar formulario
                    $('#nombre').val(cliente.nombre || '');
                    $('#documento').val(cliente.documento || '');
                    $('#telefono').val(cliente.telefono || '');
                    $('#correo').val(cliente.correo || '');

                    // Validar campos
                    validateNombreField($('#nombre'));
                    validateDocumentoField($('#documento'));
                    validateTelefonoField($('#telefono'));
                    if (cliente.correo) {
                        validateCorreoField($('#correo'));
                    }

                    $('#modalTitle').text('Editar Cliente');
                    isEditMode = true;
                    currentClienteId = id;
                    clienteModal.show();
                } else {
                    showNotification('No se pudo cargar el cliente', 'error');
                }
            },
            error: function(xhr) {
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();
                console.error('Error al obtener cliente:', xhr);
                showNotification('Error al cargar los datos del cliente', 'error');
            }
        });
    }

    /**
     * Maneja el cambio de estado de un cliente (activar/desactivar)
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
                title: '¿Desactivar cliente?',
                text: `El cliente "${rowData.nombre}" dejará de estar activo.`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar'
            }).then((result) => {
                if (result.isConfirmed) {
                    toggleClienteStatus(id, checkbox, rowDT);
                } else {
                    checkbox.prop('checked', prevState);
                }
            });
        } else {
            toggleClienteStatus(id, checkbox, rowDT);
        }
    }

    /**
     * Ejecuta el cambio de estado y actualiza la fila localmente
     */
    function toggleClienteStatus(id, checkbox, rowDT) {
        const newState = checkbox.is(':checked');

        // Mostrar loading
        Swal.fire({
            title: 'Cambiando estado...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        $.ajax({
            url: ENDPOINTS.toggleStatus(id),
            method: 'PUT',
            success: function(response) {
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();

                if (response.success) {
                    showNotification(response.message || 'Estado actualizado', 'success');

                    // Actualizar datos de la fila SIN recargar toda la tabla
                    let rowData = rowDT.data() || {};
                    rowData.estado = newState ? 1 : 0;

                    rowDT.data(rowData).invalidate().draw(false);

                    // Aplicar/remover clase visual
                    const node = $(rowDT.node());
                    if (newState) {
                        node.removeClass('table-secondary');
                    } else {
                        node.addClass('table-secondary');
                    }

                    // Sincronizar checkbox
                    checkbox.prop('checked', newState);
                } else {
                    showNotification(response.message || 'Error al cambiar el estado', 'error');
                    checkbox.prop('checked', !newState);
                }
            },
            error: function(xhr) {
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();
                console.error('Error al cambiar estado:', xhr);
                showNotification('Error de conexión con el servidor', 'error');
                checkbox.prop('checked', !newState);
            }
        });
    }

    function handleDelete(e) {
        e.preventDefault();
        const id = $(this).data('id');
        const rowNode = $(this).closest('tr');
        const rowDT = dataTable.row(rowNode);
        const rowData = rowDT.data();

        Swal.fire({
            title: '¿Eliminar cliente?',
            text: `¿Está seguro de eliminar al cliente "${rowData.nombre}"? Esta acción no se puede deshacer.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonColor: '#3085d6',
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar',
            reverseButtons: true
        }).then((result) => {
            if (result.isConfirmed) {
                executeDelete(id, rowDT);
            }
        });
    }

    function executeDelete(id, rowDT) {
        Swal.fire({
            title: 'Eliminando...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        $.ajax({
            url: ENDPOINTS.delete(id),
            method: 'DELETE',
            success: function(response) {
                console.log('🔥 Respuesta del servidor:', response);

                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();

                if (response.success) {
                    // 🔥 Mostrar mensaje de éxito
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'El cliente ha sido eliminado',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    });

                    // 🔥 OPCIÓN 1: Eliminar la fila visualmente sin recargar (más rápido)
                    if (rowDT && rowDT.node()) {
                        setTimeout(() => {
                            $(rowDT.node()).fadeOut(400, function() {
                                rowDT.remove().draw(false);
                                console.log('✅ Fila eliminada visualmente');
                            });
                        }, 300);
                    } else {
                        // 🔥 OPCIÓN 2: Recargar tabla si no se puede eliminar la fila
                        setTimeout(() => {
                            console.log('🔄 Recargando DataTable...');
                            dataTable.ajax.reload(null, false);
                        }, 2100);
                    }
                } else {
                    console.error('❌ Error del servidor:', response.message);
                    Swal.fire(
                        'Error',
                        response.message || 'No se pudo eliminar el cliente',
                        'error'
                    );
                }
            },
            error: function(xhr) {
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();
                console.error('❌ Error en petición AJAX:', xhr);

                let message = 'Error al eliminar el cliente';

                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }

                Swal.fire('Error', message, 'error');
            }
        });
    }

    // ===================== UTILIDADES =====================

    function showNotification(message, type = 'info') {
        // 🔥 CERRAR cualquier Swal existente antes de mostrar notificación
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
        // 🔥 CERRAR cualquier Swal existente
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

    // Exponer API pública (opcional, para debugging)
    window.ClientesApp = {
        reload: () => dataTable.ajax.reload(),
        getDataTable: () => dataTable,
        openNew: openModalForNew
    };

    console.log('✅ clientes.js inicializado correctamente');
}
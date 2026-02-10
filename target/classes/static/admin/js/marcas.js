'use strict';
$(window).on('load', function() {
    setTimeout(initializeMarcasApp, 500);
});

function initializeMarcasApp() {
    console.log('=== INICIALIZANDO APLICACIÓN MARCAS ===');

    // ⭐ CONFIGURAR TIPO DE ENTIDAD PARA IMAGENES.JS
    if (typeof setTipoEntidad === 'function') {
        setTipoEntidad('marcas');
        console.log('✅ Tipo de entidad configurado: marcas');
    }

    let dataTable;
    let marcaModal;
    let isEditMode = false;
    let currentMarcaId = null;

    // Constantes de validación
    const VALIDATION = {
        NOMBRE_MIN_LENGTH: 2,
        NOMBRE_MAX_LENGTH: 100
    };

    // ===================== CONFIGURACIÓN DE ENDPOINTS =====================
    const API_BASE = '/marcas/api';

    const ENDPOINTS = {
        list: `${API_BASE}/datatables`,
        create: `${API_BASE}/crear`,
        update: (id) => `${API_BASE}/actualizar/${id}`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        updateImage: (id) => `${API_BASE}/${id}/imagen`,
        deleteImage: (id) => `${API_BASE}/${id}/imagen`
    };
    try {
        initializeModals();
        initializeDataTable();
        setupEventListeners();
        console.log('✅ Aplicación de marcas inicializada');
    } catch (error) {
        console.error('❌ Error inicializando marcas:', error);
        showError('Error al inicializar la aplicación', error.message);
    }

    // ===================== INICIALIZACIÓN =====================

    function initializeModals() {
        const marcaModalEl = document.getElementById('marcaModal');
        if (!marcaModalEl) {
            throw new Error('No se encontró el modal #marcaModal');
        }
        if (typeof bootstrap === 'undefined') {
            throw new Error('Bootstrap no está cargado');
        }
        marcaModal = new bootstrap.Modal(marcaModalEl);

        // Limpiar formulario al cerrar modal
        marcaModalEl.addEventListener('hidden.bs.modal', function() {
            resetForm();
        });
    }

    function initializeDataTable() {
        const tableElement = $('#tablaMarcas');
        if (tableElement.length === 0) {
            throw new Error('No se encontró #tablaMarcas');
        }

        dataTable = tableElement.DataTable({
            responsive: true,
            processing: false,
            serverSide: false,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(json) {
                    console.log('🔍 DATOS CRUDOS DEL SERVIDOR:', json.data);

                    // Ver estados de todos los registros
                    const conteoEstados = {
                        activos: json.data.filter(m => m.estado === 1).length,
                        inactivos: json.data.filter(m => m.estado === 0).length,
                        eliminados: json.data.filter(m => m.estado === 2).length,
                        otros: json.data.filter(m => ![0,1,2].includes(m.estado)).length
                    };
                    console.log('📊 Conteo por estado:', conteoEstados);

                    // 🔥 FILTRAR registros eliminados (estado = 2) en el frontend
                    const dataFiltrada = json.data.filter(item => item.estado !== 2);
                    console.log(`📊 Total registros: ${json.data.length}, Mostrados: ${dataFiltrada.length}`);

                    // Mostrar los últimos 3 registros para debugging
                    console.log('🆕 Últimas 3 marcas:', dataFiltrada.slice(-3).map(m => ({
                        id: m.id,
                        nombre: m.nombre,
                        estado: m.estado,
                        imagen: m.imagen ? 'SÍ' : 'NO'
                    })));

                    return dataFiltrada;
                },
                error: function(xhr, error, thrown) {
                    console.error('Error en DataTables:', error, thrown);
                    showError('Error al cargar las marcas',
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
                    data: 'imagen',
                    title: 'Imagen',
                    width: '10%',
                    className: 'text-center',
                    orderable: false,
                    render: function(data) {
                        const placeholderSVG = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNDUlIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMTYiIGZpbGw9IiM5OTk5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiPjxpbWFnZW4+PC90ZXh0Pjx0ZXh0IHg9IjUwJSIgeT0iNTUlIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiNiYmJiYmIiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlNpbiBpbWFnZW48L3RleHQ+PC9zdmc+';

                        const imgSrc = data || placeholderSVG;

                        return `<img src="${escapeHtml(imgSrc)}"
                                class="product-image-preview"
                                alt="Imagen de marca"
                                onerror="if(this.src!=='${placeholderSVG}'){this.src='${placeholderSVG}';this.onerror=null;}">`;
                    }
                },
                {
                    data: 'nombre',
                    title: 'Nombre',
                    render: function(data) {
                        return escapeHtml(data);
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
            <div class="btn-group btn-group-sm" role="group">
                <button class="btn btn-primary action-edit"
                        data-id="${row.id}"
                        title="Editar">
                    <i class="bi bi-pencil-square"></i>
                </button>
                <div class="form-check form-switch text-center">
                    <input
                        class="form-check-input toggle-status"
                        type="checkbox"
                        data-id="${row.id}"
                        ${isActive ? 'checked' : ''}>
                </div>
                <button class="btn btn-danger action-delete"
                        data-id="${row.id}"
                        title="Eliminar"
                        aria-label="Eliminar marca ${escapeHtml(row.nombre)}">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
    }

    function setupEventListeners() {
        // Botón nuevo registro
        $('#btnNuevoRegistro').on('click', openModalForNew);

        // Formulario
        $('#formMarca').on('submit', handleFormSubmit);

        // Validación en tiempo real del nombre
        $('#nombre').on('input', function() {
            validateNombreField($(this));
        });

        // Acciones de la tabla (event delegation)
        $('#tablaMarcas tbody')
            .on('click', '.action-edit', handleEdit)
            .on('change', '.toggle-status', handleToggleStatus)
            .on('click', '.action-delete', handleDelete);
    }

    // ===================== MANEJO DE MODAL =====================

    function openModalForNew() {
        isEditMode = false;
        currentMarcaId = null;
        resetForm();

        // Resetear estado de imágenes
        if (typeof resetImagenState === 'function') {
            resetImagenState();
        }

        $('#modalTitle').text('Agregar Nueva Marca');
        marcaModal.show();
    }

    function resetForm() {
        $('#formMarca')[0].reset();
        clearValidationErrors();

        // Limpiar previews de imagen
        if (typeof clearAllImagePreviews === 'function') {
            clearAllImagePreviews();
        }

        isEditMode = false;
        currentMarcaId = null;
    }

    function clearValidationErrors() {
        $('.is-invalid').removeClass('is-invalid');
        $('.invalid-feedback').remove();
    }

    // ===================== VALIDACIONES =====================

    function validateNombreField($input) {
        const nombre = $input.val().trim();
        const $formGroup = $input.closest('.mb-3');

        // Limpiar errores previos
        $input.removeClass('is-invalid');
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

    function showFieldError($input, message) {
        $input.addClass('is-invalid');
        const $feedback = $('<div class="invalid-feedback"></div>').text(message);
        $input.closest('.mb-3').append($feedback);
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

        // Validar nombre
        const $nombreInput = $('#nombre');
        const nombre = $nombreInput.val().trim();

        clearValidationErrors();

        if (!validateNombreField($nombreInput)) {
            $nombreInput.focus();
            return;
        }

        // Deshabilitar botón de envío
        const $submitBtn = $('#formMarca button[type="submit"]');
        const originalText = $submitBtn.html();
        $submitBtn.prop('disabled', true)
            .html('<i class="fas fa-spinner fa-spin"></i> Guardando...');

        try {
            let imagenUrl = null;
            console.log('🔍 Nombre validado:', nombre);

            // 1️⃣ Obtener imágenes seleccionadas
            const selectedImages = typeof getSelectedImages === 'function'
                ? getSelectedImages()
                : null;

            console.log('🔍 selectedImages:', selectedImages);
            console.log('🔍 Tipo primer elemento:', selectedImages?.[0]?.constructor?.name);

            // 2️⃣ Subir imagen si hay archivos nuevos
            if (selectedImages && selectedImages.length > 0) {
                const primerElemento = selectedImages[0];

                // Si es un objeto File (archivo nuevo)
                if (primerElemento instanceof File) {
                    console.log('📤 Subiendo imagen al servidor...');

                    try {
                        // 🔥 IMPORTANTE: uploadImageToServer puede abrir un Swal
                        imagenUrl = await uploadImageToServer(primerElemento, 'marcas');
                        console.log('✅ Imagen subida:', imagenUrl);

                        // 🔥 Cerrar cualquier Swal que haya abierto uploadImageToServer
                        if (typeof Swal !== 'undefined' && Swal.isVisible()) {
                            console.log('🔥 Cerrando Swal de uploadImageToServer');
                            Swal.close();
                        }
                    } catch (error) {
                        console.error('❌ Error al subir imagen:', error);
                        // 🔥 Cerrar Swal en caso de error
                        if (typeof Swal !== 'undefined' && Swal.isVisible()) {
                            Swal.close();
                        }
                        showNotification('Error al subir la imagen: ' + error.message, 'error');
                        $submitBtn.prop('disabled', false).html(originalText);
                        return;
                    }
                }
                // Si es una URL (imagen existente o desde input URL)
                else if (typeof primerElemento === 'string') {
                    imagenUrl = primerElemento;
                    console.log('🔗 Usando URL existente:', imagenUrl);
                }
            }

            // 3️⃣ Determinar endpoint y método
            const url = isEditMode && currentMarcaId
                ? ENDPOINTS.update(currentMarcaId)
                : ENDPOINTS.create;
            const method = isEditMode ? 'PUT' : 'POST';

            let ajaxConfig;

            // 4️⃣ Configurar petición según el método
            if (isEditMode) {
                // PUT: enviar como JSON
                console.log('🔄 Modo edición: enviando JSON');
                ajaxConfig = {
                    url: url,
                    method: method,
                    data: JSON.stringify({
                        nombre: nombre,
                        imagenUrl: imagenUrl || null
                    }),
                    contentType: 'application/json',
                    processData: false
                };
            } else {
                // POST: enviar como FormData
                console.log('➕ Modo creación: enviando FormData');
                const formData = new FormData();
                formData.append('nombre', nombre);
                if (imagenUrl) {
                    formData.append('imagenUrl', imagenUrl);
                }

                ajaxConfig = {
                    url: url,
                    method: method,
                    data: formData,
                    processData: false,
                    contentType: false
                };
            }

            // 5️⃣ Enviar datos
            $.ajax({
                ...ajaxConfig,
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
                ? 'Marca actualizada correctamente'
                : 'Marca creada correctamente';

            // 🔥 IMPORTANTE: Cerrar modal Y resetear ANTES de recargar
            marcaModal.hide();
            resetForm();

            // 🔥 Mostrar notificación ANTES de recargar la tabla
            Swal.fire({
                icon: 'success',
                title: message,
                text: 'La imagen se reflejará automáticamente en la sección de Marcas de tu página web.',
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 4000,
                timerProgressBar: true
            });

            // 🔥 Recargar tabla DESPUÉS de mostrar notificación (con delay mínimo)
            setTimeout(() => {
                dataTable.ajax.reload(null, false);
            }, 100);

        } else {
            showNotification(response.message || 'Error al guardar la marca', 'error');
        }
    }

    function handleSubmitError(xhr) {
        console.error('Error en submit:', xhr);

        let message = 'Error al guardar la marca';

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
                    const marca = response.data;

                    // Llenar formulario
                    $('#nombre').val(marca.nombre || '');

                    // Mostrar imagen existente si la tiene
                    if (marca.imagen && typeof mostrarImagenesExistentes === 'function') {
                        mostrarImagenesExistentes([marca.imagen]);
                    } else if (typeof clearAllImagePreviews === 'function') {
                        clearAllImagePreviews();
                    }

                    $('#modalTitle').text('Editar Marca');
                    isEditMode = true;
                    currentMarcaId = id;
                    marcaModal.show();
                } else {
                    showNotification('No se pudo cargar la marca', 'error');
                }
            },
            error: function(xhr) {
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();
                console.error('Error al obtener marca:', xhr);
                showNotification('Error al cargar los datos de la marca', 'error');
            }
        });
    }

    /**
     * Maneja el cambio de estado de una marca (activar/desactivar)
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
                title: '¿Desactivar marca?',
                text: `La marca "${rowData.nombre}" dejará de estar visible.`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar'
            }).then((result) => {
                if (result.isConfirmed) {
                    toggleMarcaStatus(id, checkbox, rowDT);
                } else {
                    checkbox.prop('checked', prevState);
                }
            });
        } else {
            toggleMarcaStatus(id, checkbox, rowDT);
        }
    }

    /**
     * Ejecuta el cambio de estado y actualiza la fila localmente
     */
    function toggleMarcaStatus(id, checkbox, rowDT) {
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
            data: JSON.stringify({ estado: newState ? 1 : 0 }),
            contentType: 'application/json',
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
            title: '¿Eliminar marca?',
            text: `¿Está seguro de eliminar la marca "${rowData.nombre}"? Esta acción no se puede deshacer.`,
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
                console.log('📥 Respuesta del servidor:', response);

                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();

                if (response.success) {
                    // 🔥 Mostrar mensaje de éxito
                    Swal.fire({
                        title: '¡Eliminado!',
                        text: 'La marca ha sido eliminada',
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
                        response.message || 'No se pudo eliminar la marca',
                        'error'
                    );
                }
            },
            error: function(xhr) {
                // 🔥 CERRAR SIEMPRE el Swal
                Swal.close();
                console.error('❌ Error en petición AJAX:', xhr);

                let message = 'Error al eliminar la marca';

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
    window.MarcasApp = {
        reload: () => dataTable.ajax.reload(),
        getDataTable: () => dataTable,
        openNew: openModalForNew
    };

    console.log('✅ marcas.js inicializado correctamente');
}
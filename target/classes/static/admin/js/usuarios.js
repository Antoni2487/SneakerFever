$(document).ready(function() {
    // Variables globales
    let dataTable;
    let isEditing = false;
    let usuarioModal;
    let currentUserId = null; // ID del usuario logueado
    let currentUserPerfilId = null; // Perfil del usuario logueado
    let totalAdmins = 0; // Total de admins activos

    // Configuración inicial
    const API_BASE = '/usuarios/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        profiles: `${API_BASE}/perfiles`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
    };

    // Inicializar Componentes
    initializeDataTable();
    usuarioModal = new bootstrap.Modal(document.getElementById('usuarioModal'));
    loadProfiles();
    setupEventListeners();

    /**
     * Inicializa DataTable
     */
    function initializeDataTable() {
        dataTable = $('#tablaUsuarios').DataTable({
            responsive: true,
            processing: true,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: function(json) {
                    console.log('📦 Respuesta del servidor:', json);
                    if (json.success && json.data) {
                        // ⚡ SEGURIDAD: Capturamos datos del usuario actual
                        if (json.currentUserId) {
                            currentUserId = json.currentUserId;
                            currentUserPerfilId = json.currentUserPerfilId;
                            totalAdmins = json.totalAdmins || 0;
                            
                            console.log("👤 Usuario logueado ID:", currentUserId);
                            console.log("🔑 Perfil ID:", currentUserPerfilId);
                            console.log("👥 Total Admins activos:", totalAdmins);
                        }
                        return json.data;
                    } else {
                        console.error('❌ Error en respuesta del servidor:', json);
                        return [];
                    }
                },
                error: function(xhr, error, thrown) {
                    console.error('❌ Error AJAX:', error, thrown);
                    showNotification('Error al cargar usuarios: ' + error, 'error');
                }
            },
            columns: [
                { data: 'id' },
                { data: 'nombre' },
                { data: 'usuario' },
                {
                    data: 'perfil.nombre',
                    defaultContent: 'Sin perfil',
                    render: (data) => data || 'Sin perfil'
                },
                { data: 'correo' },
                {
                    data: 'estado',
                    render: (data) => data === 1
                        ? '<span class="badge text-bg-success">Activo</span>'
                        : '<span class="badge text-bg-danger">Inactivo</span>'
                },
                {
                    data: null,
                    orderable: false,
                    searchable: false,
                    render: (data, type, row) => createActionButtons(row)
                }
            ],
            columnDefs: [
                { responsivePriority: 1, targets: 1 },
                { responsivePriority: 2, targets: 6 },
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
            rowCallback: function(row, data) {
                // Resaltar la fila del usuario logueado
                if (currentUserId && data.id === currentUserId) {
                    $(row).addClass('table-primary').css({
                        'font-weight': '600',
                        'background-color': '#cfe2ff'
                    });
                } else if (data.estado !== 1) {
                    $(row).addClass('table-secondary');
                } else {
                    $(row).removeClass('table-secondary table-primary');
                }
            }
        });
    }

    /**
     * Crea los botones de acción para cada fila
     * ⚡ MODIFICADO: Validaciones robustas de seguridad
     */
    function createActionButtons(row) {
        const isCurrentUser = currentUserId && row.id === currentUserId;
        const isAdmin = row.perfil && row.perfil.nombre === 'Administrador';
        const isLastActiveAdmin = isAdmin && row.estado === 1 && totalAdmins === 1;

        // 🛡️ CASO 1: Es el usuario actual (TÚ)
        if (isCurrentUser) {
            return `
                <div class="d-flex gap-1 align-items-center">
                    <button data-id="${row.id}" class="btn btn-sm btn-primary action-edit" title="Editar mi perfil">
                        <i class="bi bi-pencil-square"></i>
                    </button>
                    <span class="badge bg-info text-dark">
                        <i class="bi bi-person-check-fill"></i> Eres tú
                    </span>
                </div>
            `;
        }

        // 🛡️ CASO 2: Es el último admin activo del sistema
        if (isLastActiveAdmin) {
            return `
                <div class="d-flex gap-1 align-items-center">
                    <button data-id="${row.id}" class="btn btn-sm btn-primary action-edit" title="Editar">
                        <i class="bi bi-pencil-square"></i>
                    </button>
                    <span class="badge bg-warning text-dark" title="No se puede desactivar/eliminar al único administrador">
                        <i class="bi bi-shield-lock-fill"></i> Único Admin
                    </span>
                </div>
            `;
        }

        // 🛡️ CASO 3: Usuario normal - Todos los controles disponibles
        return `
            <div class="d-flex gap-1">
                <button data-id="${row.id}" class="btn btn-sm btn-primary action-edit" title="Editar">
                    <i class="bi bi-pencil-square"></i>
                </button>
                <div class="form-check form-switch text-center">
                    <input
                        class="form-check-input toggle-status"
                        type="checkbox"
                        data-id="${row.id}"
                        data-is-admin="${isAdmin}"
                        ${row.estado === 1 ? 'checked' : ''}>
                </div>
                <button class="btn btn-sm btn-danger action-delete" data-id="${row.id}" data-is-admin="${isAdmin}" title="Eliminar">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;
    }

    /**
     * Configura todos los event listeners
     */
    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formUsuario').on('submit', (e) => { e.preventDefault(); saveUsuario(); });
        $('#tablaUsuarios tbody').on('click', '.action-edit', handleEdit);
        $('#tablaUsuarios tbody').on('change', '.toggle-status', handleToggleStatus);
        $('#tablaUsuarios tbody').on('click', '.action-delete', handleDelete);
    }

    /**
     * Recarga la tabla
     */
    function reloadTable() {
        if (dataTable) {
            dataTable.ajax.reload(null, false);
        }
    }

    /**
     * Carga los perfiles en el select del modal
     */
    function loadProfiles() {
        fetch(ENDPOINTS.profiles)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const select = $('#id_perfil');
                    select.empty().append('<option value="">Seleccione un perfil...</option>');
                    data.data.forEach(profile => {
                        select.append(`<option value="${profile.id}">${profile.nombre}</option>`);
                    });
                } else {
                    showNotification('Error al cargar perfiles', 'error');
                }
            }).catch(error => {
                console.error('Error cargando perfiles:', error);
                showNotification('Error de conexión al cargar perfiles', 'error');
            });
    }

    /**
     * Guarda un usuario (crear o actualizar)
     */
    function saveUsuario() {
        clearFieldErrors();

        const formData = {
            id: $('#id').val() || null,
            nombre: $('#nombre').val().trim(),
            usuario: $('#usuario').val().trim(),
            clave: $('#clave').val(),
            correo: $('#correo').val().trim(),
            perfil: {
                id: $('#id_perfil').val()
            }
        };

        // ⚡ VALIDACIÓN CRÍTICA: No puedes cambiar tu propio perfil a uno inferior
        if (isEditing && formData.id == currentUserId) {
            const newPerfilId = parseInt(formData.perfil.id);
            if (currentUserPerfilId === 1 && newPerfilId !== 1) {
                showNotification('⛔ No puedes cambiar tu propio perfil de Administrador', 'error');
                showFieldError('id_perfil', 'No puedes reducir tu propio nivel de acceso');
                return;
            }
        }

        // Validación básica del lado cliente
        if (!validateForm(formData)) {
            return;
        }

        // Si es edición y la clave está vacía, no enviarla
        if (isEditing && !formData.clave) {
            delete formData.clave;
        }

        showLoading(true);
        fetch(ENDPOINTS.save, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                usuarioModal.hide();
                showNotification(data.message, 'success');
                reloadTable();
            } else {
                if (data.errors) {
                    Object.keys(data.errors).forEach(field => {
                        showFieldError(field, data.errors[field]);
                    });
                } else {
                    showNotification(data.message, 'error');
                }
            }
        })
        .catch(error => showNotification('Error de conexión', 'error'))
        .finally(() => showLoading(false));
    }

    /**
     * Maneja la edición de un usuario
     */
    function handleEdit(e) {
        const id = $(this).data('id');
        showLoading(true);
        fetch(ENDPOINTS.get(id))
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    openModalForEdit(data.data);
                } else {
                    showNotification('Error al cargar usuario', 'error');
                }
            })
            .catch(error => showNotification('Error de conexión', 'error'))
            .finally(() => showLoading(false));
    }

    /**
     * Maneja el cambio de estado de un usuario
     * ⚡ CON VALIDACIONES DE SEGURIDAD
     */
    function handleToggleStatus(e) {
        e.preventDefault();

        const checkbox = $(this);
        const id = checkbox.data('id');
        const isAdmin = checkbox.data('is-admin');
        const newState = checkbox.is(':checked');
        const prevState = !newState;

        // 🛡️ VALIDACIÓN 1: No puedes desactivarte a ti mismo
        if (currentUserId && id === currentUserId) {
            showNotification('⛔ No puedes desactivar tu propia cuenta', 'error');
            checkbox.prop('checked', prevState);
            return;
        }

        // 🛡️ VALIDACIÓN 2: No puedes desactivar al último admin
        if (isAdmin && !newState && totalAdmins === 1) {
            showNotification('⛔ No puedes desactivar al único administrador del sistema', 'error');
            checkbox.prop('checked', prevState);
            return;
        }

        const rowNode = checkbox.closest('tr');
        const rowDT = dataTable.row(rowNode);

        // Preguntar solo si se está desactivando
        if (!newState) {
            Swal.fire({
                title: '¿Estás seguro?',
                text: "¡El usuario se desactivará y no podrá iniciar sesión!",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar'
            }).then((result) => {
                if (result.isConfirmed) {
                    toggleStatus(id, checkbox, rowDT, isAdmin);
                } else {
                    checkbox.prop('checked', prevState);
                }
            });
        } else {
            toggleStatus(id, checkbox, rowDT, isAdmin);
        }
    }

    /**
     * Envia el request y actualiza la fila localmente
     */
    function toggleStatus(id, checkbox, rowDT, isAdmin) {
        const newState = checkbox.is(':checked');
        showLoading(true);

        fetch(ENDPOINTS.toggleStatus(id), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ estado: newState ? 1 : 0 })
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showNotification(data.message, 'success');

                // Actualizar contador de admins
                if (isAdmin) {
                    totalAdmins += newState ? 1 : -1;
                    console.log("👥 Admins activos actualizados:", totalAdmins);
                }

                let rowData = rowDT.data() || {};
                rowData.estado = newState ? 1 : 0;
                rowDT.data(rowData).invalidate().draw(false);

                const node = $(rowDT.node());
                if (newState) node.removeClass('table-secondary');
                else node.addClass('table-secondary');

                $(`input[type="checkbox"][data-id="${id}"]`).prop('checked', newState);

            } else {
                showNotification(data.message, 'error');
                checkbox.prop('checked', !newState);
            }
        })
        .catch(() => {
            showNotification('Error de conexión', 'error');
            checkbox.prop('checked', !newState);
        })
        .finally(() => showLoading(false));
    }

    /**
     * Maneja la eliminación de un usuario
     * ⚡ CON VALIDACIONES DE SEGURIDAD
     */
    function handleDelete(e) {
        const id = $(this).data('id');
        const isAdmin = $(this).data('is-admin');

        // 🛡️ VALIDACIÓN 1: No puedes eliminarte a ti mismo
        if (currentUserId && id === currentUserId) {
            showNotification('⛔ No puedes eliminar tu propia cuenta', 'error');
            return;
        }

        // 🛡️ VALIDACIÓN 2: No puedes eliminar al último admin
        if (isAdmin && totalAdmins === 1) {
            showNotification('⛔ No puedes eliminar al único administrador del sistema', 'error');
            return;
        }

        Swal.fire({
            title: '¿Estás seguro?',
            text: "¡No podrás revertir esta acción!",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, ¡eliminar!',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                showLoading(true);
                fetch(ENDPOINTS.delete(id), { method: 'DELETE' })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            showNotification(data.message, 'success');
                            
                            // Actualizar contador de admins
                            if (isAdmin) {
                                totalAdmins--;
                                console.log("👥 Admins activos tras eliminación:", totalAdmins);
                            }
                            
                            reloadTable();
                        } else {
                            showNotification(data.message, 'error');
                        }
                    })
                    .catch(error => showNotification('Error de conexión', 'error'))
                    .finally(() => showLoading(false));
            }
        });
    }

    /**
     * Valida el formulario del lado cliente
     */
    function validateForm(formData) {
        let hasErrors = false;

        if (!formData.nombre) {
            showFieldError('nombre', 'El nombre es obligatorio');
            hasErrors = true;
        } else if (formData.nombre.length < 2) {
            showFieldError('nombre', 'El nombre debe tener al menos 2 caracteres');
            hasErrors = true;
        }

        if (!formData.usuario) {
            showFieldError('usuario', 'El usuario es obligatorio');
            hasErrors = true;
        } else if (formData.usuario.length < 3) {
            showFieldError('usuario', 'El usuario debe tener al menos 3 caracteres');
            hasErrors = true;
        }

        if (!formData.perfil.id) {
            showFieldError('id_perfil', 'Debe seleccionar un perfil');
            hasErrors = true;
        }

        if (!isEditing && !formData.clave) {
            showFieldError('clave', 'La contraseña es obligatoria');
            hasErrors = true;
        } else if (formData.clave && formData.clave.length < 6) {
            showFieldError('clave', 'La contraseña debe tener al menos 6 caracteres');
            hasErrors = true;
        }

        if (!formData.correo) {
            showFieldError('correo', 'El correo es obligatorio');
            hasErrors = true;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.correo)) {
            showFieldError('correo', 'El formato del correo no es válido');
            hasErrors = true;
        }

        return !hasErrors;
    }

    // Funciones de utilidad para el modal y formulario
    function openModalForNew() {
        isEditing = false;
        clearForm();
        $('#modalTitle').text('Agregar Usuario');
        $('#clave').prop('required', true).attr('placeholder', '');
        usuarioModal.show();
    }

    function openModalForEdit(usuario) {
        isEditing = true;
        clearForm();
        $('#modalTitle').text('Editar Usuario');
        $('#id').val(usuario.id);
        $('#nombre').val(usuario.nombre);
        $('#usuario').val(usuario.usuario);
        $('#correo').val(usuario.correo);
        $('#id_perfil').val(usuario.perfil ? usuario.perfil.id : '');
        $('#clave').val('').prop('required', false).attr('placeholder', 'Dejar en blanco para no cambiar');
        
        // Si estás editando tu propio perfil, mostrar advertencia
        if (usuario.id === currentUserId) {
            showNotification('ℹ️ Estás editando tu propio perfil. No podrás cambiar tu rol.', 'info');
        }
        
        usuarioModal.show();
    }

    function clearForm() {
        $('#formUsuario')[0].reset();
        clearFieldErrors();
    }

    function showFieldError(field, message) {
        $(`#${field}`).addClass('is-invalid');
        $(`#${field}-error`).text(message);
    }

    function clearFieldErrors() {
        $('.form-control').removeClass('is-invalid');
        $('.invalid-feedback').text('');
    }

    // Funciones de UI (notificaciones, loading)
    function showNotification(message, type = 'success') {
        const toastColors = {
            'success': 'text-bg-success',
            'error': 'text-bg-danger',
            'info': 'text-bg-info'
        };
        const toastClass = toastColors[type] || 'text-bg-success';
        const notification = $(`
            <div class="toast align-items-center ${toastClass} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">${message}</div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `);
        $('#notification-container').append(notification);
        const toast = new bootstrap.Toast(notification, { delay: 5000 });
        toast.show();
    }

    function showLoading(show) {
        const overlayId = 'loading-overlay';
        let $overlay = $(`#${overlayId}`);
        if (show) {
            if ($overlay.length === 0) {
                $('body').append('<div id="loading-overlay" class="loading-overlay"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>');
            }
        } else {
            $overlay.remove();
        }
    }
});
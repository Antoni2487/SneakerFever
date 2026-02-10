/**
 * Script para la gestión de perfiles y permisos
 * Archivo: src/main/resources/static/js/perfiles.js
 */

$(document).ready(function() {
    // Variables globales
    let dataTable;
    let isEditing = false;
    let perfilModal;
    let permisosModal;

    // Configuración inicial
    const API_BASE = '/perfiles/api';
    const ENDPOINTS = {
        list: `${API_BASE}/listar`,
        save: `${API_BASE}/guardar`,
        get: (id) => `${API_BASE}/${id}`,
        toggleStatusPerfil: (id) => `${API_BASE}/cambiar-estado/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`,
        options: `${API_BASE}/opciones`
    };

    // Inicializar Componentes
    perfilModal = new bootstrap.Modal(document.getElementById('perfilModal'));
    permisosModal = new bootstrap.Modal(document.getElementById('permisosModal'));
    initializeDataTable();
    setupEventListeners();

    /**
     * Inicializa DataTable
     */
    function initializeDataTable() {
        dataTable = $('#tablaPerfiles').DataTable({
            responsive: true,
            processing: true,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: 'data'
            },
            columns: [
                { data: 'id' },
                { data: 'nombre' },
                { data: 'descripcion' },
                {
                    data: 'estado',
                    render: (data) => {
                        // El backend devuelve boolean (true/false)
                        const isActive = data === true || data === 'true' || data === 1;
                        return isActive
                            ? '<span class="badge text-bg-success">Activo</span>'
                            : '<span class="badge text-bg-danger">Inactivo</span>';
                    }
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
                { responsivePriority: 2, targets: 4 },
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
                // El backend devuelve boolean
                const isActive = data.estado === true || data.estado === 'true' || data.estado === 1;
                if (!isActive) {
                    $(row).addClass('table-secondary');
                } else {
                    $(row).removeClass('table-secondary');
                }
            }
        });
    }

    /**
     * Crea los botones de acción para cada fila
     */
    function createActionButtons(row) {
        // El backend devuelve boolean
        const isActive = row.estado === true || row.estado === 'true' || row.estado === 1;

        return `
            <div class="d-flex gap-1">
                <button data-id="${row.id}" class="btn btn-sm btn-info action-permissions" title="Permisos">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-shield-lock-fill" viewBox="0 0 16 16"><path d="M5.338 1.59a61.44 61.44 0 0 0-2.837.856.481.481 0 0 0-.328.39c-.554 4.157.726 7.19 2.253 9.188a10.725 10.725 0 0 0 2.287 2.233c.346.244.652.42.893.533.12.056.255.115.385.17.117.05.238.097.36.133.124.037.25.06.377.06s.253-.023.377-.06a2.1 2.1 0 0 0 .745-.265c.16-.085.312-.18.456-.282.14-.1.274-.213.396-.333a10.726 10.726 0 0 0 2.287-2.233c1.527-1.997 2.807-5.031 2.253-9.188a.48.48 0 0 0-.328-.39c-.651-.213-1.75-.56-2.837-.855C9.552 1.29 8.5 1 8 1s-1.552.29-2.662.59zM10 8.5a1.5 1.5 0 0 1-1.5 1.5h-1a1.5 1.5 0 1 1 0-3h1A1.5 1.5 0 0 1 10 8.5z"/></svg>
                    Permisos
                </button>
                <button data-id="${row.id}" class="btn btn-sm btn-primary action-edit" title="Editar">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-pencil-square" viewBox="0 0 16 16"><path d="M15.502 1.94a.5.5 0 0 1 0 .706L14.459 3.69l-2-2L13.502.646a.5.5 0 0 1 .707 0l1.293 1.293zm-1.75 2.456-2-2L4.939 9.21a.5.5 0 0 0-.121.196l-.805 2.414a.25.25 0 0 0 .316.316l2.414-.805a.5.5 0 0 0 .196-.12l6.813-6.814z"/><path fill-rule="evenodd" d="M1 13.5A1.5 1.5 0 0 0 2.5 15h11a1.5 1.5 0 0 0 1.5-1.5v-6a.5.5 0 0 0-1 0v6a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5v-11a.5.5 0 0 1 .5-.5H9a.5.5 0 0 0 0-1H2.5A1.5 1.5 0 0 0 1 2.5z"/></svg>
                </button>
                <div class="form-check form-switch text-center">
                    <input class="form-check-input toggle-status" type="checkbox" data-id="${row.id}" ${isActive ? 'checked' : ''}>
                </div>
                <button class="btn btn-sm btn-danger action-delete" data-id="${row.id}" title="Eliminar">
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
        $('#formPerfil').on('submit', (e) => { e.preventDefault(); savePerfil(); });
        $('#tablaPerfiles tbody').on('click', '.action-edit', handleEdit);
        $('#tablaPerfiles tbody').on('change', '.toggle-status', handleToggleStatusPerfil);
        $('#tablaPerfiles tbody').on('click', '.action-permissions', handlePermissions);
        $('#btnGuardarPermisos').on('click', savePermissions);
        $('#tablaPerfiles tbody').on('click', '.action-delete', handleDelete);
    }

    /**
     * Recarga la tabla
     */
    function reloadTable() {
        dataTable.ajax.reload(null, false); // false = mantiene la página actual
    }

    /**
     * Guarda un perfil (crear o actualizar)
     */
    function savePerfil() {
        const perfilData = {
            id: $('#id').val() || null,
            nombre: $('#nombre').val().trim(),
            descripcion: $('#descripcion').val().trim(),
            estado: true // Por defecto activo al crear/editar
        };

        if (!perfilData.nombre) {
            showFieldError('nombre', 'El nombre es obligatorio');
            return;
        }

        showLoading(true);
        fetch(ENDPOINTS.save, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(perfilData)
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showNotification(data.message, 'success');
                perfilModal.hide();
                reloadTable();
            } else {
                showNotification(data.message, 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showNotification('Error de conexión', 'error');
        })
        .finally(() => showLoading(false));
    }

    /**
     * Maneja la edición de un perfil
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
                    showNotification('Error al cargar perfil', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showNotification('Error de conexión', 'error');
            })
            .finally(() => showLoading(false));
    }

    /**
     * Maneja el cambio de estado de un perfil
     */
    function handleToggleStatusPerfil(e) {
        e.preventDefault();

        const checkbox = $(this);
        const id = checkbox.data('id');
        const newState = checkbox.is(':checked');
        const row = checkbox.closest('tr');

        if (!newState) {
            Swal.fire({
                title: '¿Estás seguro?',
                text: "¡El perfil se desactivará y no podrá utilizarse!",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar'
            }).then(result => {
                if (result.isConfirmed) {
                    toggleStatusPerfil(id, checkbox, row);
                } else {
                    checkbox.prop('checked', true);
                }
            });
        } else {
            toggleStatusPerfil(id, checkbox, row);
        }
    }

    /**
     * Realiza el cambio de estado en el servidor
     */
    function toggleStatusPerfil(id, checkbox, row) {
        showLoading(true);

        fetch(ENDPOINTS.toggleStatusPerfil(id), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                showNotification(data.message, 'success');
                // Recargar la tabla para obtener el estado actualizado del servidor
                reloadTable();
            } else {
                showNotification(data.message, 'error');
                // Revertir el checkbox si falló
                checkbox.prop('checked', !checkbox.is(':checked'));
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showNotification('Error de conexión', 'error');
            // Revertir el checkbox si hubo error
            checkbox.prop('checked', !checkbox.is(':checked'));
        })
        .finally(() => showLoading(false));
    }

    /**
     * Maneja la eliminación de un perfil
     */
    function handleDelete(e) {
        const id = $(this).data('id');

        Swal.fire({
            title: '¿Estás seguro?',
            text: "¡No podrás revertir esta acción! Se eliminará el perfil permanentemente.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, ¡eliminar!',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                showLoading(true);
                fetch(ENDPOINTS.delete(id), {
                    method: 'DELETE'
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showNotification(data.message, 'success');
                        reloadTable();
                    } else {
                        showNotification(data.message, 'error');
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    showNotification('Error de conexión al eliminar el perfil.', 'error');
                })
                .finally(() => {
                    showLoading(false);
                });
            }
        });
    }

    /**
     * Maneja la apertura del modal de permisos
     */
    async function handlePermissions(e) {
        const id = $(this).data('id');
        showLoading(true);
        $('#permisoPerfilId').val(id);

        try {
            const [perfilRes, opcionesRes] = await Promise.all([
                fetch(ENDPOINTS.get(id)),
                fetch(ENDPOINTS.options)
            ]);

            const perfilData = await perfilRes.json();
            const opcionesData = await opcionesRes.json();

            if (perfilData.success && opcionesData.success) {
                $('#permisoPerfilNombre').text(perfilData.data.nombre);
                const listaOpciones = $('#listaOpciones');
                listaOpciones.empty();

                opcionesData.data.forEach(opcion => {
                    const isChecked = perfilData.data.opciones.includes(opcion.id);
                    const item = `
                        <label class="list-group-item">
                            <input class="form-check-input me-1" type="checkbox" value="${opcion.id}" ${isChecked ? 'checked' : ''}>
                            ${opcion.nombre}
                        </label>
                    `;
                    listaOpciones.append(item);
                });
                permisosModal.show();
            } else {
                showNotification('Error al cargar datos de permisos', 'error');
            }
        } catch (error) {
            console.error('Error:', error);
            showNotification('Error de conexión al cargar permisos', 'error');
        } finally {
            showLoading(false);
        }
    }

    /**
     * Guarda los permisos seleccionados para un perfil
     */
    async function savePermissions() {
        const perfilId = $('#permisoPerfilId').val();
        const selectedOpciones = $('#listaOpciones input:checked').map(function() {
            return { id: $(this).val() };
        }).get();

        showLoading(true);
        try {
            // Primero, obtenemos el perfil completo para no perder sus otros datos
            const perfilRes = await fetch(ENDPOINTS.get(perfilId));
            const perfilData = await perfilRes.json();

            if (!perfilData.success) {
                showNotification('No se pudo obtener el perfil para actualizar', 'error');
                return;
            }

            const perfilToUpdate = perfilData.data;
            perfilToUpdate.opciones = selectedOpciones;

            // Ahora guardamos el perfil con las opciones actualizadas
            const saveRes = await fetch(ENDPOINTS.save, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(perfilToUpdate)
            });
            const saveData = await saveRes.json();

            if (saveData.success) {
                permisosModal.hide();
                showNotification('Permisos actualizados correctamente', 'success');
                reloadTable();
            } else {
                showNotification(saveData.message || 'Error al guardar permisos', 'error');
            }
        } catch (error) {
            console.error('Error:', error);
            showNotification('Error de conexión al guardar permisos', 'error');
        } finally {
            showLoading(false);
        }
    }

    // Funciones de utilidad para el modal y formulario
    function openModalForNew() {
        isEditing = false;
        clearForm();
        $('#modalTitle').text('Agregar Perfil');
        perfilModal.show();
    }

    function openModalForEdit(perfil) {
        isEditing = true;
        clearForm();
        $('#modalTitle').text('Editar Perfil');
        $('#id').val(perfil.id);
        $('#nombre').val(perfil.nombre);
        $('#descripcion').val(perfil.descripcion);
        perfilModal.show();
    }

    function clearForm() {
        $('#formPerfil')[0].reset();
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
        const toastClass = type === 'success' ? 'text-bg-success' : 'text-bg-danger';
        const notification = $(`<div class="toast align-items-center ${toastClass} border-0" role="alert" aria-live="assertive" aria-atomic="true"><div class="d-flex"><div class="toast-body">${message}</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div></div>`);
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
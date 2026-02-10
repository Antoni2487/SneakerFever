$(document).ready(function() {
    // Variables globales
    let dataTable;
    let isEditMode = false;
    let currentCategoriaId = null;
    let categoriaModal;

    // Configuración de API
    const API_BASE = '/categorias/api';
    const ENDPOINTS = {
        list: `${API_BASE}/datatables`,
        create: `${API_BASE}/crear`,
        get: (id) => `${API_BASE}/${id}`,
        update: (id) => `${API_BASE}/actualizar/${id}`,
        toggleStatus: (id) => `${API_BASE}/cambiar-estado/${id}`,
        delete: (id) => `${API_BASE}/eliminar/${id}`
    };

    // Inicializar
    categoriaModal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    initializeDataTable();
    setupEventListeners();

    /**
     * Inicializar DataTable
     */
    function initializeDataTable() {
        dataTable = $('#tablaCategorias').DataTable({
            responsive: true,
            processing: true,
            ajax: {
                url: ENDPOINTS.list,
                dataSrc: 'data'
            },
            columns: [
                { data: 'id' },
                { data: 'nombre' },
                {
                    data: 'estado',
                    render: (data) => {
                        const isActive = data === true || data === 'true' || data === 1 || data === '1';
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
                { responsivePriority: 2, targets: 3 }
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
                const isActive = data.estado === true || data.estado === 'true' || data.estado === 1 || data.estado === '1';
                if (!isActive) {
                    $(row).addClass('table-secondary');
                } else {
                    $(row).removeClass('table-secondary');
                }
            }
        });
    }

    /**
     * Crear botones de acción para cada fila
     */
    function createActionButtons(row) {
        const isActive = row.estado === true || row.estado === 1 || row.estado === 'true';

        return `
            <div class="d-flex gap-1 justify-content-center">
                <button class="btn btn-sm btn-primary action-edit" data-id="${row.id}" title="Editar">
                    <i class="bi bi-pencil-square"></i>
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
     * Configurar event listeners
     */
    function setupEventListeners() {
        $('#btnNuevoRegistro').on('click', openModalForNew);
        $('#formCategoria').on('submit', handleFormSubmit);
        $('#tablaCategorias tbody').on('click', '.action-edit', handleEdit);
        $('#tablaCategorias tbody').on('change', '.toggle-status', handleToggleStatus);
        $('#tablaCategorias tbody').on('click', '.action-delete', handleDelete);
    }

    /**
     * Manejar envío del formulario (Crear/Actualizar)
     */
    function handleFormSubmit(e) {
        e.preventDefault();

        const formData = {
            nombre: $('#nombre').val().trim(),
            descripcion: $('#descripcion').val().trim()
        };

        // Validar campos requeridos
        if (!formData.nombre) {
            showNotification('El nombre de la categoría es requerido', 'error');
            return;
        }

        // Determinar si es creación o actualización
        if (isEditMode && currentCategoriaId) {
            updateCategoria(currentCategoriaId, formData);
        } else {
            createCategoria(formData);
        }
    }

    /**
     * Crear nueva categoría
     */
    function createCategoria(data) {
        $.ajax({
            url: ENDPOINTS.create,
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: function() {
                showLoading('Creando categoría...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showNotification('Categoría creada exitosamente', 'success');
                    categoriaModal.hide();
                    clearForm();
                    dataTable.ajax.reload();
                } else {
                    showNotification(response.message || 'Error al crear la categoría', 'error');
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al crear la categoría';
                showNotification(errorMsg, 'error');
            }
        });
    }

    /**
     * Actualizar categoría existente
     */
    function updateCategoria(id, data) {
        $.ajax({
            url: ENDPOINTS.update(id),
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: function() {
                showLoading('Actualizando categoría...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showNotification('Categoría actualizada exitosamente', 'success');
                    categoriaModal.hide();
                    clearForm();
                    dataTable.ajax.reload();
                } else {
                    showNotification(response.message || 'Error al actualizar la categoría', 'error');
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al actualizar la categoría';
                showNotification(errorMsg, 'error');
            }
        });
    }

    /**
     * Editar categoría
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
     * Cambiar estado de categoría (Activar/Desactivar)
     */
    function handleToggleStatus(e) {
        e.preventDefault();

        const checkbox = $(this);
        const id = checkbox.data('id');
        const newState = checkbox.is(':checked');

        // Si se está desactivando, pedir confirmación
        if (!newState) {
            Swal.fire({
                title: '¿Estás seguro?',
                text: "¡La categoría se desactivará y no podrá utilizarse!",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#dc3545',
                cancelButtonColor: '#6c757d',
                confirmButtonText: 'Sí, desactivar',
                cancelButtonText: 'Cancelar'
            }).then(result => {
                if (result.isConfirmed) {
                    toggleCategoriaStatus(id, checkbox);
                } else {
                    // Revertir el checkbox si cancela
                    checkbox.prop('checked', true);
                }
            });
        } else {
            // Si se activa, hacerlo directamente
            toggleCategoriaStatus(id, checkbox);
        }
    }

    /**
     * Ejecutar cambio de estado en el servidor
     */
    function toggleCategoriaStatus(id, checkbox) {
        $.ajax({
            url: ENDPOINTS.toggleStatus(id),
            method: 'PUT',
            beforeSend: function() {
                showLoading('Cambiando estado...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showNotification('Estado cambiado exitosamente', 'success');
                    dataTable.ajax.reload(null, false);
                } else {
                    showNotification(response.message || 'Error al cambiar el estado', 'error');
                    checkbox.prop('checked', !checkbox.is(':checked'));
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al cambiar el estado';
                showNotification(errorMsg, 'error');
                checkbox.prop('checked', !checkbox.is(':checked'));
            }
        });
    }

    /**
     * Eliminar categoría
     */
    function handleDelete() {
        const id = $(this).data('id');
        const row = dataTable.row($(this).closest('tr')).data();

        Swal.fire({
            title: '¿Eliminar categoría?',
            text: `¿Está seguro de eliminar la categoría "${row.nombre}"? Esta acción no se puede deshacer.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#dc3545',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                deleteCategoria(id);
            }
        });
    }

    /**
     * Ejecutar eliminación
     */
    function deleteCategoria(id) {
        $.ajax({
            url: ENDPOINTS.delete(id),
            method: 'DELETE',
            beforeSend: function() {
                showLoading('Eliminando categoría...');
            },
            success: function(response) {
                hideLoading();
                if (response.success) {
                    showNotification('Categoría eliminada exitosamente', 'success');
                    dataTable.ajax.reload();
                } else {
                    showNotification(response.message || 'Error al eliminar la categoría', 'error');
                }
            },
            error: function(xhr) {
                hideLoading();
                const errorMsg = xhr.responseJSON?.message || 'Error al eliminar la categoría';
                showNotification(errorMsg, 'error');
            }
        });
    }

    /**
     * FUNCIONES DE MODALES
     */

    /**
     * Abrir modal para nueva categoría
     */
    function openModalForNew() {
        isEditMode = false;
        currentCategoriaId = null;
        clearForm();
        $('#modalTitle').text('Agregar Categoría');

        if (categoriaModal) {
            categoriaModal.show();
        } else {
            showNotification('Modal no disponible', 'error');
        }
    }

    /**
     * Abrir modal para editar categoría
     */
    function openModalForEdit(categoria) {
        isEditMode = true;
        currentCategoriaId = categoria.id;

        // Llenar formulario con datos
        $('#id').val(categoria.id);
        $('#nombre').val(categoria.nombre);
        $('#descripcion').val(categoria.descripcion || '');

        $('#modalTitle').text('Editar Categoría');

        if (categoriaModal) {
            categoriaModal.show();
        }
    }

    /**
     * Limpiar formulario
     */
    function clearForm() {
        $('#formCategoria')[0].reset();
        $('#formCategoria .is-invalid').removeClass('is-invalid');
        $('.invalid-feedback').hide();
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
});

/**
 * Mostrar tabla básica cuando DataTables no está disponible
 */
function showBasicTable() {
    console.log('📋 Inicializando tabla básica...');
    
    // Solo configurar eventos básicos
    $('#btnNuevoRegistro').on('click', function() {
        alert('Funcionalidad no disponible sin DataTables');
    });
    
    // Mostrar mensaje en la tabla
    $('#tablaCategorias tbody').html(`
        <tr>
            <td colspan="4" class="text-center">
                <div class="alert alert-warning">
                    <strong>Modo básico:</strong> DataTables no disponible. 
                    <br>Verifica tu conexión a internet.
                </div>
            </td>
        </tr>
    `);
}
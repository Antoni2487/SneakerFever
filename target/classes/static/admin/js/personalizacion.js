// ==================== CONFIGURACIÓN ====================
let logoActual = null;
let slidesActuales = [];
let marcasDisponibles = [];
let slideSeleccionado = null;
const modalAsignarMarca = new bootstrap.Modal(document.getElementById('modalAsignarMarca'));

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ personalizacion.js cargado');

    // Configurar tipo de entidad para imagenes.js
    setTipoEntidad('personalizacion');

    // Cargar datos iniciales
    cargarLogo();
    cargarSlides();
    cargarMarcasDisponibles();

    // Inicializar Select2
    inicializarSelect2();

    // Event listeners
    inicializarEventListeners();
});

// ==================== INICIALIZAR SELECT2 ====================
function inicializarSelect2() {
    $('#selectMarca').select2({
        theme: 'bootstrap-5',
        placeholder: 'Selecciona una marca',
        allowClear: true,
        language: {
            noResults: function() {
                return "No se encontraron marcas";
            }
        }
    });
}

// ==================== EVENT LISTENERS ====================
function inicializarEventListeners() {
    // Upload logo
    const logoFileInput = document.getElementById('logoFile');
    if (logoFileInput) {
        logoFileInput.addEventListener('change', handleLogoUpload);
    }

    // Eliminar logo
    const btnEliminarLogo = document.getElementById('btnEliminarLogo');
    if (btnEliminarLogo) {
        btnEliminarLogo.addEventListener('click', confirmarEliminarLogo);
    }

    // Guardar marca del modal
    const btnGuardarMarca = document.getElementById('btnGuardarMarca');
    if (btnGuardarMarca) {
        btnGuardarMarca.addEventListener('click', guardarMarcaSlide);
    }
}

// ==================== CARGAR LOGO ====================
async function cargarLogo() {
    try {
        const response = await fetch('/personalizacion/api/logo');
        const result = await response.json();

        if (result.success && result.data) {
            logoActual = result.data;
            mostrarLogoPreview(result.data.imagenUrl);
        } else {
            mostrarLogoPlaceholder();
        }
    } catch (error) {
        console.error('Error al cargar logo:', error);
        showNotification('Error al cargar el logo', 'error');
    }
}

function mostrarLogoPreview(url) {
    const logoPreview = document.getElementById('logoPreview');
    const btnEliminarLogo = document.getElementById('btnEliminarLogo');

    if (logoPreview && url) {
        logoPreview.innerHTML = `
            <img src="${url}" alt="Logo" onerror="this.src='/admin/images/no-image.png'">
        `;

        if (btnEliminarLogo) {
            btnEliminarLogo.disabled = false;
        }
    } else {
        mostrarLogoPlaceholder();
    }
}

function mostrarLogoPlaceholder() {
    const logoPreview = document.getElementById('logoPreview');
    const btnEliminarLogo = document.getElementById('btnEliminarLogo');

    if (logoPreview) {
        logoPreview.innerHTML = `
            <div class="logo-preview-placeholder">
                <i class="bi bi-image"></i>
                <p class="mb-0">Sin logo</p>
                <small>500x500px sin fondo</small>
            </div>
        `;
    }

    if (btnEliminarLogo) {
        btnEliminarLogo.disabled = true;
    }
}

// ==================== UPLOAD LOGO ====================
async function handleLogoUpload(e) {
    const file = e.target.files[0];
    if (!file) return;

    // Validar que sea imagen
    if (!file.type.startsWith('image/')) {
        showNotification('Por favor selecciona una imagen válida', 'error');
        e.target.value = '';
        return;
    }

    // Validar tamaño (5MB)
    if (file.size > 5 * 1024 * 1024) {
        showNotification('La imagen no debe superar los 5MB', 'error');
        e.target.value = '';
        return;
    }

    try {
        Swal.fire({
            title: 'Subiendo logo...',
            text: 'Por favor espera',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        // Subir imagen al servidor
        const imageUrl = await uploadImageToServer(file, 'personalizacion');

        // Actualizar logo en base de datos
        const formData = new URLSearchParams();
        formData.append('imagenUrl', imageUrl);

        const response = await fetch('/personalizacion/api/logo/imagen', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            logoActual = result.data;
            mostrarLogoPreview(imageUrl);
            Swal.fire({
                icon: 'success',
                title: '¡Logo actualizado!',
                text: 'El logo se ha actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
            });
        } else {
            throw new Error(result.message || 'Error al actualizar logo');
        }

    } catch (error) {
        console.error('Error al subir logo:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: error.message || 'Error al subir el logo'
        });
    } finally {
        e.target.value = '';
    }
}

// ==================== ELIMINAR LOGO ====================
async function confirmarEliminarLogo() {
    const result = await Swal.fire({
        title: '¿Eliminar logo?',
        text: 'Esta acción eliminará el logo actual del sitio',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    });

    if (result.isConfirmed) {
        await eliminarLogo();
    }
}

async function eliminarLogo() {
    try {
        Swal.fire({
            title: 'Eliminando logo...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        const response = await fetch('/personalizacion/api/logo/imagen', {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            logoActual = result.data;
            mostrarLogoPlaceholder();
            Swal.fire({
                icon: 'success',
                title: '¡Logo eliminado!',
                timer: 2000,
                showConfirmButton: false
            });
        } else {
            throw new Error(result.message || 'Error al eliminar logo');
        }

    } catch (error) {
        console.error('Error al eliminar logo:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: error.message || 'Error al eliminar el logo'
        });
    }
}

// ==================== CARGAR SLIDES ====================
async function cargarSlides() {
    try {
        const response = await fetch('/personalizacion/api/slides/con-marca');
        const result = await response.json();

        if (result.success) {
            slidesActuales = result.data;
            renderizarSlides(result.data);
        } else {
            showNotification('Error al cargar slides', 'error');
        }
    } catch (error) {
        console.error('Error al cargar slides:', error);
        showNotification('Error al cargar slides', 'error');
    }
}

function renderizarSlides(slides) {
    const container = document.getElementById('slidesContainer');
    if (!container) return;

    container.innerHTML = '';

    slides.forEach(slide => {
        const slideCard = crearSlideCard(slide);
        container.appendChild(slideCard);
    });
}

function crearSlideCard(slide) {
    const col = document.createElement('div');
    col.className = 'col-md-6 col-lg-4';

    const tieneImagen = slide.imagenUrl && slide.imagenUrl.trim() !== '';
    const tieneMarca = slide.marca && slide.marca.id;

    col.innerHTML = `
        <div class="card slide-card shadow-sm">
            <div class="card-body">
                <div class="slide-preview-container">
                    <div class="slide-preview">
                        <div class="slide-order-badge">${slide.orden}</div>
                        ${tieneImagen ? `
                            <img src="${slide.imagenUrl}" alt="Slide ${slide.orden}" onerror="this.src='/admin/images/no-image.png'">
                        ` : `
                            <div class="slide-preview-placeholder">
                                <i class="bi bi-image"></i>
                                <p class="mb-0">Slide vacío</p>
                                <small>1080x720px</small>
                            </div>
                        `}
                    </div>
                </div>

                <div class="marca-info ${tieneMarca ? '' : 'empty'} mb-3">
                    <i class="bi bi-tag me-2"></i>
                    <span>${tieneMarca ? slide.marca.nombre : 'Sin marca asignada'}</span>
                </div>

                <div class="d-grid gap-2">
                    <label class="btn btn-primary btn-sm" for="slideFile${slide.orden}">
                        <i class="bi bi-cloud-upload me-2"></i>Subir Imagen
                    </label>
                    <input type="file"
                           id="slideFile${slide.orden}"
                           class="d-none"
                           accept="image/*"
                           onchange="handleSlideUpload(event, ${slide.orden})">

                    <button class="btn btn-outline-secondary btn-sm" onclick="abrirModalAsignarMarca(${slide.orden})">
                        <i class="bi bi-tag me-2"></i>Asignar Marca
                    </button>

                    <button class="btn btn-outline-danger btn-sm"
                            onclick="confirmarLimpiarSlide(${slide.orden})"
                            ${!tieneImagen ? 'disabled' : ''}>
                        <i class="bi bi-trash me-2"></i>Limpiar
                    </button>
                </div>
            </div>
        </div>
    `;

    return col;
}

// ==================== UPLOAD SLIDE ====================
async function handleSlideUpload(e, orden) {
    const file = e.target.files[0];
    if (!file) return;

    // Validar que sea imagen
    if (!file.type.startsWith('image/')) {
        showNotification('Por favor selecciona una imagen válida', 'error');
        e.target.value = '';
        return;
    }

    // Validar tamaño (5MB)
    if (file.size > 5 * 1024 * 1024) {
        showNotification('La imagen no debe superar los 5MB', 'error');
        e.target.value = '';
        return;
    }

    try {
        Swal.fire({
            title: 'Subiendo imagen...',
            text: 'Por favor espera',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        // Subir imagen al servidor
        const imageUrl = await uploadImageToServer(file, 'personalizacion');

        // Actualizar slide en base de datos
        const formData = new URLSearchParams();
        formData.append('imagenUrl', imageUrl);

        const response = await fetch(`/personalizacion/api/slide/${orden}/imagen`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            Swal.fire({
                icon: 'success',
                title: '¡Imagen actualizada!',
                text: 'El slide se ha actualizado correctamente',
                timer: 2000,
                showConfirmButton: false
            });
            cargarSlides(); // Recargar slides
        } else {
            throw new Error(result.message || 'Error al actualizar slide');
        }

    } catch (error) {
        console.error('Error al subir imagen del slide:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: error.message || 'Error al subir la imagen'
        });
    } finally {
        e.target.value = '';
    }
}

// ==================== CARGAR MARCAS DISPONIBLES ====================
async function cargarMarcasDisponibles() {
    try {
        const response = await fetch('/personalizacion/api/marcas-disponibles');
        const result = await response.json();

        if (result.success) {
            marcasDisponibles = result.data;
            llenarSelectMarcas(result.data);
        }
    } catch (error) {
        console.error('Error al cargar marcas:', error);
    }
}

function llenarSelectMarcas(marcas) {
    const select = document.getElementById('selectMarca');
    if (!select) return;

    // Limpiar opciones excepto la primera
    $('#selectMarca').empty().append('<option value="">Sin marca</option>');

    marcas.forEach(marca => {
        const option = new Option(marca.nombre, marca.id);
        $('#selectMarca').append(option);
    });
}

// ==================== MODAL ASIGNAR MARCA ====================
function abrirModalAsignarMarca(orden) {
    slideSeleccionado = orden;

    // Buscar slide actual
    const slide = slidesActuales.find(s => s.orden === orden);

    // Pre-seleccionar marca si existe
    if (slide && slide.marca) {
        $('#selectMarca').val(slide.marca.id).trigger('change');
    } else {
        $('#selectMarca').val('').trigger('change');
    }

    document.getElementById('slideOrdenModal').value = orden;
    modalAsignarMarca.show();
}

async function guardarMarcaSlide() {
    const orden = slideSeleccionado;
    const marcaId = $('#selectMarca').val();

    if (!orden) {
        showNotification('Error: No se seleccionó un slide', 'error');
        return;
    }

    try {
        modalAsignarMarca.hide();

        Swal.fire({
            title: 'Guardando...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        const formData = new URLSearchParams();
        if (marcaId) {
            formData.append('marcaId', marcaId);
        }

        const response = await fetch(`/personalizacion/api/slide/${orden}/marca`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            Swal.fire({
                icon: 'success',
                title: '¡Marca actualizada!',
                timer: 2000,
                showConfirmButton: false
            });
            cargarSlides(); // Recargar slides
        } else {
            throw new Error(result.message || 'Error al asignar marca');
        }

    } catch (error) {
        console.error('Error al asignar marca:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: error.message || 'Error al asignar la marca'
        });
    }
}

// ==================== LIMPIAR SLIDE ====================
async function confirmarLimpiarSlide(orden) {
    const result = await Swal.fire({
        title: '¿Limpiar slide?',
        text: 'Se eliminará la imagen y la marca asignada',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545',
        cancelButtonColor: '#6c757d',
        confirmButtonText: 'Sí, limpiar',
        cancelButtonText: 'Cancelar'
    });

    if (result.isConfirmed) {
        await limpiarSlide(orden);
    }
}

async function limpiarSlide(orden) {
    try {
        Swal.fire({
            title: 'Limpiando slide...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });

        const response = await fetch(`/personalizacion/api/slide/${orden}`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            Swal.fire({
                icon: 'success',
                title: '¡Slide limpiado!',
                timer: 2000,
                showConfirmButton: false
            });
            cargarSlides(); // Recargar slides
        } else {
            throw new Error(result.message || 'Error al limpiar slide');
        }

    } catch (error) {
        console.error('Error al limpiar slide:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: error.message || 'Error al limpiar el slide'
        });
    }
}

// ==================== NOTIFICACIONES ====================
function showNotification(message, type = 'info') {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: message,
            icon: type,
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000
        });
    } else {
        alert(message);
    }
}

console.log('✅ personalizacion.js inicializado correctamente');
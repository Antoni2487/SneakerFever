// ==================== CONFIGURACIÓN ====================
const MAX_IMAGES = 5;
let tipoEntidad = 'productos';
let sortableInstance = null; 

// ==================== INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    initTabs();        // Tus tabs originales
    initInputs();      // Listeners para URL y Archivos
    initSortable();    // Magia para ordenar
    
    console.log('✅ Sistema de imágenes optimizado cargado');
});

function setTipoEntidad(tipo) {
    tipoEntidad = tipo;
}

// ==================== 1. ORDENAMIENTO VISUAL (Drag & Drop) ====================
function initSortable() {
    const container = document.getElementById('imagePreviewContainer');
    if (container && typeof Sortable !== 'undefined') {
        sortableInstance = new Sortable(container, {
            animation: 150,
            ghostClass: 'sortable-ghost', // Clase cuando arrastras
            onEnd: function () {
                renumerarImagenes(); // Recalcular números 1, 2, 3...
            }
        });
    } else {
        console.warn('⚠️ SortableJS no está cargado. El arrastre no funcionará, pero la carga sí.');
    }
}

function renumerarImagenes() {
    document.querySelectorAll('.image-preview-item .image-number').forEach((span, index) => {
        span.textContent = index + 1;
    });
}

// ==================== 2. GESTIÓN DE PREVIEWS (La "Fuente de la Verdad") ====================
function showImagePreview(source, type, fileObject = null) {
    const container = document.getElementById('imagePreviewContainer');
    // Quitar placeholder
    const placeholder = document.getElementById('imagePreviewPlaceholder');
    if (placeholder) placeholder.remove();

    const displayIndex = container.children.length + 1;

    const div = document.createElement('div');
    div.className = `image-preview-item ${type === 'FILE' ? 'new-image' : 'existing-image'}`;
    
    // 🧠 TRUCO: Guardamos el archivo en el mismo elemento HTML
    if (fileObject) {
        div.fileObject = fileObject; 
    } else {
        div.dataset.url = source;
    }

    div.innerHTML = `
        <img src="${source}" class="preview-image" onerror="this.src='/admin/images/no-image.png'">
        <span class="image-number">${displayIndex}</span>
        <button type="button" class="btn-remove-preview"><i class="bi bi-x"></i></button>
    `;

    // Botón eliminar
    div.querySelector('.btn-remove-preview').onclick = function() {
        div.remove();
        if (container.children.length === 0) mostrarPlaceholder();
        renumerarImagenes();
    };

    container.appendChild(div);
}

function mostrarPlaceholder() {
    const container = document.getElementById('imagePreviewContainer');
    container.innerHTML = `
        <div id="imagePreviewPlaceholder" class="image-preview-placeholder">
            <i class="bi bi-image"></i>
            <p>Arrastra imágenes aquí o selecciona archivos</p>
            <small>Máximo ${MAX_IMAGES} imágenes</small>
        </div>
    `;
    // Limpiar inputs
    document.getElementById('imagenFile').value = '';
    document.getElementById('imagenUrl').value = '';
}

// ==================== 3. MANEJO DE TUS TABS Y INPUTS ====================
function initTabs() {
    const tabs = document.querySelectorAll('.image-source-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            // Quitar activo a todos
            document.querySelectorAll('.image-source-tab').forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content-panel').forEach(p => p.classList.remove('active'));
            
            // Poner activo al actual
            this.classList.add('active');
            const targetId = this.getAttribute('data-tab');
            document.getElementById(targetId).classList.add('active');
        });
    });
}

function initInputs() {
    // Input Archivo
    const fileInput = document.getElementById('imagenFile');
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            handleFiles(e.target.files);
            this.value = ''; 
        });
    }

    // Input URL
    const urlInput = document.getElementById('imagenUrl');
    if (urlInput) {
        urlInput.addEventListener('change', function() {
            if (this.value.trim()) {
                if (validarLimite()) {
                    showImagePreview(this.value.trim(), 'URL');
                    this.value = '';
                }
            }
        });
    }
}

function handleFiles(files) {
    if (!validarLimite(files.length)) return;

    Array.from(files).forEach(file => {
        if (!file.type.startsWith('image/')) return alert('Solo imágenes');
        
        const reader = new FileReader();
        reader.onload = (e) => {
            showImagePreview(e.target.result, 'FILE', file);
        };
        reader.readAsDataURL(file);
    });
}

function validarLimite(nuevas = 1) {
    const actuales = document.querySelectorAll('.image-preview-item').length;
    if (actuales + nuevas > MAX_IMAGES) {
        alert(`Límite de ${MAX_IMAGES} imágenes excedido.`);
        return false;
    }
    return true;
}

// ==================== 4. CARGAR AL EDITAR ====================
function mostrarImagenesExistentes(urls) {
    const container = document.getElementById('imagePreviewContainer');
    container.innerHTML = ''; // Limpiar todo
    if (urls && urls.length > 0) {
        urls.forEach(url => showImagePreview(url, 'URL'));
    } else {
        mostrarPlaceholder();
    }
}

// ==================== 5. LA FUNCIÓN MÁGICA (ESTA USAS AL GUARDAR) ====================
async function obtenerImagenesOrdenadas() {
    const items = document.querySelectorAll('.image-preview-item');
    const urlsFinales = [];

    // Recorremos los elementos EN EL ORDEN QUE LOS VES
    for (const item of items) {
        // A) Es un archivo nuevo -> Lo subimos
        if (item.classList.contains('new-image') && item.fileObject) {
            try {
                const url = await uploadOneImage(item.fileObject);
                urlsFinales.push(url);
                // Lo convertimos en "existente" para no resubirlo si falla otro paso
                item.classList.remove('new-image');
                item.dataset.url = url;
                delete item.fileObject;
            } catch (err) {
                console.error(err);
                throw new Error("Error subiendo imagen nueva");
            }
        } 
        // B) Es una URL existente -> La guardamos tal cual
        else if (item.dataset.url) {
            urlsFinales.push(item.dataset.url);
        }
    }
    return urlsFinales;
}

async function uploadOneImage(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    // Usa tu endpoint existente
    const res = await fetch(`/api/upload/${tipoEntidad}/imagen`, { method: 'POST', body: formData });
    if (!res.ok) throw new Error('Fallo upload');
    const data = await res.json();
    return data.url;
}
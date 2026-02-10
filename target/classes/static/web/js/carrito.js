// ✅ carrito.js - CON CONTROLES DE CANTIDAD

document.addEventListener("DOMContentLoaded", function() {
    actualizarCarritoVisual();
});

// 1. AGREGAR (Desde el catálogo)
window.agregarAlCarrito = function(idProducto) {
    const btn = document.querySelector(`button[onclick*="${idProducto}"]`);
    let originalContent = '';
    
    if(btn) {
        originalContent = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
        btn.disabled = true;
    }

    fetch(`/carrito/api/agregar/${idProducto}`, { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                const offcanvasEl = document.getElementById('offcanvasCarrito');
                if (offcanvasEl) {
                    const offcanvas = new bootstrap.Offcanvas(offcanvasEl);
                    offcanvas.show();
                }
                actualizarCarritoVisual();
            } else {
                alert(data.message || 'Error al agregar');
            }
        })
        .catch(err => console.error("Error:", err))
        .finally(() => {
            if(btn) {
                btn.innerHTML = originalContent;
                btn.disabled = false;
            }
        });
};

// 2. ACTUALIZAR CANTIDAD (+ / -)
window.cambiarCantidad = function(idProducto, nuevaCantidad) {
    if (nuevaCantidad < 1) return; // Evitar negativos (el botón eliminar es para borrar)

    fetch(`/carrito/api/actualizar/${idProducto}?cantidad=${nuevaCantidad}`, { method: 'PUT' })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                actualizarCarritoVisual();
            }
        });
};

// 3. ELIMINAR
window.eliminarDelCarrito = function(id) {
    fetch(`/carrito/api/eliminar/${id}`, { method: 'DELETE' })
        .then(() => actualizarCarritoVisual());
};

// 4. RENDERIZAR
function actualizarCarritoVisual() {
    fetch('/carrito/api/ver')
        .then(res => res.json())
        .then(carrito => {
            // Badge
            const count = carrito.items.reduce((acc, item) => acc + item.cantidad, 0);
            document.querySelectorAll('.cart-badge').forEach(el => el.innerText = count);
            
            // Contenedor
            const container = document.getElementById('carrito-items-container');
            if (!container) return;

            if (carrito.items.length === 0) {
                container.innerHTML = `
                    <div class="text-center text-muted mt-5">
                        <i class="fa-solid fa-cart-shopping fa-3x mb-3 opacity-25"></i>
                        <p>Tu carrito está vacío</p>
                    </div>`;
                document.getElementById('carrito-total').innerText = 'S/ 0.00';
            } else {
                let html = '';
                carrito.items.forEach(item => {
                    html += `
                        <div class="card mb-3 border-0 shadow-sm">
                            <div class="card-body p-2">
                                <div class="d-flex align-items-center">
                                    <img src="${item.imagen || '/web/images/placeholder.png'}" 
                                         style="width: 60px; height: 60px; object-fit: cover;" 
                                         class="rounded me-3">
                                    
                                    <div class="flex-grow-1">
                                        <h6 class="mb-1 text-truncate" style="max-width: 160px; font-size: 0.9rem;">
                                            ${item.nombre}
                                        </h6>
                                        <div class="text-muted small mb-2">S/ ${item.precio.toFixed(2)} c/u</div>
                                        
                                        <div class="d-flex justify-content-between align-items-center">
                                            <div class="input-group input-group-sm" style="width: 90px;">
                                                <button class="btn btn-outline-secondary px-2" 
                                                        onclick="cambiarCantidad(${item.productoId}, ${item.cantidad - 1})"
                                                        ${item.cantidad <= 1 ? 'disabled' : ''}>
                                                    -
                                                </button>
                                                <input type="text" class="form-control text-center px-0 bg-white" 
                                                       value="${item.cantidad}" readonly style="font-size: 0.8rem;">
                                                <button class="btn btn-outline-secondary px-2" 
                                                        onclick="cambiarCantidad(${item.productoId}, ${item.cantidad + 1})">
                                                    +
                                                </button>
                                            </div>
                                            
                                            <div class="text-end">
                                                <div class="fw-bold small mb-1">S/ ${item.subtotal.toFixed(2)}</div>
                                                <button onclick="eliminarDelCarrito(${item.productoId})" 
                                                        class="btn btn-link text-danger p-0 text-decoration-none" 
                                                        style="font-size: 0.8rem;">
                                                    <i class="fas fa-trash-alt"></i> Eliminar
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                });
                container.innerHTML = html;
                
                // Total
                if(document.getElementById('carrito-total')) {
                    document.getElementById('carrito-total').innerText = 'S/ ' + parseFloat(carrito.total).toFixed(2);
                }
            }
        });
}
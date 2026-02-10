// src/main/resources/static/web/js/servicios.js

const API_URL = "http://localhost:3000/api";

document.addEventListener("DOMContentLoaded", function() {
    
    // 1. CARGAR INFO DEL FOOTER (Se ejecuta si existe el footer)
    if(document.getElementById('node-direccion')) {
        cargarInfoTienda();
    }

    // 2. CARGAR PREGUNTAS FRECUENTES (Se ejecuta si estamos en la pagina de preguntas)
    if(document.getElementById('lista-preguntas')) {
        cargarPreguntas();
    }

    // 3. MANEJAR FORMULARIO DE CONTACTO (Se ejecuta si existe el formulario)
    const formContacto = document.getElementById('formContacto');
    if(formContacto) {
        formContacto.addEventListener('submit', enviarContacto);
    }
});

// --- FUNCIONES ---

function cargarInfoTienda() {
    fetch(`${API_URL}/informacion-tienda`)
        .then(res => res.json())
        .then(data => {
            if(data.success) {
                const info = data.data;
                // Actualiza los textos del footer
                const dir = document.getElementById('node-direccion');
                const email = document.getElementById('node-email');
                const horario = document.getElementById('node-horario');

                if(dir) dir.innerText = `${info.direccion.calle}, ${info.direccion.distrito}`;
                if(email) email.innerText = info.contacto.email;
                if(horario) horario.innerText = info.horarios.lunes_viernes;
            }
        })
        .catch(err => console.error("Error cargando info tienda:", err));
}

function cargarPreguntas() {
    fetch(`${API_URL}/preguntas-frecuentes`)
        .then(res => res.json())
        .then(data => {
            const contenedor = document.getElementById('lista-preguntas');
            contenedor.innerHTML = ''; 

            if(data.success) {
                data.data.forEach((item, index) => {
                    contenedor.innerHTML += `
                        <div class="accordion-item">
                            <h2 class="accordion-header" id="heading${index}">
                                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse${index}">
                                    ${item.categoria}: ${item.pregunta}
                                </button>
                            </h2>
                            <div id="collapse${index}" class="accordion-collapse collapse" data-bs-parent="#accordionExample">
                                <div class="accordion-body">
                                    ${item.respuesta}
                                </div>
                            </div>
                        </div>`;
                });
            }
        });
}

function enviarContacto(e) {
    e.preventDefault();
    
    const datos = {
        nombre: document.getElementById('nombre').value,
        email: document.getElementById('email').value,
        mensaje: document.getElementById('mensaje').value
    };

    fetch(`${API_URL}/contacto`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(datos)
    })
    .then(res => res.json())
    .then(data => {
        const div = document.getElementById('respuestaNode');
        if(data.success) {
            div.innerHTML = `<div class="alert alert-success mt-3">✅ Enviado! ID: ${data.data.id}</div>`;
            document.getElementById('formContacto').reset();
        } else {
            div.innerHTML = `<div class="alert alert-danger mt-3">❌ Error</div>`;
        }
    });
}
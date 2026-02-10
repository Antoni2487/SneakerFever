// dashboard.js - Mejorado y corregido

let graficoVentas = null;

// ==================== CARGAR DATOS AL INICIAR ====================
$(document).ready(function() {
    console.log('Dashboard cargado');
    cargarEstadisticas();
    cargarClientes();
    cargarProductos();
});

// ==================== CARGAR ESTADÍSTICAS GENERALES ====================
function cargarEstadisticas() {
    $.ajax({
        url: '/admin/api/estadisticas',
        method: 'GET',
        success: function(response) {
            console.log('Estadísticas recibidas:', response);
            
            if (response.success && response.data) {
                const data = response.data;
                
                // Actualizar Total Ventas
                $('#totalVentas').text(data.totalVentas || 0);
                
                // Actualizar Monto Total
                const montoTotal = data.montoTotal || 0;
                $('#montoTotal').text('S/ ' + montoTotal.toFixed(2));
                
                // Si hay datos de gráfico, crear el gráfico
                if (data.ventasPorMes && data.ventasPorMes.length > 0) {
                    crearGraficoVentas(data.ventasPorMes);
                } else {
                    crearGraficoVacio();
                }
            } else {
                console.error('Error en respuesta:', response.message);
                mostrarError('estadísticas');
            }
        },
        error: function(xhr, status, error) {
            console.error('Error al cargar estadísticas:', error);
            console.error('Status:', status);
            console.error('Response:', xhr.responseText);
            mostrarError('estadísticas');
        }
    });
}

// ==================== CARGAR CLIENTES ====================
function cargarClientes() {
    $.ajax({
        url: '/admin/api/count-clientes',
        method: 'GET',
        success: function(response) {
            console.log('Clientes recibidos:', response);
            
            if (response.success) {
                $('#totalClientes').text(response.total || 0);
            } else {
                console.error('Error al contar clientes:', response.message);
                $('#totalClientes').text('Error');
            }
        },
        error: function(xhr, status, error) {
            console.error('Error al cargar clientes:', error);
            console.error('Response:', xhr.responseText);
            $('#totalClientes').text('Error');
        }
    });
}

// ==================== CARGAR PRODUCTOS ====================
function cargarProductos() {
    $.ajax({
        url: '/admin/api/listar-productos',
        method: 'GET',
        success: function(response) {
            console.log('Productos recibidos:', response);
            
            if (response.success && response.data) {
                // Contar solo productos activos
                const productosActivos = response.data.filter(p => p.estado === true || p.activo === true);
                $('#totalProductos').text(productosActivos.length);
            } else {
                console.error('Error al listar productos:', response.message);
                $('#totalProductos').text('Error');
            }
        },
        error: function(xhr, status, error) {
            console.error('Error al cargar productos:', error);
            console.error('Response:', xhr.responseText);
            $('#totalProductos').text('Error');
        }
    });
}

// ==================== CREAR GRÁFICO DE VENTAS ====================
function crearGraficoVentas(ventasPorMes) {
    const ctx = document.getElementById('graficoVentas');
    
    if (!ctx) {
        console.error('No se encontró el canvas del gráfico');
        return;
    }
    
    // Destruir gráfico anterior si existe
    if (graficoVentas) {
        graficoVentas.destroy();
    }
    
    // Preparar datos para el gráfico
    const labels = ventasPorMes.map(item => item.mes || item.fecha || 'Mes');
    const datos = ventasPorMes.map(item => item.total || item.cantidad || 0);
    
    graficoVentas = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Ventas por Mes',
                data: datos,
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.1)',
                tension: 0.4,
                fill: true,
                borderWidth: 3,
                pointRadius: 5,
                pointHoverRadius: 7,
                pointBackgroundColor: 'rgb(75, 192, 192)',
                pointBorderColor: '#fff',
                pointBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    titleFont: {
                        size: 14
                    },
                    bodyFont: {
                        size: 13
                    },
                    callbacks: {
                        label: function(context) {
                            return 'Ventas: ' + context.parsed.y;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

// ==================== CREAR GRÁFICO VACÍO ====================
function crearGraficoVacio() {
    const ctx = document.getElementById('graficoVentas');
    
    if (!ctx) return;
    
    if (graficoVentas) {
        graficoVentas.destroy();
    }
    
    graficoVentas = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun'],
            datasets: [{
                label: 'Sin datos disponibles',
                data: [0, 0, 0, 0, 0, 0],
                borderColor: 'rgb(200, 200, 200)',
                backgroundColor: 'rgba(200, 200, 200, 0.1)',
                tension: 0.4,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

// ==================== MOSTRAR ERROR ====================
function mostrarError(tipo) {
    switch(tipo) {
        case 'estadísticas':
            $('#totalVentas').text('Error');
            $('#montoTotal').text('Error');
            crearGraficoVacio();
            break;
    }
}

// ==================== RECARGAR DASHBOARD ====================
function recargarDashboard() {
    cargarEstadisticas();
    cargarClientes();
    cargarProductos();
}
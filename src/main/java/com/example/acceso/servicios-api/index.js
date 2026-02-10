// Importar dependencias
const express = require('express');
const cors = require('cors');

// Importar rutas
const contactoRoutes = require('./routes/contacto');
const preguntasRoutes = require('./routes/preguntas');
const tiendaRoutes = require('./routes/tienda');

// Crear aplicación Express
const app = express();
const PORT = 3000;

// Middlewares
app.use(cors());
app.use(express.json());

// Rutas principales
app.use('/api/contacto', contactoRoutes);
app.use('/api/preguntas-frecuentes', preguntasRoutes);
app.use('/api/informacion-tienda', tiendaRoutes);

// Ruta de bienvenida
app.get('/', (req, res) => {
  res.json({
    mensaje: '✅ API de Servicios - SneackerFever Perú',
    servicios_disponibles: [
      'GET  /api/preguntas-frecuentes',
      'GET  /api/informacion-tienda',
      'GET  /api/contacto',
      'POST /api/contacto'
    ],
    estado: 'Servidor funcionando correctamente'
  });
});

// Manejo de errores
app.use((err, req, res, next) => {
  console.error('❌ Error:', err.message);
  res.status(500).json({
    success: false,
    mensaje: 'Error en el servidor',
    error: err.message
  });
});

// Iniciar servidor
app.listen(PORT, () => {
  console.log('========================================');
  console.log('🚀 SERVIDOR INICIADO CORRECTAMENTE');
  console.log(`📡 URL: http://localhost:${PORT}`);
  console.log('========================================');
  console.log('📋 Servicios disponibles:');
  console.log('   GET  /api/preguntas-frecuentes');
  console.log('   GET  /api/informacion-tienda');
  console.log('   GET  /api/contacto');
  console.log('   POST /api/contacto');
  console.log('========================================');
});
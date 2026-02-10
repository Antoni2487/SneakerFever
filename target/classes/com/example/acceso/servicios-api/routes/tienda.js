const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');

// Ruta del archivo JSON
const tiendaPath = path.join(__dirname, '../data/tienda.json');

// GET - Obtener toda la información de la tienda
router.get('/', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(tiendaPath, 'utf-8'));
    res.json({
      success: true,
      data: data.informacion
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener información de la tienda',
      error: error.message
    });
  }
});

// GET - Obtener solo datos de contacto
router.get('/contacto', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(tiendaPath, 'utf-8'));
    res.json({
      success: true,
      data: {
        nombre: data.informacion.nombre,
        direccion: data.informacion.direccion,
        contacto: data.informacion.contacto
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener datos de contacto',
      error: error.message
    });
  }
});

// GET - Obtener solo horarios
router.get('/horarios', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(tiendaPath, 'utf-8'));
    res.json({
      success: true,
      data: data.informacion.horarios
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener horarios',
      error: error.message
    });
  }
});

// GET - Obtener redes sociales
router.get('/redes-sociales', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(tiendaPath, 'utf-8'));
    res.json({
      success: true,
      data: data.informacion.redes_sociales
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener redes sociales',
      error: error.message
    });
  }
});

module.exports = router;
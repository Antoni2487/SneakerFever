const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');

// Ruta del archivo JSON
const preguntasPath = path.join(__dirname, '../data/preguntas.json');

// GET - Obtener todas las preguntas frecuentes
router.get('/', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(preguntasPath, 'utf-8'));
    res.json({
      success: true,
      total: data.preguntas_frecuentes.length,
      data: data.preguntas_frecuentes
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener las preguntas frecuentes',
      error: error.message
    });
  }
});

// GET - Obtener preguntas por categoría
router.get('/categoria/:categoria', (req, res) => {
  try {
    const { categoria } = req.params;
    const data = JSON.parse(fs.readFileSync(preguntasPath, 'utf-8'));
    
    const preguntasFiltradas = data.preguntas_frecuentes.filter(
      p => p.categoria.toLowerCase() === categoria.toLowerCase()
    );

    res.json({
      success: true,
      categoria: categoria,
      total: preguntasFiltradas.length,
      data: preguntasFiltradas
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al filtrar preguntas',
      error: error.message
    });
  }
});

// GET - Obtener una pregunta por ID
router.get('/:id', (req, res) => {
  try {
    const { id } = req.params;
    const data = JSON.parse(fs.readFileSync(preguntasPath, 'utf-8'));
    
    const pregunta = data.preguntas_frecuentes.find(p => p.id === parseInt(id));

    if (!pregunta) {
      return res.status(404).json({
        success: false,
        mensaje: 'Pregunta no encontrada'
      });
    }

    res.json({
      success: true,
      data: pregunta
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener la pregunta',
      error: error.message
    });
  }
});

module.exports = router;
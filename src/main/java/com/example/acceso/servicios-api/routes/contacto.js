const express = require('express');
const router = express.Router();
const fs = require('fs');
const path = require('path');

// Ruta del archivo JSON
const contactoPath = path.join(__dirname, '../data/contacto.json');

// GET - Obtener todos los mensajes de contacto
router.get('/', (req, res) => {
  try {
    const data = JSON.parse(fs.readFileSync(contactoPath, 'utf-8'));
    res.json({
      success: true,
      total: data.mensajes.length,
      data: data.mensajes
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al obtener los mensajes',
      error: error.message
    });
  }
});

// POST - Enviar un nuevo mensaje de contacto
router.post('/', (req, res) => {
  try {
    const { nombre, email, mensaje } = req.body;

    // Validación básica
    if (!nombre || !email || !mensaje) {
      return res.status(400).json({
        success: false,
        mensaje: 'Todos los campos son obligatorios (nombre, email, mensaje)'
      });
    }

    // Leer archivo actual
    const data = JSON.parse(fs.readFileSync(contactoPath, 'utf-8'));

    // Crear nuevo mensaje
    const nuevoMensaje = {
      id: data.mensajes.length + 1,
      nombre,
      email,
      mensaje,
      fecha: new Date().toISOString(),
      estado: 'pendiente'
    };

    // Agregar mensaje
    data.mensajes.push(nuevoMensaje);

    // Guardar en el archivo
    fs.writeFileSync(contactoPath, JSON.stringify(data, null, 2));

    res.status(201).json({
      success: true,
      mensaje: 'Mensaje enviado correctamente. Te responderemos pronto.',
      data: nuevoMensaje
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      mensaje: 'Error al enviar el mensaje',
      error: error.message
    });
  }
});

module.exports = router;
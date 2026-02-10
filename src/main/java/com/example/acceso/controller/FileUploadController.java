package com.example.acceso.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${upload.path:src/main/resources/static/uploads}")
    private String uploadPath;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_FILES_PER_PRODUCT = 5;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_TYPES = Arrays.asList("productos", "marcas", "categorias", "personalizacion");

    /**
     * Endpoint para subir UNA imagen
     * POST /api/upload/productos/imagen
     * POST /api/upload/marcas/imagen
     */
    @PostMapping("/{tipo}/imagen")
    public ResponseEntity<Map<String, Object>> uploadImagen(
            @PathVariable String tipo,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validar tipo permitido
            if (!ALLOWED_TYPES.contains(tipo.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo no válido. Permitidos: " + ALLOWED_TYPES);
                return ResponseEntity.badRequest().body(response);
            }

            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "El archivo está vacío");
                return ResponseEntity.badRequest().body(response);
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("message", "El archivo excede el tamaño máximo permitido (5MB)");
                return ResponseEntity.badRequest().body(response);
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG, GIF y WEBP");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                response.put("success", false);
                response.put("message", "Nombre de archivo inválido");
                return ResponseEntity.badRequest().body(response);
            }

            String extension = getFileExtension(originalFilename).toLowerCase();

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                response.put("success", false);
                response.put("message", "Extensión de archivo no permitida");
                return ResponseEntity.badRequest().body(response);
            }

            String newFilename = UUID.randomUUID().toString() + "." + extension;

            // Crear directorio específico para el tipo (uploads/productos, uploads/marcas, etc.)
            Path uploadDir = Paths.get(uploadPath, tipo);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + tipo + "/" + newFilename;

            response.put("success", true);
            response.put("message", "Imagen subida exitosamente");
            response.put("url", fileUrl);
            response.put("filename", newFilename);
            response.put("originalFilename", originalFilename);
            response.put("size", file.getSize());
            response.put("tipo", tipo);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al guardar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para subir MÚLTIPLES imágenes
     * POST /api/upload/productos/imagenes
     * POST /api/upload/marcas/imagenes
     */
    @PostMapping("/{tipo}/imagenes")
    public ResponseEntity<Map<String, Object>> uploadImagenes(
            @PathVariable String tipo,
            @RequestParam("files") MultipartFile[] files) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Validar tipo permitido
            if (!ALLOWED_TYPES.contains(tipo.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo no válido. Permitidos: " + ALLOWED_TYPES);
                return ResponseEntity.badRequest().body(response);
            }

            if (files == null || files.length == 0) {
                response.put("success", false);
                response.put("message", "No se han enviado archivos");
                return ResponseEntity.badRequest().body(response);
            }

            if (files.length > MAX_FILES_PER_PRODUCT) {
                response.put("success", false);
                response.put("message", "Solo se permiten hasta " + MAX_FILES_PER_PRODUCT + " imágenes");
                return ResponseEntity.badRequest().body(response);
            }

            // Crear directorio específico para el tipo
            Path uploadDir = Paths.get(uploadPath, tipo);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];

                try {
                    String validationError = validateFile(file);
                    if (validationError != null) {
                        errors.add("Archivo " + (i + 1) + ": " + validationError);
                        continue;
                    }

                    String originalFilename = file.getOriginalFilename();
                    String extension = getFileExtension(originalFilename).toLowerCase();

                    String newFilename = UUID.randomUUID().toString() + "." + extension;

                    Path filePath = uploadDir.resolve(newFilename);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("url", "/uploads/" + tipo + "/" + newFilename);
                    fileInfo.put("filename", newFilename);
                    fileInfo.put("originalFilename", originalFilename);
                    fileInfo.put("size", file.getSize());
                    fileInfo.put("order", i);

                    uploadedFiles.add(fileInfo);

                } catch (IOException e) {
                    errors.add("Archivo " + (i + 1) + ": Error al guardar - " + e.getMessage());
                }
            }

            if (uploadedFiles.isEmpty()) {
                response.put("success", false);
                response.put("message", "No se pudo subir ninguna imagen");
                response.put("errors", errors);
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("message", "Se subieron " + uploadedFiles.size() + " de " + files.length + " imágenes");
            response.put("files", uploadedFiles);
            response.put("totalUploaded", uploadedFiles.size());
            response.put("totalFiles", files.length);
            response.put("tipo", tipo);

            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para eliminar una imagen
     * DELETE /api/upload/productos/imagen/{filename}
     * DELETE /api/upload/marcas/imagen/{filename}
     */
    @DeleteMapping("/{tipo}/imagen/{filename}")
    public ResponseEntity<Map<String, Object>> deleteImagen(
            @PathVariable String tipo,
            @PathVariable String filename) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validar tipo permitido
            if (!ALLOWED_TYPES.contains(tipo.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                response.put("success", false);
                response.put("message", "Nombre de archivo inválido");
                return ResponseEntity.badRequest().body(response);
            }

            Path filePath = Paths.get(uploadPath, tipo).resolve(filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                response.put("success", true);
                response.put("message", "Imagen eliminada exitosamente");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Archivo no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error al eliminar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint para eliminar múltiples imágenes
     * DELETE /api/upload/productos/imagenes
     * DELETE /api/upload/marcas/imagenes
     */
    @DeleteMapping("/{tipo}/imagenes")
    public ResponseEntity<Map<String, Object>> deleteImagenes(
            @PathVariable String tipo,
            @RequestBody List<String> filenames) {

        Map<String, Object> response = new HashMap<>();
        List<String> deletedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            // Validar tipo permitido
            if (!ALLOWED_TYPES.contains(tipo.toLowerCase())) {
                response.put("success", false);
                response.put("message", "Tipo no válido");
                return ResponseEntity.badRequest().body(response);
            }

            if (filenames == null || filenames.isEmpty()) {
                response.put("success", false);
                response.put("message", "No se especificaron archivos para eliminar");
                return ResponseEntity.badRequest().body(response);
            }

            for (String filename : filenames) {
                try {
                    if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                        errors.add(filename + ": Nombre de archivo inválido");
                        continue;
                    }

                    Path filePath = Paths.get(uploadPath, tipo).resolve(filename);

                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        deletedFiles.add(filename);
                    } else {
                        errors.add(filename + ": Archivo no encontrado");
                    }
                } catch (IOException e) {
                    errors.add(filename + ": Error al eliminar - " + e.getMessage());
                }
            }

            response.put("success", !deletedFiles.isEmpty());
            response.put("message", "Se eliminaron " + deletedFiles.size() + " de " + filenames.size() + " imágenes");
            response.put("deletedFiles", deletedFiles);
            response.put("totalDeleted", deletedFiles.size());

            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "El archivo está vacío";
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "El archivo excede el tamaño máximo permitido (5MB)";
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return "Tipo de archivo no permitido. Solo se aceptan imágenes JPG, PNG, GIF y WEBP";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "Nombre de archivo inválido";
        }

        String extension = getFileExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "Extensión de archivo no permitida";
        }

        return null;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1);
        }
        return "";
    }
}
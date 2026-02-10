package com.example.acceso.controller;

import com.example.acceso.model.Brand;
import com.example.acceso.service.BrandService;
import com.example.acceso.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate; // ⚠️ IMPORTANTE

import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    private final BrandService brandService;
    private final ProductService productService;
    private final RestTemplate restTemplate; // ⚠️ Inyección para llamar a Node

    // Constructor con inyección de dependencias
    public WebController(BrandService brandService, ProductService productService, RestTemplate restTemplate) {
        this.brandService = brandService;
        this.productService = productService;
        this.restTemplate = restTemplate;
    }

    // --- TUS OTRAS RUTAS (Index, Catalogo, etc) SIGUEN IGUAL ---
    @GetMapping("/")
    public String index(Model model) {
        cargarDatosComunes(model);
        model.addAttribute("zapatillas", productService.obtenerDestacadosPorCategoria("Zapatillas"));
        model.addAttribute("ropa", productService.obtenerDestacadosPorCategoria("Ropa"));
        model.addAttribute("accesorios", productService.obtenerDestacadosPorCategoria("Accesorios"));
        model.addAttribute("zapatillasMasVendidas", productService.obtenerZapatillasMasVendidas());
        try {
            String urlNode = "http://127.0.0.1:3000/api/preguntas-frecuentes";
            Object rawResponse = restTemplate.getForObject(urlNode, Object.class);
            
            if (rawResponse instanceof Map) {
                Map<String, Object> mapa = (Map<String, Object>) rawResponse;
                if (mapa.containsKey("data")) {
                    model.addAttribute("listaPreguntas", mapa.get("data"));
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Node no disponible en Home: " + e.getMessage());
            // No hacemos nada, simplemente no se mostrará la sección
        }

        return "web/sneacker";
    }

    @GetMapping("/catalogo")
    public String catalogo(Model model) {
        cargarDatosComunes(model);
        return "web/catalogo"; // Asegúrate de tener este template
    }


    @GetMapping("/contacto")
    public String verContacto(Model model) {
        cargarDatosComunes(model);
        return "web/contacto";
    }

    // Método auxiliar para cargar datos globales
    private void cargarDatosComunes(Model model) {
        // 1. Cargar Marcas (Lo que ya tenías)
        List<Brand> marcas = brandService.listarMarcasConImagen();
        marcas.forEach(marca -> {
            if (marca.getImagen() != null && !marca.getImagen().startsWith("/")) {
                marca.setImagen("/" + marca.getImagen());
            }
        });
        model.addAttribute("marcas", marcas);

        // 🚀 2. NUEVO: Traer Info de Tienda desde NODE.JS
        try {
            // Usamos la IP directa para evitar errores
            String urlNode = "http://127.0.0.1:3000/api/informacion-tienda";
            
            // Pedimos los datos a Node
            Map<String, Object> response = restTemplate.getForObject(urlNode, Map.class);

            // Verificamos si vino en "data" o directo
            if (response != null) {
                if (response.containsKey("data")) {
                    model.addAttribute("infoTienda", response.get("data"));
                } else {
                    model.addAttribute("infoTienda", response);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Node no responde (Info Tienda): " + e.getMessage());
            
            // FALLBACK: Si Node falla, usamos datos por defecto para que no se vea feo
            model.addAttribute("infoTienda", Map.of(
                "direccion", "Tienda Principal - Lima",
                "telefono", "+51 999 000 999",
                "email", "contacto@sneakers.com"
            ));
        }
    }

    @org.springframework.web.bind.annotation.PostMapping("/contacto/enviar")
    public String enviarMensajeContacto(
            @org.springframework.web.bind.annotation.RequestParam String nombre,
            @org.springframework.web.bind.annotation.RequestParam String correo,
            @org.springframework.web.bind.annotation.RequestParam String asunto,
            @org.springframework.web.bind.annotation.RequestParam String mensaje,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        try {
            // 1. Preparar datos para Node.js
            String urlNode = "http://127.0.0.1:3000/api/contacto";
            
            // Creamos un mapa con los datos
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("nombre", nombre);
            body.put("correo", correo);
            body.put("asunto", asunto);
            body.put("mensaje", mensaje);

            // 2. Enviar a Node.js usando RestTemplate
            restTemplate.postForObject(urlNode, body, String.class);

            // 3. Mensaje de éxito
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Mensaje enviado! Se guardó en nuestro sistema externo.");

        } catch (Exception e) {
            // Si Node falla, al menos le decimos al usuario que "se envió" (para que no se asuste)
            // pero logueamos el error.
            System.err.println("❌ Error enviando contacto a Node: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError", "Hubo un problema de conexión, pero intentaremos procesar tu mensaje.");
        }

        return "redirect:/contacto";
    }
    @GetMapping("/producto/{id}")
    public String verDetalleProducto(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        cargarDatosComunes(model); 
        
        com.example.acceso.model.Product producto = productService.obtenerProductoPorId(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
            
        model.addAttribute("producto", producto);
        
        // Simulamos tallas para la vista (Hardcoded por tiempo)
        model.addAttribute("tallas", java.util.Arrays.asList("US 7", "US 8", "US 9", "US 10", "US 11"));
        
        return "web/producto-detalle";
    }
}
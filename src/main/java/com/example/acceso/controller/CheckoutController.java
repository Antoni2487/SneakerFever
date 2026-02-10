package com.example.acceso.controller;

import com.example.acceso.dto.Carrito;
import com.example.acceso.dto.CrearVentaRequest;
import com.example.acceso.dto.CrearVentaRequest.DetalleVentaRequest;
import com.example.acceso.dto.ItemCarrito;
import com.example.acceso.model.Cliente;
import com.example.acceso.model.EnumVentas.FormaPago;
import com.example.acceso.model.EnumVentas.TipoComprobante;
import com.example.acceso.model.Usuario;
import com.example.acceso.repository.ClienteRepository;
import com.example.acceso.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/carrito")
public class CheckoutController {

    private final VentaService ventaService;
    private final ClienteRepository clienteRepository;

    public CheckoutController(VentaService ventaService, ClienteRepository clienteRepository) {
        this.ventaService = ventaService;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/checkout")
    public String verCheckout(HttpSession session, Model model) {
        // 1. Validar Usuario Logueado
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        // 2. Validar Carrito
        Carrito carrito = (Carrito) session.getAttribute("carrito");
        if (carrito == null || carrito.getItems().isEmpty()) {
            return "redirect:/catalogo";
        }

        // 3. Buscar datos del cliente (Vinculación por Correo)
        // Como no hay relación directa en BD, buscamos si este usuario ya compró antes
        Optional<Cliente> clienteOpt = clienteRepository.findByCorreo(usuario.getCorreo());
        
        if (clienteOpt.isPresent()) {
            model.addAttribute("cliente", clienteOpt.get());
        } else {
            // Si es nuevo, pre-llenamos con datos del usuario
            Cliente clienteNuevo = new Cliente();
            clienteNuevo.setNombre(usuario.getNombre());
            clienteNuevo.setCorreo(usuario.getCorreo());
            model.addAttribute("cliente", clienteNuevo);
        }

        return "web/checkout";
    }

    @PostMapping("/procesar")
    public String procesarCompra(
            @RequestParam String documento,
            @RequestParam String nombre,
            @RequestParam String telefono,
            @RequestParam String tipoComprobanteStr, // "BOLETA" o "FACTURA"
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            // 1. Obtener datos de sesión
            Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
            Carrito carrito = (Carrito) session.getAttribute("carrito");

            if (usuario == null || carrito == null || carrito.getItems().isEmpty()) {
                return "redirect:/catalogo";
            }

            // 2. Construir el Request DTO para el VentaService
            CrearVentaRequest request = new CrearVentaRequest();
            
            // Datos del Cliente (El service lo busca o crea por documento)
            request.setDocumento(documento); 
            // OJO: El service necesita lógica para actualizar nombre/teléfono si ya existe,
            // pero por ahora enviamos el documento que es lo crítico.
            
            // Datos de Venta
            request.setTipoComprobante(TipoComprobante.valueOf(tipoComprobanteStr));
            request.setSerie(tipoComprobanteStr.equals("FACTURA") ? "F001" : "B001");
            request.setFormaPago(FormaPago.CONTADO); // Por defecto Contado en web
            request.setObservaciones("Compra Web - Usuario: " + usuario.getUsuario());
            
            // 3. Convertir Items del Carrito a Detalles de Venta
            List<DetalleVentaRequest> detalles = new ArrayList<>();
            for (ItemCarrito item : carrito.getItems()) {
                DetalleVentaRequest detalle = new DetalleVentaRequest();
                detalle.setProductoId(item.getProductoId());
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(item.getPrecio());
                detalle.setDescuentoPorcentaje(BigDecimal.ZERO); // O lógica de ofertas
                detalles.add(detalle);
            }
            request.setDetalles(detalles);

            // 4. LLAMAR AL SERVICIO (EL MOMENTO DE LA VERDAD)
            ventaService.crearVenta(request, usuario.getUsuario());

            // 5. Limpiar carrito y éxito
            session.removeAttribute("carrito");
            redirectAttributes.addFlashAttribute("mensajeExito", "¡Compra realizada con éxito! Tu pedido está siendo procesado.");
            return "redirect:/catalogo";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mensajeError", "Error al procesar la compra: " + e.getMessage());
            return "redirect:/carrito/checkout";
        }
    }
}
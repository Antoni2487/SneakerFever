package com.example.acceso.controller;

import com.example.acceso.model.Cliente;
import com.example.acceso.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ===================== Vistas =====================

    /**
     * Página principal de gestión de clientes
     */
    @GetMapping({"", "/"})
    public String listarClientes(Model model) {
        model.addAttribute("title", "Gestión de Clientes");
        model.addAttribute("totalClientes", clienteService.contarClientes());
        model.addAttribute("totalTodosClientes", clienteService.listarTodosClientes().size());
        return "admin/clientes";
    }

    @GetMapping("/listar")
    public String listarClientesRedirect() {
        return "redirect:/clientes";
    }

    // ===================== API REST - Listados =====================

    /**
     * Obtener todos los clientes activos (JSON)
     */
    @GetMapping("/api/listar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarClientesJson() {
        try {
            List<Cliente> clientes = clienteService.listarClientes();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", clientes);
            response.put("total", clientes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al listar clientes: " + e.getMessage());
        }
    }

    /**
     * Obtener todos los clientes (activos e inactivos) para DataTables
     */
    @GetMapping("/api/datatables")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarParaDataTables() {
        try {
            List<Cliente> clientes = clienteService.listarTodosClientes();

            Map<String, Object> response = new HashMap<>();
            response.put("draw", 1);
            response.put("recordsTotal", clientes.size());
            response.put("recordsFiltered", clientes.size());
            response.put("data", clientes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al cargar datos para la tabla: " + e.getMessage());
        }
    }

    /**
     * Obtener estadísticas de clientes (activos, inactivos, eliminados)
     */
    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            List<Cliente> todosClientes = clienteService.listarTodosClientes();

            // Contar por estado
            long activos = todosClientes.stream()
                .filter(c -> c.getEstado() == 1)
                .count();

            long inactivos = todosClientes.stream()
                .filter(c -> c.getEstado() == 0)
                .count();

            long eliminados = todosClientes.stream()
                .filter(c -> c.getEstado() == 2)
                .count();

            // Preparar datos de estadísticas
            Map<String, Object> stats = new HashMap<>();
            stats.put("activos", activos);
            stats.put("inactivos", inactivos);
            stats.put("eliminados", eliminados);
            stats.put("total", todosClientes.size());

            // Respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Error al obtener estadísticas: " + e.getMessage());
        }
    }

    // ===================== API REST - Consulta Externa =====================

    /**
     * Consultar documento (DNI/RUC) en API externa
     */
    @GetMapping("/api/consultar-documento/{documento}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consultarDocumento(@PathVariable String documento) {
        try {
            Map<String, Object> resultado = clienteService.consultarDocumento(documento);

            if ((Boolean) resultado.get("success")) {
                return ResponseEntity.ok(resultado);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resultado);
            }
        } catch (Exception e) {
            return createErrorResponse("Error al consultar documento: " + e.getMessage());
        }
    }

    // ===================== API REST - CRUD =====================

    /**
     * Obtener cliente por ID
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerCliente(@PathVariable Long id) {
        try {
            Optional<Cliente> cliente = clienteService.obtenerClientePorId(id);

            Map<String, Object> response = new HashMap<>();
            if (cliente.isPresent()) {
                response.put("success", true);
                response.put("data", cliente.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Cliente no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return createErrorResponse("Error al obtener el cliente: " + e.getMessage());
        }
    }

    /**
     * Crear nuevo cliente
     */
    @PostMapping("/api/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearCliente(@Valid @RequestBody Cliente cliente, BindingResult result) {
        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }

        try {
            // Validar duplicados
            if (clienteService.existeCliente(cliente.getDocumento())) {
                return createErrorResponse("Ya existe un cliente con ese documento");
            }

            Cliente nuevoCliente = clienteService.guardarCliente(cliente);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente creado exitosamente");
            response.put("data", nuevoCliente);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ClienteService.ClienteException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al crear el cliente: " + e.getMessage());
        }
    }

    /**
     * Actualizar cliente existente
     */
    @PutMapping("/api/actualizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody Cliente cliente,
            BindingResult result) {

        if (result.hasErrors()) {
            return createValidationErrorResponse(result);
        }

        try {
            // Verificar que el cliente existe
            Optional<Cliente> clienteExistenteOpt = clienteService.obtenerClientePorId(id);
            if (!clienteExistenteOpt.isPresent()) {
                return createErrorResponse("Cliente no encontrado");
            }

            // Validar duplicados (excluyendo el actual)
            if (clienteService.existeClienteParaActualizar(cliente.getDocumento(), id)) {
                return createErrorResponse("Ya existe otro cliente con ese documento");
            }

            // Asignar ID y guardar
            cliente.setId(id);
            Cliente clienteActualizado = clienteService.guardarCliente(cliente);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente actualizado exitosamente");
            response.put("data", clienteActualizado);

            return ResponseEntity.ok(response);

        } catch (ClienteService.ClienteException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al actualizar el cliente: " + e.getMessage());
        }
    }

    /**
     * Eliminar cliente (eliminación lógica)
     */
    @DeleteMapping("/api/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarCliente(@PathVariable Long id) {
        try {
            clienteService.eliminarCliente(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (ClienteService.ClienteException e) {
            return createErrorResponse(e.getMessage());
        } catch (Exception e) {
            return createErrorResponse("Error al eliminar el cliente: " + e.getMessage());
        }
    }

    /**
     * Cambiar estado de cliente (activar/desactivar)
     */
    @PutMapping("/api/cambiar-estado/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cambiarEstado(@PathVariable Long id) {
        try {
            Optional<Cliente> cliente = clienteService.cambiarEstadoCliente(id);

            Map<String, Object> response = new HashMap<>();
            if (cliente.isPresent()) {
                response.put("success", true);
                response.put("message", "Estado cambiado exitosamente");
                response.put("data", cliente.get());
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Cliente no encontrado");
            }

        } catch (Exception e) {
            return createErrorResponse("Error al cambiar el estado: " + e.getMessage());
        }
    }

    // ===================== API REST - Búsquedas =====================

    /**
     * Buscar clientes por nombre
     */
    @GetMapping("/api/buscar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarClientes(@RequestParam String q) {
        try {
            List<Cliente> clientes = clienteService.buscarClientesActivos(q);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", clientes);
            response.put("total", clientes.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error en la búsqueda: " + e.getMessage());
        }
    }

    /**
     * Buscar cliente por documento
     */
    @GetMapping("/api/documento/{documento}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarPorDocumento(@PathVariable String documento) {
        try {
            Optional<Cliente> cliente = clienteService.findByDocumento(documento);

            Map<String, Object> response = new HashMap<>();
            if (cliente.isPresent()) {
                response.put("success", true);
                response.put("data", cliente.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Cliente no encontrado con ese documento");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            return createErrorResponse("Error al buscar cliente: " + e.getMessage());
        }
    }

    // ===================== API REST - Contadores =====================

    /**
     * Contar clientes activos
     */
    @GetMapping("/api/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> contarClientes() {
        try {
            long total = clienteService.contarClientes();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("total", total);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return createErrorResponse("Error al contar clientes: " + e.getMessage());
        }
    }

    // ===================== Métodos de utilidad =====================

    /**
     * Crear respuesta de error estándar
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Crear respuesta de errores de validación
     */
    private ResponseEntity<Map<String, Object>> createValidationErrorResponse(BindingResult result) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        result.getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        response.put("success", false);
        response.put("message", "Errores de validación");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }
}
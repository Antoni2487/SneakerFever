package com.example.acceso.service;

import com.example.acceso.dto.DniResponseDTO;
import com.example.acceso.dto.RucResponseDTO;
import com.example.acceso.model.Cliente;
import com.example.acceso.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final RestTemplate restTemplate;

    @Value("${miapi.token}")
    private String miapiToken;

    @Value("${miapi.url.dni}")
    private String dniUrl;

    @Value("${miapi.url.ruc}")
    private String rucUrl;

    public ClienteService(ClienteRepository clienteRepository, RestTemplate restTemplate) {
        this.clienteRepository = clienteRepository;
        this.restTemplate = restTemplate;
    }

    // ===================== CONSULTAR DOCUMENTO EN API EXTERNA =====================
    @Transactional(readOnly = true)
    public Map<String, Object> consultarDocumento(String documento) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            if (documento == null || documento.trim().isEmpty()) {
                resultado.put("success", false);
                resultado.put("message", "El documento es obligatorio");
                return resultado;
            }

            documento = documento.trim();

            // Validar formato
            if (!documento.matches("^[0-9]{8}$|^[0-9]{11}$")) {
                resultado.put("success", false);
                resultado.put("message", "El documento debe ser un DNI (8 dígitos) o RUC (11 dígitos)");
                return resultado;
            }

            // Configurar headers con el token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + miapiToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String nombre = null;
            String tipo = null;

            // DNI (8 dígitos)
            if (documento.length() == 8) {
                String url = dniUrl + documento;
                ResponseEntity<DniResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    DniResponseDTO.class
                );

                if (response.getBody() != null && response.getBody().isSuccess()) {
                    nombre = response.getBody().getDatos().getNombreCompleto();
                    tipo = "DNI";
                    resultado.put("success", true);
                    resultado.put("nombre", nombre);
                    resultado.put("tipo", tipo);
                } else {
                    resultado.put("success", false);
                    resultado.put("message", "DNI no encontrado en la base de datos");
                }
            }
            // RUC (11 dígitos)
            else if (documento.length() == 11) {
                String url = rucUrl + documento;
                ResponseEntity<RucResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    RucResponseDTO.class
                );

                if (response.getBody() != null && response.getBody().isSuccess()) {
                    nombre = response.getBody().getDatos().getRazonSocial();
                    tipo = "RUC";
                    resultado.put("success", true);
                    resultado.put("nombre", nombre);
                    resultado.put("tipo", tipo);
                } else {
                    resultado.put("success", false);
                    resultado.put("message", "RUC no encontrado en la base de datos");
                }
            }

        } catch (Exception e) {
            resultado.put("success", false);
            resultado.put("message", "Error al consultar la API: " + e.getMessage());
            resultado.put("advertencia", "Puede continuar ingresando el nombre manualmente");
        }

        return resultado;
    }
    /**
     * Obtiene un cliente por documento. Si no existe, lo consulta en la API y lo crea.
     */
    @Transactional
    public Cliente obtenerOCrearCliente(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new ClienteException("El documento es obligatorio");
        }

        documento = documento.trim();

        // 1. Buscar en la base de datos
        Optional<Cliente> clienteExistente = findByDocumento(documento);
        if (clienteExistente.isPresent()) {
            Cliente cliente = clienteExistente.get();
            if (cliente.getEstado() == 2) {
                throw new ClienteException("El cliente con documento " + documento + " está eliminado");
            }
            return cliente;
        }

        // 2. No existe, consultar en la API
        Map<String, Object> resultado = consultarDocumento(documento);

        if (!(Boolean) resultado.get("success")) {
            String mensaje = (String) resultado.get("message");
            throw new ClienteException("No se pudo obtener datos del documento: " + mensaje);
        }

        // 3. Crear nuevo cliente con los datos de la API
        String nombre = (String) resultado.get("nombre");
        String tipo = (String) resultado.get("tipo");

        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setDocumento(documento);
        nuevoCliente.setNombre(nombre);
        nuevoCliente.setTelefono("000000000"); // Teléfono por defecto
        nuevoCliente.setCorreo(null); // Sin correo inicial
        nuevoCliente.setEstado(1); // Activo

        // 4. Guardar y retornar
        return clienteRepository.save(nuevoCliente);
    }

    // ===================== LISTADOS =====================
    @Transactional(readOnly = true)
    public List<Cliente> listarClientes() {
        return clienteRepository.findAllByEstado(1);
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarTodosClientes() {
        return clienteRepository.findAll();
    }

    // ===================== GUARDAR / ACTUALIZAR =====================
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        validarCliente(cliente);

        try {
            // ===================== Normalizar texto =====================
            cliente.setNombre(cliente.getNombre().trim());
            cliente.setDocumento(cliente.getDocumento().trim());
            cliente.setTelefono(cliente.getTelefono().trim());

            if (cliente.getCorreo() != null) {
                cliente.setCorreo(cliente.getCorreo().trim());
                if (cliente.getCorreo().isEmpty()) {
                    cliente.setCorreo(null);
                }
            }

            // ===================== VALIDAR DUPLICADOS =====================
            if (cliente.getId() != null) {
                // 🔥 ACTUALIZACIÓN: Verificar si OTRO cliente tiene el mismo documento
                if (existeClienteParaActualizar(cliente.getDocumento(), cliente.getId())) {
                    throw new ClienteException("Ya existe otro cliente con ese documento");
                }

                Optional<Cliente> existente = obtenerClientePorId(cliente.getId());
                if (existente.isPresent()) {
                    if (cliente.getEstado() == null) {
                        cliente.setEstado(existente.get().getEstado());
                    }
                } else {
                    throw new ClienteException("Cliente no encontrado para actualizar");
                }
            } else {
                // 🔥 NUEVO: Verificar si existe un cliente activo/inactivo con ese documento
                if (existeCliente(cliente.getDocumento())) {
                    throw new ClienteException("Ya existe un cliente con ese documento");
                }
                cliente.setEstado(1); // Nuevo cliente → activo
            }

            // ===================== Guardar =====================
            return clienteRepository.save(cliente);

        } catch (ClienteException e) {
            throw e; // Re-lanzar excepciones de negocio
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("documento") || msg.contains("unique")) {
                throw new ClienteException("Ya existe un cliente con ese documento");
            }
            throw new ClienteException("Error de integridad de datos");
        } catch (Exception e) {
            throw new ClienteException("Error al guardar el cliente: " + e.getMessage(), e);
        }
    }

    // ===================== CONTADORES =====================
    @Transactional(readOnly = true)
    public long contarClientes() {
        return clienteRepository.countByEstado(1);
    }

    // ===================== OBTENCIÓN =====================
    @Transactional(readOnly = true)
    public Optional<Cliente> obtenerClientePorId(Long id) {
        if (id == null || id <= 0) return Optional.empty();
        return clienteRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> findByDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) return Optional.empty();
        return clienteRepository.findByDocumento(documento.trim());
    }

    // ===================== ELIMINACIÓN / ESTADO =====================
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = obtenerClientePorId(id)
                .orElseThrow(() -> new ClienteException("Cliente no encontrado"));

        cliente.setEstado(2); // 2 = Eliminado
        clienteRepository.save(cliente);
    }

    @Transactional
    public Optional<Cliente> cambiarEstadoCliente(Long id) {
        if (id == null || id <= 0) return Optional.empty();

        return obtenerClientePorId(id).map(c -> {
            // Cambiar entre 1 (activo) y 0 (inactivo)
            if (c.getEstado() == 1) {
                c.setEstado(0);
            } else if (c.getEstado() == 0) {
                c.setEstado(1);
            }
            return clienteRepository.save(c);
        });
    }

    // ===================== EXISTENCIAS =====================
    /**
     * 🔥 CORREGIDO: Verifica si existe un cliente ACTIVO o INACTIVO con el documento
     * (excluye eliminados con estado=2)
     */
    @Transactional(readOnly = true)
    public boolean existeCliente(String documento) {
        if (documento == null || documento.trim().isEmpty()) return false;
        // Buscar clientes con estado diferente de 2 (excluye eliminados)
        return clienteRepository.existsByDocumentoAndEstadoNot(documento.trim(), 2);
    }

    /**
     * 🔥 CORREGIDO: Verifica si existe OTRO cliente (diferente al ID dado)
     * con el mismo documento (excluye eliminados)
     */
    @Transactional(readOnly = true)
    public boolean existeClienteParaActualizar(String documento, Long id) {
        if (documento == null || documento.trim().isEmpty()) return false;
        // Buscar otro cliente (id diferente) con estado diferente de 2
        return clienteRepository.existsByDocumentoAndIdNotAndEstadoNot(documento.trim(), id, 2);
    }

    // ===================== BÚSQUEDAS =====================
    @Transactional(readOnly = true)
    public List<Cliente> buscarClientes(String texto) {
        if (texto == null || texto.trim().isEmpty()) return listarClientes();
        return clienteRepository.findByNombreContainingIgnoreCaseAndEstado(texto.trim(), 1);
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarClientesActivos(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return clienteRepository.findAllByEstado(1);
        }
        return clienteRepository.findByNombreContainingIgnoreCaseAndEstado(texto.trim(), 1);
    }

    // ===================== VALIDACIONES =====================
    private void validarCliente(Cliente cliente) {
        if (cliente == null) throw new ClienteException("Cliente no puede ser nulo");
        if (cliente.getNombre() == null || cliente.getNombre().trim().isEmpty()) {
            throw new ClienteException("El nombre del cliente es obligatorio");
        }
        if (cliente.getDocumento() == null || cliente.getDocumento().trim().isEmpty()) {
            throw new ClienteException("El documento del cliente es obligatorio");
        }
        if (cliente.getTelefono() == null || cliente.getTelefono().trim().isEmpty()) {
            throw new ClienteException("El teléfono del cliente es obligatorio");
        }
    }

    // ===================== EXCEPCIÓN PERSONALIZADA =====================
    public static class ClienteException extends RuntimeException {
        public ClienteException(String message) {
            super(message);
        }
        public ClienteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
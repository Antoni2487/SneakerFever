package com.example.acceso.service;

import com.example.acceso.dto.*;
import com.example.acceso.model.EntidadesVenta.CreditoVenta;
import com.example.acceso.model.EntidadesVenta.CuotaPago;
import com.example.acceso.model.EntidadesVenta.RegistroPago;
import com.example.acceso.model.EntidadesVenta.Venta;
import com.example.acceso.model.EnumVentas.EstadoCredito;
import com.example.acceso.model.EnumVentas.EstadoCuota;
import com.example.acceso.model.EnumVentas.EstadoVenta;
import com.example.acceso.repository.RepositorioVentas.CreditoVentaRepository;
import com.example.acceso.repository.RepositorioVentas.CuotaPagoRepository;
import com.example.acceso.repository.RepositorioVentas.RegistroPagoRepository;
import com.example.acceso.repository.RepositorioVentas.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditoVentaService {

    private final CreditoVentaRepository creditoVentaRepository;
    private final CuotaPagoRepository cuotaPagoRepository;
    private final RegistroPagoRepository registroPagoRepository;
    private final VentaRepository  ventaRepository;

    // ===================== CREAR CRÉDITO =====================

    /**
     * Crea un crédito para una venta y genera automáticamente las cuotas
     */
    @Transactional
    public CreditoVentaResponse crearCredito(Venta venta, CrearVentaRequest.CreditoVentaRequest creditoRequest) {
        log.info("Creando crédito para venta ID: {}", venta.getId());

        CreditoVenta credito = CreditoVenta.builder()
                .venta(venta)
                .montoTotal(venta.getTotal())
                .montoInicial(creditoRequest.getMontoInicial() != null ?
                    creditoRequest.getMontoInicial() : BigDecimal.ZERO)
                .interesPorcentaje(creditoRequest.getInteresPorcentaje() != null ?
                    creditoRequest.getInteresPorcentaje() : BigDecimal.ZERO)
                .numeroCuotas(creditoRequest.getNumeroCuotas())
                .intervaloCuotas(creditoRequest.getIntervaloCuotas())
                .fechaInicio(LocalDate.now())
                .estado(EstadoCredito.ACTIVO)
                .build();

        credito.calcularMontoConInteres();
        credito.calcularFechaFin();

        // Generar cuotas ANTES del save
        generarCuotas(credito);

        // UN SOLO SAVE con todo incluido
        CreditoVenta creditoGuardado = creditoVentaRepository.save(credito);

        log.info("✅ Crédito creado - ID: {}, Monto: {}, Cuotas: {}, Interés: {}%",
            creditoGuardado.getId(),
            creditoGuardado.getMontoConInteres(),
            creditoGuardado.getNumeroCuotas(),
            creditoGuardado.getInteresPorcentaje());

        return convertirACreditoVentaResponse(creditoGuardado);
    }

    /**
     * Genera las cuotas del crédito considerando inicial e interés
     */
    private void generarCuotas(CreditoVenta credito) {
        BigDecimal saldoFinanciar = credito.getMontoConInteres().subtract(credito.getMontoInicial());
        BigDecimal montoCuota = saldoFinanciar
                .divide(BigDecimal.valueOf(credito.getNumeroCuotas()), 2, RoundingMode.HALF_UP);

        LocalDate fechaVencimiento = credito.getFechaInicio();
        int diasIntervalo = credito.getIntervaloCuotas().getDias();

        for (int i = 1; i <= credito.getNumeroCuotas(); i++) {
            fechaVencimiento = fechaVencimiento.plusDays(diasIntervalo);

            BigDecimal montoCuotaFinal = montoCuota;
            if (i == credito.getNumeroCuotas()) {
                BigDecimal sumaCuotasAnteriores = montoCuota.multiply(
                    BigDecimal.valueOf(credito.getNumeroCuotas() - 1)
                );
                montoCuotaFinal = saldoFinanciar.subtract(sumaCuotasAnteriores);
            }

            CuotaPago cuota = CuotaPago.builder()
                    .numeroCuota(i)
                    .montoCuota(montoCuotaFinal)
                    .fechaVencimiento(fechaVencimiento)
                    .montoPagado(BigDecimal.ZERO)
                    .saldoPendiente(montoCuotaFinal)
                    .estado(EstadoCuota.PENDIENTE)
                    .build();

            credito.addCuota(cuota);
        }

        log.info("✅ {} cuotas generadas para crédito ID: {}", credito.getNumeroCuotas(), credito.getId());
    }
    // ===================== GESTIÓN DE PAGOS =====================

    /**
     * Registra un pago para una cuota específica
     */
    @Transactional
    public RegistroPagoResponse registrarPago(RegistrarPagoRequest request, String usuario) {
        log.info("🧾 Iniciando registro de pago - Usuario: {}, Cuota ID: {}", usuario, request.getCuotaId());

        // 1. Validar que la cuota existe
        CuotaPago cuota = cuotaPagoRepository.findById(request.getCuotaId())
                .orElseThrow(() -> new RuntimeException(" Cuota no encontrada con ID: " + request.getCuotaId()));

        // 2. Validar que la cuota no esté completamente pagada
        if (cuota.getEstado() == EstadoCuota.PAGADA) {
            throw new RuntimeException(String.format(
                    "La cuota #%d ya está completamente pagada", cuota.getNumeroCuota()
            ));
        }

        // 3. Validar que el monto no exceda el saldo pendiente
        if (request.getMontoPagado().compareTo(cuota.getSaldoPendiente()) > 0) {
            throw new RuntimeException(String.format(
                    "El monto a pagar (%.2f) no puede ser mayor al saldo pendiente (%.2f)",
                    request.getMontoPagado(), cuota.getSaldoPendiente()
            ));
        }

        // 4. Crear y guardar registro de pago
        RegistroPago pago = RegistroPago.builder()
                .credito(cuota.getCredito())
                .cuota(cuota)
                .montoPagado(request.getMontoPagado())
                .metodoPago(request.getMetodoPago())
                .numeroOperacion(request.getNumeroOperacion())
                .observaciones(request.getObservaciones())
                .usuarioRegistroPago(usuario)
                .build();

        RegistroPago pagoGuardado = registroPagoRepository.save(pago);

        // 5. Actualizar montos de la cuota
        cuota.registrarPago(request.getMontoPagado());
        cuotaPagoRepository.save(cuota);

        // 6. Actualizar montos del crédito
        CreditoVenta credito = cuota.getCredito();
        credito.actualizarMontosPagados();
        credito.actualizarEstado();
        creditoVentaRepository.save(credito);

        if (credito.getEstado() == EstadoCredito.PAGADO) {
            Venta venta = credito.getVenta();
            venta.setEstado(EstadoVenta.PAGADA);
            ventaRepository.save(venta);
            log.info("✅ Venta ID: {} marcada como PAGADA", venta.getId());
        }

        // 7. Log detallado del resultado
        log.info("""
                ✅ Pago registrado exitosamente:
                    • ID Pago: {}
                    • Crédito ID: {}
                    • Cuota: {}/{}
                    • Monto: {}
                    • Usuario: {}
                    • Nuevo saldo de cuota: {}
                    • Nuevo saldo del crédito: {}
                """,
                pagoGuardado.getId(),
                credito.getId(),
                cuota.getNumeroCuota(),
                credito.getNumeroCuotas(),
                pagoGuardado.getMontoPagado(),
                usuario,
                cuota.getSaldoPendiente(),
                credito.getSaldoPendiente()
        );

        return convertirARegistroPagoResponse(pagoGuardado);
    }


        /**
         * Lista todos los pagos de un crédito
         */
        @Transactional(readOnly = true)
        public List<RegistroPagoResponse> listarPagosPorCredito(Long creditoId) {
            return registroPagoRepository.findByCreditoIdOrderByFechaPagoDesc(creditoId).stream()
                .map(this::convertirARegistroPagoResponse)
                .collect(Collectors.toList());
        }

        // ===================== GESTIÓN DE CUOTAS =====================

        /**
         * Lista todas las cuotas de un crédito
         */
        @Transactional(readOnly = true)
        public List<CuotaPagoResponse> listarCuotasPorCredito(Long creditoId) {
            CreditoVenta credito = creditoVentaRepository.findByIdWithCuotas(creditoId)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado con ID: " + creditoId));

            return credito.getCuotas().stream()
                .map(this::convertirACuotaPagoResponse)
                .collect(Collectors.toList());
        }

        /**
         * Obtiene una cuota por su ID
         */
        @Transactional(readOnly = true)
        public CuotaPagoResponse obtenerCuotaPorId(Long cuotaId) {
            CuotaPago cuota = cuotaPagoRepository.findById(cuotaId)
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada con ID: " + cuotaId));
            return convertirACuotaPagoResponse(cuota);
        }

        /**
         * Lista todas las cuotas vencidas del sistema
         */
        @Transactional(readOnly = true)
        public List<CuotaPagoResponse> listarCuotasVencidas() {
            return cuotaPagoRepository.findCuotasVencidas(LocalDate.now()).stream()
                .map(this::convertirACuotaPagoResponse)
                .collect(Collectors.toList());
        }

        // ===================== CONSULTAS ADICIONALES =====================

        /**
         * Lista todos los créditos activos
         */
        @Transactional(readOnly = true)
        public List<CreditoVentaResponse> listarCreditosActivos() {
            return listarCreditosPorEstado(EstadoCredito.ACTIVO);
        }

        /**
         * Lista créditos próximos a vencer en los próximos N días
         */
        @Transactional(readOnly = true)
        public List<CreditoVentaResponse> listarCreditosProximosVencer(int dias) {
            LocalDate fechaLimite = LocalDate.now().plusDays(dias);

            return creditoVentaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoCredito.ACTIVO).stream()
                .filter(credito -> credito.getFechaFin() != null &&
                                  !credito.getFechaFin().isAfter(fechaLimite) &&
                                  credito.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0)
                .map(this::convertirACreditoVentaResponse)
                .collect(Collectors.toList());
        }

        // ===================== REPORTES =====================

        /**
         * Genera reporte general de créditos
         */
        @Transactional(readOnly = true)
        public ReporteCreditosDTO generarReporteCreditos() {
            List<CreditoVenta> todosCreditos = creditoVentaRepository.findAll();

            long totalCreditos = todosCreditos.size();
            long creditosActivos = todosCreditos.stream()
                .filter(c -> c.getEstado() == EstadoCredito.ACTIVO)
                .count();
            long creditosPagados = todosCreditos.stream()
                .filter(c -> c.getEstado() == EstadoCredito.PAGADO)
                .count();
            long creditosVencidos = todosCreditos.stream()
                .filter(c -> c.getEstado() == EstadoCredito.VENCIDO)
                .count();

            BigDecimal montoTotal = todosCreditos.stream()
                .map(CreditoVenta::getMontoConInteres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal montoPagado = todosCreditos.stream()
                .map(CreditoVenta::getMontoPagado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldoPendiente = todosCreditos.stream()
                .map(CreditoVenta::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            ReporteCreditosDTO reporte = ReporteCreditosDTO.builder()
                .totalCreditos(totalCreditos)
                .creditosActivos(creditosActivos)
                .creditosPagados(creditosPagados)
                .creditosVencidos(creditosVencidos)
                .montoTotalCreditos(montoTotal)
                .montoTotalPagado(montoPagado)
                .saldoPendienteTotal(saldoPendiente)
                .build();

            reporte.setPorcentajeRecuperacion(reporte.calcularPorcentajeRecuperacion());

            return reporte;
        }

        // ===================== CONVERSIÓN A DTO - REGISTRO PAGO =====================

        /**
         * Convierte RegistroPago a DTO Response
         */
        private RegistroPagoResponse convertirARegistroPagoResponse(RegistroPago pago) {
            return RegistroPagoResponse.builder()
                    .id(pago.getId())
                    .creditoId(pago.getCredito().getId())
                    .cuotaId(pago.getCuota() != null ? pago.getCuota().getId() : null)
                    .numeroCuota(pago.getCuota() != null ? pago.getCuota().getNumeroCuota() : null)
                    .montoPagado(pago.getMontoPagado())
                    .metodoPago(pago.getMetodoPago())
                    .numeroOperacion(pago.getNumeroOperacion())
                    .observaciones(pago.getObservaciones())
                    .usuarioRegistroPago(pago.getUsuarioRegistroPago())
                    .fechaPago(pago.getFechaPago())
                    .build();
        }

    // ===================== CONSULTAR CRÉDITOS =====================

    @Transactional(readOnly = true)
    public CreditoVentaResponse obtenerCreditoPorId(Long id) {
        CreditoVenta credito = creditoVentaRepository.findByIdWithCuotas(id)
            .orElseThrow(() -> new RuntimeException("Crédito no encontrado con ID: " + id));
        return convertirACreditoVentaResponse(credito);
    }

    @Transactional(readOnly = true)
    public CreditoVentaResponse obtenerCreditoPorVentaId(Long ventaId) {
        CreditoVenta credito = creditoVentaRepository.findByVentaId(ventaId)
            .orElseThrow(() -> new RuntimeException("No existe crédito para la venta ID: " + ventaId));
        return convertirACreditoVentaResponse(credito);
    }

    @Transactional(readOnly = true)
    public List<CreditoVentaResponse> listarCreditosPorCliente(Long clienteId) {
        return creditoVentaRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId).stream()
            .map(this::convertirACreditoVentaResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditoVentaResponse> listarCreditosPorEstado(EstadoCredito estado) {
        return creditoVentaRepository.findByEstadoOrderByFechaCreacionDesc(estado).stream()
            .map(this::convertirACreditoVentaResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CreditoVentaResponse> listarCreditosVencidos() {
        return creditoVentaRepository.findCreditosVencidos(LocalDate.now()).stream()
            .map(this::convertirACreditoVentaResponse)
            .collect(Collectors.toList());
    }

    // ===================== CANCELAR CRÉDITO =====================

    @Transactional
    public void cancelarCredito(Long ventaId) {
        CreditoVenta credito = creditoVentaRepository.findByVentaId(ventaId)
            .orElseThrow(() -> new RuntimeException("No existe crédito para la venta ID: " + ventaId));

        credito.setEstado(EstadoCredito.CANCELADO);
        creditoVentaRepository.save(credito);
        log.info("✅ Crédito cancelado para venta ID: {}", ventaId);
    }

    // ===================== ACTUALIZAR ESTADOS AUTOMÁTICAMENTE =====================

    /**
     * Actualiza estados de todos los créditos activos
     * Útil para ejecutar en tareas programadas
     */
    @Transactional
    public void actualizarEstadosCreditos() {
        List<CreditoVenta> creditosActivos = creditoVentaRepository
            .findByEstadoOrderByFechaCreacionDesc(EstadoCredito.ACTIVO);

        for (CreditoVenta credito : creditosActivos) {
            credito.actualizarEstado();
            creditoVentaRepository.save(credito);
        }

        log.info("✅ Estados de créditos actualizados. Total procesados: {}", creditosActivos.size());
    }

    /**
     * Actualiza estados de todas las cuotas pendientes
     * Marca como VENCIDAS las que superaron la fecha
     */
    @Transactional
    public void actualizarEstadosCuotas() {
        List<CuotaPago> cuotasVencidas = cuotaPagoRepository.findCuotasVencidas(LocalDate.now());

        for (CuotaPago cuota : cuotasVencidas) {
            if (cuota.getEstado() == EstadoCuota.PENDIENTE || cuota.getEstado() == EstadoCuota.PARCIAL) {
                cuota.actualizarEstado();
                cuotaPagoRepository.save(cuota);
            }
        }

        log.info("✅ Estados de cuotas actualizados. Cuotas vencidas: {}", cuotasVencidas.size());
    }

    // ===================== MÉTODOS PRIVADOS DE CONVERSIÓN =====================

    /**
     * Convierte CreditoVenta a DTO Response
     */
    private CreditoVentaResponse convertirACreditoVentaResponse(CreditoVenta credito) {
        List<CuotaPagoResponse> cuotasResponse = new ArrayList<>();
        if (credito.getCuotas() != null) {
            cuotasResponse = credito.getCuotas().stream()
                    .map(this::convertirACuotaPagoResponse)
                    .collect(Collectors.toList());
        }

        CreditoVentaResponse response = CreditoVentaResponse.builder()
                .id(credito.getId())
                .ventaId(credito.getVenta().getId())
                .tipoComprobante(credito.getVenta().getTipoComprobante().getDescripcion())
                .serie(credito.getVenta().getSerie())
                .numero(credito.getVenta().getNumero())
                .comprobanteCompleto(credito.getVenta().getComprobanteCompleto())
                .clienteId(credito.getVenta().getCliente().getId())
                .clienteNombre(credito.getVenta().getCliente().getNombre())
                .clienteDocumento(credito.getVenta().getCliente().getDocumento())
                .montoTotal(credito.getMontoTotal())
                .montoInicial(credito.getMontoInicial())
                .interesPorcentaje(credito.getInteresPorcentaje())
                .montoConInteres(credito.getMontoConInteres())
                .numeroCuotas(credito.getNumeroCuotas())
                .intervaloCuotas(credito.getIntervaloCuotas())
                .fechaInicio(credito.getFechaInicio())
                .fechaFin(credito.getFechaFin())
                .montoPagado(credito.getMontoPagado())
                .saldoPendiente(credito.getSaldoPendiente())
                .estado(credito.getEstado())
                .fechaCreacion(credito.getFechaCreacion())
                .fechaActualizacion(credito.getFechaActualizacion())
                .cuotas(cuotasResponse)
                .build();

        // Calcular campos adicionales
        response.setPorcentajePagado(credito.getPorcentajePagado());
        response.setDiasRestantes(response.calcularDiasRestantes());

        return response;
    }

    /**
     * Convierte CuotaPago a DTO Response
     */
    private CuotaPagoResponse convertirACuotaPagoResponse(CuotaPago cuota) {
        CuotaPagoResponse response = CuotaPagoResponse.builder()
                .id(cuota.getId())
                .creditoId(cuota.getCredito().getId())
                .numeroCuota(cuota.getNumeroCuota())
                .montoCuota(cuota.getMontoCuota())
                .fechaVencimiento(cuota.getFechaVencimiento())
                .montoPagado(cuota.getMontoPagado())
                .saldoPendiente(cuota.getSaldoPendiente())
                .fechaPago(cuota.getFechaPago())
                .estado(cuota.getEstado())
                .build();

        // Calcular campos adicionales
        response.setDiasParaVencer(response.calcularDiasParaVencer());
        response.setDiasVencida(response.calcularDiasVencida());
        response.setPorcentajePagado(response.calcularPorcentajePagado());

        return response;
    }
}
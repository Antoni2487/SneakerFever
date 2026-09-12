package com.example.acceso.venta;

import com.example.acceso.cliente.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("CreditoVentaService - tests unitarios con Mockito")
class CreditoVentaServiceTest {

    @Mock
    private CreditoVentaRepository creditoVentaRepository;

    @Mock
    private CuotaPagoRepository cuotaPagoRepository;

    @Mock
    private RegistroPagoRepository registroPagoRepository;

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private CreditoVentaService creditoVentaService;

    // ===================== Fixtures =====================

    private Venta ventaDePrueba(Long id) {
        Cliente cliente = Cliente.builder()
                .id(1L)
                .nombre("Ana Torres")
                .documento("12345678")
                .build();

        return Venta.builder()
                .id(id)
                .cliente(cliente)
                .tipoComprobante(TipoComprobante.BOLETA)
                .serie("B001")
                .numero(String.format("%06d", id))
                .total(new BigDecimal("1000.00"))
                .estado(EstadoVenta.PENDIENTE)
                .build();
    }

    /**
     * Crédito de 1000.00 sin interés ni inicial, con el saldo pendiente indicado.
     */
    private CreditoVenta creditoDePrueba(Long id, LocalDate fechaFin, BigDecimal saldoPendiente,
                                         EstadoCredito estado) {
        BigDecimal montoConInteres = new BigDecimal("1000.00");

        return CreditoVenta.builder()
                .id(id)
                .venta(ventaDePrueba(100L + id))
                .montoTotal(montoConInteres)
                .montoInicial(BigDecimal.ZERO)
                .interesPorcentaje(BigDecimal.ZERO)
                .montoConInteres(montoConInteres)
                .montoPagado(montoConInteres.subtract(saldoPendiente))
                .saldoPendiente(saldoPendiente)
                .numeroCuotas(4)
                .intervaloCuotas(IntervaloCredito.MENSUAL)
                .fechaInicio(LocalDate.now().minusDays(30))
                .fechaFin(fechaFin)
                .estado(estado)
                .cuotas(new ArrayList<>())
                .build();
    }

    // ===================== Cálculos derivados (movidos de los DTOs acá, Fase C) =====================

    private CuotaPago cuotaDePrueba(Long id, LocalDate fechaVencimiento, BigDecimal montoCuota,
                                     BigDecimal montoPagado, EstadoCuota estado) {
        return CuotaPago.builder()
                .id(id)
                // Mínimo indispensable: convertirACuotaPagoResponse lee
                // cuota.getCredito().getId() para el campo credito_id, pero
                // los cálculos bajo prueba solo usan estado, fechaVencimiento,
                // montoCuota y montoPagado.
                .credito(CreditoVenta.builder().id(1L).build())
                .numeroCuota(1)
                .montoCuota(montoCuota)
                .fechaVencimiento(fechaVencimiento)
                .montoPagado(montoPagado)
                .saldoPendiente(montoCuota.subtract(montoPagado))
                .estado(estado)
                .build();
    }

    @Test
    @DisplayName("Cuota pendiente que aún no vence: dias_para_vencer > 0, dias_vencida = 0")
    void cuotaPendienteNoVencida_calculaDiasParaVencerYPorcentajePagado() {
        CreditoVenta credito = creditoDePrueba(1L, LocalDate.now().plusDays(60), BigDecimal.ZERO, EstadoCredito.ACTIVO);
        CuotaPago cuota = cuotaDePrueba(
                50L, LocalDate.now().plusDays(10), new BigDecimal("250.00"), new BigDecimal("100.00"),
                EstadoCuota.PARCIAL);
        credito.setCuotas(new ArrayList<>(List.of(cuota)));
        when(creditoVentaRepository.findByIdWithCuotas(1L)).thenReturn(Optional.of(credito));

        List<CuotaPagoResponse> resultado = creditoVentaService.listarCuotasPorCredito(1L);

        CuotaPagoResponse response = resultado.get(0);
        assertThat(response.getDiasParaVencer()).isEqualTo(10L);
        assertThat(response.getDiasVencida()).isEqualTo(0L);
        assertThat(response.getPorcentajePagado()).isEqualByComparingTo("40.00"); // 100/250 * 100
    }

    @Test
    @DisplayName("Cuota vencida sin pagar: dias_para_vencer = 0, dias_vencida > 0")
    void cuotaVencidaSinPagar_calculaDiasVencida() {
        CreditoVenta credito = creditoDePrueba(1L, LocalDate.now().plusDays(60), BigDecimal.ZERO, EstadoCredito.ACTIVO);
        CuotaPago cuota = cuotaDePrueba(
                51L, LocalDate.now().minusDays(7), new BigDecimal("250.00"), BigDecimal.ZERO,
                EstadoCuota.VENCIDA);
        credito.setCuotas(new ArrayList<>(List.of(cuota)));
        when(creditoVentaRepository.findByIdWithCuotas(1L)).thenReturn(Optional.of(credito));

        List<CuotaPagoResponse> resultado = creditoVentaService.listarCuotasPorCredito(1L);

        CuotaPagoResponse response = resultado.get(0);
        assertThat(response.getDiasParaVencer()).isEqualTo(0L);
        assertThat(response.getDiasVencida()).isEqualTo(7L);
        assertThat(response.getPorcentajePagado()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Cuota ya pagada: dias_para_vencer y dias_vencida quedan en 0 aunque la fecha ya haya pasado")
    void cuotaPagada_diasQuedanEnCero() {
        CreditoVenta credito = creditoDePrueba(1L, LocalDate.now().plusDays(60), BigDecimal.ZERO, EstadoCredito.ACTIVO);
        CuotaPago cuota = cuotaDePrueba(
                52L, LocalDate.now().minusDays(20), new BigDecimal("250.00"), new BigDecimal("250.00"),
                EstadoCuota.PAGADA);
        credito.setCuotas(new ArrayList<>(List.of(cuota)));
        when(creditoVentaRepository.findByIdWithCuotas(1L)).thenReturn(Optional.of(credito));

        List<CuotaPagoResponse> resultado = creditoVentaService.listarCuotasPorCredito(1L);

        CuotaPagoResponse response = resultado.get(0);
        assertThat(response.getDiasParaVencer()).isEqualTo(0L);
        assertThat(response.getDiasVencida()).isEqualTo(0L);
        assertThat(response.getPorcentajePagado()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Crédito: dias_restantes se calcula contra la fecha de fin, y porcentaje_pagado viene de la entidad")
    void credito_calculaDiasRestantesYUsaElPorcentajeDeLaEntidad() {
        CreditoVenta credito = creditoDePrueba(
                1L, LocalDate.now().plusDays(20), new BigDecimal("300.00"), EstadoCredito.ACTIVO);
        when(creditoVentaRepository.findByIdWithCuotas(1L)).thenReturn(Optional.of(credito));

        CreditoVentaResponse response = creditoVentaService.obtenerCreditoPorId(1L);

        assertThat(response.getDiasRestantes()).isEqualTo(20L);
        assertThat(response.getPorcentajePagado()).isEqualByComparingTo(credito.getPorcentajePagado());
    }

    // ===================== Créditos vencidos =====================

    /**
     * El filtrado por fecha vive en la consulta JPQL del repositorio
     * ({@code findCreditosVencidos}), no en el servicio. Con Mockito puro lo que
     * se puede verificar —y lo que este test verifica— es que el servicio consulta
     * con la fecha de HOY y mapea correctamente lo que recibe.
     */
    @Test
    @DisplayName("Listar créditos vencidos: consulta con la fecha de hoy y mapea el resultado")
    void listarCreditosVencidos_consultaConLaFechaDeHoyYMapeaElResultado() {
        CreditoVenta vencido = creditoDePrueba(
                1L, LocalDate.now().minusDays(15), new BigDecimal("400.00"), EstadoCredito.VENCIDO);
        when(creditoVentaRepository.findCreditosVencidos(any(LocalDate.class)))
                .thenReturn(List.of(vencido));

        List<CreditoVentaResponse> resultado = creditoVentaService.listarCreditosVencidos();

        ArgumentCaptor<LocalDate> fechaCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(creditoVentaRepository).findCreditosVencidos(fechaCaptor.capture());
        assertThat(fechaCaptor.getValue()).isEqualTo(LocalDate.now());

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(1L);
        assertThat(resultado.get(0).getEstado()).isEqualTo(EstadoCredito.VENCIDO);
        assertThat(resultado.get(0).getSaldoPendiente()).isEqualByComparingTo("400.00");
        assertThat(resultado.get(0).getClienteNombre()).isEqualTo("Ana Torres");
    }

    @Test
    @DisplayName("Listar créditos vencidos: sin resultados devuelve lista vacía, no null")
    void listarCreditosVencidos_sinResultadosDevuelveListaVacia() {
        when(creditoVentaRepository.findCreditosVencidos(any(LocalDate.class)))
                .thenReturn(List.of());

        List<CreditoVentaResponse> resultado = creditoVentaService.listarCreditosVencidos();

        assertThat(resultado).isNotNull().isEmpty();
    }

    // ===================== Créditos próximos a vencer =====================

    @Test
    @DisplayName("Próximos a vencer: incluye solo los que vencen dentro del plazo y tienen saldo")
    void listarCreditosProximosVencer_incluyeSoloLosQueVencenDentroDelPlazoYConSaldo() {
        CreditoVenta dentroDelPlazo = creditoDePrueba(
                1L, LocalDate.now().plusDays(3), new BigDecimal("500.00"), EstadoCredito.ACTIVO);
        CreditoVenta fueraDelPlazo = creditoDePrueba(
                2L, LocalDate.now().plusDays(60), new BigDecimal("500.00"), EstadoCredito.ACTIVO);
        CreditoVenta sinSaldoPendiente = creditoDePrueba(
                3L, LocalDate.now().plusDays(3), BigDecimal.ZERO, EstadoCredito.ACTIVO);

        when(creditoVentaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoCredito.ACTIVO))
                .thenReturn(Arrays.asList(dentroDelPlazo, fueraDelPlazo, sinSaldoPendiente));

        List<CreditoVentaResponse> resultado = creditoVentaService.listarCreditosProximosVencer(7);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Próximos a vencer: el límite del plazo es inclusivo (vence justo el último día)")
    void listarCreditosProximosVencer_incluyeElCreditoQueVenceElUltimoDiaDelPlazo() {
        CreditoVenta enElLimite = creditoDePrueba(
                1L, LocalDate.now().plusDays(7), new BigDecimal("250.00"), EstadoCredito.ACTIVO);
        when(creditoVentaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoCredito.ACTIVO))
                .thenReturn(List.of(enElLimite));

        List<CreditoVentaResponse> resultado = creditoVentaService.listarCreditosProximosVencer(7);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(1L);
    }

    /**
     * Este test documenta el comportamiento ACTUAL, no el deseable: el filtro usa
     * {@code !fechaFin.isAfter(fechaLimite)}, que no pone suelo inferior, así que un
     * crédito con fecha de fin ya pasada también entra en "próximos a vencer".
     * Ver el informe: es un hallazgo pendiente de decisión, no está corregido.
     */
    @Test
    @DisplayName("Próximos a vencer: hoy también devuelve créditos con la fecha de fin ya pasada")
    void listarCreditosProximosVencer_tambienIncluyeCreditosYaVencidos() {
        CreditoVenta yaVencido = creditoDePrueba(
                4L, LocalDate.now().minusDays(10), new BigDecimal("300.00"), EstadoCredito.ACTIVO);
        when(creditoVentaRepository.findByEstadoOrderByFechaCreacionDesc(EstadoCredito.ACTIVO))
                .thenReturn(List.of(yaVencido));

        List<CreditoVentaResponse> resultado = creditoVentaService.listarCreditosProximosVencer(7);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(4L);
    }

    // ===================== Registro de pagos =====================

    @Test
    @DisplayName("Registrar pago parcial: la cuota queda PARCIAL y el crédito sigue ACTIVO")
    void registrarPagoParcial_dejaLaCuotaEnEstadoParcial() {
        CreditoVenta credito = creditoDePrueba(
                1L, LocalDate.now().plusDays(60), new BigDecimal("1000.00"), EstadoCredito.ACTIVO);
        CuotaPago cuota = CuotaPago.builder()
                .id(50L)
                .numeroCuota(1)
                .montoCuota(new BigDecimal("250.00"))
                .fechaVencimiento(LocalDate.now().plusDays(15))
                .montoPagado(BigDecimal.ZERO)
                .saldoPendiente(new BigDecimal("250.00"))
                .estado(EstadoCuota.PENDIENTE)
                .build();
        credito.addCuota(cuota);

        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCreditoId(1L);
        request.setCuotaId(50L);
        request.setMontoPagado(new BigDecimal("100.00"));
        request.setMetodoPago(MetodoPago.EFECTIVO);
        request.setNumeroOperacion("OP-001");

        when(cuotaPagoRepository.findById(50L)).thenReturn(Optional.of(cuota));
        when(registroPagoRepository.save(any(RegistroPago.class))).thenAnswer(inv -> {
            RegistroPago pago = inv.getArgument(0);
            pago.setId(900L);
            return pago;
        });

        RegistroPagoResponse response = creditoVentaService.registrarPago(request, "admin");

        // Estado de la cuota actualizado
        assertThat(cuota.getMontoPagado()).isEqualByComparingTo("100.00");
        assertThat(cuota.getSaldoPendiente()).isEqualByComparingTo("150.00");
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PARCIAL);
        assertThat(cuota.getFechaPago()).isNull();

        // Estado del crédito recalculado, pero aún no saldado
        assertThat(credito.getMontoPagado()).isEqualByComparingTo("100.00");
        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("900.00");
        assertThat(credito.getEstado()).isEqualTo(EstadoCredito.ACTIVO);

        // Persistencia invocada donde corresponde
        verify(cuotaPagoRepository).save(cuota);
        verify(creditoVentaRepository).save(credito);
        verify(ventaRepository, never()).save(any(Venta.class));

        // DTO devuelto
        assertThat(response.getId()).isEqualTo(900L);
        assertThat(response.getCreditoId()).isEqualTo(1L);
        assertThat(response.getCuotaId()).isEqualTo(50L);
        assertThat(response.getMontoPagado()).isEqualByComparingTo("100.00");
        assertThat(response.getUsuarioRegistroPago()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Registrar pago total de la última cuota: cuota PAGADA, crédito PAGADO y venta PAGADA")
    void registrarPagoTotal_marcaLaCuotaElCreditoYLaVentaComoPagados() {
        Venta venta = ventaDePrueba(200L);
        CreditoVenta credito = CreditoVenta.builder()
                .id(9L)
                .venta(venta)
                .montoTotal(new BigDecimal("250.00"))
                .montoInicial(BigDecimal.ZERO)
                .interesPorcentaje(BigDecimal.ZERO)
                .montoConInteres(new BigDecimal("250.00"))
                .montoPagado(BigDecimal.ZERO)
                .saldoPendiente(new BigDecimal("250.00"))
                .numeroCuotas(1)
                .intervaloCuotas(IntervaloCredito.MENSUAL)
                .fechaInicio(LocalDate.now().minusDays(30))
                .fechaFin(LocalDate.now().plusDays(30))
                .estado(EstadoCredito.ACTIVO)
                .cuotas(new ArrayList<>())
                .build();
        CuotaPago cuota = CuotaPago.builder()
                .id(51L)
                .numeroCuota(1)
                .montoCuota(new BigDecimal("250.00"))
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .montoPagado(BigDecimal.ZERO)
                .saldoPendiente(new BigDecimal("250.00"))
                .estado(EstadoCuota.PENDIENTE)
                .build();
        credito.addCuota(cuota);

        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCreditoId(9L);
        request.setCuotaId(51L);
        request.setMontoPagado(new BigDecimal("250.00"));
        request.setMetodoPago(MetodoPago.YAPE);

        when(cuotaPagoRepository.findById(51L)).thenReturn(Optional.of(cuota));
        when(registroPagoRepository.save(any(RegistroPago.class))).thenAnswer(inv -> {
            RegistroPago pago = inv.getArgument(0);
            pago.setId(901L);
            return pago;
        });

        creditoVentaService.registrarPago(request, "cajero1");

        assertThat(cuota.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(cuota.getEstado()).isEqualTo(EstadoCuota.PAGADA);
        assertThat(cuota.getFechaPago()).isEqualTo(LocalDate.now());

        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(credito.getEstado()).isEqualTo(EstadoCredito.PAGADO);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.PAGADA);
        verify(ventaRepository).save(venta);
    }

    @Test
    @DisplayName("Registrar pago sobre una cuota ya PAGADA: lanza excepción y no persiste nada")
    void registrarPagoSobreCuotaYaPagada_lanzaExcepcion() {
        CuotaPago cuotaPagada = CuotaPago.builder()
                .id(52L)
                .numeroCuota(2)
                .montoCuota(new BigDecimal("250.00"))
                .fechaVencimiento(LocalDate.now().minusDays(5))
                .montoPagado(new BigDecimal("250.00"))
                .saldoPendiente(BigDecimal.ZERO)
                .estado(EstadoCuota.PAGADA)
                .build();

        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCreditoId(1L);
        request.setCuotaId(52L);
        request.setMontoPagado(new BigDecimal("50.00"));
        request.setMetodoPago(MetodoPago.EFECTIVO);

        when(cuotaPagoRepository.findById(52L)).thenReturn(Optional.of(cuotaPagada));

        assertThatThrownBy(() -> creditoVentaService.registrarPago(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está completamente pagada");

        verify(registroPagoRepository, never()).save(any(RegistroPago.class));
        verify(cuotaPagoRepository, never()).save(any(CuotaPago.class));
        verify(creditoVentaRepository, never()).save(any(CreditoVenta.class));
    }

    @Test
    @DisplayName("Registrar pago mayor al saldo pendiente: lanza excepción y no persiste nada")
    void registrarPagoMayorAlSaldoPendiente_lanzaExcepcion() {
        CuotaPago cuota = CuotaPago.builder()
                .id(53L)
                .numeroCuota(3)
                .montoCuota(new BigDecimal("250.00"))
                .fechaVencimiento(LocalDate.now().plusDays(10))
                .montoPagado(BigDecimal.ZERO)
                .saldoPendiente(new BigDecimal("250.00"))
                .estado(EstadoCuota.PENDIENTE)
                .build();

        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCreditoId(1L);
        request.setCuotaId(53L);
        request.setMontoPagado(new BigDecimal("500.00"));
        request.setMetodoPago(MetodoPago.TARJETA);

        when(cuotaPagoRepository.findById(53L)).thenReturn(Optional.of(cuota));

        assertThatThrownBy(() -> creditoVentaService.registrarPago(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no puede ser mayor al saldo pendiente");

        verify(registroPagoRepository, never()).save(any(RegistroPago.class));
        assertThat(cuota.getMontoPagado()).isEqualByComparingTo("0.00");
        assertThat(cuota.getSaldoPendiente()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Registrar pago sobre una cuota inexistente: lanza excepción")
    void registrarPagoSobreCuotaInexistente_lanzaExcepcion() {
        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setCreditoId(1L);
        request.setCuotaId(999L);
        request.setMontoPagado(new BigDecimal("50.00"));
        request.setMetodoPago(MetodoPago.EFECTIVO);

        when(cuotaPagoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditoVentaService.registrarPago(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cuota no encontrada");

        verify(registroPagoRepository, never()).save(any(RegistroPago.class));
    }
}

package com.example.acceso.venta;

import com.example.acceso.cliente.Cliente;
import com.example.acceso.cliente.ClienteRepository;
import com.example.acceso.cliente.ClienteService;
import com.example.acceso.inventario.MovimientoInventarioService;
import com.example.acceso.producto.Producto;
import com.example.acceso.producto.ProductoRepository;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService - tests unitarios con Mockito")
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private CreditoVentaService creditoVentaService;

    @Mock
    private ComprobanteSecuenciaService comprobanteSecuenciaService;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoRepository productRepository;

    @Mock
    private ClienteService clienteService;

    @Mock
    private MovimientoInventarioService movimientoInventarioService;

    @Mock
    private DetalleVentaRepository detalleVentaRepository;

    @InjectMocks
    private VentaService ventaService;

    // ===================== Fixtures =====================

    private Cliente clienteActivo(Long id, String documento) {
        return Cliente.builder().id(id).nombre("Juan Pérez").documento(documento).estado(1).build();
    }

    private Producto producto(Long id, String nombre, BigDecimal precio, int stock) {
        return Producto.builder().id(id).nombre(nombre).precio(precio).stock(stock).estado(1).build();
    }

    private CrearVentaRequest.DetalleVentaRequest detalleRequest(Long productoId, int cantidad, BigDecimal precio) {
        CrearVentaRequest.DetalleVentaRequest d = new CrearVentaRequest.DetalleVentaRequest();
        d.setProductoId(productoId);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(precio);
        return d;
    }

    private CrearVentaRequest requestBase(Long clienteId, FormaPago formaPago,
                                           CrearVentaRequest.DetalleVentaRequest... detalles) {
        CrearVentaRequest r = new CrearVentaRequest();
        r.setClienteId(clienteId);
        r.setTipoComprobante(TipoComprobante.BOLETA);
        r.setSerie("B001");
        r.setFormaPago(formaPago);
        r.setDetalles(List.of(detalles));
        return r;
    }

    private void mockUsuarioCreacion() {
        when(usuarioRepository.findByUsuario("admin"))
                .thenReturn(Optional.of(Usuario.builder().id(1L).usuario("admin").build()));
    }

    private void mockComprobante(String serie, String numero) {
        when(comprobanteSecuenciaService.generarNumeroComprobante(any(TipoComprobante.class), anyString()))
                .thenReturn(new String[]{serie, numero});
    }

    private void mockGuardarVentaConId(Long id) {
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(id);
            return v;
        });
    }

    // ===================== Crear venta - CONTADO =====================

    @Test
    @DisplayName("Crear venta CONTADO: queda PAGADA, descuenta stock y no crea crédito")
    void crearVentaContado_quedaPagadaYDescuentaStock() {
        Cliente cliente = clienteActivo(1L, "12345678");
        Producto producto = producto(10L, "Zapatilla X", new BigDecimal("100.00"), 5);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        mockComprobante("B001", "000123");
        mockGuardarVentaConId(500L);
        mockUsuarioCreacion();

        CrearVentaRequest request = requestBase(1L, FormaPago.CONTADO,
                detalleRequest(10L, 2, new BigDecimal("100.00")));

        VentaResponse response = ventaService.crearVenta(request, "admin");

        assertThat(response.getEstado()).isEqualTo(EstadoVenta.PAGADA);
        assertThat(response.getFormaPago()).isEqualTo(FormaPago.CONTADO);
        assertThat(response.getTotal()).isEqualByComparingTo("200.00");
        assertThat(response.getSerie()).isEqualTo("B001");
        assertThat(response.getComprobanteCompleto()).isEqualTo("B001-000123");
        assertThat(producto.getStock()).isEqualTo(3);
        verify(productRepository).save(producto);
        verify(creditoVentaService, never()).crearCredito(any(), any());
    }

    // ===================== Crear venta - CREDITO =====================

    @Test
    @DisplayName("Crear venta CREDITO: queda PENDIENTE y delega la creación del crédito")
    void crearVentaCredito_quedaPendienteYCreaCredito() {
        Cliente cliente = clienteActivo(1L, "12345678");
        Producto producto = producto(10L, "Zapatilla X", new BigDecimal("100.00"), 5);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        mockComprobante("B001", "000124");
        mockGuardarVentaConId(501L);
        mockUsuarioCreacion();

        CrearVentaRequest.CreditoVentaRequest creditoRequest = new CrearVentaRequest.CreditoVentaRequest();
        creditoRequest.setNumeroCuotas(4);
        creditoRequest.setIntervaloCuotas(IntervaloCredito.MENSUAL);

        CrearVentaRequest request = requestBase(1L, FormaPago.CREDITO,
                detalleRequest(10L, 1, new BigDecimal("100.00")));
        request.setCredito(creditoRequest);

        VentaResponse response = ventaService.crearVenta(request, "admin");

        assertThat(response.getEstado()).isEqualTo(EstadoVenta.PENDIENTE);

        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(creditoVentaService).crearCredito(ventaCaptor.capture(), eq(creditoRequest));
        assertThat(ventaCaptor.getValue().getId()).isEqualTo(501L);
    }

    // ===================== Stock insuficiente =====================

    /**
     * Hallazgo real detectado escribiendo este test: el número de comprobante
     * se genera y CONSUME (persistido dentro de ComprobanteSecuenciaService,
     * mockeado aquí) ANTES de validar el stock — si el stock falla, la venta no
     * se guarda pero el número de secuencia ya se incrementó. Este test
     * documenta que {@code generarNumeroComprobante} sí se invoca aunque la
     * venta termine fallando.
     */
    @Test
    @DisplayName("Crear venta con stock insuficiente: lanza excepción y no guarda la venta")
    void crearVentaConStockInsuficiente_lanzaExcepcionYNoGuardaVenta() {
        Cliente cliente = clienteActivo(1L, "12345678");
        Producto producto = producto(10L, "Zapatilla X", new BigDecimal("100.00"), 1);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        mockComprobante("B001", "000125");

        CrearVentaRequest request = requestBase(1L, FormaPago.CONTADO,
                detalleRequest(10L, 5, new BigDecimal("100.00")));

        assertThatThrownBy(() -> ventaService.crearVenta(request, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(ventaRepository, never()).save(any(Venta.class));
        verify(comprobanteSecuenciaService).generarNumeroComprobante(any(), anyString());
    }

    // ===================== Resolución de cliente =====================

    @Test
    @DisplayName("Cliente inactivo por ID: lanza excepción y no guarda la venta")
    void crearVentaConClienteInactivo_lanzaExcepcion() {
        Cliente inactivo = Cliente.builder().id(1L).nombre("Juan").documento("12345678").estado(0).build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(inactivo));

        CrearVentaRequest request = requestBase(1L, FormaPago.CONTADO,
                detalleRequest(10L, 1, new BigDecimal("100.00")));

        assertThatThrownBy(() -> ventaService.crearVenta(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no está activo");

        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    @DisplayName("Cliente por documento (sin clienteId): delega en ClienteService en vez del repositorio")
    void crearVentaPorDocumento_delegaEnClienteService() {
        Cliente cliente = clienteActivo(2L, "87654321");
        Producto producto = producto(10L, "Zapatilla X", new BigDecimal("50.00"), 5);
        when(clienteService.obtenerOCrearCliente("87654321")).thenReturn(cliente);
        when(productRepository.findById(10L)).thenReturn(Optional.of(producto));
        mockComprobante("B001", "000126");
        mockGuardarVentaConId(502L);
        mockUsuarioCreacion();

        CrearVentaRequest request = requestBase(null, FormaPago.CONTADO,
                detalleRequest(10L, 1, new BigDecimal("50.00")));
        request.setDocumento("87654321");

        VentaResponse response = ventaService.crearVenta(request, "admin");

        assertThat(response.getClienteId()).isEqualTo(2L);
        verify(clienteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Cliente por documento: si ClienteService falla, se envuelve en RuntimeException")
    void crearVentaPorDocumento_errorDeClienteService_seEnvuelveEnRuntimeException() {
        when(clienteService.obtenerOCrearCliente("00000000"))
                .thenThrow(new ClienteService.ClienteException("RENIEC no respondió"));

        CrearVentaRequest request = requestBase(null, FormaPago.CONTADO,
                detalleRequest(10L, 1, new BigDecimal("50.00")));
        request.setDocumento("00000000");

        assertThatThrownBy(() -> ventaService.crearVenta(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al procesar cliente");

        verify(ventaRepository, never()).save(any(Venta.class));
    }

    // ===================== Regla FACTURA requiere RUC =====================

    @Test
    @DisplayName("Emitir FACTURA a un cliente con DNI (8 dígitos): lanza excepción")
    void crearVentaFacturaConDni_lanzaExcepcion() {
        Cliente clienteConDni = clienteActivo(1L, "12345678");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteConDni));

        CrearVentaRequest request = requestBase(1L, FormaPago.CONTADO,
                detalleRequest(10L, 1, new BigDecimal("100.00")));
        request.setTipoComprobante(TipoComprobante.FACTURA);

        assertThatThrownBy(() -> ventaService.crearVenta(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Se requiere RUC");

        verify(ventaRepository, never()).save(any(Venta.class));
        verify(comprobanteSecuenciaService, never()).generarNumeroComprobante(any(), anyString());
    }

    // ===================== Serie de comprobante sin secuencia activa =====================

    @Test
    @DisplayName("Sin secuencia activa para tipo/serie: propaga la excepción y no guarda la venta")
    void crearVentaSinSecuenciaActiva_propagaExcepcion() {
        Cliente cliente = clienteActivo(1L, "12345678");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(comprobanteSecuenciaService.generarNumeroComprobante(any(), anyString()))
                .thenThrow(new RuntimeException("No existe una secuencia activa para el tipo BOLETA y serie B001"));

        CrearVentaRequest request = requestBase(1L, FormaPago.CONTADO,
                detalleRequest(10L, 1, new BigDecimal("100.00")));

        assertThatThrownBy(() -> ventaService.crearVenta(request, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("secuencia activa");

        verify(ventaRepository, never()).save(any(Venta.class));
    }

    // ===================== Anular venta =====================

    @Test
    @DisplayName("Anular venta: devuelve el stock de cada producto y marca ANULADA")
    void anularVenta_devuelveStockYMarcaAnulada() {
        Producto producto = producto(10L, "Zapatilla X", new BigDecimal("100.00"), 3);
        DetalleVenta detalle = DetalleVenta.builder().producto(producto).cantidad(2)
                .precioUnitario(new BigDecimal("100.00")).descuentoPorcentaje(BigDecimal.ZERO)
                .subtotal(new BigDecimal("200.00")).build();
        Venta venta = Venta.builder().id(700L).cliente(clienteActivo(1L, "12345678"))
                .tipoComprobante(TipoComprobante.BOLETA).serie("B001").numero("000700")
                .formaPago(FormaPago.CONTADO).subtotal(new BigDecimal("200.00"))
                .total(new BigDecimal("200.00")).estado(EstadoVenta.PAGADA)
                .detalles(new java.util.ArrayList<>(List.of(detalle))).build();
        detalle.setVenta(venta);

        when(ventaRepository.findByIdWithDetalles(700L)).thenReturn(Optional.of(venta));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaResponse response = ventaService.anularVenta(700L);

        assertThat(producto.getStock()).isEqualTo(5);
        assertThat(response.getEstado()).isEqualTo(EstadoVenta.ANULADA);
        verify(creditoVentaService, never()).cancelarCredito(any());
    }

    @Test
    @DisplayName("Anular una venta ya anulada: lanza excepción")
    void anularVentaYaAnulada_lanzaExcepcion() {
        Venta anulada = Venta.builder().id(701L).cliente(clienteActivo(1L, "12345678"))
                .tipoComprobante(TipoComprobante.BOLETA).serie("B001").numero("000701")
                .formaPago(FormaPago.CONTADO).subtotal(BigDecimal.TEN).total(BigDecimal.TEN)
                .estado(EstadoVenta.ANULADA).detalles(List.of()).build();
        when(ventaRepository.findByIdWithDetalles(701L)).thenReturn(Optional.of(anulada));

        assertThatThrownBy(() -> ventaService.anularVenta(701L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está anulada");

        verify(ventaRepository, never()).save(any(Venta.class));
    }
}

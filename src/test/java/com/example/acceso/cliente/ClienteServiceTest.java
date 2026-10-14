package com.example.acceso.cliente;

import com.example.acceso.cliente.ClienteService.ClienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - consulta DNI/RUC, alta y duplicados")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeEach
    void configurarApiExterna() {
        ReflectionTestUtils.setField(clienteService, "miapiToken", "token");
        ReflectionTestUtils.setField(clienteService, "dniUrl", "https://api.test/dni/");
        ReflectionTestUtils.setField(clienteService, "rucUrl", "https://api.test/ruc/");
    }

    private ResponseEntity<DniResponseDTO> respuestaDni(String nombres, String paterno, String materno) {
        DniResponseDTO.DatosDni datos = new DniResponseDTO.DatosDni();
        datos.setNombres(nombres);
        datos.setApePaterno(paterno);
        datos.setApeMaterno(materno);
        DniResponseDTO body = new DniResponseDTO();
        body.setSuccess(true);
        body.setDatos(datos);
        return ResponseEntity.ok(body);
    }

    @Test
    void consultarDocumento_formatoInvalido_noLlamaALaApi() {
        Map<String, Object> r = clienteService.consultarDocumento("123");

        assertThat(r.get("success")).isEqualTo(false);
        assertThat((String) r.get("message")).contains("8 dígitos").contains("11 dígitos");
        verifyNoInteractions(restTemplate);
    }

    @Test
    void consultarDocumento_dniValido_devuelveNombreCompleto() {
        when(restTemplate.exchange(eq("https://api.test/dni/12345678"), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(DniResponseDTO.class))).thenReturn(respuestaDni("JUAN", "PEREZ", "LOPEZ"));

        Map<String, Object> r = clienteService.consultarDocumento("12345678");

        assertThat(r).containsEntry("success", true).containsEntry("nombre", "JUAN PEREZ LOPEZ").containsEntry("tipo", "DNI");
    }

    @Test
    void consultarDocumento_apiCaida_noExponeElErrorTecnico() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(DniResponseDTO.class)))
                .thenThrow(new ResourceAccessException("I/O error on GET request for https://api.test/dni/12345678"));

        Map<String, Object> r = clienteService.consultarDocumento("12345678");

        assertThat(r.get("success")).isEqualTo(false);
        assertThat((String) r.get("message")).doesNotContain("api.test");
    }

    @Test
    void obtenerOCrear_existente_noConsultaLaApi() {
        Cliente existente = Cliente.builder().id(1L).documento("12345678").nombre("Juan").estado(1).build();
        when(clienteRepository.findByDocumento("12345678")).thenReturn(Optional.of(existente));

        assertThat(clienteService.obtenerOCrearCliente(" 12345678 ")).isSameAs(existente);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void obtenerOCrear_eliminado_seRechaza() {
        Cliente eliminado = Cliente.builder().id(1L).documento("12345678").nombre("Juan").estado(2).build();
        when(clienteRepository.findByDocumento("12345678")).thenReturn(Optional.of(eliminado));

        assertThatThrownBy(() -> clienteService.obtenerOCrearCliente("12345678"))
                .isInstanceOf(ClienteException.class).hasMessageContaining("eliminado");
    }

    @Test
    void obtenerOCrear_nuevo_seCreaConLosDatosDeLaApi() {
        when(clienteRepository.findByDocumento("12345678")).thenReturn(Optional.empty());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(DniResponseDTO.class)))
                .thenReturn(respuestaDni("ANA", "RIOS", "VEGA"));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente nuevo = clienteService.obtenerOCrearCliente("12345678");

        assertThat(nuevo.getNombre()).isEqualTo("ANA RIOS VEGA");
        assertThat(nuevo.getEstado()).isEqualTo(1);
    }

    @Test
    void guardarNuevo_conDocumentoDuplicado_seRechaza() {
        when(clienteRepository.existsByDocumentoAndEstadoNot("12345678", 2)).thenReturn(true);
        Cliente c = Cliente.builder().nombre("Juan").documento("12345678").telefono("999999999").build();

        assertThatThrownBy(() -> clienteService.guardarCliente(c))
                .isInstanceOf(ClienteException.class).hasMessageContaining("Ya existe");
        verify(clienteRepository, never()).save(any());
    }
}

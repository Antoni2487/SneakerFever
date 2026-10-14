package com.example.acceso.seguridad;

import com.example.acceso.perfil.Opcion;
import com.example.acceso.perfil.OpcionRepository;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilRepository;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de acceso de punta a punta (filtro JWT + SecurityConfig +
 * @PreAuthorize + GlobalExceptionHandler) con la app completa sobre H2.
 */
@SpringBootTest(properties = "app.cors.allowed-origins=https://tienda.test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Seguridad - integración con la app completa")
class SeguridadIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OpcionRepository opcionRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario vendedor;

    @BeforeAll
    void crearVendedorConPermisoDeVentas() {
        Opcion ventas = opcionRepository.save(Opcion.builder()
                .nombre("Ventas IT").ruta("/ventas").permiso(Permisos.VENTAS).build());
        Perfil perfil = perfilRepository.save(Perfil.builder()
                .nombre("Vendedor IT").estado(true).opciones(Set.of(ventas)).build());
        vendedor = usuarioRepository.save(Usuario.builder()
                .nombre("Vendedor").usuario("vendedor-it").clave("$2a$10$hashDePruebaNoUsado000000000000000000000000000000000").correo("vendedor-it@test.com")
                .estado(1).perfil(perfil).build());
    }

    private String bearer() {
        return "Bearer " + jwtService.generateAccessToken(vendedor);
    }

    @Test
    void sinToken_apiDelPanel_401() throws Exception {
        mvc.perform(get("/ventas/api/listar")).andExpect(status().isUnauthorized());
    }

    @Test
    void sinToken_apisPublicas_200() throws Exception {
        mvc.perform(get("/api/public/productos")).andExpect(status().isOk());
        mvc.perform(get("/personalizacion/api/public/slides")).andExpect(status().isOk());
    }

    @Test
    void conPermiso_200() throws Exception {
        mvc.perform(get("/ventas/api/listar").header("Authorization", bearer())).andExpect(status().isOk());
    }

    @Test
    void sinPermisoDelDominio_403EnJson() throws Exception {
        mvc.perform(get("/productos/api/listar").header("Authorization", bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(delete("/api/upload/productos/imagen/x.png").header("Authorization", bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rutaSinRegla_seDeniega() throws Exception {
        mvc.perform(get("/no-existe").header("Authorization", bearer())).andExpect(status().isForbidden());
    }

    @Test
    void refreshTokenNoSirveComoAccessToken() throws Exception {
        String refresh = jwtService.generateRefreshToken(vendedor);
        mvc.perform(get("/ventas/api/listar").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cors_soloElOrigenConfigurado() throws Exception {
        mvc.perform(options("/ventas/api/listar")
                        .header("Origin", "https://tienda.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://tienda.test"));
        mvc.perform(options("/ventas/api/listar")
                        .header("Origin", "https://malicioso.test")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}

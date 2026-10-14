package com.example.acceso.seguridad;

import com.example.acceso.perfil.Opcion;
import com.example.acceso.perfil.OpcionRepository;
import com.example.acceso.perfil.Perfil;
import com.example.acceso.perfil.PerfilRepository;
import com.example.acceso.usuario.Usuario;
import com.example.acceso.usuario.UsuarioRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Logout del lado del servidor: los tokens de la sesión cerrada dejan de servir. */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Logout - revoca los tokens de la sesión en el servidor")
class LogoutIntegrationTest {

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

    private Usuario usuario;

    @BeforeAll
    void crearUsuario() {
        Opcion ventas = opcionRepository.save(Opcion.builder()
                .nombre("Ventas Logout").ruta("/ventas").permiso(Permisos.VENTAS).build());
        Perfil perfil = perfilRepository.save(Perfil.builder()
                .nombre("Vendedor Logout").estado(true).opciones(Set.of(ventas)).build());
        usuario = usuarioRepository.save(Usuario.builder()
                .nombre("Vendedor").usuario("vendedor-logout").clave("$2a$10$hashDePruebaNoUsado000000000000000000000000000000000")
                .correo("vendedor-logout@test.com").estado(1).perfil(perfil).build());
    }

    @Test
    void trasLogout_elAccessYElRefreshDeEsaSesionDejanDeServir() throws Exception {
        String access = jwtService.generateAccessToken(usuario);
        String refresh = jwtService.generateRefreshToken(usuario);
        mvc.perform(get("/ventas/api/pagina").header("Authorization", "Bearer " + access)).andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + access)
                        .cookie(new Cookie("refreshToken", refresh)))
                .andExpect(status().isOk());

        mvc.perform(get("/ventas/api/pagina").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEnUnDispositivo_noCierraLaSesionDeOtro() throws Exception {
        String accessCelular = jwtService.generateAccessToken(usuario);
        String accessLaptop = jwtService.generateAccessToken(usuario);
        String refreshLaptop = jwtService.generateRefreshToken(usuario);

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + accessCelular))
                .andExpect(status().isOk());

        mvc.perform(get("/ventas/api/pagina").header("Authorization", "Bearer " + accessLaptop))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", refreshLaptop)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutSinTokens_igualResponde200() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer basura"))
                .andExpect(status().isOk());
    }
}

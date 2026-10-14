package com.example.acceso.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lecturas de otro dominio que las pantallas necesitan (p. ej. Nueva venta
 * busca clientes y productos): permitidas al dominio que las usa, sin abrir el
 * resto del dominio dueño.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Permisos cruzados - lecturas compartidas entre pantallas")
class PermisosCruzadosIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(authorities = Permisos.VENTAS)
    void vendedor_puedeBuscarClientesYProductosParaUnaVenta() throws Exception {
        mvc.perform(get("/productos/api/listarDisponibles")).andExpect(status().isOk());
        mvc.perform(get("/clientes/api/documento/12345678")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = Permisos.VENTAS)
    void vendedor_noGestionaProductosNiClientes() throws Exception {
        mvc.perform(get("/productos/api/pagina")).andExpect(status().isForbidden());
        mvc.perform(post("/productos/api/crear").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/clientes/api/pagina")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = Permisos.PRODUCTOS)
    void catalogo_puedeCargarCategoriasYMarcasDelFormulario() throws Exception {
        mvc.perform(get("/categorias/api/activas")).andExpect(status().isOk());
        mvc.perform(get("/marcas/api/activas")).andExpect(status().isOk());
        mvc.perform(get("/categorias/api/listar")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = Permisos.INVENTARIO)
    void inventario_puedeListarProductosYStockBajo() throws Exception {
        mvc.perform(get("/productos/api/listar")).andExpect(status().isOk());
        mvc.perform(get("/productos/api/stock-bajo")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = Permisos.DASHBOARD)
    void dashboard_puedeLeerVentasYStockBajo() throws Exception {
        mvc.perform(get("/ventas/api/listar")).andExpect(status().isOk());
        mvc.perform(get("/productos/api/stock-bajo")).andExpect(status().isOk());
        mvc.perform(get("/ventas/api/pagina")).andExpect(status().isForbidden());
    }
}

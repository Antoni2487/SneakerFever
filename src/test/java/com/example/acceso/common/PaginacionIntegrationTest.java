package com.example.acceso.common;

import com.example.acceso.categoria.Categoria;
import com.example.acceso.categoria.CategoriaRepository;
import com.example.acceso.cliente.Cliente;
import com.example.acceso.cliente.ClienteRepository;
import com.example.acceso.marca.Marca;
import com.example.acceso.marca.MarcaRepository;
import com.example.acceso.producto.Genero;
import com.example.acceso.producto.Producto;
import com.example.acceso.producto.ProductoRepository;
import com.example.acceso.seguridad.Permisos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints /api/pagina contra la base H2 real: tamaño, total, búsqueda y exclusión de eliminados. */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Paginación - endpoints /api/pagina")
class PaginacionIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @BeforeAll
    void sembrar() {
        // 23 clientes activos "Pag Cliente NN" + 1 eliminado que no debe aparecer.
        clienteRepository.saveAll(IntStream.rangeClosed(1, 23).mapToObj(i -> Cliente.builder()
                .nombre(String.format("Pag Cliente %02d", i)).documento(String.format("7%07d", i))
                .telefono("9000000" + String.format("%02d", i)).estado(1).build()).toList());
        clienteRepository.save(Cliente.builder().nombre("Pag Cliente Eliminado").documento("79999999")
                .telefono("900000099").estado(2).build());

        Categoria categoria = categoriaRepository.save(Categoria.builder().nombre("Pag Categoria").estado(1).build());
        Marca marca = marcaRepository.save(Marca.builder().nombre("Pag Marca").estado(1).build());
        productoRepository.saveAll(IntStream.rangeClosed(1, 12).mapToObj(i -> Producto.builder()
                .nombre("Pag Zapatilla " + i).precio(new BigDecimal("100")).stock(5).stockMinimo(1)
                .destacado(false).genero(Genero.HOMBRE).category(categoria).brand(marca).estado(1)
                .imagenes(List.of("/uploads/productos/" + i + "a.png", "/uploads/productos/" + i + "b.png"))
                .build()).toList());
    }

    @Test
    @WithMockUser(authorities = Permisos.CLIENTES)
    void clientes_paginaConTotalesYSinEliminados() throws Exception {
        mvc.perform(get("/clientes/api/pagina").param("q", "Pag Cliente").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(23))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.data[0].nombre").value("Pag Cliente 21"));
    }

    @Test
    @WithMockUser(authorities = Permisos.CLIENTES)
    void clientes_buscaPorDocumento() throws Exception {
        mvc.perform(get("/clientes/api/pagina").param("q", "70000007"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Pag Cliente 07"));
    }

    @Test
    @WithMockUser(authorities = Permisos.CLIENTES)
    void tamanoSeAcotaAlMaximo() throws Exception {
        mvc.perform(get("/clientes/api/pagina").param("size", "5000"))
                .andExpect(jsonPath("$.size").value(Paginacion.TAMANO_MAXIMO));
    }

    @Test
    @WithMockUser(authorities = Permisos.PRODUCTOS)
    void productos_paginaConImagenesYRelaciones() throws Exception {
        mvc.perform(get("/productos/api/pagina").param("q", "pag zapatilla").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.data[0].imagenes", hasSize(2)));
    }

    @Test
    @WithMockUser(authorities = Permisos.VENTAS)
    void ventas_sinVentasDevuelvePaginaVacia() throws Exception {
        mvc.perform(get("/ventas/api/pagina").param("q", "no-existe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(authorities = Permisos.VENTAS)
    void sinPermisoDelDominio_403() throws Exception {
        mvc.perform(get("/clientes/api/pagina")).andExpect(status().isForbidden());
    }
}

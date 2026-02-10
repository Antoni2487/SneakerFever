package com.example.acceso.service;

import com.example.acceso.model.EnumVentas.TipoComprobante;
import com.example.acceso.model.EntidadesVenta.ComprobanteSecuencia;
import com.example.acceso.repository.RepositorioVentas.ComprobanteSecuenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteSecuenciaService {

    private final ComprobanteSecuenciaRepository comprobanteSecuenciaRepository;

    // ===================== Generar Serie y Número =====================

    @Transactional
    public String[] generarNumeroComprobante(TipoComprobante tipoComprobante, String serie) {
        // Buscar la secuencia activa para el tipo y serie
        var secuencia = comprobanteSecuenciaRepository
                .findByTipoComprobanteAndSerieWithLock(tipoComprobante, serie)
                .orElseThrow(() -> new RuntimeException(
                        "No existe una secuencia activa para el tipo " + tipoComprobante + " y serie " + serie
                ));

        // Generar siguiente número y guardar
        String numeroGenerado = secuencia.generarSiguienteNumero();
        comprobanteSecuenciaRepository.save(secuencia);

        log.info("Comprobante generado → {} - {}", secuencia.getSerie(), numeroGenerado);
        return new String[]{secuencia.getSerie(), numeroGenerado};
    }

    @Transactional(readOnly = true)
    public String visualizarSiguienteNumero(TipoComprobante tipoComprobante, String serie) {
        return comprobanteSecuenciaRepository
                .findByTipoComprobanteAndSerie(tipoComprobante, serie)
                .map(ComprobanteSecuencia::visualizarSiguienteNumero)
                .orElseThrow(() -> new RuntimeException(
                        "No existe secuencia para el tipo " + tipoComprobante + " y serie " + serie
                ));
    }
}

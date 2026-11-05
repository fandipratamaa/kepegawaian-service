package cc.kertaskerja.kepegawaian.integration.simpeg;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cc.kertaskerja.kepegawaian.integration.simpeg.dto.BaseSimpegResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegJabatanResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegOpdResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegPegawaiResponse;

@Component
@ConditionalOnProperty(prefix = "integration.simpeg", name = "base-url")
public class SimpegClient {

    private static final Logger log = LoggerFactory.getLogger(SimpegClient.class);

    private final RestClient restClient;

    public SimpegClient(SimpegClientProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl() + "/" + properties.apiPath())
                .build();
    }

    public List<SimpegOpdResponse> findAllOpd(String activeCode) {
        log.info("Finding all opds for active code {}", activeCode);
        BaseSimpegResponse<SimpegOpdResponse> response = restClient
                .get()
                .uri("/master_opd/{active}", activeCode)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return response != null && response.data() != null
                ? response.data()
                : List.of();
    }

    public List<SimpegJabatanResponse> findJabatanByKodeOpd(String kodeOpd) {
        log.info("Finding jabatan by kode opd {}", kodeOpd);
        BaseSimpegResponse<SimpegJabatanResponse> response = restClient
                .get()
                .uri("/master_jabatan/{kodeOpd}", kodeOpd)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return response != null && response.data() != null
                ? response.data()
                : List.of();
    }

    public List<SimpegPegawaiResponse> findPegawaiByKodeOpd(String kodeOpd) {
        log.info("Finding pegawai by kode opd {}", kodeOpd);
        BaseSimpegResponse<SimpegPegawaiResponse> response = restClient
                .get()
                .uri("/list_pegawai/{kodeOpd}", kodeOpd)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        return response != null && response.data() != null
                ? response.data()
                : List.of();
    }
}

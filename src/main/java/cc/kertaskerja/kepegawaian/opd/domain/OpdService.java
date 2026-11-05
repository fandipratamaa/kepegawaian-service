package cc.kertaskerja.kepegawaian.opd.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cc.kertaskerja.kepegawaian.config.KertaskerjaProperties;
import cc.kertaskerja.kepegawaian.integration.simpeg.SimpegClient;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegOpdResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.mapper.SimpegOpdMapper;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.MappingOpd;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.MappingOpdRepository;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.SumberMapping;

@Service
@Transactional(readOnly = true)
public class OpdService {

    private final OpdRepository opdRepository;
    private final KertaskerjaProperties kertaskerjaProperties;
    private final SimpegClient simpegClient;
    private final MappingOpdRepository mappingOpdRepository;

    public OpdService(
            OpdRepository opdRepository,
            KertaskerjaProperties kertaskerjaProperties,
            SimpegClient simpegClient,
            MappingOpdRepository mappingOpdRepository) {
        this.opdRepository = opdRepository;
        this.kertaskerjaProperties = kertaskerjaProperties;
        this.simpegClient = simpegClient;
        this.mappingOpdRepository = mappingOpdRepository;
    }

    public Page<Opd> allOpd(Pageable pageable) {
        return opdRepository.findAll(pageable);
    }

    public List<Opd> findAllOpdAktifInLembaga() {
        return opdRepository
                .findAllByKodeLembagaAndStatusOpdOrderByNamaOpdAsc(
                        kertaskerjaProperties.kodeLembaga(),
                        OpdStatus.AKTIF);
    }

    public void findOpdByKodeOpd(String kodeOpd) {
        if (kodeOpd == null || kodeOpd.isBlank()) {
            throw new IllegalArgumentException("Kode OPD tidak boleh kosong");
        }

        opdRepository.findByKodeOpd(kodeOpd)
                .orElseThrow(() -> new OpdNotFoundException(kodeOpd));
    }

    public Opd findOpdById(Long id) {
        return opdRepository.findById(id)
                .orElseThrow(() -> new OpdNotFoundException(id));
    }

    @Transactional
    public Opd save(Opd newOpd) {
        String kodeOpd = newOpd.kodeOpd();

        // guard
        if (opdRepository.existsByKodeOpd(kodeOpd)) {
            throw new OpdAlreadyExistsException(kodeOpd);
        }

        return opdRepository.save(Opd.of(
                kertaskerjaProperties.kodeLembaga(),
                kodeOpd,
                newOpd.namaOpd(),
                newOpd.singkatanOpd()));
    }

    @Transactional
    public Opd update(Long id, Opd updatedOpd) {
        Opd existingOpd = findOpdById(id);

        opdRepository.findByKodeOpd(updatedOpd.kodeOpd())
                .filter(opd -> !opd.id().equals(id))
                .ifPresent(opd -> {
                    throw new OpdAlreadyExistsException(updatedOpd.kodeOpd());
                });

        return opdRepository.save(
                existingOpd.update(
                        updatedOpd.kodeOpd(),
                        updatedOpd.namaOpd(),
                        updatedOpd.singkatanOpd()));
    }

    @Transactional
    public void delete(Long id) {
        // guard opd not found
        findOpdById(id);
        opdRepository.deleteById(id);
    }

    @Transactional
    public void syncSimpeg() {

        String sumberMapping = SumberMapping.SIMPEG.name();

        List<MappingOpd> mappingOpds = mappingOpdRepository.findBySumber(sumberMapping);

        Map<String, String> mappingByKodeSumber = mappingOpds.stream()
                .collect(Collectors.toMap(
                        MappingOpd::kodeSumber,
                        MappingOpd::kodeMaster));

        List<SimpegOpdResponse> responses = simpegClient.findAllOpd("1");

        SimpegOpdMapper mapper = new SimpegOpdMapper(kertaskerjaProperties);

        List<Opd> opds = responses.stream()
                .filter(response -> mappingByKodeSumber.containsKey(
                        response.opdKode()))
                .map(response -> mapper.toDomain(
                        response,
                        mappingByKodeSumber.get(
                                response.opdKode())))
                .toList();

        opdRepository.saveAll(opds);
    }
}

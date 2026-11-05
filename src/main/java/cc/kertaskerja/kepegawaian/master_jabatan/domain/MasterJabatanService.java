package cc.kertaskerja.kepegawaian.master_jabatan.domain;

import cc.kertaskerja.kepegawaian.integration.simpeg.SimpegClient;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegJabatanResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.mapper.SimpegJabatanMapper;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.MappingOpd;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.MappingOpdRepository;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.SumberMapping;
import cc.kertaskerja.kepegawaian.opd.domain.Opd;
import cc.kertaskerja.kepegawaian.opd.domain.OpdNotFoundException;
import cc.kertaskerja.kepegawaian.opd.domain.OpdRepository;
import com.github.slugify.Slugify;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MasterJabatanService {
    private final MasterJabatanRepository masterJabatanRepository;
    private final SimpegClient simpegClient;
    private final OpdRepository opdRepository;
    private final MappingOpdRepository mappingOpdRepository;

    public MasterJabatanService(
            MasterJabatanRepository masterJabatanRepository,
            SimpegClient simpegClient,
            OpdRepository opdRepository,
            MappingOpdRepository mappingOpdRepository) {
        this.masterJabatanRepository = masterJabatanRepository;
        this.simpegClient = simpegClient;
        this.opdRepository = opdRepository;
        this.mappingOpdRepository = mappingOpdRepository;
    }

    public Page<MasterJabatan> allJabatan(Pageable pageable) {
        return masterJabatanRepository.findAll(pageable);
    }

    public List<MasterJabatanJenjang> listJenjangJabatan() {
        return List.of(MasterJabatanJenjang.values());
    }

    public List<MasterJabatanKategori> listKategoriJabatan() {
        return List.of(MasterJabatanKategori.values());
    }

    public List<MasterJabatanStatus> listStatusJabatan() {
        return List.of(MasterJabatanStatus.values());
    }

    public List<MasterJabatan> findAllByOpdId(Long opdId) {
        return masterJabatanRepository.findAllByOpdIdAndStatusJabatanOrderByNamaJabatan(opdId,
                MasterJabatanStatus.AKTIF);
    }

    public MasterJabatan findMasterJabatanById(Long id) {
        return masterJabatanRepository.findById(id)
                .orElseThrow(() -> new MasterJabatanNotFoundException(id));
    }

    @Transactional
    public MasterJabatan create(MasterJabatan masterJabatan) {
        String kodeJabatan = createKodeJabatan(masterJabatan);

        if (masterJabatanRepository.existsByOpdIdAndKodeJabatan(masterJabatan.opdId(), kodeJabatan)) {
            throw new MasterJabatanAlreadyExistsException(masterJabatan.namaJabatan());
        }

        return masterJabatanRepository.save(MasterJabatan.of(
                masterJabatan.opdId(),
                kodeJabatan,
                masterJabatan.namaJabatan(),
                masterJabatan.jenjangJabatan(),
                MasterJabatanStatus.AKTIF));
    }

    @Transactional
    public MasterJabatan update(Long id, MasterJabatan updatedMasterJabatan) {
        MasterJabatan existing = findMasterJabatanById(id);

        String kodeJabatan = createKodeJabatan(updatedMasterJabatan);

        masterJabatanRepository.findByKodeJabatan(kodeJabatan)
                .filter(mj -> !mj.id().equals(id))
                .ifPresent(mj -> {
                    throw new MasterJabatanAlreadyExistsException(kodeJabatan);
                });

        return masterJabatanRepository.save(
                existing.update(
                        updatedMasterJabatan.opdId(),
                        updatedMasterJabatan.namaJabatan(),
                        updatedMasterJabatan.jenjangJabatan(),
                        kodeJabatan));
    }

    @Transactional
    public String delete(Long id) {
        MasterJabatan jabatan = findMasterJabatanById(id);

        masterJabatanRepository.deleteById(id);

        return jabatan.namaJabatan();
    }

    @Transactional
    public void syncJabatanDariSimpeg(Long opdId) {
        // OPD ID DARI INTERNAL
        // ambil opd dulu
        // Cari OPD sekali saja
        Opd opd = opdRepository.findById(opdId)
                .orElseThrow(() -> new OpdNotFoundException(opdId));
        // cari kode opd simpeg dulu
        String sumberMapping = SumberMapping.SIMPEG.name();

        // find opd mapper
        // ambil kode opd simpeg (01, 02) dari
        // kode opd asli (1.01.0.00.0.00.01.0000)
        Optional<MappingOpd> mappingOpd = mappingOpdRepository
                .findBySumberAndKodeMaster(
                        sumberMapping,
                        opd.kodeOpd());

        if (mappingOpd.isEmpty()) {
            return;
        }
        // ambil dari simpeg
        // pakai kode opd simpeg untuk sync
        List<SimpegJabatanResponse> responses = simpegClient.findJabatanByKodeOpd(mappingOpd.get().kodeSumber());

        SimpegJabatanMapper mapper = new SimpegJabatanMapper();

        // unique master jabatan
        List<MasterJabatan> masterJabatans = responses.stream()
                .map(response -> mapper.toDomain(response, opdId))
                .collect(Collectors.toMap(
                        MasterJabatan::kodeJabatan,
                        Function.identity(),
                        (existing, duplicate) -> existing))
                .values()
                .stream()
                .toList();

        masterJabatanRepository.saveAll(masterJabatans);
    }

    private String createKodeJabatan(MasterJabatan jabatan) {
        final Slugify slg = Slugify.builder().build();
        String slug = slg.slugify(jabatan.namaJabatan());
        String suffix = jabatan.jenjangJabatan().name();

        return slug.toUpperCase(Locale.ROOT) + "-" + suffix.toUpperCase(Locale.ROOT);
    }
}

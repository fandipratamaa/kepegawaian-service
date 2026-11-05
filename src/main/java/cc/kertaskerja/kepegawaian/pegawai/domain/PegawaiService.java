package cc.kertaskerja.kepegawaian.pegawai.domain;

import cc.kertaskerja.kepegawaian.config.IdentityProperties;
import cc.kertaskerja.kepegawaian.identity.domain.User;
import cc.kertaskerja.kepegawaian.identity.domain.CreateUserResult;
import cc.kertaskerja.kepegawaian.identity.domain.IdentityService;
import cc.kertaskerja.kepegawaian.integration.simpeg.SimpegClient;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegJabatanPegawai;
import cc.kertaskerja.kepegawaian.integration.simpeg.dto.SimpegPegawaiResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.mapper.SimpegPegawaiMapper;
import cc.kertaskerja.kepegawaian.jabatan_pegawai.domain.JabatanPegawai;
import cc.kertaskerja.kepegawaian.jabatan_pegawai.domain.JabatanPegawaiNotFoundException;
import cc.kertaskerja.kepegawaian.jabatan_pegawai.domain.JabatanPegawaiRepository;
import cc.kertaskerja.kepegawaian.jabatan_pegawai.domain.JabatanPegawaiView;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.MappingOpd;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.MappingOpdRepository;
import cc.kertaskerja.kepegawaian.mapping_opd.domain.SumberMapping;
import cc.kertaskerja.kepegawaian.master_jabatan.domain.MasterJabatan;
import cc.kertaskerja.kepegawaian.master_jabatan.domain.MasterJabatanRepository;
import cc.kertaskerja.kepegawaian.opd.domain.Opd;
import cc.kertaskerja.kepegawaian.opd.domain.OpdNotFoundException;
import cc.kertaskerja.kepegawaian.opd.domain.OpdRepository;
import cc.kertaskerja.kepegawaian.role_pegawai.domain.AssignRoleResult;
import cc.kertaskerja.kepegawaian.role_pegawai.domain.RolePegawaiNotFoundException;
import cc.kertaskerja.kepegawaian.role_pegawai.domain.RolePegawaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class PegawaiService {
    private final PegawaiRepository pegawaiRepository;
    private final OpdRepository opdRepository;
    private final MappingOpdRepository mappingOpdRepository;
    private final SimpegClient simpegClient;
    private final MasterJabatanRepository masterJabatanRepository;
    private final JabatanPegawaiRepository jabatanPegawaiRepository;
    private final IdentityService identityService;
    private final IdentityProperties identityProperties;
    private final RolePegawaiService rolePegawaiService;

    private static final Logger log = LoggerFactory.getLogger(PegawaiService.class);
    private static final String DEFAULT_KODE_OPD = "0.00.0.00.0.0000";
    private static final String DEFAULT_NAMA_OPD = "UNKNOWN";

    public PegawaiService(PegawaiRepository pegawaiRepository,
            OpdRepository opdRepository,
            MappingOpdRepository mappingOpdRepository,
            SimpegClient simpegClient,
            MasterJabatanRepository masterJabatanRepository,
            JabatanPegawaiRepository jabatanPegawaiRepository,
            IdentityService identityService,
            IdentityProperties identityProperties,
            RolePegawaiService rolePegawaiService) {
        this.pegawaiRepository = pegawaiRepository;
        this.opdRepository = opdRepository;
        this.mappingOpdRepository = mappingOpdRepository;
        this.simpegClient = simpegClient;
        this.masterJabatanRepository = masterJabatanRepository;
        this.jabatanPegawaiRepository = jabatanPegawaiRepository;
        this.identityService = identityService;
        this.identityProperties = identityProperties;
        this.rolePegawaiService = rolePegawaiService;
    }

    public List<Pegawai> findAll(Long opdId, String roleUser) {
        if (roleUser == null || roleUser.isBlank()) {
            return pegawaiRepository.findDistinctByJabatanPegawaiOpdId(opdId);
        }
        return pegawaiRepository.findDistinctByJabatanPegawaiOpdIdAndRolePegawaiRoleNama(opdId, roleUser);
    }

    public Page<Pegawai> allPegawai(Pageable pageable) {
        return pegawaiRepository.findAll(pageable);
    }

    public PegawaiView findPegawaiByPegawaiId(String nip) {
        Pegawai pegawai = pegawaiRepository.findByNip(nip)
                .orElseThrow(() -> new PegawaiNotFoundException(nip));

        JabatanPegawaiView jabatanPegawai = jabatanPegawaiRepository.findAllByPegawaiId(pegawai.id())
                .stream().filter(JabatanPegawai::isAktif)
                .map(JabatanPegawai::toJabatanPegawaiView)
                .findFirst()
                .orElseThrow(JabatanPegawaiNotFoundException::new);

        AssignRoleResult role = rolePegawaiService.findAllByPegawaiId(pegawai.id())
                .stream()
                .findFirst()
                .orElseThrow(RolePegawaiNotFoundException::new);

        return new PegawaiView(pegawai, jabatanPegawai, role);
    }

    public Pegawai findPegawaiById(Long id) {
        return pegawaiRepository.findById(id)
                .orElseThrow(() -> new PegawaiNotFoundException(id));
    }

    public PegawaiDetails findHistoriPegawai(Long pegawaiId, PegawaiJenisHistori jenisHistori, Integer bulan,
            Integer tahun) {
        // guard pegawai
        Pegawai pegawai = findPegawaiById(pegawaiId);

        List<JabatanPegawaiView> jabatanPegawais = jabatanPegawaiRepository.findAllByPegawaiId(pegawaiId)
                .stream().map(JabatanPegawai::toJabatanPegawaiView).toList();

        List<AssignRoleResult> rolePegawais = rolePegawaiService.findAllByPegawaiId(pegawaiId);

        return new PegawaiDetails(
                pegawai.id(),
                pegawai.nip(),
                pegawai.namaPegawai(),
                jabatanPegawais,
                rolePegawais);
    }

    @Transactional
    public Pegawai create(Pegawai newPegawai, String initialPassword) {
        String nip = newPegawai.nip();

        if (pegawaiRepository.existsByNip(nip)) {
            throw new PegawaiAlreadyExistsException(nip);
        }

        return pegawaiRepository.save(newPegawai);
    }

    @Transactional
    public Pegawai update(Long id, Pegawai updatePegawai) {
        Pegawai existingPegawai = findPegawaiById(id);

        pegawaiRepository.findByNip(updatePegawai.nip())
                .filter(p -> !p.id().equals(id))
                .ifPresent(opd -> {
                    throw new PegawaiNotFoundException(updatePegawai.nip());
                });

        return pegawaiRepository.save(
                existingPegawai.update(
                        updatePegawai.nip(),
                        updatePegawai.namaPegawai(),
                        updatePegawai.jenisKelamin(),
                        updatePegawai.tempatLahir(),
                        updatePegawai.tanggalLahir(),
                        updatePegawai.jenisPegawai(),
                        updatePegawai.statusPegawai()));
    }

    @Transactional
    public String delete(Long id) {
        Pegawai pegawai = findPegawaiById(id);
        pegawaiRepository.deleteById(id);
        return pegawai.namaPegawai();
    }

    @Transactional
    public MigrationSummary migratePegawaiToKeycloak(Long opdId) {

        // filter hanya yang lengkap yang kita migrasi ke keycloak
        // opd / jabatan
        // role
        List<Pegawai> pegawais = findAll(opdId, "")
                .stream()
                .filter(peg -> {
                    // ONLY MIGRATE YANG PUNYA JABATAN DAN ROLE
                    var jabatanPegawai = jabatanPegawaiRepository.findActivePrimaryByPegawaiId(peg.id());
                    var rolePegawai = rolePegawaiService.findAllByPegawaiId(peg.id());
                    return jabatanPegawai.isPresent() && !rolePegawai.isEmpty();
                }).toList();

        List<PegawaiIdentityData> identities = mapToIdentityData(pegawais);

        int created = 0;
        int failed = 0;
        int linked = 0;

        for (PegawaiIdentityData identity : identities) {
            try {
                MigrationResult result = migratePegawai(identity);
                if (result == MigrationResult.CREATED) {
                    created++;
                } else if (result == MigrationResult.LINKED) {
                    linked++;
                }
            } catch (Exception ex) {
                failed++;

                log.error(
                        "Failed migrating pegawai id={}, nip={}",
                        identity.pegawai().id(),
                        identity.pegawai().nip(),
                        ex);
            }
        }

        log.info(
                "Migration finished. total={}, created={}, linked={}, failed={}",
                pegawais.size(),
                created,
                linked,
                failed);

        return new MigrationSummary(
                pegawais.size(),
                created,
                linked,
                failed);
    }

    // mapping keycloak attributes from pegawai records
    private List<PegawaiIdentityData> mapToIdentityData(
            List<Pegawai> pegawais) {
        List<Long> pegawaiIds = pegawais.stream()
                .map(Pegawai::id)
                .toList();

        Map<Long, JabatanPegawai> jabatanByPegawaiId = jabatanPegawaiRepository
                .findActivePrimaryByPegawaiIdIn(pegawaiIds)
                .stream()
                .collect(Collectors.toMap(
                        JabatanPegawai::pegawaiId,
                        Function.identity()));

        List<AssignRoleResult> assignments = rolePegawaiService.findAllByPegawaiIdIn(pegawaiIds);

        Map<Long, Set<String>> rolesByPegawaiId = assignments.stream()
                .collect(Collectors.groupingBy(
                        result -> result.assignment().pegawaiId(),
                        Collectors.mapping(
                                result -> result.role().namaRole(),
                                Collectors.toSet())));

        return pegawais.stream()
                .map(pegawai -> {
                    JabatanPegawai jabatan = jabatanByPegawaiId.get(pegawai.id());

                    return new PegawaiIdentityData(
                            pegawai,
                            jabatan != null
                                    ? jabatan.kodeOpd()
                                    : DEFAULT_KODE_OPD,
                            jabatan != null
                                    ? jabatan.namaOpd()
                                    : DEFAULT_NAMA_OPD,
                            rolesByPegawaiId.getOrDefault(
                                    pegawai.id(),
                                    Set.of()));
                })
                .toList();
    }

    protected MigrationResult migratePegawai(PegawaiIdentityData data) {
        IdentityMapper identityMapper = new IdentityMapper();
        CreateUserResult result = identityService.createUser(
                identityMapper.toCreateIdentityRequest(data));

        if (result.created()) {
            identityService.resetPassword(
                    result.userId(),
                    identityProperties.migration().defaultPassword(),
                    identityProperties.migration().temporaryPassword());
        }

        log.info("ASSIGN ROLES FOR pegawai={}", data.pegawai().namaPegawai());

        identityService.assignRoles(
                result.userId(),
                data.roles());

        // update pegawai keycloak id
        linkKeycloakUser(data.pegawai(), result.userId());

        return result.created()
                ? MigrationResult.CREATED
                : MigrationResult.LINKED;
    }

    @Transactional
    protected void linkKeycloakUser(
            Pegawai pegawai,
            String userId) {
        pegawaiRepository.save(
                pegawai.withKeycloakUserId(userId));
    }

    @Transactional
    public void syncPegawaiDariSimpeg(Long opdId) {
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
        List<SimpegPegawaiResponse> responses = simpegClient.findPegawaiByKodeOpd(mappingOpd.get().kodeSumber());

        SimpegPegawaiMapper mapper = new SimpegPegawaiMapper();

        // jabatan pegawai
        List<SimpegJabatanPegawai> jabatanPegawais = responses.stream()
                .map(source -> mapper.toJabatanPegawai(source, opdId))
                .toList();
        // unique pegawai
        List<Pegawai> pegawais = responses.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(
                        Pegawai::nip,
                        Function.identity(),
                        (existing, duplicate) -> existing))
                .values()
                .stream()
                .toList();

        // simpan all pegawai
        pegawaiRepository.upsertAll(pegawais);

        // cari pegawai id dari tersimpan
        Map<String, Long> pegawaiIds = pegawaiRepository
                .findByNipIn(
                        pegawais
                                .stream()
                                .map(Pegawai::nip).toList())
                .stream()
                .collect(Collectors.toMap(
                        Pegawai::nip,
                        Pegawai::id));

        // jabatan pegawai dari master jabatan
        Map<String, Long> jabatanIds = masterJabatanRepository
                .findByKodeJabatanIn(
                        jabatanPegawais
                                .stream()
                                .map(SimpegJabatanPegawai::kodeJabatan)
                                .toList())
                .stream()
                .collect(Collectors.toMap(
                        MasterJabatan::kodeJabatan,
                        MasterJabatan::id));

        List<JabatanPegawai> listJabatanPegawais = jabatanPegawais.stream()
                .flatMap(jabatan -> {
                    Long pegawaiId = pegawaiIds.get(jabatan.nip());
                    Long masterJabatanId = jabatanIds.get(jabatan.kodeJabatan());

                    if (pegawaiId == null || masterJabatanId == null) {
                        return Stream.empty();
                    }

                    return Stream.of(JabatanPegawai.aktif(
                            pegawaiId,
                            masterJabatanId,
                            jabatan.namaJabatan(),
                            opdId,
                            opd.kodeOpd(),
                            opd.namaOpd(),
                            jabatan.mulaiJabatan()));
                })
                .toList();

        // simpan jabatan pegawai
        jabatanPegawaiRepository.saveAll(listJabatanPegawais);
    }

    public List<User> findAllWithUserId() {
        return identityService.getUsers();
    }
}

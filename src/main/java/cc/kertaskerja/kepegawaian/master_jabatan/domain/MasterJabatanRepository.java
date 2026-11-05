package cc.kertaskerja.kepegawaian.master_jabatan.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface MasterJabatanRepository extends CrudRepository<MasterJabatan, Long>, MasterJabatanRepositoryCustom {
    List<MasterJabatan> findAll();

    List<MasterJabatan> findAllByOpdIdAndStatusJabatanOrderByNamaJabatan(Long opdId, MasterJabatanStatus jabatanStatus);

    boolean existsByOpdIdAndKodeJabatan(Long opdId, String kodeJabatan);

    Optional<MasterJabatan> findByKodeJabatan(String kodeJabatan);

    List<MasterJabatan> findByKodeJabatanIn(List<String> kodeJabatans);
}

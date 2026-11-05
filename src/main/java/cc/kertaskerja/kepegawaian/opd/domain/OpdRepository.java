package cc.kertaskerja.kepegawaian.opd.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface OpdRepository extends CrudRepository<Opd, Long>, OpdRepositoryCustom {
    List<Opd> findAllByKodeLembagaAndStatusOpdOrderByNamaOpdAsc(String kodeLembaga, OpdStatus statusOpd);

    Optional<Opd> findByKodeOpd(String kodeOpd);

    boolean existsByKodeOpd(String kodeOpd);
}

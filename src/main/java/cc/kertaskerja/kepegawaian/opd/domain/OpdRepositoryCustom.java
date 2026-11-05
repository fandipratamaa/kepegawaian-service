package cc.kertaskerja.kepegawaian.opd.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OpdRepositoryCustom {
    Page<Opd> findAll(Pageable pageable);
}

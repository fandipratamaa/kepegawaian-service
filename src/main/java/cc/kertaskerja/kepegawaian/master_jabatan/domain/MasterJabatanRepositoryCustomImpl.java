package cc.kertaskerja.kepegawaian.master_jabatan.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public class MasterJabatanRepositoryCustomImpl implements MasterJabatanRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public MasterJabatanRepositoryCustomImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<MasterJabatan> findAll(Pageable pageable) {

        String sql = """
                SELECT *
                FROM master_jabatan
                ORDER BY nama_jabatan ASC
                LIMIT ? OFFSET ?
                """;

        List<MasterJabatan> masterJabatan = jdbcTemplate.query(
                sql,
                masterJabatanPaginationRowMapper,
                pageable.getPageSize(),
                pageable.getOffset());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM master_jabatan",
                Long.class);

        return new PageImpl<>(
                masterJabatan,
                pageable,
                total != null ? total : 0);
    }

    private final RowMapper<MasterJabatan> masterJabatanPaginationRowMapper = (rs, rowNum) -> new MasterJabatan(
            rs.getLong("id"),
            rs.getLong("opd_id"),
            rs.getString("kode_jabatan"),
            rs.getString("nama_jabatan"),
            rs.getString("jenjang_jabatan") != null
                    ? MasterJabatanJenjang.valueOf(rs.getString("jenjang_jabatan"))
                    : null,
            rs.getString("status_jabatan") != null
                    ? MasterJabatanStatus.valueOf(rs.getString("status_jabatan"))
                    : null,
            rs.getTimestamp("created_date") != null
                    ? rs.getTimestamp("created_date").toInstant()
                    : null,
            rs.getTimestamp("last_modified_date") != null
                    ? rs.getTimestamp("last_modified_date").toInstant()
                    : null);
}

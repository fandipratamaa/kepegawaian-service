package cc.kertaskerja.kepegawaian.opd.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public class OpdRepositoryCustomImpl implements OpdRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    public OpdRepositoryCustomImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<Opd> findAll(Pageable pageable) {
        String sql = """
                SELECT *
                FROM opd
                ORDER BY nama_opd ASC
                LIMIT ? OFFSET ?
                """;

        List<Opd> opds = jdbcTemplate.query(
                sql,
                opdPaginationRowMapper,
                pageable.getPageSize(),
                pageable.getOffset());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM opd",
                Long.class);

        return new PageImpl<>(
                opds,
                pageable,
                total != null ? total : 0);
    }

    private final RowMapper<Opd> opdPaginationRowMapper = (rs, rowNum) -> new Opd(
            rs.getObject("id", Long.class),
            rs.getString("kode_lembaga"),
            rs.getString("kode_opd"),
            rs.getString("nama_opd"),
            rs.getString("singkatan_opd"),
            rs.getString("status_opd") != null
                    ? OpdStatus.valueOf(rs.getString("status_opd"))
                    : null,
            rs.getTimestamp("created_date") != null
                    ? rs.getTimestamp("created_date").toInstant()
                    : null,
            rs.getTimestamp("last_modified_date") != null
                    ? rs.getTimestamp("last_modified_date").toInstant()
                    : null);

}

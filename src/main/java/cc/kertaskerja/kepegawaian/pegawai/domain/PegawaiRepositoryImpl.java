package cc.kertaskerja.kepegawaian.pegawai.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

@Repository
public class PegawaiRepositoryImpl implements PegawaiRepositoryCustom {

    private final JdbcTemplate jdbcTemplate;

    public PegawaiRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String UPSERT_SQL = """
            INSERT INTO pegawai (
                nip,
                nama_pegawai,
                jenis_kelamin,
                tempat_lahir,
                tanggal_lahir,
                jenis_pegawai,
                status_pegawai,
                keterangan
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (nip) DO UPDATE SET
                nama_pegawai = EXCLUDED.nama_pegawai,
                jenis_kelamin = EXCLUDED.jenis_kelamin,
                tempat_lahir = EXCLUDED.tempat_lahir,
                tanggal_lahir = EXCLUDED.tanggal_lahir,
                jenis_pegawai = EXCLUDED.jenis_pegawai,
                status_pegawai = EXCLUDED.status_pegawai,
                keterangan = EXCLUDED.keterangan,
                last_modified_date = NOW()
            """;

    @Override
    public void upsertAll(List<Pegawai> pegawais) {
        if (pegawais.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                UPSERT_SQL,
                pegawais,
                pegawais.size(),
                (ps, pegawai) -> {
                    ps.setString(1, pegawai.nip());
                    ps.setString(2, pegawai.namaPegawai());
                    ps.setString(3, pegawai.jenisKelamin());
                    ps.setString(4, pegawai.tempatLahir());
                    ps.setObject(5, pegawai.tanggalLahir());
                    ps.setString(6, pegawai.jenisPegawai());
                    ps.setString(7, pegawai.statusPegawai().name());
                    ps.setString(8, pegawai.keterangan());
                });
    }

    public List<Pegawai> findDistinctByJabatanPegawaiOpdId(Long opdId) {
        String script = """
                SELECT DISTINCT p.*
                FROM pegawai p
                JOIN jabatan_pegawai jp
                  ON jp.pegawai_id = p.id
                WHERE jp.opd_id = ?
                """;

        return jdbcTemplate.query(script, pegawaiRowMapper, opdId);
    }

    public List<Pegawai> findDistinctByJabatanPegawaiOpdIdAndRolePegawaiRoleNama(Long opdId, String roleNama) {
        String script = """
                SELECT DISTINCT p.*
                FROM pegawai p
                JOIN jabatan_pegawai jp
                  ON jp.pegawai_id = p.id
                JOIN role_pegawai rp
                  ON rp.pegawai_id = p.id
                JOIN role r
                  ON r.id = rp.role_id
                WHERE jp.opd_id = ?
                AND r.nama_role = ?
                """;

        return jdbcTemplate.query(script, pegawaiRowMapper, opdId, roleNama);
    }

    @Override
    public Page<Pegawai> findAll(Pageable pageable) {

        String sql = """
                SELECT *
                FROM pegawai
                ORDER BY nama_pegawai ASC
                LIMIT ? OFFSET ?
                """;

        List<Pegawai> pegawai = jdbcTemplate.query(
                sql,
                pegawaiPaginationRowMapper,
                pageable.getPageSize(),
                pageable.getOffset());

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pegawai",
                Long.class);

        return new PageImpl<>(
                pegawai,
                pageable,
                total != null ? total : 0);
    }

    private final RowMapper<Pegawai> pegawaiRowMapper = (rs, rowNum) -> new Pegawai(
            rs.getLong("id"),
            rs.getString("nip"),
            rs.getString("nama_pegawai"),
            rs.getString("jenis_kelamin"),
            rs.getString("tempat_lahir"),
            rs.getObject("tanggal_lahir", LocalDate.class),
            rs.getString("jenis_pegawai"),
            rs.getString("status_pegawai") != null
                    ? PegawaiStatus.valueOf(rs.getString("status_pegawai"))
                    : null,
            rs.getString("keterangan"),
            rs.getString("keycloak_user_id"),
            rs.getTimestamp("created_date") != null
                    ? rs.getTimestamp("created_date").toInstant()
                    : null,
            rs.getTimestamp("last_modified_date") != null
                    ? rs.getTimestamp("last_modified_date").toInstant()
                    : null);

    private final RowMapper<Pegawai> pegawaiPaginationRowMapper = (rs, rowNum) -> new Pegawai(
            rs.getLong("id"),
            rs.getString("nip"),
            rs.getString("nama_pegawai"),
            rs.getString("jenis_kelamin"),
            rs.getString("tempat_lahir"),
            rs.getObject("tanggal_lahir", LocalDate.class),
            rs.getString("jenis_pegawai"),
            rs.getString("status_pegawai") != null
                    ? PegawaiStatus.valueOf(rs.getString("status_pegawai"))
                    : null,
            rs.getString("keterangan"),
            rs.getString("keycloak_user_id"),
            rs.getTimestamp("created_date") != null
                    ? rs.getTimestamp("created_date").toInstant()
                    : null,
            rs.getTimestamp("last_modified_date") != null
                    ? rs.getTimestamp("last_modified_date").toInstant()
                    : null);
}

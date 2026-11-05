package cc.kertaskerja.kepegawaian.pegawai.web;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cc.kertaskerja.kepegawaian.common.web.PageResponse;
import cc.kertaskerja.kepegawaian.common.web.WebResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.web.SimpegSyncRequest;
import cc.kertaskerja.kepegawaian.pegawai.domain.MigrationSummary;
import cc.kertaskerja.kepegawaian.pegawai.domain.PegawaiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/pegawai")
@Tag(name = "Pegawai", description = "Manajemen data pegawai")
public class PegawaiController {

    private final PegawaiService pegawaiService;

    public PegawaiController(PegawaiService pegawaiService) {
        this.pegawaiService = pegawaiService;
    }

    @GetMapping
    @Operation(summary = "Daftar Pegawai By Filter", description = "Mengambil seluruh data pegawai by opd id atau nama role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daftar Pegawai berhasil diambil"),
            @ApiResponse(responseCode = "400", description = "Bad Request, filter tidak valid"),
    })
    public WebResponse<List<PegawaiResponse>> findAll(
            @Parameter(description = "Opd Id", example = "1", required = true) @RequestParam Long opdId,
            @Parameter(description = "Role", example = "level_1") @RequestParam(required = false) String role) {
        List<PegawaiResponse> responses = pegawaiService.findAll(opdId, role)
                .stream()
                .map(PegawaiResponse::from)
                .toList();

        return WebResponse.success(responses);
    }

    @GetMapping("/all")
    @Operation(summary = "All Pegawai", description = "Mengambil seluruh data pegawai")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daftar Pegawai berhasil diambil"),
    })
    public WebResponse<PageResponse<PegawaiResponse>> allPegawais(
            @ParameterObject @PageableDefault(size = 20, sort = "namaPegawai", direction = Sort.Direction.ASC) Pageable pageable

    ) {
        Page<PegawaiResponse> responses = pegawaiService.allPegawai(pageable)
                .map(PegawaiResponse::from);

        return WebResponse.success(PageResponse.from(responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find pegawai by ID", description = "Mengembalikan data pegawai berdasarkan ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pegawai berhasil ditemukan"),
            @ApiResponse(responseCode = "400", description = "ID pegawai tidak valid"),
            @ApiResponse(responseCode = "404", description = "Pegawai tidak ditemukan")
    })
    public WebResponse<PegawaiResponse> findById(
            @Parameter(description = "ID unik pegawai", example = "123", required = true) @PathVariable Long id) {
        return WebResponse.success(
                PegawaiResponse.from(pegawaiService.findPegawaiById(id)));
    }

    @GetMapping("/histori/{pegawaiId}")
    @Operation(summary = "Histori lengkap pegawai", description = "Menampilkan data histori pegawai")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data Histori pegawai berhasil diambil"),
            @ApiResponse(responseCode = "404", description = "Pegawai tidak ditemukan"),
    })
    public WebResponse<PegawaiHistoriResponse> findHistoriPegawai(
            @Parameter(description = "Pegawai id (nip)", example = "12345") @PathVariable Long pegawaiId,

            @Valid @ParameterObject PegawaiHistoriQuery query) {
        PegawaiHistoriResponse response = PegawaiHistoriResponse.from(
                pegawaiService.findHistoriPegawai(pegawaiId, query.jenisHistori(), query.bulan(), query.tahun()));

        return WebResponse.success(response);
    }

    @PostMapping
    @Operation(summary = "Tambah data pegawai", description = "Menambahkan data pegawai baru")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pegawai berhasil ditambahkan"),
            @ApiResponse(responseCode = "400", description = "Data tidak valid"),
            @ApiResponse(responseCode = "409", description = "Pegawai sudah ada")
    })
    public WebResponse<PegawaiResponse> create(
            @Valid @RequestBody PegawaiCreateRequest request) {
        PegawaiResponse response = PegawaiResponse.from(pegawaiService.create(
                request.toCommand(), request.initialPassword()));

        return WebResponse.created(
                "Pegawai berhasil ditambahkan",
                response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update data pegawai", description = "Update nama atau id pegawai")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data pegawai berhasil diperbarui"),
            @ApiResponse(responseCode = "400", description = "Data tidak valid"),
            @ApiResponse(responseCode = "404", description = "Data pegawai tidak ditemukan")
    })
    public WebResponse<PegawaiResponse> update(
            @Valid @RequestBody PegawaiUpdateRequest request,
            @PathVariable Long id) {
        PegawaiResponse response = PegawaiResponse.from(
                pegawaiService.update(id, request.toCommand()));

        return WebResponse.success(
                "Data pegawai berhasil diperbarui",
                response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus data pegawai", description = "hapus data pegawai by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data pegawai berhasil diperbarui"),
            @ApiResponse(responseCode = "404", description = "Data pegawai tidak ditemukan")
    })
    public WebResponse<Void> delete(
            @PathVariable Long id) {
        String namaPegawai = pegawaiService.delete(id);

        return WebResponse.deleted(
                "Pegawai " + namaPegawai + " berhasil dihapus");
    }

    @PostMapping("/migrate")
    @Operation(summary = "Migrasi data pegawai", description = "Migrasi existing pegawai ke identity service")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Migrasi Pegawai berhasil"),
            @ApiResponse(responseCode = "400", description = "Data tidak valid"),
            @ApiResponse(responseCode = "409", description = "Pegawai sudah ada")
    })
    public WebResponse<MigrationSummary> migrate(
            @Parameter(description = "Opd Id", example = "1", required = true) @RequestParam Long opdId) {
        MigrationSummary summary = pegawaiService.migratePegawaiToKeycloak(opdId);

        return WebResponse.ok("Migrasi Pegawai berhasil", summary);
    }

    @GetMapping("/findByNip")
    @Operation(summary = "Find pegawai by NIP", description = "Mengembalikan data pegawai berdasarkan NIP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pegawai berhasil ditemukan"),
            @ApiResponse(responseCode = "400", description = "Parameter NIP tidak valid"),
            @ApiResponse(responseCode = "404", description = "Pegawai tidak ditemukan")
    })
    public WebResponse<PegawaiDetailResponse> findByNip(
            @Parameter(description = "Nomor Induk Pegawai (NIP)", example = "198701012010011001", required = true) @RequestParam String nip) {
        return WebResponse.success(
                PegawaiDetailResponse.from(pegawaiService.findPegawaiByPegawaiId(nip)));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync Data Pegawai By Kode OPD", description = "Synchronize Pegawai data for a specific OPD from the upstream SIMPEG service.")
    public WebResponse<String> syncJabatanSimpeg(
            @Valid @RequestBody SimpegSyncRequest request) {
        pegawaiService.syncPegawaiDariSimpeg(request.opdId());
        return WebResponse.success("Sync Master Pegawai Simpeg");
    }

    @GetMapping("/users")
    @Operation(summary = "Daftar User / Pegawai", description = "Mengambil seluruh data user dan role")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daftar User berhasil diambil"),
    })
    public WebResponse<List<UserResponse>> findAllUser() {
        List<UserResponse> responses = pegawaiService.findAllWithUserId()
                .stream()
                .map(UserResponse::from)
                .toList();

        return WebResponse.success(responses);
    }
}

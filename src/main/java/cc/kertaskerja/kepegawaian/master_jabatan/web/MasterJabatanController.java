package cc.kertaskerja.kepegawaian.master_jabatan.web;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cc.kertaskerja.kepegawaian.common.web.EnumLabelResponse;
import cc.kertaskerja.kepegawaian.common.web.OptionResponse;
import cc.kertaskerja.kepegawaian.common.web.PageResponse;
import cc.kertaskerja.kepegawaian.common.web.WebResponse;
import cc.kertaskerja.kepegawaian.integration.simpeg.web.SimpegSyncRequest;
import cc.kertaskerja.kepegawaian.master_jabatan.domain.MasterJabatanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/master-jabatan")
@Tag(name = "Master Jabatan", description = "Manajemen master data jabatan pemerintah daerah")
public class MasterJabatanController {

    private final MasterJabatanService masterJabatanService;

    public MasterJabatanController(MasterJabatanService masterJabatanService) {
        this.masterJabatanService = masterJabatanService;
    }

    @GetMapping
    @Operation(summary = "Daftar seluruh jabatan by filter", description = "Mengambil seluruh data master jabatan by opd id")
    public WebResponse<List<MasterJabatanResponse>> findAll(
            @Parameter(description = "Opd Id", example = "1", required = true) @RequestParam(name = "opdId") Long opdId) {
        List<MasterJabatanResponse> responses = masterJabatanService.findAllByOpdId(opdId)
                .stream()
                .map(MasterJabatanResponse::from)
                .toList();

        return WebResponse.success(responses);
    }

    @GetMapping("/all")
    @Operation(summary = "Daftar seluruh jabatan", description = "Mengambil seluruh data master jabatan")
    public WebResponse<PageResponse<MasterJabatanResponse>> allMasterJabatans(
            @ParameterObject @PageableDefault(size = 20, sort = "namaJabatan", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<MasterJabatanResponse> responses = masterJabatanService.allJabatan(pageable)
                .map(MasterJabatanResponse::from);

        return WebResponse.success(PageResponse.from(responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detail jabatan", description = "Mengambil detail master jabatan berdasarkan ID")
    @ApiResponse(responseCode = "200", description = "Data ditemukan")
    @ApiResponse(responseCode = "404", description = "Data tidak ditemukan", content = @Content)
    public WebResponse<MasterJabatanResponse> findById(
            @Parameter(description = "ID master jabatan", example = "1") @PathVariable Long id) {
        MasterJabatanResponse response = MasterJabatanResponse.from(
                masterJabatanService.findMasterJabatanById(id));

        return WebResponse.success(response);
    }

    @GetMapping("/options/jenjang")
    @Operation(summary = "Daftar jenjang jabatan", description = "Mengambil daftar jenjang jabatan untuk kebutuhan pilihan")
    public WebResponse<List<EnumLabelResponse>> listJenjang() {
        List<EnumLabelResponse> responses = masterJabatanService.listJenjangJabatan()
                .stream()
                .map(EnumLabelResponse::of)
                .toList();

        return WebResponse.success(responses);
    }

    @GetMapping("/options/kategori")
    @Operation(summary = "Daftar kategori jabatan", description = "Mengambil daftar kategori jabatan untuk kebutuhan pilihan")
    public WebResponse<List<EnumLabelResponse>> listKategori() {
        List<EnumLabelResponse> responses = masterJabatanService.listKategoriJabatan()
                .stream()
                .map(EnumLabelResponse::of)
                .toList();

        return WebResponse.success(responses);
    }

    @GetMapping("/options/status")
    @Operation(summary = "Daftar status jabatan", description = "Mengambil daftar status jabatan untuk kebutuhan pilihan")
    public WebResponse<List<EnumLabelResponse>> listStatus() {
        List<EnumLabelResponse> responses = masterJabatanService.listStatusJabatan()
                .stream()
                .map(EnumLabelResponse::of)
                .toList();

        return WebResponse.success(responses);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tambah jabatan", description = "Menambahkan data master jabatan baru")
    @ApiResponse(responseCode = "201", description = "Data berhasil dibuat")
    @ApiResponse(responseCode = "400", description = "Validasi gagal", content = @Content)
    public WebResponse<MasterJabatanResponse> create(
            @Valid @RequestBody MasterJabatanCreateRequest request) {
        MasterJabatanResponse response = MasterJabatanResponse.from(
                masterJabatanService.create(request.toCommand()));

        return WebResponse.created(
                "Jabatan berhasil ditambahkan",
                response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Perbarui jabatan", description = "Memperbarui data master jabatan")
    @ApiResponse(responseCode = "200", description = "Data berhasil diperbarui")
    @ApiResponse(responseCode = "404", description = "Data tidak ditemukan", content = @Content)
    public WebResponse<MasterJabatanResponse> update(
            @Parameter(description = "ID master jabatan", example = "1") @PathVariable Long id,

            @Valid @RequestBody MasterJabatanUpdateRequest request) {
        MasterJabatanResponse response = MasterJabatanResponse.from(
                masterJabatanService.update(id, request.toCommand()));

        return WebResponse.success(
                "Jabatan berhasil diperbarui",
                response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus jabatan", description = "Menghapus data master jabatan")
    @ApiResponse(responseCode = "200", description = "Data berhasil dihapus")
    @ApiResponse(responseCode = "404", description = "Data tidak ditemukan", content = @Content)
    public WebResponse<Void> delete(
            @Parameter(description = "ID master jabatan", example = "1") @PathVariable Long id) {
        String namaJabatan = masterJabatanService.delete(id);

        return WebResponse.deleted(
                "Jabatan " + namaJabatan + " berhasil dihapus");
    }

    @GetMapping("/options")
    @Operation(summary = "Daftar Master Jabatan untuk dropdown", description = "Simplifikasi Data Master Jabatan untuk dropdown", security = @SecurityRequirement(name = "sessionId"))
    public WebResponse<List<OptionResponse>> dropdownAll(
            @RequestParam(required = false) Long opdId) {
        List<OptionResponse> responses = masterJabatanService.findAllByOpdId(opdId)
                .stream()
                .map(OptionResponse::of)
                .toList();

        return new WebResponse<>(
                200,
                "success",
                "Dropdown master jabatan",
                responses);
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync Data Master Jabatan By Kode OPD", description = "Synchronize master Jabatan data for a specific OPD from the upstream SIMPEG service.")
    public WebResponse<String> syncJabatanSimpeg(
            @Valid @RequestBody SimpegSyncRequest request) {
        masterJabatanService.syncJabatanDariSimpeg(request.opdId());
        return WebResponse.success("Sync Master Jabatan Simpeg");
    }

}

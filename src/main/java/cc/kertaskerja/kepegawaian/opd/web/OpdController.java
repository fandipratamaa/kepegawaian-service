package cc.kertaskerja.kepegawaian.opd.web;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cc.kertaskerja.kepegawaian.common.web.OptionResponse;
import cc.kertaskerja.kepegawaian.common.web.PageResponse;
import cc.kertaskerja.kepegawaian.common.web.WebResponse;
import cc.kertaskerja.kepegawaian.opd.domain.Opd;
import cc.kertaskerja.kepegawaian.opd.domain.OpdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/opd")
@Tag(name = "OPD", description = "Manajemen Organisasi Perangkat Daerah")
public class OpdController {

    private final OpdService opdService;

    public OpdController(OpdService opdService) {
        this.opdService = opdService;
    }

    @GetMapping("/all")
    @Operation(summary = "Daftar OPD aktif", description = "Mengambil seluruh OPD aktif berdasarkan kode lembaga yang dikonfigurasi.", security = @SecurityRequirement(name = "sessionId"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daftar OPD berhasil diambil", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OpdResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token tidak valid"),
    })
    public WebResponse<PageResponse<OpdResponse>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "namaOpd", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<OpdResponse> responses = opdService.allOpd(pageable)
                .map(OpdResponse::from);

        return WebResponse.success(PageResponse.from(responses));
    }

    @GetMapping("/options")
    @Operation(summary = "Daftar OPD untuk dropdown", description = "Simplifikasi Data OPD untuk dropdown", security = @SecurityRequirement(name = "sessionId"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daftar OPD berhasil diambil", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OptionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token tidak valid"),
    })
    public WebResponse<List<OptionResponse>> dropdownAll() {
        List<OptionResponse> responses = opdService.findAllOpdAktifInLembaga()
                .stream()
                .map(OptionResponse::of)
                .toList();

        return new WebResponse<>(
                200,
                "success",
                "Dropdown opd",
                responses);
    }

    @GetMapping("/show/{id}")
    @Operation(summary = "Detail OPD", description = "Mengambil detail OPD berdasarkan id OPD.", security = @SecurityRequirement(name = "sessionId"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detail OPD berhasil ditemukan", content = @Content(schema = @Schema(implementation = Opd.class))),
            @ApiResponse(responseCode = "401", description = "Token tidak valid"),
            @ApiResponse(responseCode = "404", description = "OPD tidak ditemukan")
    })
    public WebResponse<OpdResponse> show(
            @Parameter(description = "id OPD", example = "123") @PathVariable Long id) {
        return WebResponse.success(OpdResponse.from(opdService.findOpdById(id)));
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Membuat OPD", description = "Membuat data OPD baru.", security = @SecurityRequirement(name = "sessionId"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OPD berhasil dibuat"),
            @ApiResponse(responseCode = "400", description = "Data tidak valid"),
            @ApiResponse(responseCode = "401", description = "Token tidak valid"),
            @ApiResponse(responseCode = "409", description = "Kode OPD sudah terdaftar")
    })
    public WebResponse<Opd> create(
            @Valid @RequestBody OpdCreateRequest request) {
        Opd savedOpd = opdService.save(request.toCommand());
        return WebResponse.success(savedOpd);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "Memperbarui OPD", description = "Memperbarui data OPD berdasarkan id OPD.", security = @SecurityRequirement(name = "sessionId"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OPD berhasil diperbarui"),
            @ApiResponse(responseCode = "400", description = "Data tidak valid"),
            @ApiResponse(responseCode = "401", description = "Token tidak valid"),
    })
    public WebResponse<Opd> update(
            @PathVariable Long id,
            @Valid @RequestBody OpdUpdateRequest request) {
        Opd updatedOpd = opdService.update(id, request.toCommand());
        return WebResponse.success("Update data opd berhasil", updatedOpd);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Menghapus OPD", description = "Menghapus data OPD berdasarkan id OPD.", security = @SecurityRequirement(name = "sessionId"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OPD berhasil dihapus"),
            @ApiResponse(responseCode = "401", description = "Token tidak valid"),
            @ApiResponse(responseCode = "404", description = "OPD tidak ditemukan")
    })
    public WebResponse<Void> delete(
            @PathVariable Long id) {
        String namaOpd = opdService.findOpdById(id).namaOpd();
        opdService.delete(id);
        return WebResponse.deleted("OPD " + namaOpd + " berhasil dihapus");
    }

    @PostMapping("/sync")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Sync OPD Simpeg", description = "Update OPD Data From Upstream")
    public WebResponse<String> syncSimpeg() {
        opdService.syncSimpeg();
        return WebResponse.success("Sync OPD Simpeg");
    }
}

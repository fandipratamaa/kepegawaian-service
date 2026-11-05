package cc.kertaskerja.kepegawaian.role_pegawai.web;

import cc.kertaskerja.kepegawaian.common.web.WebResponse;
import cc.kertaskerja.kepegawaian.role_pegawai.domain.RoleAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/role-pegawai")
public class RolePegawaiController {
    private final RoleAssignmentService roleAssignmentService;

    public RolePegawaiController(RoleAssignmentService roleAssignmentService) {
        this.roleAssignmentService = roleAssignmentService;
    }

    @PostMapping("/assign")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Assign role to pegawai",
            description = "Assign new role to pegawai"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Role Assigned"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "request body tidak invalid"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Pegawai / role / tidak ditemukan"
            )
    })
    public WebResponse<AssignRolePegawaiResponse> assignRole(
            @Valid
            @RequestBody
            AssignRolePegawaiRequest request
    ) {
        AssignRolePegawaiResponse response = AssignRolePegawaiResponse.from(
                roleAssignmentService.assign(request.toCommand())
        );

        return WebResponse.created("Role Assigned", response);
    }

}

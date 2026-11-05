package cc.kertaskerja.kepegawaian.opd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import cc.kertaskerja.kepegawaian.opd.domain.Opd;
import cc.kertaskerja.kepegawaian.opd.domain.OpdService;
import cc.kertaskerja.kepegawaian.opd.web.OpdController;

@WebMvcTest(OpdController.class)
class OpdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpdService opdService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldReturnOpdList() throws Exception {

        Opd opd = Opd.of(
                "63.11",
                "5.01.5.05.0.00.01.0000",
                "BKPSDM",
                "BKPSDM");

        Pageable pageable = PageRequest.of(0, 20, Sort.by("namaOpd").ascending());

        when(opdService.allOpd(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(opd), pageable, 1));

        mockMvc.perform(get("/opd/all")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].kode_opd")
                        .value("5.01.5.05.0.00.01.0000"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.total_pages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }
}

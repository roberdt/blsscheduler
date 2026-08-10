package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.repository.CompanyRepository;
import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import info.safetynetaccess.blsscheduler.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(CompanyController.class)
public class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyRepository companyRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
    public void testGetAllCompanies() throws Exception {
        Company company1 = new Company("Acme Corp", "Purpose 1");
        company1.setId("id-123");
        Company company2 = new Company("Stark Industries", "Purpose 2");
        company2.setId("id-456");

        Mockito.when(companyRepository.findAll()).thenReturn(Flux.just(company1, company2));

        mockMvc.perform(get("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-123"))
                .andExpect(jsonPath("$[0].name").value("Acme Corp"))
                .andExpect(jsonPath("$[0].purpose").value("Purpose 1"))
                .andExpect(jsonPath("$[0].approval").value("No"))
                .andExpect(jsonPath("$[1].id").value("id-456"))
                .andExpect(jsonPath("$[1].name").value("Stark Industries"))
                .andExpect(jsonPath("$[1].purpose").value("Purpose 2"))
                .andExpect(jsonPath("$[1].approval").value("No"));
    }
}

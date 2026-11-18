package info.safetynetaccess.blsscheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import info.safetynetaccess.blsscheduler.dto.RegistrationResponse;
import info.safetynetaccess.blsscheduler.util.JwtUtil;
import info.safetynetaccess.blsscheduler.service.RegistrationService;
import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(RegistrationController.class)
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
    public void testRegister_Success() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Acme Corp",
                "To manage basic life support training slots",
                "John Doe",
                "john.doe@example.com",
                "123-456-7890",
                "password123",
                "password123"
        );

        RegistrationResponse response = new RegistrationResponse(
                true,
                "Company and administrator user registered successfully",
                "company-id-123",
                "user-id-456"
        );

        Mockito.when(registrationService.register(any(RegistrationRequest.class)))
                .thenReturn(Mono.just(response));

        MvcResult mvcResult = mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Company and administrator user registered successfully"))
                .andExpect(jsonPath("$.companyId").value("company-id-123"))
                .andExpect(jsonPath("$.userId").value("user-id-456"));
    }

    @Test
    @WithMockUser
    public void testRegister_PasswordsDoNotMatch() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Acme Corp",
                "To manage basic life support training slots",
                "John Doe",
                "john.doe@example.com",
                "password123",
                "differentPassword"
        );

        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    public void testRegister_InvalidEmail() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "Acme Corp",
                "To manage basic life support training slots",
                "John Doe",
                "invalid-email",
                "password123",
                "password123"
        );

        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    public void testRegister_BlankFields() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "",
                "",
                "",
                "john.doe@example.com",
                "password123",
                "password123"
        );

        mockMvc.perform(post("/api/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}

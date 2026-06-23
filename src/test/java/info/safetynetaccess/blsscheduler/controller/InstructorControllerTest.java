package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Instructor;
import info.safetynetaccess.blsscheduler.repository.InstructorRepository;
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
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(InstructorController.class)
public class InstructorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstructorRepository instructorRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
    public void testGetAllInstructors() throws Exception {
        Instructor instructor1 = new Instructor("CERT-111", "Jane", "Miller", "RN", true);
        instructor1.setId("id-111");
        Instructor instructor2 = new Instructor("CERT-222", "David", "Chen", "MD", false);
        instructor2.setId("id-222");

        Mockito.when(instructorRepository.findAll()).thenReturn(Flux.just(instructor1, instructor2));

        MvcResult mvcResult = mockMvc.perform(get("/api/instructors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-111"))
                .andExpect(jsonPath("$[0].certificationid").value("CERT-111"))
                .andExpect(jsonPath("$[0].firstname").value("Jane"))
                .andExpect(jsonPath("$[0].lastname").value("Miller"))
                .andExpect(jsonPath("$[0].postnominalcredential").value("RN"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value("id-222"))
                .andExpect(jsonPath("$[1].certificationid").value("CERT-222"))
                .andExpect(jsonPath("$[1].firstname").value("David"))
                .andExpect(jsonPath("$[1].lastname").value("Chen"))
                .andExpect(jsonPath("$[1].postnominalcredential").value("MD"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    @WithMockUser
    public void testAddInstructor() throws Exception {
        Instructor instructor = new Instructor("CERT-333", "Sarah", "O'Connor", "Paramedic", true);
        instructor.setId("id-333");

        Mockito.when(instructorRepository.save(Mockito.any(Instructor.class))).thenReturn(Mono.just(instructor));

        String requestBody = "{\"certificationid\":\"CERT-333\",\"firstname\":\"Sarah\",\"lastname\":\"O'Connor\",\"postnominalcredential\":\"Paramedic\",\"active\":true}";

        MvcResult mvcResult = mockMvc.perform(post("/api/instructors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-333"))
                .andExpect(jsonPath("$.certificationid").value("CERT-333"))
                .andExpect(jsonPath("$.firstname").value("Sarah"))
                .andExpect(jsonPath("$.lastname").value("O'Connor"))
                .andExpect(jsonPath("$.postnominalcredential").value("Paramedic"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    public void testPopulateInstructors() throws Exception {
        Instructor instructor1 = new Instructor("CERT-58301", "Jane", "Miller", "NRP, EMS-I", true);
        instructor1.setId("seeded-1");

        Mockito.when(instructorRepository.save(Mockito.any(Instructor.class))).thenReturn(Mono.just(instructor1));

        MvcResult mvcResult = mockMvc.perform(post("/api/instructors/populate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].certificationid").value("CERT-58301"))
                .andExpect(jsonPath("$[0].firstname").value("Jane"))
                .andExpect(jsonPath("$[0].lastname").value("Miller"))
                .andExpect(jsonPath("$[0].postnominalcredential").value("NRP, EMS-I"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser
    public void testUpdateInstructor() throws Exception {
        Instructor instructor = new Instructor("CERT-333", "Sarah", "O'Connor", "Paramedic", false);
        instructor.setId("id-333");

        Mockito.when(instructorRepository.save(Mockito.any(Instructor.class))).thenReturn(Mono.just(instructor));

        String requestBody = "{\"certificationid\":\"CERT-333\",\"firstname\":\"Sarah\",\"lastname\":\"O'Connor\",\"postnominalcredential\":\"Paramedic\",\"active\":false}";

        MvcResult mvcResult = mockMvc.perform(put("/api/instructors/id-333")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-333"))
                .andExpect(jsonPath("$.certificationid").value("CERT-333"))
                .andExpect(jsonPath("$.firstname").value("Sarah"))
                .andExpect(jsonPath("$.lastname").value("O'Connor"))
                .andExpect(jsonPath("$.postnominalcredential").value("Paramedic"))
                .andExpect(jsonPath("$.active").value(false));
    }
}

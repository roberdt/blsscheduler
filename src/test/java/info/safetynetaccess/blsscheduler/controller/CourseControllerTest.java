package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Course;
import info.safetynetaccess.blsscheduler.repository.CourseRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(CourseController.class)
public class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseRepository courseRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
    public void testGetAllCourses() throws Exception {
        Course course1 = new Course("BLS Provider Course", "BLS Provider", true);
        course1.setId("id-111");
        Course course2 = new Course("Heartsaver CPR", "CPR", false);
        course2.setId("id-222");

        Mockito.when(courseRepository.findAll()).thenReturn(Flux.just(course1, course2));

        MvcResult mvcResult = mockMvc.perform(get("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("id-111"))
                .andExpect(jsonPath("$[0].coursetitle").value("BLS Provider Course"))
                .andExpect(jsonPath("$[0].coursetype").value("BLS Provider"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].id").value("id-222"))
                .andExpect(jsonPath("$[1].coursetitle").value("Heartsaver CPR"))
                .andExpect(jsonPath("$[1].coursetype").value("CPR"))
                .andExpect(jsonPath("$[1].active").value(false));
    }

    @Test
    @WithMockUser
    public void testAddCourse() throws Exception {
        Course course = new Course("First Aid CPR AED Course", "First Aid", true);
        course.setId("id-333");

        Mockito.when(courseRepository.save(Mockito.any(Course.class))).thenReturn(Mono.just(course));

        String requestBody = "{\"coursetitle\":\"First Aid CPR AED Course\",\"coursetype\":\"First Aid\",\"active\":true}";

        MvcResult mvcResult = mockMvc.perform(post("/api/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("id-333"))
                .andExpect(jsonPath("$.coursetitle").value("First Aid CPR AED Course"))
                .andExpect(jsonPath("$.coursetype").value("First Aid"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    public void testUpdateCourse() throws Exception {
        Course course = new Course("Updated Course Title", "Updated Type", false);
        course.setId("id-333");

        Mockito.when(courseRepository.save(Mockito.any(Course.class))).thenReturn(Mono.just(course));

        String requestBody = "{\"coursetitle\":\"Updated Course Title\",\"coursetype\":\"Updated Type\",\"active\":false}";

        MvcResult mvcResult = mockMvc.perform(put("/api/courses/id-333")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id-333"))
                .andExpect(jsonPath("$.coursetitle").value("Updated Course Title"))
                .andExpect(jsonPath("$.coursetype").value("Updated Type"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser
    public void testPopulateCourses() throws Exception {
        Course course1 = new Course("BLS Provider Course", "BLS Provider", true);
        course1.setId("seeded-1");

        Mockito.when(courseRepository.save(Mockito.any(Course.class))).thenReturn(Mono.just(course1));

        MvcResult mvcResult = mockMvc.perform(post("/api/courses/populate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].coursetitle").value("BLS Provider Course"))
                .andExpect(jsonPath("$[0].coursetype").value("BLS Provider"))
                .andExpect(jsonPath("$[0].active").value(true));
    }
}

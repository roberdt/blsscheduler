package info.safetynetaccess.blsscheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import info.safetynetaccess.blsscheduler.model.ClassSlot;
import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.model.Date;
import info.safetynetaccess.blsscheduler.model.Time;
import info.safetynetaccess.blsscheduler.util.JwtUtil;
import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassSlotController.class)
public class ClassSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Firestore firestore;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    // Mock Firestore elements
    private CollectionReference companiesCollection;
    private DocumentReference companyDocRef;
    private CollectionReference datesCollection;
    private DocumentReference dateDocRef;
    private CollectionReference timesCollection;
    private DocumentReference timeDocRef;
    private CollectionReference slotsCollection;
    private DocumentReference slotDocRef;

    @BeforeEach
    public void setUp() {
        companiesCollection = Mockito.mock(CollectionReference.class);
        companyDocRef = Mockito.mock(DocumentReference.class);
        datesCollection = Mockito.mock(CollectionReference.class);
        dateDocRef = Mockito.mock(DocumentReference.class);
        timesCollection = Mockito.mock(CollectionReference.class);
        timeDocRef = Mockito.mock(DocumentReference.class);
        slotsCollection = Mockito.mock(CollectionReference.class);
        slotDocRef = Mockito.mock(DocumentReference.class);

        // Chain mocks
        Mockito.when(firestore.collection("companies")).thenReturn(companiesCollection);
        Mockito.when(companiesCollection.document(anyString())).thenReturn(companyDocRef);
        Mockito.when(companyDocRef.collection("dates")).thenReturn(datesCollection);
        Mockito.when(datesCollection.document(anyString())).thenReturn(dateDocRef);
        Mockito.when(dateDocRef.collection("times")).thenReturn(timesCollection);
        Mockito.when(timesCollection.document(anyString())).thenReturn(timeDocRef);
        Mockito.when(timeDocRef.collection("classslots")).thenReturn(slotsCollection);
        Mockito.when(slotsCollection.document(anyString())).thenReturn(slotDocRef);
    }

    @Test
    @WithMockUser(username = "user")
    public void testGetDates_Success() throws Exception {
        QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
        QueryDocumentSnapshot docSnapshot = Mockito.mock(QueryDocumentSnapshot.class);

        Date mockDate = new Date("2026-06-15");
        mockDate.setId("date-id-123");

        Mockito.when(docSnapshot.getId()).thenReturn("date-id-123");
        Mockito.when(docSnapshot.toObject(Date.class)).thenReturn(mockDate);
        Mockito.when(querySnapshot.getDocuments()).thenReturn(Collections.singletonList(docSnapshot));
        
        // Use real, completed ApiFuture instead of brittle mock
        Mockito.when(datesCollection.get()).thenReturn(ApiFutures.immediateFuture(querySnapshot));

        MvcResult mvcResult = mockMvc.perform(get("/api/companies/acme-corp/dates"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("date-id-123"))
                .andExpect(jsonPath("$[0].dateValue").value("2026-06-15"));
    }

    @Test
    @WithMockUser(username = "user")
    public void testGetTimes_Success() throws Exception {
        QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
        QueryDocumentSnapshot docSnapshot = Mockito.mock(QueryDocumentSnapshot.class);

        Time mockTime = new Time();
        mockTime.setId("time-id-456");
        mockTime.setStarttime("1:00 pm");
        mockTime.setEndtime("1:45 pm");
        mockTime.setNumberslots(8);

        Mockito.when(docSnapshot.getId()).thenReturn("time-id-456");
        Mockito.when(docSnapshot.toObject(Time.class)).thenReturn(mockTime);
        Mockito.when(querySnapshot.getDocuments()).thenReturn(Collections.singletonList(docSnapshot));

        // Use real, completed ApiFuture
        Mockito.when(timesCollection.get()).thenReturn(ApiFutures.immediateFuture(querySnapshot));

        MvcResult mvcResult = mockMvc.perform(get("/api/companies/acme-corp/dates/date-id-123/times"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("time-id-456"))
                .andExpect(jsonPath("$[0].starttime").value("1:00 pm"))
                .andExpect(jsonPath("$[0].endtime").value("1:45 pm"))
                .andExpect(jsonPath("$[0].numberslots").value(8));
    }

    @Test
    @WithMockUser(username = "user")
    public void testGetTime_Success() throws Exception {
        DocumentSnapshot docSnapshot = Mockito.mock(DocumentSnapshot.class);

        Time mockTime = new Time();
        mockTime.setId("time-id-456");
        mockTime.setStarttime("1:00 pm");
        mockTime.setEndtime("1:45 pm");
        mockTime.setNumberslots(8);

        DocumentSnapshot dateDocSnapshot = Mockito.mock(DocumentSnapshot.class);
        Date mockDate = new Date("2026-06-15");
        Mockito.when(dateDocSnapshot.exists()).thenReturn(true);
        Mockito.when(dateDocSnapshot.getId()).thenReturn("date-id-123");
        Mockito.when(dateDocSnapshot.toObject(Date.class)).thenReturn(mockDate);

        Mockito.when(docSnapshot.exists()).thenReturn(true);
        Mockito.when(docSnapshot.getId()).thenReturn("time-id-456");
        Mockito.when(docSnapshot.toObject(Time.class)).thenReturn(mockTime);

        // Use real, completed ApiFutures
        Mockito.when(dateDocRef.get()).thenReturn(ApiFutures.immediateFuture(dateDocSnapshot));
        Mockito.when(timeDocRef.get()).thenReturn(ApiFutures.immediateFuture(docSnapshot));

        MvcResult mvcResult = mockMvc.perform(get("/api/companies/acme-corp/dates/date-id-123/times/time-id-456"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateId").value("date-id-123"))
                .andExpect(jsonPath("$.dateValue").value("2026-06-15"))
                .andExpect(jsonPath("$.classInfo.id").value("time-id-456"))
                .andExpect(jsonPath("$.classInfo.starttime").value("1:00 pm"))
                .andExpect(jsonPath("$.classInfo.endtime").value("1:45 pm"))
                .andExpect(jsonPath("$.classInfo.numberslots").value(8));
    }

    @Test
    @WithMockUser(username = "user")
    public void testGetTime_NotFound() throws Exception {
        DocumentSnapshot dateDocSnapshot = Mockito.mock(DocumentSnapshot.class);
        DocumentSnapshot timeDocSnapshot = Mockito.mock(DocumentSnapshot.class);

        Mockito.when(dateDocSnapshot.exists()).thenReturn(true);
        Mockito.when(timeDocSnapshot.exists()).thenReturn(false);

        // Use real, completed ApiFutures
        Mockito.when(dateDocRef.get()).thenReturn(ApiFutures.immediateFuture(dateDocSnapshot));
        Mockito.when(timeDocRef.get()).thenReturn(ApiFutures.immediateFuture(timeDocSnapshot));

        MvcResult mvcResult = mockMvc.perform(get("/api/companies/acme-corp/dates/date-id-123/times/time-id-nonexistent"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Class with ID \"time-id-nonexistent\" not found."));
    }

    @Test
    @WithMockUser(username = "user")
    public void testGetClassSlots_Success() throws Exception {
        QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
        QueryDocumentSnapshot docSnapshot = Mockito.mock(QueryDocumentSnapshot.class);

        ClassSlot mockSlot = new ClassSlot();
        mockSlot.setId("slot-id-789");
        mockSlot.setAhaid("AHA-999");
        mockSlot.setFirstname("Jane");
        mockSlot.setLastname("Doe");
        mockSlot.setPostnominalcredential("RN");
        mockSlot.setAttendance("Show");
        mockSlot.setPassfail("Pass");
        mockSlot.setScore("100");

        Mockito.when(docSnapshot.getId()).thenReturn("slot-id-789");
        Mockito.when(docSnapshot.toObject(ClassSlot.class)).thenReturn(mockSlot);
        Mockito.when(querySnapshot.getDocuments()).thenReturn(Collections.singletonList(docSnapshot));

        // Use real, completed ApiFuture
        Mockito.when(slotsCollection.get()).thenReturn(ApiFutures.immediateFuture(querySnapshot));

        MvcResult mvcResult = mockMvc.perform(get("/api/companies/acme-corp/dates/date-id-123/times/time-id-456/classslots"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("slot-id-789"))
                .andExpect(jsonPath("$[0].ahaid").value("AHA-999"))
                .andExpect(jsonPath("$[0].firstname").value("Jane"))
                .andExpect(jsonPath("$[0].lastname").value("Doe"))
                .andExpect(jsonPath("$[0].postnominalcredential").value("RN"))
                .andExpect(jsonPath("$[0].attendance").value("Show"))
                .andExpect(jsonPath("$[0].passfail").value("Pass"))
                .andExpect(jsonPath("$[0].score").value("100"));
    }

    @Test
    @WithMockUser(username = "user")
    public void testUpdateClassSlot_Success() throws Exception {
        DocumentSnapshot docSnapshot = Mockito.mock(DocumentSnapshot.class);
        WriteResult writeResult = Mockito.mock(WriteResult.class);

        ClassSlot existingSlot = new ClassSlot();
        existingSlot.setId("slot-id-789");
        existingSlot.setAhaid("");
        existingSlot.setFirstname("");
        existingSlot.setLastname("");
        existingSlot.setPostnominalcredential("");
        existingSlot.setAttendance("No Show");
        existingSlot.setPassfail("No Grade");
        existingSlot.setScore("");

        Mockito.when(docSnapshot.exists()).thenReturn(true);
        Mockito.when(docSnapshot.toObject(ClassSlot.class)).thenReturn(existingSlot);

        // Use real, completed ApiFuture
        Mockito.when(slotDocRef.get()).thenReturn(ApiFutures.immediateFuture(docSnapshot));
        Mockito.when(slotDocRef.set(any(ClassSlot.class))).thenReturn(ApiFutures.immediateFuture(writeResult));

        ClassSlot requestBody = new ClassSlot();
        requestBody.setAhaid("AHA-Updated");
        requestBody.setFirstname("John");
        requestBody.setLastname("Smith");
        requestBody.setPostnominalcredential("EMT");
        requestBody.setAttendance("Show");
        requestBody.setPassfail("Pass");
        requestBody.setScore("90");

        MvcResult mvcResult = mockMvc.perform(put("/api/companies/acme-corp/dates/date-id-123/times/time-id-456/classslots/slot-id-789")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ahaid").value("AHA-Updated"))
                .andExpect(jsonPath("$.firstname").value("John"))
                .andExpect(jsonPath("$.lastname").value("Smith"))
                .andExpect(jsonPath("$.postnominalcredential").value("EMT"))
                .andExpect(jsonPath("$.attendance").value("Show"))
                .andExpect(jsonPath("$.passfail").value("Pass"))
                .andExpect(jsonPath("$.score").value("90"));
    }

    @Test
    @WithMockUser(username = "user")
    public void testCreateDateSchedule_ValidationFailed_NegativeSlots() throws Exception {
        ClassSlotController.DateScheduleRequest request = new ClassSlotController.DateScheduleRequest();
        request.setDateValue("2026-06-15");
        request.setStarttime("1:00 pm");
        request.setEndtime("1:45 pm");
        request.setNumberslots(0); // Invalid slots (must be 1-30)

        MvcResult mvcResult = mockMvc.perform(post("/api/companies/acme-corp/dates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Slots must be between 1 and 30 (inclusive)."));
    }

    @Test
    @WithMockUser(username = "unauthorized_user")
    public void testCreateDateSchedule_AccessDenied_NotUser() throws Exception {
        ClassSlotController.DateScheduleRequest request = new ClassSlotController.DateScheduleRequest();
        request.setDateValue("2026-06-15");
        request.setStarttime("1:00 pm");
        request.setEndtime("1:45 pm");
        request.setNumberslots(8);

        MvcResult mvcResult = mockMvc.perform(post("/api/companies/acme-corp/dates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only the default 'user' can create date schedules."));
    }
    @Test
    @WithMockUser(username = "user")
    public void testBatchAddClassSlots_Success() throws Exception {
        WriteResult writeResult = Mockito.mock(WriteResult.class);
        Mockito.when(slotDocRef.set(any(ClassSlot.class))).thenReturn(ApiFutures.immediateFuture(writeResult));

        ClassSlot slot1 = new ClassSlot();
        slot1.setId("slot-1");
        slot1.setFirstname("Alice");
        
        ClassSlot slot2 = new ClassSlot();
        slot2.setId("slot-2");
        slot2.setFirstname("Bob");

        MvcResult mvcResult = mockMvc.perform(post("/api/companies/acme-corp/dates/date-1/times/time-1/classslots/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Arrays.asList(slot1, slot2))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully populated 2 slots."));
    }

    @Test
    @WithMockUser(username = "user")
    public void testBatchUpdateClassSlots_Success() throws Exception {
        DocumentSnapshot docSnapshot = Mockito.mock(DocumentSnapshot.class);
        WriteResult writeResult = Mockito.mock(WriteResult.class);
        
        ClassSlot existingSlot = new ClassSlot();
        existingSlot.setId("slot-1");
        existingSlot.setFirstname("OldName");

        Mockito.when(docSnapshot.exists()).thenReturn(true);
        Mockito.when(docSnapshot.toObject(ClassSlot.class)).thenReturn(existingSlot);
        Mockito.when(docSnapshot.getId()).thenReturn("slot-1");
        
        Mockito.when(slotDocRef.get()).thenReturn(ApiFutures.immediateFuture(docSnapshot));
        Mockito.when(slotDocRef.set(any(ClassSlot.class))).thenReturn(ApiFutures.immediateFuture(writeResult));

        ClassSlot updateSlot = new ClassSlot();
        updateSlot.setId("slot-1");
        updateSlot.setFirstname("NewName");

        MvcResult mvcResult = mockMvc.perform(put("/api/companies/acme-corp/dates/date-1/times/time-1/classslots/update")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Collections.singletonList(updateSlot))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully updated 1 slots."));
    }

    @Test
    @WithMockUser(username = "user")
    public void testBatchResetClassSlots_Success() throws Exception {
        WriteResult writeResult = Mockito.mock(WriteResult.class);
        Mockito.when(slotDocRef.set(any(ClassSlot.class))).thenReturn(ApiFutures.immediateFuture(writeResult));

        MvcResult mvcResult = mockMvc.perform(post("/api/companies/acme-corp/dates/date-1/times/time-1/classslots/delete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Arrays.asList("slot-1", "slot-2"))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully reset 2 slots to defaults."));
    }
}

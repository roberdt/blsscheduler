package info.safetynetaccess.blsscheduler.controller;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import info.safetynetaccess.blsscheduler.model.ClassSlot;
import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.model.Date;
import info.safetynetaccess.blsscheduler.model.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies/{companyId}")
public class ClassSlotController {

    @Autowired
    private Firestore firestore;

    /**
     * Helper utility to convert a Google ApiFuture into a Reactor Mono.
     */
    private static <T> Mono<T> fromApiFuture(ApiFuture<T> future) {
        return Mono.create(sink -> ApiFutures.addCallback(future, new ApiFutureCallback<T>() {
            @Override
            public void onFailure(Throwable t) {
                sink.error(t);
            }

            @Override
            public void onSuccess(T result) {
                sink.success(result);
            }
        }, MoreExecutors.directExecutor()));
    }

    /**
     * Requirement: Only available to the “user” a screen to create a “date” for the class.
     * This endpoint schedules a new Date, Time, and pre-populates blank ClassSlots.
     */
    @PostMapping("/dates")
    public Mono<ResponseEntity<Object>> createDateSchedule(
            @PathVariable String companyId,
            Principal principal,
            @RequestBody DateScheduleRequest request) {

        // Authenticated user check (enforced by SecurityConfig, but safe to keep check)
        if (principal == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body((Object) new ErrorResponse("Authentication required.")));
        }

        // Validate basic inputs
        if (request.getDateValue() == null || request.getDateValue().trim().isEmpty() ||
            request.getStarttime() == null || request.getStarttime().trim().isEmpty() ||
            request.getEndtime() == null || request.getEndtime().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body((Object) new ErrorResponse("dateValue, starttime, and endtime are required.")));
        }

        // Slots limit: 1-30, cannot be 0
        int slots = request.getNumberslots();
        if (slots < 1 || slots > 30) {
            return Mono.just(ResponseEntity.badRequest()
                    .body((Object) new ErrorResponse("Slots must be between 1 and 30 (inclusive).")));
        }

        DocumentReference companyRef = firestore.collection("companies").document(companyId);

        // Fetch company and verify Active status
        return fromApiFuture(companyRef.get())
                .flatMap(companyDoc -> {
                    if (!companyDoc.exists()) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body((Object) new ErrorResponse("Company with ID \"" + companyId + "\" does not exist.")));
                    }

                    Company company = companyDoc.toObject(Company.class);
                    if (!"Yes".equalsIgnoreCase(company.getApproval())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body((Object) new ErrorResponse("Scheduling failed. Company \"" + company.getName() + "\" is currently NOT ACTIVE.")));
                    }

                    // 1. Check if Date document already exists under this company's subcollection
                    CollectionReference datesRef = companyRef.collection("dates");
                    return fromApiFuture(datesRef.whereEqualTo("dateValue", request.getDateValue().trim()).get())
                            .flatMap(dateQuery -> {
                                DocumentReference dateDocRef;
                                Mono<Void> dateSaveMono;

                                if (dateQuery.isEmpty()) {
                                    // Generate new Date document
                                    String dateId = UUID.randomUUID().toString();
                                    dateDocRef = datesRef.document(dateId);
                                    Date newDate = new Date(request.getDateValue().trim());
                                    newDate.setId(dateId);
                                    dateSaveMono = fromApiFuture(dateDocRef.set(newDate)).then();
                                } else {
                                    // Reuse existing Date document
                                    dateDocRef = dateQuery.getDocuments().get(0).getReference();
                                    dateSaveMono = Mono.empty();
                                }

                                final DocumentReference targetDateDocRef = dateDocRef;

                                // 2. Check if Time document already exists under dates/{dateId}/times/
                                CollectionReference timesRef = targetDateDocRef.collection("times");
                                String courseTitle = request.getCoursetitle() != null ? request.getCoursetitle().trim() : "";
                                String startTime = request.getStarttime().trim();

                                return fromApiFuture(timesRef
                                        .whereEqualTo("starttime", startTime)
                                        .whereEqualTo("coursetitle", courseTitle)
                                        .get())
                                        .flatMap(timeQuery -> {
                                            if (!timeQuery.isEmpty()) {
                                                // Time already exists, skip creation and return existing
                                                QueryDocumentSnapshot timeDoc = timeQuery.getDocuments().get(0);
                                                String existingTimeId = timeDoc.getId();

                                                // Optionally fetch existing slots to return in SuccessResponse
                                                return fromApiFuture(timeDoc.getReference().collection("classslots").get())
                                                        .map(slotsSnapshot -> {
                                                            List<ClassSlot> existingSlots = slotsSnapshot.getDocuments().stream()
                                                                    .map(doc -> {
                                                                        ClassSlot s = doc.toObject(ClassSlot.class);
                                                                        s.setId(doc.getId());
                                                                        return s;
                                                                    })
                                                                    .collect(Collectors.toList());

                                                            return ResponseEntity.ok((Object) new SuccessResponse(
                                                                    "Date and Time slot already exist. No new slots were created.",
                                                                    targetDateDocRef.getId(),
                                                                    existingTimeId,
                                                                    existingSlots
                                                            ));
                                                        });
                                            }

                                            // 3. Create the Time document under dates/{dateId}/times/
                                            String timeId = UUID.randomUUID().toString();
                                            DocumentReference timeDocRef = timesRef.document(timeId);

                                            Time newTime = new Time();
                                            newTime.setId(timeId);
                                            newTime.setStarttime(startTime);
                                            newTime.setEndtime(request.getEndtime().trim());
                                            newTime.setNumberslots(slots);
                                            newTime.setApproved(request.isApproved());
                                            newTime.setCoursetitle(courseTitle);
                                            newTime.setCoursetype(request.getCoursetype() != null ? request.getCoursetype().trim() : "");
                                            newTime.setInstructorname(request.getInstructorname() != null ? request.getInstructorname().trim() : "");

                                            // 4. Pre-populate blank ClassSlots with "No Show"
                                            List<Mono<WriteResult>> slotSaveMonos = new ArrayList<>();
                                            List<ClassSlot> createdSlots = new ArrayList<>();

                                            CollectionReference slotsRef = timeDocRef.collection("classslots");
                                            for (int i = 0; i < slots; i++) {
                                                String slotId = UUID.randomUUID().toString();
                                                DocumentReference slotDocRef = slotsRef.document(slotId);

                                                ClassSlot slot = new ClassSlot();
                                                slot.setId(slotId);
                                                slot.setFirstname("");
                                                slot.setLastname("");
                                                slot.setPostnominalcredential("");
                                                slot.setAttendance("No Show");
                                                slot.setPassfail("No Grade");
                                                slot.setScore("");

                                                createdSlots.add(slot);
                                                slotSaveMonos.add(fromApiFuture(slotDocRef.set(slot)));
                                            }

                                            // Run all operations reactively in chain
                                            return dateSaveMono
                                                    .then(fromApiFuture(timeDocRef.set(newTime)))
                                                    .then(Mono.defer(() -> Flux.concat(slotSaveMonos).then()))
                                                    .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body((Object) new SuccessResponse(
                                                            "Date, time slot, and " + slots + " pre-populated class slots created successfully.",
                                                            targetDateDocRef.getId(),
                                                            timeId,
                                                            createdSlots
                                                    )));
                                        });
                            });
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("An error occurred: " + t.getMessage()))));
    }

    /**
     * Get all classes (times) for a company across all dates.
     */
    @GetMapping("/all-classes")
    public Mono<ResponseEntity<List<CompanyClassResponse>>> getAllClasses(@PathVariable String companyId) {
        CollectionReference datesRef = firestore.collection("companies").document(companyId).collection("dates");

        return fromApiFuture(datesRef.get())
                .flatMapMany(dateSnapshot -> Flux.fromIterable(dateSnapshot.getDocuments()))
                .flatMap(dateDoc -> {
                    Date date = dateDoc.toObject(Date.class);
                    String dateValue = date.getDateValue();
                    String dateId = dateDoc.getId();

                    CollectionReference timesRef = dateDoc.getReference().collection("times");
                    return fromApiFuture(timesRef.get())
                            .flatMapMany(timeSnapshot -> Flux.fromIterable(timeSnapshot.getDocuments()))
                            .map(timeDoc -> {
                                Time time = timeDoc.toObject(Time.class);
                                time.setId(timeDoc.getId());
                                return new CompanyClassResponse(dateValue, dateId, time);
                            });
                })
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * Get all dates for a company.
     */
    @GetMapping("/dates")
    public Mono<ResponseEntity<List<Date>>> getDates(@PathVariable String companyId) {
        CollectionReference datesRef = firestore.collection("companies").document(companyId).collection("dates");
        return fromApiFuture(datesRef.get())
                .map(snapshot -> {
                    List<Date> dates = snapshot.getDocuments().stream()
                            .map(doc -> {
                                Date d = doc.toObject(Date.class);
                                d.setId(doc.getId());
                                return d;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(dates);
                })
                .onErrorResume(t -> {
                    t.printStackTrace();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    /**
     * Get all times scheduled under a company's date.
     */
    @GetMapping("/dates/{dateId}/times")
    public Mono<ResponseEntity<List<Time>>> getTimes(
            @PathVariable String companyId,
            @PathVariable String dateId) {
        CollectionReference timesRef = firestore.collection("companies")
                .document(companyId)
                .collection("dates")
                .document(dateId)
                .collection("times");

        return fromApiFuture(timesRef.get())
                .map(snapshot -> {
                    List<Time> times = snapshot.getDocuments().stream()
                            .map(doc -> {
                                Time t = doc.toObject(Time.class);
                                t.setId(doc.getId());
                                return t;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(times);
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * Get all class slots for a specific company, date, and time.
     */
    @GetMapping("/dates/{dateId}/times/{timeId}/classslots")
    public Mono<ResponseEntity<List<ClassSlot>>> getClassSlots(
            @PathVariable String companyId,
            @PathVariable String dateId,
            @PathVariable String timeId) {
        CollectionReference slotsRef = firestore.collection("companies")
                .document(companyId)
                .collection("dates")
                .document(dateId)
                .collection("times")
                .document(timeId)
                .collection("classslots");

        return fromApiFuture(slotsRef.get())
                .map(snapshot -> {
                    List<ClassSlot> slots = snapshot.getDocuments().stream()
                            .map(doc -> {
                                ClassSlot s = doc.toObject(ClassSlot.class);
                                s.setId(doc.getId());
                                return s;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(slots);
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    /**
     * Update (book) a specific class slot.
     */
    @PutMapping("/dates/{dateId}/times/{timeId}/classslots/{slotId}")
    public Mono<ResponseEntity<Object>> updateClassSlot(
            @PathVariable String companyId,
            @PathVariable String dateId,
            @PathVariable String timeId,
            @PathVariable String slotId,
            @RequestBody ClassSlot requestSlot) {

        DocumentReference slotRef = firestore.collection("companies")
                .document(companyId)
                .collection("dates")
                .document(dateId)
                .collection("times")
                .document(timeId)
                .collection("classslots")
                .document(slotId);

        return fromApiFuture(slotRef.get())
                .flatMap(doc -> {
                    if (!doc.exists()) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body((Object) new ErrorResponse("Class slot with ID \"" + slotId + "\" not found.")));
                    }

                    ClassSlot slot = doc.toObject(ClassSlot.class);
                    slot.setId(slotId);

                    if (requestSlot.getFirstname() != null) {
                        slot.setFirstname(requestSlot.getFirstname());
                    }
                    if (requestSlot.getLastname() != null) {
                        slot.setLastname(requestSlot.getLastname());
                    }
                    if (requestSlot.getPostnominalcredential() != null) {
                        slot.setPostnominalcredential(requestSlot.getPostnominalcredential());
                    }
                    if (requestSlot.getAttendance() != null) {
                        slot.setAttendance(requestSlot.getAttendance());
                    }
                    if (requestSlot.getPassfail() != null) {
                        slot.setPassfail(requestSlot.getPassfail());
                    }
                    if (requestSlot.getScore() != null) {
                        slot.setScore(requestSlot.getScore());
                    }

                    return fromApiFuture(slotRef.set(slot))
                            .map(writeResult -> ResponseEntity.ok((Object) slot));
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to update class slot: " + t.getMessage()))));
    }


    // DTOs & Helper Classes
    public static class DateScheduleRequest {
        private String dateValue;
        private String starttime;
        private String endtime;
        private int numberslots;
        private boolean approved;
        private String coursetitle;
        private String coursetype;
        private String instructorname;

        public String getDateValue() {
            return dateValue;
        }

        public void setDateValue(String dateValue) {
            this.dateValue = dateValue;
        }

        public String getStarttime() {
            return starttime;
        }

        public void setStarttime(String starttime) {
            this.starttime = starttime;
        }

        public String getEndtime() {
            return endtime;
        }

        public void setEndtime(String endtime) {
            this.endtime = endtime;
        }

        public int getNumberslots() {
            return numberslots;
        }

        public void setNumberslots(int numberslots) {
            this.numberslots = numberslots;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public String getCoursetitle() {
            return coursetitle;
        }

        public void setCoursetitle(String coursetitle) {
            this.coursetitle = coursetitle;
        }

        public String getCoursetype() {
            return coursetype;
        }

        public void setCoursetype(String coursetype) {
            this.coursetype = coursetype;
        }

        public String getInstructorname() {
            return instructorname;
        }

        public void setInstructorname(String instructorname) {
            this.instructorname = instructorname;
        }
    }

    public static class CompanyClassResponse {
        private String dateValue;
        private String dateId;
        private Time classInfo;

        public CompanyClassResponse() {}

        public CompanyClassResponse(String dateValue, String dateId, Time classInfo) {
            this.dateValue = dateValue;
            this.dateId = dateId;
            this.classInfo = classInfo;
        }

        public String getDateValue() {
            return dateValue;
        }

        public void setDateValue(String dateValue) {
            this.dateValue = dateValue;
        }

        public String getDateId() {
            return dateId;
        }

        public void setDateId(String dateId) {
            this.dateId = dateId;
        }

        public Time getClassInfo() {
            return classInfo;
        }

        public void setClassInfo(Time classInfo) {
            this.classInfo = classInfo;
        }
    }

    public static class SuccessResponse {
        private final String message;
        private final String dateId;
        private final String timeId;
        private final List<ClassSlot> classSlots;

        public SuccessResponse(String message, String dateId, String timeId, List<ClassSlot> classSlots) {
            this.message = message;
            this.dateId = dateId;
            this.timeId = timeId;
            this.classSlots = classSlots;
        }

        public String getMessage() {
            return message;
        }

        public String getDateId() {
            return dateId;
        }

        public String getTimeId() {
            return timeId;
        }

        public List<ClassSlot> getClassSlots() {
            return classSlots;
        }
    }

    private static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}

package info.safetynetaccess.blsscheduler.controller;
 
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import info.safetynetaccess.blsscheduler.model.Instructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
 
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
 
@RestController
@RequestMapping("/api/companies/{companyId}/instructors")
public class InstructorController {
 
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
 
    // 1. Get all instructors for a company
    @GetMapping
    public Mono<ResponseEntity<List<Instructor>>> getAllInstructors(@PathVariable String companyId) {
        CollectionReference instructorsRef = firestore.collection("companies").document(companyId).collection("instructors");
        return fromApiFuture(instructorsRef.get())
                .map(snapshot -> {
                    List<Instructor> instructors = snapshot.getDocuments().stream()
                            .map(doc -> {
                                Instructor inst = doc.toObject(Instructor.class);
                                inst.setId(doc.getId());
                                return inst;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(instructors);
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }
 
    // 2. Add a new instructor for a company
    @PostMapping
    public Mono<ResponseEntity<Object>> addInstructor(@PathVariable String companyId, @RequestBody Instructor instructor) {
        if (instructor.getCertificationid() == null || instructor.getCertificationid().isEmpty() ||
            instructor.getFirstname() == null || instructor.getFirstname().isEmpty() ||
            instructor.getLastname() == null || instructor.getLastname().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Certification ID, First Name, and Last Name are required.")));
        }
 
        String instructorId = (instructor.getId() == null || instructor.getId().isEmpty()) ? UUID.randomUUID().toString() : instructor.getId();
        instructor.setId(instructorId);
 
        DocumentReference instructorRef = firestore.collection("companies").document(companyId).collection("instructors").document(instructorId);
 
        return fromApiFuture(instructorRef.set(instructor))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body((Object) instructor))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to add instructor: " + t.getMessage()))));
    }
 
    // 3. Update an existing instructor for a company
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> updateInstructor(@PathVariable String companyId, @PathVariable String id, @RequestBody Instructor instructor) {
        if (instructor.getCertificationid() == null || instructor.getCertificationid().isEmpty() ||
            instructor.getFirstname() == null || instructor.getFirstname().isEmpty() ||
            instructor.getLastname() == null || instructor.getLastname().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Certification ID, First Name, and Last Name are required.")));
        }
 
        instructor.setId(id);
        DocumentReference instructorRef = firestore.collection("companies").document(companyId).collection("instructors").document(id);
 
        return fromApiFuture(instructorRef.set(instructor))
                .map(saved -> ResponseEntity.ok((Object) instructor))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to update instructor: " + t.getMessage()))));
    }
 
    // 4. Populate company instructors with default/dummy instructors
    @PostMapping("/populate")
    public Mono<ResponseEntity<List<Instructor>>> populateInstructors(@PathVariable String companyId) {
        List<Instructor> defaultInstructors = Arrays.asList(
                new Instructor("CERT-58301", "Jane", "Miller", "NRP, EMS-I", true),
                new Instructor("CERT-92041", "David", "Chen", "MD, FAHA", true),
                new Instructor("CERT-10492", "Sarah", "O'Connor", "RN, BSN, CCRN", true),
                new Instructor("CERT-47209", "Marcus", "Vance", "Paramedic, CCEMT-P", false)
        );
 
        CollectionReference instructorsRef = firestore.collection("companies").document(companyId).collection("instructors");
        List<Mono<WriteResult>> saveMonos = defaultInstructors.stream()
                .map(inst -> {
                    String id = UUID.randomUUID().toString();
                    inst.setId(id);
                    return fromApiFuture(instructorsRef.document(id).set(inst));
                })
                .collect(Collectors.toList());
 
        return Flux.concat(saveMonos)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(defaultInstructors)))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
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

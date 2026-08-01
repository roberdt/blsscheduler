package info.safetynetaccess.blsscheduler.controller;
 
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import info.safetynetaccess.blsscheduler.model.Course;
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
@RequestMapping("/api/companies/{companyId}/courses")
public class CourseController {
 
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
 
    // 1. Get all courses for a company
    @GetMapping
    public Mono<ResponseEntity<List<Course>>> getAllCourses(@PathVariable String companyId) {
        CollectionReference coursesRef = firestore.collection("companies").document(companyId).collection("courses");
        return fromApiFuture(coursesRef.get())
                .map(snapshot -> {
                    List<Course> courses = snapshot.getDocuments().stream()
                            .map(doc -> {
                                Course c = doc.toObject(Course.class);
                                c.setId(doc.getId());
                                return c;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(courses);
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }
 
    // 2. Add a new course for a company
    @PostMapping
    public Mono<ResponseEntity<Object>> addCourse(@PathVariable String companyId, @RequestBody Course course) {
        if (course.getCoursetitle() == null || course.getCoursetitle().isEmpty() ||
            course.getCoursetype() == null || course.getCoursetype().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Course Title and Course Type are required.")));
        }
 
        String courseId = (course.getId() == null || course.getId().isEmpty()) ? UUID.randomUUID().toString() : course.getId();
        course.setId(courseId);
 
        DocumentReference courseRef = firestore.collection("companies").document(companyId).collection("courses").document(courseId);
 
        return fromApiFuture(courseRef.set(course))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body((Object) course))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to add course: " + t.getMessage()))));
    }
 
    // 3. Update an existing course for a company
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> updateCourse(@PathVariable String companyId, @PathVariable String id, @RequestBody Course course) {
        if (course.getCoursetitle() == null || course.getCoursetitle().isEmpty() ||
            course.getCoursetype() == null || course.getCoursetype().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Course Title and Course Type are required.")));
        }
 
        course.setId(id);
        DocumentReference courseRef = firestore.collection("companies").document(companyId).collection("courses").document(id);
 
        return fromApiFuture(courseRef.set(course))
                .map(saved -> ResponseEntity.ok((Object) course))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to update course: " + t.getMessage()))));
    }
 
    // 4. Populate company courses with default courses
    @PostMapping("/populate")
    public Mono<ResponseEntity<List<Course>>> populateCourses(@PathVariable String companyId) {
        List<Course> defaultCourses = Arrays.asList(
                new Course("BLS Provider Course", "BLS Provider", true),
                new Course("Heartsaver CPR AED Course", "Heartsaver CPR", true),
                new Course("First Aid CPR AED Course", "First Aid", true),
                new Course("BLS Instructor Essentials", "BLS Instructor", false)
        );
 
        CollectionReference coursesRef = firestore.collection("companies").document(companyId).collection("courses");
        List<Mono<WriteResult>> saveMonos = defaultCourses.stream()
                .map(c -> {
                    String id = UUID.randomUUID().toString();
                    c.setId(id);
                    return fromApiFuture(coursesRef.document(id).set(c));
                })
                .collect(Collectors.toList());
 
        return Flux.concat(saveMonos)
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(defaultCourses)))
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

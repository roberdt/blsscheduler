package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Course;
import info.safetynetaccess.blsscheduler.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    // 1. Get all courses
    @GetMapping
    public Flux<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // 2. Add a new course
    @PostMapping
    public Mono<ResponseEntity<Object>> addCourse(@RequestBody Course course) {
        if (course.getCoursetitle() == null || course.getCoursetitle().isEmpty() ||
            course.getCoursetype() == null || course.getCoursetype().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Course Title and Course Type are required.")));
        }

        if (course.getId() == null || course.getId().isEmpty()) {
            course.setId(UUID.randomUUID().toString());
        }

        return courseRepository.save(course)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body((Object) saved))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to add course: " + t.getMessage()))));
    }

    // 3. Update an existing course
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> updateCourse(@PathVariable String id, @RequestBody Course course) {
        if (course.getCoursetitle() == null || course.getCoursetitle().isEmpty() ||
            course.getCoursetype() == null || course.getCoursetype().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Course Title and Course Type are required.")));
        }

        course.setId(id);

        return courseRepository.save(course)
                .map(saved -> ResponseEntity.ok((Object) saved))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to update course: " + t.getMessage()))));
    }

    // 4. Populate courses table with default courses
    @PostMapping("/populate")
    public Mono<ResponseEntity<List<Course>>> populateCourses() {
        List<Course> defaultCourses = Arrays.asList(
                new Course("BLS Provider Course", "BLS Provider", true),
                new Course("Heartsaver CPR AED Course", "Heartsaver CPR", true),
                new Course("First Aid CPR AED Course", "First Aid", true),
                new Course("BLS Instructor Essentials", "BLS Instructor", false)
        );

        // Assign IDs if not present
        defaultCourses.forEach(c -> {
            if (c.getId() == null || c.getId().isEmpty()) {
                c.setId(UUID.randomUUID().toString());
            }
        });

        // Save all and collect as list to return
        return Flux.fromIterable(defaultCourses)
                .flatMap(courseRepository::save)
                .collectList()
                .map(savedList -> ResponseEntity.status(HttpStatus.CREATED).body(savedList))
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

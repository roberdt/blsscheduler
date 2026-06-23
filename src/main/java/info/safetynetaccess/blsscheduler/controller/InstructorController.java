package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Instructor;
import info.safetynetaccess.blsscheduler.repository.InstructorRepository;
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
@RequestMapping("/api/instructors")
public class InstructorController {

    @Autowired
    private InstructorRepository instructorRepository;

    // 1. Get all instructors
    @GetMapping
    public Flux<Instructor> getAllInstructors() {
        return instructorRepository.findAll();
    }

    // 2. Add a new instructor
    @PostMapping
    public Mono<ResponseEntity<Object>> addInstructor(@RequestBody Instructor instructor) {
        if (instructor.getCertificationid() == null || instructor.getCertificationid().isEmpty() ||
            instructor.getFirstname() == null || instructor.getFirstname().isEmpty() ||
            instructor.getLastname() == null || instructor.getLastname().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Certification ID, First Name, and Last Name are required.")));
        }

        if (instructor.getId() == null || instructor.getId().isEmpty()) {
            instructor.setId(UUID.randomUUID().toString());
        }

        return instructorRepository.save(instructor)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body((Object) saved))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to add instructor: " + t.getMessage()))));
    }

    // 3. Update an existing instructor
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> updateInstructor(@PathVariable String id, @RequestBody Instructor instructor) {
        if (instructor.getCertificationid() == null || instructor.getCertificationid().isEmpty() ||
            instructor.getFirstname() == null || instructor.getFirstname().isEmpty() ||
            instructor.getLastname() == null || instructor.getLastname().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Certification ID, First Name, and Last Name are required.")));
        }

        instructor.setId(id);

        return instructorRepository.save(instructor)
                .map(saved -> ResponseEntity.ok((Object) saved))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to update instructor: " + t.getMessage()))));
    }

    // 4. Populate instructors table with default/dummy instructors
    @PostMapping("/populate")
    public Mono<ResponseEntity<List<Instructor>>> populateInstructors() {
        List<Instructor> defaultInstructors = Arrays.asList(
                new Instructor("CERT-58301", "Jane", "Miller", "NRP, EMS-I", true),
                new Instructor("CERT-92041", "David", "Chen", "MD, FAHA", true),
                new Instructor("CERT-10492", "Sarah", "O'Connor", "RN, BSN, CCRN", true),
                new Instructor("CERT-47209", "Marcus", "Vance", "Paramedic, CCEMT-P", false)
        );

        // Assign IDs if not present
        defaultInstructors.forEach(inst -> {
            if (inst.getId() == null || inst.getId().isEmpty()) {
                inst.setId(UUID.randomUUID().toString());
            }
        });

        // Save all and collect as list to return
        return Flux.fromIterable(defaultInstructors)
                .flatMap(instructorRepository::save)
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

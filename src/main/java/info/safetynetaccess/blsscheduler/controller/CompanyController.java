package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyRepository companyRepository;

    // Get all registered companies (returning full company records including name, purpose and approval)
    @GetMapping
    public Flux<Company> getAllCompanies() {
        return companyRepository.findAll();
    }


    // Update company approval status
    @PutMapping("/{id}/approval")
    public Mono<ResponseEntity<Object>> updateCompanyApproval(@PathVariable String id, @RequestBody Map<String, String> request) {
        String approval = request.get("approval");
        if (approval == null || approval.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body((Object) new ErrorResponse("approval status is required")));
        }

        return companyRepository.findById(id)
                .flatMap(company -> {
                    company.setApproval(approval);
                    return companyRepository.save(company)
                            .map(saved -> ResponseEntity.ok((Object) saved));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body((Object) new ErrorResponse("Company with ID \"" + id + "\" not found."))));
    }

    // Helper Error Response DTO
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

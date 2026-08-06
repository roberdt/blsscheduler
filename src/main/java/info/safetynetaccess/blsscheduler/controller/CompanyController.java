package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<java.util.List<Company>> getAllCompanies() {
        java.util.List<Company> companies = companyRepository.findAll().collectList().block();
        return ResponseEntity.ok(companies);
    }


    // Update company approval status
    @PutMapping("/{id}/approval")
    public ResponseEntity<Object> updateCompanyApproval(@PathVariable String id, @RequestBody Map<String, String> request) {
        String approval = request.get("approval");
        if (approval == null || approval.isEmpty()) {
            return ResponseEntity.badRequest().body((Object) new ErrorResponse("approval status is required"));
        }

        Company company = companyRepository.findById(id).block();
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body((Object) new ErrorResponse("Company with ID \"" + id + "\" not found."));
        }

        company.setApproval(approval);
        Company saved = companyRepository.save(company).block();
        return ResponseEntity.ok((Object) saved);
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

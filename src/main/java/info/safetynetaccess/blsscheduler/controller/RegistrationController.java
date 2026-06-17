package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import info.safetynetaccess.blsscheduler.dto.RegistrationResponse;
import info.safetynetaccess.blsscheduler.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/api/register")
    public Mono<ResponseEntity<RegistrationResponse>> register(@Valid @RequestBody RegistrationRequest request) {
        return registrationService.register(request)
                .map(response -> {
                    if (response.isSuccess()) {
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                });
    }
}

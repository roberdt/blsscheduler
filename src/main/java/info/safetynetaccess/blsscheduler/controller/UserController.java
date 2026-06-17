package info.safetynetaccess.blsscheduler.controller;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.common.util.concurrent.MoreExecutors;
import info.safetynetaccess.blsscheduler.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companies/{companyId}/users")
public class UserController {

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

    // 1. Get all users for a company
    @GetMapping
    public Mono<ResponseEntity<List<User>>> getCompanyUsers(@PathVariable String companyId) {
        CollectionReference usersRef = firestore.collection("companies")
                .document(companyId)
                .collection("users");

        return fromApiFuture(usersRef.get())
                .map(querySnapshot -> {
                    List<User> users = querySnapshot.getDocuments().stream()
                            .map(doc -> {
                                User u = doc.toObject(User.class);
                                u.setId(doc.getId());
                                return u;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(users);
                })
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    // 2. Add a user under a company
    @PostMapping
    public Mono<ResponseEntity<Object>> addUser(@PathVariable String companyId, @RequestBody User user) {
        if (user.getName() == null || user.getName().isEmpty() ||
            user.getEmail() == null || user.getEmail().isEmpty() ||
            user.getUsername() == null || user.getUsername().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Name, Email, and Username are required.")));
        }

        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }

        DocumentReference userRef = firestore.collection("companies")
                .document(companyId)
                .collection("users")
                .document(user.getId());

        return fromApiFuture(userRef.set(user))
                .map(writeResult -> ResponseEntity.status(HttpStatus.CREATED).body((Object) user))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to add user: " + t.getMessage()))));
    }

    // 3. Update a user under a company
    @PutMapping("/{userId}")
    public Mono<ResponseEntity<Object>> updateUser(
            @PathVariable String companyId,
            @PathVariable String userId,
            @RequestBody User user) {
        
        if (user.getName() == null || user.getName().isEmpty() ||
            user.getEmail() == null || user.getEmail().isEmpty() ||
            user.getUsername() == null || user.getUsername().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(new ErrorResponse("Name, Email, and Username are required.")));
        }

        user.setId(userId);
        DocumentReference userRef = firestore.collection("companies")
                .document(companyId)
                .collection("users")
                .document(userId);

        return fromApiFuture(userRef.set(user))
                .map(writeResult -> ResponseEntity.ok((Object) user))
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to update user: " + t.getMessage()))));
    }

    // 4. Delete a user under a company
    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<Object>> deleteUser(@PathVariable String companyId, @PathVariable String userId) {
        DocumentReference userRef = firestore.collection("companies")
                .document(companyId)
                .collection("users")
                .document(userId);

        return fromApiFuture(userRef.delete())
                .map(writeResult -> (ResponseEntity<Object>) ResponseEntity.ok().build())
                .onErrorResume(t -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body((Object) new ErrorResponse("Failed to delete user: " + t.getMessage()))));
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

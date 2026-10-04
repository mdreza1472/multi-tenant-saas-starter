package com.reza.saas.sample;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Note that no method takes a tenant id: the tenant always comes from the token
 * (via TenantContext), so a client cannot address another tenant's notes at all.
 */
@RestController
@RequestMapping("/notes")
public class NoteController {

    public record CreateNoteRequest(@NotBlank @Size(max = 200) String title,
                                    @Size(max = 10_000) String body) {
    }

    public record NoteResponse(String noteId, String title, String body, String createdBy, Instant createdAt) {
        static NoteResponse from(Note n) {
            return new NoteResponse(n.getNoteId(), n.getTitle(), n.getBody(), n.getCreatedBy(), n.getCreatedAt());
        }
    }

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<NoteResponse> list() {
        return noteService.list().stream().map(NoteResponse::from).toList();
    }

    @GetMapping("/{noteId}")
    public NoteResponse get(@PathVariable String noteId) {
        return NoteResponse.from(noteService.get(noteId));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        Note created = noteService.create(request.title(), request.body(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(created));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable String noteId, @AuthenticationPrincipal Jwt jwt) {
        noteService.delete(noteId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}

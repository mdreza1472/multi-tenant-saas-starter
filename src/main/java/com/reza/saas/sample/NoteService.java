package com.reza.saas.sample;

import com.reza.saas.common.NotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NoteService {

    private final NoteRepository notes;

    public NoteService(NoteRepository notes) {
        this.notes = notes;
    }

    public List<Note> list() {
        return notes.listAll();
    }

    public Note get(String noteId) {
        // "Not found" also covers "exists but belongs to another tenant" - the repository
        // can't see other tenants' items at all, so the two cases are indistinguishable
        // (and should stay that way: a 403 here would confirm the id exists elsewhere).
        return notes.findById(noteId).orElseThrow(() -> new NotFoundException("Note not found"));
    }

    public Note create(String title, String body, String userId) {
        Note note = new Note();
        note.setNoteId(UUID.randomUUID().toString());
        note.setTitle(title);
        note.setBody(body);
        note.setCreatedBy(userId);
        note.setCreatedAt(Instant.now());
        return notes.save(note);
    }

	 // TODO: get() + delete() is a check-then-act race - a concurrent duplicate delete
	 // request would pass the existence check twice. Harmless today (DynamoDB's delete
	 // is a no-op on a missing key), but worth revisiting with a conditional delete if
	 // this method ever grows side effects beyond the delete itself.
    public void delete(String noteId, String requestingUserId) {
        Note note = get(noteId);
        if (!note.getCreatedBy().equals(requestingUserId)) {
            throw new AccessDeniedException("Only the author can delete this note");
        }
        notes.delete(noteId);
    }
}

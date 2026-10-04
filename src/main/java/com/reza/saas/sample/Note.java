package com.reza.saas.sample;

import com.reza.saas.persistence.BaseItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

/**
 * Sample tenant-owned resource: pk = TENANT#<id>, sk = NOTE#<noteId>.
 * Exists to show the controller -> service -> repository pattern end to end;
 * replace it with your real domain (jobs, invoices, clients...).
 *
 * TODO: size limits (title/body) are currently only enforced in
 * NoteController's @Valid request DTO, not here - a future direct caller of
 * NoteService.create() bypasses that check. Worth deciding whether real
 * domain entities replacing this one should self-validate (e.g. a compact
 * constructor check) rather than relying solely on the controller boundary.
 */
@DynamoDbBean
public class Note extends BaseItem {

    private String noteId;
    private String title;
    private String body;
    private String createdBy;
    private Instant createdAt;

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

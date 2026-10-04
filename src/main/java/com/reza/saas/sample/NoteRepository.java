package com.reza.saas.sample;

import com.reza.saas.config.AppProperties;
import com.reza.saas.config.DynamoDbClientHolder;
import com.reza.saas.persistence.SingleTableKeys;
import com.reza.saas.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

@Repository
public class NoteRepository extends TenantScopedRepository<Note> {

    public NoteRepository(DynamoDbClientHolder clientHolder, AppProperties props) {
        super(clientHolder, props.dynamodb().tableName(), Note.class, SingleTableKeys.NOTE_PREFIX);
    }

    @Override
    protected String idOf(Note note) {
        return note.getNoteId();
    }
}
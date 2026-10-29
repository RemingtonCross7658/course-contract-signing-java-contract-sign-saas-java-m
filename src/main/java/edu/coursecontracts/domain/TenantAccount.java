package edu.coursecontracts.domain;

import java.time.Instant;
import java.util.Objects;

public final class TenantAccount {
    public enum State { ONBOARDING, ACTIVE, SUSPENDED, CLOSED }
    private final String tenantId;
    private State state;
    private Instant changedAt;

    public TenantAccount(String tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId);
        this.state = State.ONBOARDING;
        this.changedAt = Instant.now();
    }

    public void activate() { transition(State.ONBOARDING, State.ACTIVE); }
    public void suspend() { transition(State.ACTIVE, State.SUSPENDED); }
    public void restore() { transition(State.SUSPENDED, State.ACTIVE); }
    public void close() {
        if (state == State.CLOSED) throw new IllegalStateException("Tenant is already closed");
        state = State.CLOSED;
        changedAt = Instant.now();
    }
    public boolean maySignContracts() { return state == State.ACTIVE; }
    public String tenantId() { return tenantId; }
    public State state() { return state; }
    public Instant changedAt() { return changedAt; }

    private void transition(State expected, State next) {
        if (state != expected) throw new IllegalStateException("Expected " + expected + " but was " + state);
        state = next;
        changedAt = Instant.now();
    }
}

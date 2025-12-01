package com.specflux.shared.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for domain events.
 *
 * <p>Domain events represent something that happened in the domain that domain experts care about.
 * They are immutable and capture the state at the time the event occurred.
 */
public abstract class DomainEvent {

  private final UUID eventId;
  private final Instant occurredAt;

  protected DomainEvent() {
    this.eventId = UUID.randomUUID();
    this.occurredAt = Instant.now();
  }

  /**
   * Returns the unique identifier of this event.
   *
   * @return the event ID
   */
  public UUID getEventId() {
    return eventId;
  }

  /**
   * Returns when this event occurred.
   *
   * @return the timestamp of the event
   */
  public Instant getOccurredAt() {
    return occurredAt;
  }

  /**
   * Returns the type name of this event.
   *
   * @return the event type name
   */
  public String getEventType() {
    return getClass().getSimpleName();
  }
}

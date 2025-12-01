package com.specflux.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots.
 *
 * <p>An aggregate root is an entity that serves as the entry point to an aggregate. It guarantees
 * the consistency of changes being made within the aggregate by forbidding external objects from
 * holding references to its members.
 *
 * <p>Aggregate roots can register domain events that are published after the aggregate is
 * persisted.
 *
 * @param <ID> the type of the aggregate root's identifier
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {

  private final List<DomainEvent> domainEvents = new ArrayList<>();

  /**
   * Registers a domain event to be published when the aggregate is persisted.
   *
   * @param event the domain event to register
   */
  protected void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  /**
   * Returns all registered domain events.
   *
   * @return an unmodifiable list of domain events
   */
  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  /** Clears all registered domain events. Called after events have been published. */
  public void clearDomainEvents() {
    domainEvents.clear();
  }
}

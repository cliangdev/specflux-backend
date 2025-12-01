package com.specflux.shared.domain;

import java.util.Objects;

/**
 * Base class for all domain entities.
 *
 * <p>Entities are objects with a distinct identity that runs through time and different states.
 * Equality is based on identity (ID), not attribute values.
 *
 * @param <ID> the type of the entity's identifier
 */
public abstract class Entity<ID> {

  /**
   * Returns the unique identifier of this entity.
   *
   * @return the entity's ID, may be null for transient entities
   */
  public abstract ID getId();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Entity<?> entity = (Entity<?>) o;
    return getId() != null && Objects.equals(getId(), entity.getId());
  }

  @Override
  public int hashCode() {
    return getId() != null ? getId().hashCode() : super.hashCode();
  }
}

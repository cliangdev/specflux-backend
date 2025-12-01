package com.specflux.shared.domain;

/**
 * Base class for value objects.
 *
 * <p>Value objects are immutable objects that are defined by their attributes rather than by their
 * identity. Two value objects with the same attributes are considered equal.
 *
 * <p>Subclasses must:
 *
 * <ul>
 *   <li>Be immutable (all fields should be final)
 *   <li>Override {@code equals()} and {@code hashCode()} based on all attributes
 *   <li>Not have any identity
 * </ul>
 */
public abstract class ValueObject {

  /**
   * Value objects must implement equals based on attribute values.
   *
   * @param o the object to compare
   * @return true if the objects have the same attribute values
   */
  @Override
  public abstract boolean equals(Object o);

  /**
   * Value objects must implement hashCode based on attribute values.
   *
   * @return hash code based on attributes
   */
  @Override
  public abstract int hashCode();
}

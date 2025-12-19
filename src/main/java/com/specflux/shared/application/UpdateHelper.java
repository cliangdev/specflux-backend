package com.specflux.shared.application;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility methods for applying partial updates following the empty-string-clears convention.
 *
 * <p>Convention:
 *
 * <ul>
 *   <li>{@code null} - field not provided, don't change
 *   <li>{@code ""} (empty/blank string) - explicitly clear the field
 *   <li>non-empty value - set to that value
 * </ul>
 */
public final class UpdateHelper {

  private UpdateHelper() {}

  /**
   * Applies a nullable string update following empty-string-clears convention.
   *
   * @param value the value from request (null = don't change, blank = clear, other = set)
   * @param setter the setter to call if value should be updated
   */
  public static void applyString(String value, Consumer<String> setter) {
    if (value != null) {
      setter.accept(value.isBlank() ? null : value);
    }
  }

  /**
   * Applies a nullable reference update with entity lookup.
   *
   * @param <T> the entity type
   * @param ref the reference from request (null = don't change, blank = clear, other = lookup and
   *     set)
   * @param finder function to lookup entity by reference
   * @param setter the setter to call if value should be updated
   */
  public static <T> void applyRef(String ref, Function<String, T> finder, Consumer<T> setter) {
    if (ref != null) {
      if (ref.isBlank()) {
        setter.accept(null);
      } else {
        setter.accept(finder.apply(ref));
      }
    }
  }

  /**
   * Applies a nullable reference update that stores ID instead of entity.
   *
   * @param <T> the entity type
   * @param ref the reference from request (null = don't change, blank = clear, other = lookup and
   *     set ID)
   * @param finder function to lookup entity by reference
   * @param idExtractor function to extract ID from entity
   * @param setter the setter to call with the ID
   */
  public static <T, ID> void applyRefId(
      String ref, Function<String, T> finder, Function<T, ID> idExtractor, Consumer<ID> setter) {
    if (ref != null) {
      if (ref.isBlank()) {
        setter.accept(null);
      } else {
        T entity = finder.apply(ref);
        setter.accept(idExtractor.apply(entity));
      }
    }
  }

  /**
   * Applies a nullable value update (non-string types).
   *
   * @param <T> the value type
   * @param value the value from request (null = don't change, non-null = set)
   * @param setter the setter to call if value should be updated
   */
  public static <T> void applyValue(T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }
}

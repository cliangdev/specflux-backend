package com.specflux.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for DisplayKey value object. */
class DisplayKeyTest {

  @Test
  void shouldCreateDisplayKeyFromProjectKeyAndSequence() {
    DisplayKey displayKey = DisplayKey.of("SPEC", 42);

    assertThat(displayKey.getProjectKey()).isEqualTo("SPEC");
    assertThat(displayKey.getSequenceNumber()).isEqualTo(42);
    assertThat(displayKey.getValue()).isEqualTo("SPEC-42");
  }

  @Test
  void shouldParseValidDisplayKey() {
    DisplayKey displayKey = DisplayKey.parse("PROJ-123");

    assertThat(displayKey.getProjectKey()).isEqualTo("PROJ");
    assertThat(displayKey.getSequenceNumber()).isEqualTo(123);
    assertThat(displayKey.getValue()).isEqualTo("PROJ-123");
  }

  @Test
  void shouldAcceptNumericProjectKey() {
    DisplayKey displayKey = DisplayKey.of("P1", 1);

    assertThat(displayKey.getValue()).isEqualTo("P1-1");
  }

  @Test
  void shouldAcceptMaxLengthProjectKey() {
    DisplayKey displayKey = DisplayKey.of("ABCDEFGHIJ", 1);

    assertThat(displayKey.getProjectKey()).isEqualTo("ABCDEFGHIJ");
  }

  @Test
  void shouldRejectNullProjectKey() {
    assertThatThrownBy(() -> DisplayKey.of(null, 1))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("projectKey must not be null");
  }

  @Test
  void shouldRejectEmptyProjectKey() {
    assertThatThrownBy(() -> DisplayKey.of("", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("projectKey must not be empty");
  }

  @Test
  void shouldRejectProjectKeyExceedingMaxLength() {
    assertThatThrownBy(() -> DisplayKey.of("ABCDEFGHIJK", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not exceed 10 characters");
  }

  @Test
  void shouldRejectLowercaseProjectKey() {
    assertThatThrownBy(() -> DisplayKey.of("spec", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must contain only uppercase letters and numbers");
  }

  @Test
  void shouldRejectProjectKeyWithSpecialCharacters() {
    assertThatThrownBy(() -> DisplayKey.of("SPEC-X", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must contain only uppercase letters and numbers");
  }

  @Test
  void shouldRejectZeroSequenceNumber() {
    assertThatThrownBy(() -> DisplayKey.of("SPEC", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sequenceNumber must be positive");
  }

  @Test
  void shouldRejectNegativeSequenceNumber() {
    assertThatThrownBy(() -> DisplayKey.of("SPEC", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sequenceNumber must be positive");
  }

  @Test
  void shouldRejectNullParseValue() {
    assertThatThrownBy(() -> DisplayKey.parse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value must not be null");
  }

  @Test
  void shouldRejectInvalidParseFormat() {
    assertThatThrownBy(() -> DisplayKey.parse("SPEC42"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid display key format");
  }

  @Test
  void shouldRejectLowercaseInParseFormat() {
    assertThatThrownBy(() -> DisplayKey.parse("spec-42"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid display key format");
  }

  @Test
  void shouldImplementEqualsCorrectly() {
    DisplayKey key1 = DisplayKey.of("SPEC", 42);
    DisplayKey key2 = DisplayKey.of("SPEC", 42);
    DisplayKey key3 = DisplayKey.of("SPEC", 43);
    DisplayKey key4 = DisplayKey.of("PROJ", 42);

    assertThat(key1).isEqualTo(key2);
    assertThat(key1).isNotEqualTo(key3);
    assertThat(key1).isNotEqualTo(key4);
    assertThat(key1).isNotEqualTo(null);
    assertThat(key1).isNotEqualTo("SPEC-42");
  }

  @Test
  void shouldImplementHashCodeCorrectly() {
    DisplayKey key1 = DisplayKey.of("SPEC", 42);
    DisplayKey key2 = DisplayKey.of("SPEC", 42);

    assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
  }

  @Test
  void shouldReturnValueFromToString() {
    DisplayKey displayKey = DisplayKey.of("SPEC", 42);

    assertThat(displayKey.toString()).isEqualTo("SPEC-42");
  }

  @Test
  void shouldParseAndRecreateEquivalentKey() {
    DisplayKey original = DisplayKey.of("TASK", 999);
    DisplayKey parsed = DisplayKey.parse(original.getValue());

    assertThat(parsed).isEqualTo(original);
  }
}

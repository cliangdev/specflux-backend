package com.specflux.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for PublicId value object. */
class PublicIdTest {

  @ParameterizedTest
  @EnumSource(EntityType.class)
  void shouldGenerateValidPublicIdForAllEntityTypes(EntityType entityType) {
    PublicId publicId = PublicId.generate(entityType);

    assertThat(publicId.getValue()).startsWith(entityType.getPrefix() + "_");
    assertThat(publicId.getValue().length()).isLessThanOrEqualTo(24);
    assertThat(publicId.getValue()).matches("^[a-z]+_[a-z0-9]+$");
  }

  @Test
  void shouldGenerateUniquePublicIds() {
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      ids.add(PublicId.generate(EntityType.USER).getValue());
    }

    assertThat(ids).hasSize(1000);
  }

  @Test
  void shouldCreatePublicIdFromValidValue() {
    PublicId publicId = PublicId.of("user_abc123def456ghi7");

    assertThat(publicId.getValue()).isEqualTo("user_abc123def456ghi7");
  }

  @Test
  void shouldAcceptAllEntityPrefixes() {
    assertThat(PublicId.of("user_abc123").getValue()).startsWith("user_");
    assertThat(PublicId.of("proj_abc123").getValue()).startsWith("proj_");
    assertThat(PublicId.of("epic_abc123").getValue()).startsWith("epic_");
    assertThat(PublicId.of("task_abc123").getValue()).startsWith("task_");
    assertThat(PublicId.of("rel_abc123").getValue()).startsWith("rel_");
  }

  @Test
  void shouldRejectNullEntityType() {
    assertThatThrownBy(() -> PublicId.generate(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("entityType must not be null");
  }

  @Test
  void shouldRejectNullValue() {
    assertThatThrownBy(() -> PublicId.of(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("value must not be null");
  }

  @Test
  void shouldRejectInvalidPrefix() {
    assertThatThrownBy(() -> PublicId.of("invalid_abc123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid public ID format");
  }

  @Test
  void shouldRejectValueExceedingMaxLength() {
    String tooLong = "user_" + "a".repeat(25);

    assertThatThrownBy(() -> PublicId.of(tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not exceed 24 characters");
  }

  @Test
  void shouldRejectInvalidCharacters() {
    assertThatThrownBy(() -> PublicId.of("user_ABC123"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid public ID format");
  }

  @Test
  void shouldImplementEqualsCorrectly() {
    PublicId id1 = PublicId.of("user_abc123");
    PublicId id2 = PublicId.of("user_abc123");
    PublicId id3 = PublicId.of("user_xyz789");

    assertThat(id1).isEqualTo(id2);
    assertThat(id1).isNotEqualTo(id3);
    assertThat(id1).isNotEqualTo(null);
    assertThat(id1).isNotEqualTo("user_abc123");
  }

  @Test
  void shouldImplementHashCodeCorrectly() {
    PublicId id1 = PublicId.of("user_abc123");
    PublicId id2 = PublicId.of("user_abc123");

    assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
  }

  @Test
  void shouldReturnValueFromToString() {
    PublicId publicId = PublicId.of("user_abc123");

    assertThat(publicId.toString()).isEqualTo("user_abc123");
  }
}

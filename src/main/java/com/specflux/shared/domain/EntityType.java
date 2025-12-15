package com.specflux.shared.domain;

/** Entity types for public ID generation. */
public enum EntityType {
  USER("user"),
  PROJECT("proj"),
  EPIC("epic"),
  TASK("task"),
  RELEASE("rel"),
  API_KEY("key");

  private final String prefix;

  EntityType(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }
}

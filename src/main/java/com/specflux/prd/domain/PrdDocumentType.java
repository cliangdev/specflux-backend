package com.specflux.prd.domain;

/** Document type values for PRD supporting documents. */
public enum PrdDocumentType {
  PRD("prd"),
  WIREFRAME("wireframe"),
  MOCKUP("mockup"),
  DESIGN("design"),
  OTHER("other");

  private final String value;

  PrdDocumentType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static PrdDocumentType fromValue(String value) {
    for (PrdDocumentType type : values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown PRD document type: " + value);
  }
}

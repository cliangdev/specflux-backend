package com.specflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for SpecFlux Backend.
 *
 * <p>SpecFlux is an AI-Powered Multi-Repo Development Orchestrator that manages Claude Code agents
 * across multiple software repositories with spec-driven workflows.
 */
@SpringBootApplication
public class SpecFluxApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpecFluxApplication.class, args);
  }
}

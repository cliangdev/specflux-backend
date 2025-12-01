package com.specflux.shared.infrastructure.firebase;

import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Configuration for Firebase Admin SDK.
 *
 * <p>Supports two modes:
 *
 * <ul>
 *   <li><b>Emulator mode</b> (firebase.emulator.enabled=true): Connects to Firebase Emulator, no
 *       service account required
 *   <li><b>Production mode</b>: Requires FIREBASE_CONFIG_PATH to point to service account JSON
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

  @Value("${firebase.config-path:}")
  private String configPath;

  @Value("${firebase.emulator.enabled:false}")
  private boolean emulatorEnabled;

  @Value("${firebase.emulator.host:localhost:9099}")
  private String emulatorHost;

  /**
   * Initializes and returns the FirebaseApp instance.
   *
   * @return the FirebaseApp singleton
   * @throws IOException if the service account file cannot be read (production mode only)
   */
  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseOptions.Builder builder = FirebaseOptions.builder();

      if (emulatorEnabled) {
        // Emulator mode - use demo project with mock credentials
        System.setProperty("FIREBASE_AUTH_EMULATOR_HOST", emulatorHost);
        builder
            .setProjectId("demo-specflux")
            .setCredentials(GoogleCredentials.create(new AccessToken("emulator-token", null)));
        log.info("Firebase configured for emulator at {}", emulatorHost);
      } else {
        // Production mode - require service account
        if (configPath == null || configPath.isBlank()) {
          throw new IllegalStateException(
              "Firebase config path not set. Set FIREBASE_CONFIG_PATH environment variable.");
        }

        try (FileInputStream serviceAccount = new FileInputStream(configPath)) {
          builder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
          log.info("Firebase configured with service account from {}", configPath);
        }
      }

      return FirebaseApp.initializeApp(builder.build());
    }
    return FirebaseApp.getInstance();
  }

  /**
   * Returns the FirebaseAuth instance for token verification.
   *
   * @param firebaseApp the FirebaseApp instance
   * @return the FirebaseAuth singleton
   */
  @Bean
  public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
    return FirebaseAuth.getInstance(firebaseApp);
  }
}

package com.specflux.shared.infrastructure.firebase;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Configuration for Firebase Admin SDK.
 *
 * <p>Initializes Firebase from a service account JSON file specified by the FIREBASE_CONFIG_PATH
 * environment variable. When the config path is not set, Firebase initialization is skipped.
 */
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

  @Value("${firebase.config-path:}")
  private String configPath;

  /**
   * Initializes and returns the FirebaseApp instance.
   *
   * @return the FirebaseApp singleton
   * @throws IOException if the service account file cannot be read
   */
  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      if (configPath == null || configPath.isBlank()) {
        throw new IllegalStateException(
            "Firebase config path not set. Set FIREBASE_CONFIG_PATH environment variable.");
      }

      try (FileInputStream serviceAccount = new FileInputStream(configPath)) {
        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        return FirebaseApp.initializeApp(options);
      }
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

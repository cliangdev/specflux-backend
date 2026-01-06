package com.specflux.github.domain;

import java.time.Instant;

import com.specflux.shared.domain.AggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GitHub installation aggregate root representing a user's GitHub App installation.
 *
 * <p>Stores OAuth tokens and installation metadata for GitHub App integration.
 */
@Entity
@Table(name = "github_installations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubInstallation extends AggregateRoot<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 24)
  private String publicId;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "installation_id", nullable = false)
  private Long installationId;

  @Setter
  @Column(name = "access_token", nullable = false, length = 500)
  private String accessToken;

  @Setter
  @Column(name = "access_token_expires_at", nullable = false)
  private Instant accessTokenExpiresAt;

  @Setter
  @Column(name = "refresh_token", length = 500)
  private String refreshToken;

  @Setter
  @Column(name = "refresh_token_expires_at")
  private Instant refreshTokenExpiresAt;

  @Setter
  @Column(name = "github_username", nullable = false)
  private String githubUsername;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /**
   * Creates a new GitHub installation.
   *
   * @param publicId the public identifier
   * @param userId the user ID who owns this installation
   * @param installationId the GitHub installation ID
   * @param accessToken the OAuth access token
   * @param accessTokenExpiresAt when the access token expires
   * @param refreshToken the OAuth refresh token
   * @param refreshTokenExpiresAt when the refresh token expires
   * @param githubUsername the GitHub username
   */
  public GithubInstallation(
      String publicId,
      Long userId,
      Long installationId,
      String accessToken,
      Instant accessTokenExpiresAt,
      String refreshToken,
      Instant refreshTokenExpiresAt,
      String githubUsername) {
    this.publicId = publicId;
    this.userId = userId;
    this.installationId = installationId;
    this.accessToken = accessToken;
    this.accessTokenExpiresAt = accessTokenExpiresAt;
    this.refreshToken = refreshToken;
    this.refreshTokenExpiresAt = refreshTokenExpiresAt;
    this.githubUsername = githubUsername;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /**
   * Checks if the access token is expired or will expire within the threshold.
   *
   * @param thresholdMinutes number of minutes before expiry to consider expired
   * @return true if token needs refresh
   */
  public boolean needsTokenRefresh(int thresholdMinutes) {
    Instant threshold = Instant.now().plusSeconds(thresholdMinutes * 60L);
    return accessTokenExpiresAt.isBefore(threshold);
  }

  /**
   * Updates the OAuth tokens after refresh.
   *
   * @param newAccessToken the new access token
   * @param newAccessTokenExpiresAt when the new access token expires
   * @param newRefreshToken the new refresh token
   * @param newRefreshTokenExpiresAt when the new refresh token expires
   */
  public void updateTokens(
      String newAccessToken,
      Instant newAccessTokenExpiresAt,
      String newRefreshToken,
      Instant newRefreshTokenExpiresAt) {
    this.accessToken = newAccessToken;
    this.accessTokenExpiresAt = newAccessTokenExpiresAt;
    this.refreshToken = newRefreshToken;
    this.refreshTokenExpiresAt = newRefreshTokenExpiresAt;
    this.updatedAt = Instant.now();
  }
}

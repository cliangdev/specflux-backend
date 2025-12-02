package com.specflux.migration;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.shared.application.CurrentUserService;
import com.specflux.user.domain.User;

import lombok.RequiredArgsConstructor;

/** REST controller for migration operations. */
@RestController
@RequestMapping("/migration")
@RequiredArgsConstructor
public class MigrationController {

  private final MigrationService migrationService;
  private final CurrentUserService currentUserService;

  @PostMapping("/run")
  public ResponseEntity<MigrationResult> runMigration(@RequestBody MigrationRequest request) {
    User currentUser = currentUserService.getCurrentUser();
    MigrationResult result = migrationService.migrate(request.getSqlitePath(), currentUser);
    return ResponseEntity.ok(result);
  }

  public static class MigrationRequest {
    private String sqlitePath;

    public String getSqlitePath() {
      return sqlitePath;
    }

    public void setSqlitePath(String sqlitePath) {
      this.sqlitePath = sqlitePath;
    }
  }
}

package com.specflux.user.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.specflux.api.generated.UsersApi;
import com.specflux.api.generated.model.UpdateUserRequestDto;
import com.specflux.api.generated.model.UserDto;
import com.specflux.user.application.UserApplicationService;

import lombok.RequiredArgsConstructor;

/** REST controller for User endpoints. Implements generated OpenAPI interface. */
@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

  private final UserApplicationService userApplicationService;

  @Override
  public ResponseEntity<UserDto> getCurrentUser() {
    UserDto user = userApplicationService.getCurrentUser();
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<UserDto> updateCurrentUser(UpdateUserRequestDto request) {
    UserDto user = userApplicationService.updateCurrentUser(request);
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<UserDto> getUser(String ref) {
    UserDto user = userApplicationService.getUserByPublicId(ref);
    return ResponseEntity.ok(user);
  }
}

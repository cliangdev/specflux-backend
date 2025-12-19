package com.specflux.user.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.specflux.api.generated.model.UpdateUserRequestDto;
import com.specflux.api.generated.model.UserDto;
import com.specflux.shared.application.CurrentUserService;
import com.specflux.shared.application.UpdateHelper;
import com.specflux.user.domain.User;
import com.specflux.user.domain.UserRepository;
import com.specflux.user.interfaces.rest.UserMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/** Application service for User operations. */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

  private final UserRepository userRepository;
  private final CurrentUserService currentUserService;
  private final TransactionTemplate transactionTemplate;

  /**
   * Gets the current authenticated user, auto-provisioning if needed.
   *
   * @return the current user DTO
   */
  public UserDto getCurrentUser() {
    User user = currentUserService.getOrCreateCurrentUser();
    return UserMapper.toDto(user);
  }

  /**
   * Updates the current user's profile.
   *
   * @param request the update request
   * @return the updated user DTO
   */
  public UserDto updateCurrentUser(UpdateUserRequestDto request) {
    return transactionTemplate.execute(
        status -> {
          User user = currentUserService.getOrCreateCurrentUser();

          UpdateHelper.applyString(request.getDisplayName(), user::setDisplayName);
          UpdateHelper.applyString(request.getAvatarUrl(), user::setAvatarUrl);

          User saved = userRepository.save(user);
          return UserMapper.toDto(saved);
        });
  }

  /**
   * Gets a user by their public ID.
   *
   * @param publicId the user's public ID (usr_xxx)
   * @return the user DTO
   * @throws EntityNotFoundException if user not found
   */
  public UserDto getUserByPublicId(String publicId) {
    User user =
        userRepository
            .findByPublicId(publicId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + publicId));
    return UserMapper.toDto(user);
  }
}

package com.gradapptracker.backend.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gradapptracker.backend.user.dto.LoginResponseDTO;
import com.gradapptracker.backend.user.dto.UserRegisterRequest;
import com.gradapptracker.backend.user.dto.UserResponse;
import com.gradapptracker.backend.user.dto.UserUpdateRequest;
import com.gradapptracker.backend.user.entity.User;
import com.gradapptracker.backend.user.repository.UserRepository;
import com.gradapptracker.backend.exception.EmailAlreadyExistsException;
import com.gradapptracker.backend.exception.NotFoundException;
import com.gradapptracker.backend.exception.UnauthorizedException;
import com.gradapptracker.backend.exception.ValidationException;
import com.gradapptracker.backend.security.JwtUtils;
import com.gradapptracker.backend.security.UserPrincipal;

@Service
/**
 * Service layer for user-related business logic.
 *
 * <p>
 * This class provides transactional, application-level operations around the
 * {@code User} entity. It performs input validation, maps persistence results
 * to DTOs, enforces business rules (e.g. email uniqueness) and delegates raw
 * data access to
 * {@link com.gradapptracker.backend.user.repository.UserRepository}.
 */
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Register a new user.
     *
     * Creates a new {@code User} record. Basic field validation is handled by
     * DTO annotations. This method ensures the email is not already in use and
     * hashes the password before persistence.
     *
     * @param dto the registration payload; validated by @Valid in controller
     * @return a {@link UserResponse} representing the newly created user
     * @throws EmailAlreadyExistsException if the email is already registered
     * @implNote this method lower-cases the email before storing it.
     */
    public UserResponse register(UserRegisterRequest dto) {
        String email = dto.getEmail().trim().toLowerCase();

        // Business rule validation: check email uniqueness
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists: " + email);
        }

        User user = new User();
        user.setName(dto.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        User saved = userRepository.save(user);

        return new UserResponse(saved.getUserId(), saved.getName(), saved.getEmail());
    }

    /**
     * Authenticate a user and return a token plus user information.
     *
     * <p>
     * This method performs credential validation and intentionally returns a
     * generic {@code UnauthorizedException} on failure (rather than a
     * NotFoundException) to avoid leaking whether an email address exists in
     * the system.
     *
     * @param email       the user's email (case-insensitive)
     * @param rawPassword the plain-text password to verify
     * @return a {@link LoginResponseDTO} containing a JWT token and the
     *         authenticated user's DTO
     * @throws UnauthorizedException if authentication fails (user not found or
     *                               password mismatch)
     */
    public LoginResponseDTO login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw new UnauthorizedException("invalid credentials");
        }

        var maybe = userRepository.findByEmail(email.trim().toLowerCase());
        if (maybe.isEmpty()) {
            throw new UnauthorizedException("invalid credentials");
        }

        User user = maybe.get();

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new UnauthorizedException("invalid credentials");
        }

        UserResponse userDto = new UserResponse(user.getUserId(), user.getName(), user.getEmail());

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtUtils.generateToken(principal);

        return new LoginResponseDTO(token, userDto);
    }

    /**
     * Retrieve a user by id.
     *
     * @param id the user id to fetch
     * @return a {@link UserResponse} for the requested user
     * @throws NotFoundException if no user exists with the provided id
     */
    public UserResponse getById(Integer id) {
        var user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new NotFoundException("User not found with id: " + id);
        }
        User u = user.get();
        return new UserResponse(u.getUserId(), u.getName(), u.getEmail());
    }

    /**
     * Search users by a name fragment.
     *
     * <p>
     * This is a read-only helper which returns zero-or-more users whose
     * names contain the provided fragment (case-insensitive).
     *
     * @param part a non-blank substring to search for in user names
     * @return a list of {@link UserResponse}; empty when there are no matches
     * @throws ValidationException if {@code part} is null or blank
     */
    public java.util.List<UserResponse> searchByName(String part) {
        if (part == null || part.trim().isEmpty()) {
            throw new ValidationException("name search part is required");
        }

        java.util.List<User> users = userRepository.findByNameContainingIgnoreCase(part.trim());
        java.util.List<UserResponse> out = new java.util.ArrayList<>();
        for (User u : users) {
            out.add(new UserResponse(u.getUserId(), u.getName(), u.getEmail()));
        }
        return out;
    }

    /**
     * Suggest users by an email fragment.
     *
     * <p>
     * Useful for autocomplete or suggestion UIs. Returns a possibly empty
     * list â€” absence of matches is not an error.
     *
     * @param part a non-blank substring to search for in user emails
     * @return a list of matching {@link UserResponse}; possibly empty
     * @throws ValidationException if {@code part} is null or blank
     */
    public java.util.List<UserResponse> smartSearchByEmail(String part) {
        if (part == null || part.trim().isEmpty()) {
            throw new ValidationException("email search part is required");
        }

        java.util.List<User> users = userRepository.findByEmailContainingIgnoreCase(part.trim());
        java.util.List<UserResponse> out = new java.util.ArrayList<>();
        for (User u : users) {
            out.add(new UserResponse(u.getUserId(), u.getName(), u.getEmail()));
        }
        return out;
    }

    /**
     * Update mutable fields of an existing user.
     *
     * <p>
     * Fields not present in {@code dto} are left unchanged. Basic field validation
     * is handled by DTO annotations. This method enforces business rules like
     * email uniqueness.
     *
     * @param id  the id of the user to update
     * @param dto the payload with fields to update (name, email, password)
     * @return the updated {@link UserResponse}
     * @throws NotFoundException           if the user does not exist
     * @throws EmailAlreadyExistsException if the new email is already used by
     *                                     another account
     */
    public UserResponse updateUser(Integer id, UserUpdateRequest dto) {
        var maybe = userRepository.findById(id);
        if (maybe.isEmpty()) {
            throw new NotFoundException("User not found with id: " + id);
        }
        User user = maybe.get();

        // name
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            user.setName(dto.getName().trim());
        }

        // email - business rule: check uniqueness
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String email = dto.getEmail().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new EmailAlreadyExistsException("Email already exists: " + email);
            }
            user.setEmail(email);
        }

        // password
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        User saved = userRepository.save(user);
        return new UserResponse(saved.getUserId(), saved.getName(), saved.getEmail());
    }

    /**
     * Update a user's password.
     *
     * <p>
     * The method verifies the provided {@code oldPass} against the stored
     * hash and validates the {@code newPass} for minimal strength.
     *
     * @param id      the id of the user whose password will be changed
     * @param oldPass the current (plain-text) password
     * @param newPass the new (plain-text) password
     * @throws NotFoundException     if the user does not exist
     * @throws UnauthorizedException if {@code oldPass} does not match the
     *                               current password
     * @throws ValidationException   if {@code newPass} is too short or weak
     * @implNote password strength checks are intentionally conservative; you
     *           can replace them with an institutional policy or a library.
     */
    public void updatePassword(Integer id, String oldPass, String newPass) {
        var maybe = userRepository.findById(id);
        if (maybe.isEmpty()) {
            throw new NotFoundException("User not found with id: " + id);
        }
        User user = maybe.get();

        if (oldPass == null || !passwordEncoder.matches(oldPass, user.getPassword())) {
            throw new UnauthorizedException("invalid current password");
        }

        if (newPass == null || newPass.length() < 8) {
            throw new ValidationException("new password must be at least 8 characters");
        }

        // Add simple strength checks (one digit and one letter)
        boolean hasDigit = newPass.chars().anyMatch(Character::isDigit);
        boolean hasLetter = newPass.chars().anyMatch(Character::isLetter);
        if (!hasDigit || !hasLetter) {
            throw new ValidationException("new password must contain letters and digits");
        }

        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
    }

    /**
     * Delete a user by id with authorization check.
     *
     * @param authenticatedUserId the id of the user making the request
     * @param targetUserId        the id of the user to delete
     * @throws NotFoundException     if the user does not exist
     * @throws UnauthorizedException if the authenticated user is not authorized to
     *                               delete the target user
     * @implNote cascading deletes are handled by the database schema (ON
     *           DELETE CASCADE). The service does not attempt to translate
     *           foreign-key errors here. Users can only delete their own account.
     */
    public void deleteUser(Integer authenticatedUserId, Integer targetUserId) {
        if (authenticatedUserId == null) {
            throw new UnauthorizedException("missing or invalid token");
        }

        var targetUser = userRepository.findById(targetUserId);
        if (targetUser.isEmpty()) {
            throw new NotFoundException("User not found with id: " + targetUserId);
        }

        // Business rule: users can only delete their own account
        if (!authenticatedUserId.equals(targetUserId)) {
            throw new UnauthorizedException("not authorized to delete this user");
        }

        userRepository.deleteById(targetUserId);
    }

}

package com.gradapptracker.backend.user.controller;

import com.gradapptracker.backend.user.dto.LoginResponseDTO;
import com.gradapptracker.backend.user.dto.UserLoginRequest;
import com.gradapptracker.backend.user.dto.UserRegisterRequest;
import com.gradapptracker.backend.user.dto.UserResponse;
import com.gradapptracker.backend.user.dto.UserUpdateRequest;
import com.gradapptracker.backend.user.service.UserService;
import com.gradapptracker.backend.security.JwtUtils;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

/**
 * REST controller exposing user-related endpoints.
 *
 * <p>
 * Controllers in this project act as thin adapters that validate and map
 * incoming HTTP requests to service-layer calls. All error handling is
 * centralized in {@code GlobalExceptionHandler} so controller methods must not
 * catch or translate application exceptions.
 */
@RestController
@Validated
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final JwtUtils jwtUtils;

	public UserController(UserService userService, JwtUtils jwtUtils) {
		this.userService = userService;
		this.jwtUtils = jwtUtils;
	}

	/**
	 * Extract the authenticated user ID from the JWT token in the request header.
	 * 
	 * @param req the HTTP request containing the Authorization header
	 * @return the user ID extracted from the JWT token, or null if extraction fails
	 */
	private Integer extractUserId(HttpServletRequest req) {
		String auth = req.getHeader("Authorization");
		String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : auth;
		return jwtUtils.getUserIdFromToken(token);
	}

	/**
	 * Register a new user.
	 * 
	 * @param req request body with name, email, and password
	 * @return UserResponse DTO containing the created user's information
	 * @throws EmailAlreadyExistsException if email is already registered
	 * @throws ValidationException         if required fields are missing or invalid
	 */
	@PostMapping("/register")
	public UserResponse register(@RequestBody @Valid UserRegisterRequest req) {
		return userService.register(req);
	}

	/**
	 * Authenticate user and return token plus user info.
	 * 
	 * @param req login request with email and password
	 * @return LoginResponseDTO containing JWT token and user information
	 * @throws UnauthorizedException if credentials are invalid (user not found or
	 *                               password mismatch)
	 */
	@PostMapping("/login")
	public LoginResponseDTO login(@RequestBody @Valid UserLoginRequest req) {
		return userService.login(req.getEmail(), req.getPassword());
	}

	/**
	 * Get a user by id.
	 * 
	 * @param id the user ID to retrieve
	 * @return UserResponse DTO containing the user's information
	 * @throws NotFoundException if user doesn't exist
	 */
	@GetMapping("/{id}")
	public UserResponse getById(@PathVariable Integer id) {
		return userService.getById(id);
	}

	/**
	 * Search users by name fragment (case-insensitive contains search).
	 * 
	 * @param q the query string to search for in user names
	 * @return List of UserResponse DTOs matching the search criteria
	 */
	@GetMapping("/search")
	public List<UserResponse> searchByName(@RequestParam("q") @NotBlank String q) {
		return userService.searchByName(q);
	}

	/**
	 * Suggest users by email fragment (smart search supporting partial email
	 * matching).
	 * 
	 * @param q the query string to search for in email addresses
	 * @return List of UserResponse DTOs with matching email addresses
	 */
	@GetMapping("/suggest/email")
	public List<UserResponse> suggestByEmail(@RequestParam("q") @NotBlank String q) {
		return userService.smartSearchByEmail(q);
	}

	/**
	 * Update a user's information (name and/or email).
	 * 
	 * @param id  the user ID to update
	 * @param dto the update payload containing fields to modify
	 * @return UserResponse DTO with the updated user information
	 * @throws NotFoundException           if user doesn't exist
	 * @throws EmailAlreadyExistsException if new email is already in use
	 */
	@PutMapping("/{id}")
	public UserResponse updateUser(@PathVariable Integer id, @RequestBody @Valid UserUpdateRequest dto) {
		return userService.updateUser(id, dto);
	}

	/**
	 * Update password for a user.
	 * 
	 * @param id      the user ID whose password to change
	 * @param oldPass the current password for verification
	 * @param newPass the new password to set
	 * @throws NotFoundException     if user doesn't exist
	 * @throws UnauthorizedException if old password is incorrect
	 * @throws ValidationException   if new password doesn't meet requirements
	 */
	@PostMapping("/{id}/password")
	public void updatePassword(@PathVariable Integer id, @RequestParam("old") @NotBlank String oldPass,
			@RequestParam("new") @NotBlank String newPass) {
		userService.updatePassword(id, oldPass, newPass);
	}

	/**
	 * Delete a user by id. Authorization check is performed in the service layer.
	 * 
	 * @param req the HTTP request containing the Authorization header
	 * @param id  the user ID to delete
	 * @throws UnauthorizedException if authenticated user doesn't match the target
	 *                               user
	 * @throws NotFoundException     if user doesn't exist
	 */
	@DeleteMapping("/{id}")
	public void deleteUser(HttpServletRequest req, @PathVariable Integer id) {
		Integer authenticatedUserId = extractUserId(req);
		userService.deleteUser(authenticatedUserId, id);
	}

}

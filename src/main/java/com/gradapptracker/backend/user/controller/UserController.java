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

	private Integer extractUserId(HttpServletRequest req) {
		String auth = req.getHeader("Authorization");
		String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : auth;
		return jwtUtils.getUserIdFromToken(token);
	}

	/**
	 * Register a new user.
	 * 
	 * @param req request body with name/email/password
	 * @return created user DTO
	 */
	@PostMapping("/register")
	public UserResponse register(@RequestBody @Valid UserRegisterRequest req) {
		return userService.register(req);
	}

	/**
	 * Authenticate user and return token + user info.
	 * 
	 * @param req login request with email and password
	 * @return login response containing JWT and user DTO
	 */
	@PostMapping("/login")
	public LoginResponseDTO login(@RequestBody @Valid UserLoginRequest req) {
		return userService.login(req.getEmail(), req.getPassword());
	}

	/**
	 * Get a user by id.
	 */
	@GetMapping("/{id}")
	public UserResponse getById(@PathVariable Integer id) {
		return userService.getById(id);
	}

	/**
	 * Search users by name fragment.
	 */
	@GetMapping("/search")
	public List<UserResponse> searchByName(@RequestParam("q") @NotBlank String q) {
		return userService.searchByName(q);
	}

	/**
	 * Suggest users by email fragment.
	 */
	@GetMapping("/suggest/email")
	public List<UserResponse> suggestByEmail(@RequestParam("q") @NotBlank String q) {
		return userService.smartSearchByEmail(q);
	}

	/**
	 * Update a user.
	 */
	@PutMapping("/{id}")
	public UserResponse updateUser(@PathVariable Integer id, @RequestBody @Valid UserUpdateRequest dto) {
		return userService.updateUser(id, dto);
	}

	/**
	 * Update password for a user.
	 */
	@PostMapping("/{id}/password")
	public void updatePassword(@PathVariable Integer id, @RequestParam("old") @NotBlank String oldPass,
			@RequestParam("new") @NotBlank String newPass) {
		userService.updatePassword(id, oldPass, newPass);
	}

	/**
	 * Delete a user by id. Authorization check is performed in the service layer.
	 */
	@DeleteMapping("/{id}")
	public void deleteUser(HttpServletRequest req, @PathVariable Integer id) {
		Integer authenticatedUserId = extractUserId(req);
		userService.deleteUser(authenticatedUserId, id);
	}

}

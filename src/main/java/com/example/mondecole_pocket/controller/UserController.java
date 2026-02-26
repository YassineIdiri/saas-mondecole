package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.DashboardStatsResponse;
import com.example.mondecole_pocket.dto.PageResponse;
import com.example.mondecole_pocket.dto.UserResponse;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(userService.getDashboardStats());
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<PageResponse<UserResponse>> listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean active) {

    PageResponse<UserResponse> response = userService.listStudents(page, size, active);
    return ResponseEntity.ok(response);
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> listTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean active) {

        PageResponse<UserResponse> response = userService.listTeachers(page, size, active);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active
    ){
        PageResponse<UserResponse> response = userService.listUsers(page, size, role, active);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.toggleUserStatus(userId));
    }

    @PatchMapping("/{userId}/toggle-lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleLock(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.toggleLocked(userId));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}

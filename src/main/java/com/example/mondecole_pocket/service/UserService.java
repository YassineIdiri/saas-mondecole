package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.Organization;
import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.exception.OrganizationNotFoundException;
import com.example.mondecole_pocket.exception.UserNotFoundException;
import com.example.mondecole_pocket.repository.CourseEnrollmentRepository;
import com.example.mondecole_pocket.repository.OrganizationRepository;
import com.example.mondecole_pocket.repository.UserRepository;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;

    private Organization getCurrentOrganization(){
        Long organizationId = TenantContext.getTenantId();

        if(organizationId == null){
            throw new IllegalStateException("Organization context not set");
        }
        return organizationRepository.findById(organizationId).orElseThrow(() -> new OrganizationNotFoundException("" +
                "Organization not found" + organizationId));
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(){
        Organization organization = getCurrentOrganization();

        long totalUsers = userRepository.countByOrganization(organization);
        long totalTeachers = userRepository.countByOrganizationAndRole(organization, UserRole.TEACHER);
        long totalStudent = userRepository.countByOrganizationAndRole(organization, UserRole.STUDENT);
        long activeUsers = userRepository.countByOrganizationAndActive(organization, true);
        long lockedUsers = userRepository.countByOrganizationAndLocked(organization, true);

        OrganizationBasicInfo organizationBasicInfo = new OrganizationBasicInfo(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getLogoUrl());

        return new DashboardStatsResponse(totalUsers, totalTeachers, totalStudent, activeUsers, lockedUsers, organizationBasicInfo);
    }


    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listStudents(int page, int size, Boolean active) {
        Organization organization = getCurrentOrganization();

        if(size > 100) size = 100;
        if(size < 1) size = 1;
        if(size < 0) size = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> usersPage;

        if (active != null) {
            usersPage = userRepository.findByOrganizationAndRoleAndActive(
                    organization,
                    UserRole.STUDENT,
                    active,
                    pageable
            );
        } else {
            usersPage = userRepository.findByOrganizationAndRole(
                    organization,
                    UserRole.STUDENT,
                    pageable
            );
        }

        log.debug("Found {} students (total: {})",
                usersPage.getNumberOfElements(), usersPage.getTotalElements());

        return PageResponse.from(usersPage, UserResponse::from);
    }

    @Transactional
    public PageResponse<UserResponse> listTeachers(int page, int size, Boolean active) {
        Organization organization = getCurrentOrganization();

        if(size > 100) size = 100;
        if(size < 1) size = 1;
        if(size < 0) size = 0;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> usersPage = (active != null) ? userRepository.findByOrganizationAndRoleAndActive(organization, UserRole.TEACHER, active,pageable) :
                                                  userRepository.findByOrganizationAndRole(organization, UserRole.TEACHER, pageable);

        return PageResponse.from(usersPage, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(int page, int size, UserRole role, Boolean active) {
        Organization organization = getCurrentOrganization();

        if(size > 100) size = 100;
        if(size < 1) size = 1;
        if(size < 0) size = 0;
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> usersPage;

        if (role != null && active != null) {
            usersPage = userRepository.findByOrganizationAndRoleAndActive(
                    organization, role, active, pageable);
        } else if (role != null) {
            usersPage = userRepository.findByOrganizationAndRole(
                    organization, role, pageable);
        } else if (active != null) {
            usersPage = userRepository.findByOrganizationAndActive(
                    organization, active, pageable);
        } else {
            usersPage = userRepository.findByOrganization(organization, pageable);
        }

        return PageResponse.from(usersPage, UserResponse::from);
    }

    @Transactional
    public void deleteUser(Long userId) {
        Organization organization = getCurrentOrganization();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found" + userId));

        if (!user.getOrganization().getId().equals(organization.getId())) {
            throw new UserNotFoundException("User not found" + userId);
        }

        if (user.getRole().equals(UserRole.ADMIN)) {
            throw new IllegalStateException("Cannot delete admin user");
        }

        userRepository.delete(user);
    }

    @Transactional
    public UserResponse toggleLocked(Long userId) {
        Organization organization = getCurrentOrganization();

        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found" + userId));

        if(!user.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalStateException("User not found" + userId);
        }

        user.setLocked(!user.isLocked());
        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        Organization organization = getCurrentOrganization();

        User user = userRepository.findById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found" + userId));

        if(!user.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalStateException("User not found" + userId);
        }

        user.setActive(!user.isActive());
        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }
}
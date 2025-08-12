package api.v2.cms.user.service;

import java.util.List;

import api.v2.cms.user.dto.UserRoleDto;

public interface UserRoleService {
    UserRoleDto createRole(UserRoleDto roleDto);

    UserRoleDto updateRole(Long roleId, UserRoleDto roleDto);

    void deleteRole(Long roleId);

    UserRoleDto getRole(Long roleId);

    List<UserRoleDto> getAllRoles();

    void assignRoleToUser(Long roleId, String userId);

    void removeRoleFromUser(Long roleId, String userId);
}
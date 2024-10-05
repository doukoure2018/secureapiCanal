package secure.canal.campaigns.service;

import secure.canal.campaigns.payload.RoleDto;

import java.util.List;

public interface RoleService {

    RoleDto getRoleByUserId(Long id);
    List<RoleDto> getRoles();
}

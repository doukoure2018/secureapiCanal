package secure.canal.campaigns.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import secure.canal.campaigns.entity.Role;
import secure.canal.campaigns.payload.RoleDto;
import secure.canal.campaigns.repository.RoleRepository;
import secure.canal.campaigns.repository.UserRoleRepository;
import secure.canal.campaigns.service.RoleService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository rolesRepository;
    private final ModelMapper mapper;

    @Override
    public RoleDto getRoleByUserId(Long id) {
        log.info("Fetching role for user Id : "+ id);
        return rolesRepository.getRoleByUserId(id);
    }

    @Override
    public List<RoleDto> getRoles() {
        List<Role> roles=rolesRepository.findAll();
        return roles.stream().map(roles1 -> mapper.map(roles1,RoleDto.class)).collect(Collectors.toList());
    }


}


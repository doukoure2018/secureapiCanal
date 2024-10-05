package secure.canal.campaigns.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import secure.canal.campaigns.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole,Long> {

    UserRole findUserRolesByUserId(Long userId);
}

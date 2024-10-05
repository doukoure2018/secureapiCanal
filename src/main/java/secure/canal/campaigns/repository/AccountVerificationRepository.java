package secure.canal.campaigns.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import secure.canal.campaigns.entity.AccountVerification;

public interface AccountVerificationRepository extends JpaRepository<AccountVerification,Long> {
}

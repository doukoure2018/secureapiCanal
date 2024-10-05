package secure.canal.campaigns.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import secure.canal.campaigns.entity.ResetPasswordVerification;

public interface ResetPasswordVerificationRepository extends JpaRepository<ResetPasswordVerification,Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ResetPasswordVerification r WHERE r.user.id = :userId")
    void deleteByUserId(Long userId);
}

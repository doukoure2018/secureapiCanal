package secure.canal.campaigns.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import secure.canal.campaigns.entity.TwoFactorVerification;

import java.util.Optional;

public interface TwoFactorVerificationRepository extends JpaRepository<TwoFactorVerification,Long> {

    void deleteById(Long id);
    @Query("SELECT CASE WHEN t.expirationDate < CURRENT_TIMESTAMP THEN true ELSE false END " +
            "FROM TwoFactorVerification t WHERE t.code = :code")
    Boolean isCodeExpired(@Param("code") String code);

    void deleteByCode(String code);

    Optional<TwoFactorVerification> findByCode(String code);
}

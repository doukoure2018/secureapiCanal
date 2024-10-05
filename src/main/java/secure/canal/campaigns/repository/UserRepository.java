package secure.canal.campaigns.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import secure.canal.campaigns.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(Long idUser);


    User findByTwoFactorVerification_Code(String code);

    Integer countUsersByEmail(String email);
//    Optional<Users> findUsersByAccountVerifications_Url(String url);

    @Query("SELECT u FROM User u WHERE u.id = (SELECT r.user.id FROM ResetPasswordVerification r WHERE r.url = :url)")
    User findByResetPasswordVerificationUrl(@Param("url") String url);

    @Query("SELECT u FROM User u WHERE u.id = (SELECT r.user.id FROM AccountVerification r WHERE r.url = :url)")
    User findByAccountVerificationUrl(@Param("url") String url);

    @Query("SELECT CASE WHEN r.expirationDate < CURRENT_TIMESTAMP THEN true ELSE false END FROM ResetPasswordVerification r WHERE r.url = :url")
    boolean isExpired(@Param("url") String url);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.firstName = :firstName, u.lastName = :lastName, u.email = :email, u.phone = :phone, u.address = :address, u.title = :title, u.bio = :bio WHERE u.id = :id")
    int updateUserDetails(@Param("firstName") String firstName,
                          @Param("lastName") String lastName,
                          @Param("email") String email,
                          @Param("phone") String phone,
                          @Param("address") String address,
                          @Param("title") String title,
                          @Param("bio") String bio,
                          @Param("id") Long id);
}

package secure.canal.campaigns.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "first_name", nullable = false,length = 50)
    private String firstName;
    @Column(name = "last_name", nullable = false,length = 50)
    private String lastName;


    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "bio", length = 255)
    private String bio;

    @Column(name = "enabled", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean enabled;

    @Column(name = "non_locked", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean nonLocked;

    @Column(name = "using_mfa", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean usingMfa;

    @Column(name = "created_at", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "image_url", length = 255, columnDefinition = "VARCHAR(255) DEFAULT 'https://cdn-icons-png.flaticon.com/512/149/149071.png'")
    private String imageUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserEvent> userEvent=new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private AccountVerification accountVerification;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private ResetPasswordVerification resetPasswordVerification;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private TwoFactorVerification twoFactorVerification;



}

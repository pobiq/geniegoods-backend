package com.example.geniegoods.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TB_USER")
@EntityListeners(AuditingEntityListener.class)
@ToString
@Builder
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String nickname;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String socialType;

    @Column(nullable = false)
    private String socialId;

    @Builder.Default
    @Column
    private String profileUrl = null;

    @Builder.Default
    @Column(nullable = false, length = 50)
    @org.hibernate.annotations.ColumnDefault("'USER'")
    private String role = "USER";
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'FREE'")
    @Builder.Default
    private String subscriptionPlan = "FREE";

    /**
     * 사용자 권한 반환
     * @return 사용자 권한
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isEmpty()) {
            return Collections.emptyList();
        }
        // role이 단일 문자열이므로 리스트로 변환
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    /**
     * 사용자 이름(닉네임) 반환
     * @return 사용자 이름(닉네임)
     */
    @Override
    public String getUsername() {
        return nickname;
    }

    /**
     * 자격 증명(비밀번호) 반환
     * @return 자격 증명(비밀번호)
     */
    @Override
    public String getPassword() {
        // OAuth2를 사용하는 경우 password가 없으므로 null 반환
        return null;
    }

    /**
     * 계정이 만료되지 않았는지 확인
     * @return true: 계정이 만료되지 않음, false: 계정이 만료됨
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정이 잠겨있지 않은지 확인
     * @return true: 계정이 잠기지 않음, false: 계정이 잠김
     */
    @Override
    public boolean isAccountNonLocked() {
        return !deleted;
    }

    /**
     * 자격 증명(비밀번호)이 만료되지 않았는지 확인
     * @return true: 자격 증명이 유효함, false: 자격 증명이 만료됨
     */
    @Override
    public boolean isCredentialsNonExpired() {
    	return true;
//        return true;
    }

    /**
     * 계정이 활성화되어 있는지 확인
     * @return true: 계정이 활성화됨, false: 계정이 비활성화됨
     */
    @Override
    public boolean isEnabled() {
        return !deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity user = (UserEntity) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

}

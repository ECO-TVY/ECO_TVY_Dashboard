package com.ecotvy.application.service;

import com.ecotvy.application.dto.UserDto;
import com.ecotvy.application.dto.UserProfileUpdateRequest;
import com.ecotvy.application.port.out.UserRepositoryPort;
import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import com.ecotvy.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 내 정보 수정: 주소와 좌표를 한 요청·한 트랜잭션으로 저장 */
class UserManagementServiceProfileTest {

    private UserRepositoryPort users;
    private UserManagementService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        service = new UserManagementService(users, mock(PasswordEncoder.class));
        user = User.builder()
                .id(1L).email("a@b.c").nickname("old").authProvider(AuthProvider.EMAIL)
                .address("대전 대덕구 오정동").latitude(36.0).longitude(127.0)
                .build();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void 주소와_좌표를_함께_저장() {
        UserDto result = service.updateProfile(1L, UserProfileUpdateRequest.builder()
                .nickname("  새닉네임 ")
                .address("경기 수원시 영통구 매탄동")
                .detailAddress("경기 수원시 영통구 매탄로 1")
                .latitude(37.26).longitude(127.04)
                .build());

        assertThat(result.getNickname()).isEqualTo("새닉네임");
        assertThat(result.getAddress()).isEqualTo("경기 수원시 영통구 매탄동");
        assertThat(result.getLatitude()).isEqualTo(37.26);
        assertThat(result.getLongitude()).isEqualTo(127.04);
    }

    @Test
    void 좌표를_보내지_않으면_기존_좌표_유지() {
        UserDto result = service.updateProfile(1L, UserProfileUpdateRequest.builder().nickname("새닉네임").build());

        assertThat(result.getLatitude()).isEqualTo(36.0);
        assertThat(result.getAddress()).isEqualTo("대전 대덕구 오정동");
    }

    @Test
    void 좌표를_하나만_보내면_거부하고_아무것도_저장하지_않음() {
        assertThatThrownBy(() -> service.updateProfile(1L, UserProfileUpdateRequest.builder()
                .address("경기 수원시 영통구 매탄동").latitude(37.26).build()))
                .isInstanceOf(BusinessException.class);

        verify(users, never()).save(any());
        assertThat(user.getAddress()).isEqualTo("대전 대덕구 오정동");
    }

    @Test
    void 범위를_벗어난_좌표는_거부() {
        assertThatThrownBy(() -> service.updateProfile(1L, UserProfileUpdateRequest.builder()
                .latitude(91.0).longitude(127.0).build()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.updateLocation(1L, 37.0, 181.0))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 빈_닉네임은_거부() {
        assertThatThrownBy(() -> service.updateProfile(1L, UserProfileUpdateRequest.builder().nickname("  ").build()))
                .isInstanceOf(BusinessException.class);
    }
}

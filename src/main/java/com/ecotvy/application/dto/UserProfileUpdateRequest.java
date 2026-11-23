package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    private String nickname;
    private String address;
    private String detailAddress;
    // 주소를 바꿀 때 좌표도 같은 요청으로 보내 한 트랜잭션에서 저장한다 (둘 다 보내거나 둘 다 생략)
    private Double latitude;
    private Double longitude;
}


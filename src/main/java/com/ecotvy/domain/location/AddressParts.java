package com.ecotvy.domain.location;

import java.util.Arrays;
import java.util.Optional;

/**
 * 사용자 주소 문자열 "시도 시군구 동"을 배출 규정 조회 키(city, district, dong)로 나눈다.
 *
 * <p>프론트엔드는 주소를 [시도, 시군구, 동]을 공백으로 이어 보낸다. 시군구는 "수원시 영통구"처럼
 * 두 단어일 수 있으므로 첫 단어 = 시도, 마지막 단어 = 동, 그 사이 전부 = 시군구로 본다.
 * (예전처럼 앞 세 단어를 쓰면 "경기 수원시 영통구 매탄동"의 동이 "영통구"로 잘못 잡힌다)
 *
 * <p>세 조각 미만(예: 시군구가 없는 "세종 한솔동")은 규정을 찾지 않는다.
 */
public record AddressParts(String city, String district, String dong) {

    public static Optional<AddressParts> parse(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        String[] tokens = address.trim().split("\\s+");
        if (tokens.length < 3) {
            return Optional.empty();
        }
        String city = tokens[0];
        String dong = tokens[tokens.length - 1];
        String district = String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length - 1));
        return Optional.of(new AddressParts(city, district, dong));
    }
}

package com.ecotvy.domain.location;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressPartsTest {

    @Test
    void 시도_시군구_동_세_조각() {
        assertThat(AddressParts.parse("대전 대덕구 오정동"))
                .contains(new AddressParts("대전", "대덕구", "오정동"));
    }

    @Test
    void 시군구가_두_단어면_가운데를_모두_시군구로() {
        assertThat(AddressParts.parse("경기 수원시 영통구 매탄동"))
                .contains(new AddressParts("경기", "수원시 영통구", "매탄동"));
    }

    @Test
    void 앞뒤_공백과_연속_공백은_무시() {
        assertThat(AddressParts.parse("  경기  성남시 분당구   정자동 "))
                .contains(new AddressParts("경기", "성남시 분당구", "정자동"));
    }

    @Test
    void 세_조각_미만이거나_비어_있으면_조회하지_않음() {
        assertThat(AddressParts.parse("세종 한솔동")).isEmpty();
        assertThat(AddressParts.parse("")).isEmpty();
        assertThat(AddressParts.parse("   ")).isEmpty();
        assertThat(AddressParts.parse(null)).isEmpty();
    }
}

package com.ecotvy.application.service;

import com.ecotvy.application.dto.ClassificationHistoryDto;
import com.ecotvy.application.port.out.AdminRepositoryPort;
import com.ecotvy.application.port.out.ClassificationRepositoryPort;
import com.ecotvy.application.port.out.ImageStoragePort;
import com.ecotvy.domain.user.AuthProvider;
import com.ecotvy.domain.user.User;
import com.ecotvy.domain.waste.Classification;
import com.ecotvy.domain.waste.WasteType;
import com.ecotvy.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassificationHistoryServiceTest {

    private ClassificationRepositoryPort classifications;
    private AdminRepositoryPort admin;
    private ClassificationHistoryService service;
    private User user;

    @BeforeEach
    void setUp() {
        classifications = mock(ClassificationRepositoryPort.class);
        admin = mock(AdminRepositoryPort.class);
        ImageStoragePort images = mock(ImageStoragePort.class);
        when(images.presignedUrl(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new ClassificationHistoryService(classifications, admin, images);
        user = User.builder().id(7L).email("a@b.c").nickname("u").authProvider(AuthProvider.EMAIL).build();
    }

    private Classification classification(long id, WasteType type, Boolean isCorrect) {
        return Classification.builder()
                .id(id).user(user).imageUrl("s3://img/" + id).wasteType(type).confidence(0.8)
                .aiModelVersion("v1").disposalDay("월·수·금").disposalTime("18:00 ~ 24:00")
                .isCorrect(isCorrect)
                .build();
    }

    @Test
    void 종류_필터가_없으면_전체_조회_있으면_종류별_조회() {
        when(classifications.findByUserIdOrderByCreateDtDesc(anyLong(), anyInt(), anyInt())).thenReturn(List.of());
        when(classifications.findByUserIdAndWasteTypeOrderByCreateDtDesc(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.getHistory(7L, 0, 20, null);
        service.getHistory(7L, 0, 20, " ");
        service.getHistory(7L, 1, 20, "PLASTIC");

        // null과 공백은 둘 다 '필터 없음'
        verify(classifications, times(2)).findByUserIdOrderByCreateDtDesc(7L, 0, 20);
        verify(classifications).findByUserIdAndWasteTypeOrderByCreateDtDesc(7L, WasteType.PLASTIC, 1, 20);
    }

    @Test
    void 페이지_크기는_1에서_100으로_제한() {
        when(classifications.findByUserIdOrderByCreateDtDesc(anyLong(), anyInt(), anyInt())).thenReturn(List.of());

        service.getHistory(7L, -3, 100_000, null);
        service.getHistory(7L, 0, 0, null);

        verify(classifications).findByUserIdOrderByCreateDtDesc(7L, 0, ClassificationHistoryService.MAX_PAGE_SIZE);
        verify(classifications).findByUserIdOrderByCreateDtDesc(7L, 0, 1);
    }

    @Test
    void 알_수_없는_종류는_거부() {
        assertThatThrownBy(() -> service.getHistory(7L, 0, 20, "NOPE")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 응답에_배출_시간과_신고_여부_포함() {
        when(classifications.findByUserIdOrderByCreateDtDesc(anyLong(), anyInt(), anyInt())).thenReturn(List.of(
                classification(1, WasteType.CAN, null),
                classification(2, WasteType.PAPER, false)));

        List<ClassificationHistoryDto> result = service.getHistory(7L, 0, 20, null);

        assertThat(result).extracting(ClassificationHistoryDto::getDisposalTime).containsOnly("18:00 ~ 24:00");
        assertThat(result).extracting(ClassificationHistoryDto::getReported).containsExactly(false, true);
    }

    @Test
    void 이미_신고한_기록은_다시_신고할_수_없음() {
        when(classifications.findById(2L)).thenReturn(Optional.of(classification(2, WasteType.PAPER, false)));

        assertThatThrownBy(() -> service.reportMisclassification(2L, 7L, "PLASTIC"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 신고");
        verify(admin, never()).save(any(com.ecotvy.domain.admin.MisclassificationReport.class));
    }

    @Test
    void 처음_신고하면_검수_대기열에_등록() {
        Classification c = classification(1, WasteType.CAN, null);
        when(classifications.findById(1L)).thenReturn(Optional.of(c));

        service.reportMisclassification(1L, 7L, "PLASTIC");

        assertThat(c.getIsCorrect()).isFalse();
        verify(admin).save(any(com.ecotvy.domain.admin.MisclassificationReport.class));
        verify(classifications).save(eq(c));
    }
}

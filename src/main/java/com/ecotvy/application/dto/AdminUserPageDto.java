package com.ecotvy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminUserPageDto {
    private List<AdminUserDto> users;
    /** 다음 페이지가 있는지 ("더 보기" 표시용) */
    private boolean hasMore;
}

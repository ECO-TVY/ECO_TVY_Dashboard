package com.ecotvy.application.port.in;

public interface PasswordResetUseCase {

    /**
     * 재설정 링크 메일 요청. 가입 여부를 드러내지 않도록 항상 정상 종료한다
     * (없는 이메일·비활성 계정·쿨다운 중이어도 조용히 무시).
     */
    void requestReset(String email);

    /** 메일 링크의 토큰으로 새 비밀번호 설정. 토큰이 없거나 만료·사용됨이면 예외(3007) */
    void confirmReset(String token, String newPassword);

    /**
     * 관리자 초기화: 임시 비밀번호로 바꾸고 다음 로그인 때 변경을 강제한다.
     * @return 임시 비밀번호 (응답으로 한 번만 전달, 저장하지 않음)
     */
    String resetByAdmin(Long actorId, Long targetUserId);
}

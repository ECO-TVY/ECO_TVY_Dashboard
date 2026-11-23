-- 관리자가 만든 관리자 계정은 주소·좌표가 없을 수 있도록 users의 주소 관련 컬럼을 NULL 허용으로 변경.
-- spring.jpa.hibernate.ddl-auto=update 는 기존 컬럼의 NOT NULL을 풀지 않으므로,
-- 2026-11-23 이전에 만들어진 DB에서는 이 SQL을 한 번 실행해야 한다. (새로 만든 DB는 불필요)
-- 실행하지 않으면 POST /api/admin/users (관리자 계정 생성)가 500으로 실패한다.
-- 일반 회원가입은 서비스에서 계속 주소·좌표를 필수로 검증한다.

ALTER TABLE users
    MODIFY address   VARCHAR(255) NULL,
    MODIFY latitude  DOUBLE       NULL,
    MODIFY longitude DOUBLE       NULL;

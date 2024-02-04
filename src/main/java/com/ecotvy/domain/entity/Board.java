package com.ecotvy.domain.entity;

import com.ecotvy.global.enums.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "imageSet")
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bno;

    @Column(length = 500, nullable = false)
    private String title;

    @Column(length = 2000, nullable = false)
    private String content;

    private String boardType;

    private String writer;

    @Column
    private Long fileId;

    /*
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 20) // 데이터 지연로딩 - 성능 최적화 목적, 이후 스프링 배치 활용할 예정
    */


    public void change(String title, String content) {
        this.title = title;
        this.content = content;
    }

    @Builder
    public Board(Long bno, String title, String content, String writer, Long fileId) {
        this.bno = bno;
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.fileId = fileId;
    }
}

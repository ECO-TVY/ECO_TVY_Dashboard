package com.ecotvy.adapter.out.persistence;

import com.ecotvy.application.port.out.ClassificationRepositoryPort;
import com.ecotvy.domain.waste.Classification;
import com.ecotvy.adapter.out.persistence.repository.ClassificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClassificationJpaAdapter implements ClassificationRepositoryPort {

    private final ClassificationJpaRepository classificationJpaRepository;

    @Override
    public Classification save(Classification classification) {
        return classificationJpaRepository.save(classification);
    }

    @Override
    public Optional<Classification> findById(Long id) {
        return classificationJpaRepository.findById(id);
    }

    @Override
    public List<Classification> findByUserIdOrderByCreateDtDesc(Long userId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return classificationJpaRepository.findByUserIdOrderByCreateDtDesc(userId, pageable);
    }

    @Override
    public Long countByUserId(Long userId) {
        return classificationJpaRepository.countByUserId(userId);
    }
}


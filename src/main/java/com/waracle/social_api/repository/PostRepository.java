package com.waracle.social_api.repository;

import com.waracle.social_api.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    long countByAuthor_IdAndDeletedFalse(Long authorId);

    Optional<Post> findByIdAndDeletedFalse(Long id);

    Optional<Post> findByIdAndAuthor_Id(Long id, Long authorId);

    @EntityGraph(attributePaths = "author")
    List<Post> findByDeletedFalseOrderByIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    List<Post> findByDeletedFalseAndIdLessThanOrderByIdDesc(Long id, Pageable pageable);

    @Query("""
            SELECT p.id FROM Post p
            WHERE p.deleted = true
              AND p.deletedAt IS NOT NULL
              AND p.deletedAt < :cutoff
            """)
    List<Long> findExpiredDeletedPostIds(@Param("cutoff") LocalDateTime cutoff);

    int deleteByIdIn(List<Long> ids);
}

package com.waracle.social_api.repository;

import com.waracle.social_api.entity.PostLike;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    long countByPost_Author_Id(Long authorId);

    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);

    void deleteByUser_IdAndPost_Id(Long userId, Long postId);

    void deleteByPost_IdIn(Collection<Long> postIds);

    @EntityGraph(attributePaths = {"user", "post"})
    List<PostLike> findByPost_IdIn(Collection<Long> postIds);
}

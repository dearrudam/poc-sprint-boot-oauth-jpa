package com.example.demo;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	Collection<Comment> findByPostId(Long postId);

	Collection<Comment> findByParentCommentId(Long parentCommentId);

}

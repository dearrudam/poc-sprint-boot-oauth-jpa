package com.example.demo;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

	public Collection<Post> findByAuthorUsername(String username);

}

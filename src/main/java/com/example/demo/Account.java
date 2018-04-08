package com.example.demo;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Account {

	@Id
	@GeneratedValue
	private Long id;

	private String username;

	@JsonIgnore
	private String password;

	@OneToMany(mappedBy = "author")
	private Set<Post> posts = new LinkedHashSet<>();

	@OneToMany(mappedBy = "author")
	private Set<Comment> comments = new LinkedHashSet<>();

	@SuppressWarnings("unused")
	private Account() {
		// JPA only
	}

	public Account(final String username, final String password) {
		this.username = username;
		this.password = password;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public Set<Post> getPosts() {
		return posts;
	}

	public Set<Comment> getComments() {
		return comments;
	}
}

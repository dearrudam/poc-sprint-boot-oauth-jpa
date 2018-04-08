package com.example.demo;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Post {

	@Id
	@GeneratedValue
	private Long id;

	@JsonIgnore
	@ManyToOne
	private Account author;

	@Temporal(TemporalType.TIMESTAMP)
	private Date postedOn;

	private String subject;

	private String content;

	@JsonIgnore
	@OneToMany(mappedBy = "author")
	private Set<Comment> comments = new LinkedHashSet<>();

	@SuppressWarnings("unused")
	private Post() {
		// JPA only
	}

	public Post(Account author, String subject, String content) {
		this(author, new Date(), subject, content);
	}

	public Post(Account author, Date postedOn, String subject, String content) {
		super();
		this.author = author;
		this.postedOn = postedOn;
		this.subject = subject;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public Account getAuthor() {
		return author;
	}

	public String getSubject() {
		return subject;
	}

	public String getContent() {
		return content;
	}

	public Date getPostedOn() {
		return postedOn;
	}

	public Set<Comment> getComments() {
		return comments;
	}

}

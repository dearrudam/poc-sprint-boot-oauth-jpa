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
public class Comment {

	@Id
	@GeneratedValue
	private Long id;

	@JsonIgnore
	@ManyToOne
	private Post post;

	@ManyToOne
	@JsonIgnore
	private Comment parentComment;

	@JsonIgnore
	@ManyToOne
	private Account author;

	@Temporal(TemporalType.TIMESTAMP)
	private Date commentedOn;

	private String content;

	@JsonIgnore
	@OneToMany(mappedBy = "parentComment")
	private Set<Comment> repliedComments = new LinkedHashSet<>();

	@SuppressWarnings("unused")
	private Comment() {
		// JPA Only
	}

	public Comment(Post post, Account author, String content) {
		this(post, author, new Date(), content);
	}

	public Comment(Comment parentComment, Account author, String content) {
		this(parentComment, author, new Date(), content);
	}

	public Comment(Post post, Account author, Date commentedOn, String content) {
		super();
		this.post = post;
		this.author = author;
		this.commentedOn = commentedOn;
		this.content = content;
	}

	public Comment(Comment parentComment, Account author, Date commentedOn, String content) {
		super();
		this.parentComment = parentComment;
		this.author = author;
		this.commentedOn = commentedOn;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public Post getPost() {
		return post != null ? post : getParentComment().getPost();
	}

	public Comment getParentComment() {
		return parentComment;
	}

	public Account getAuthor() {
		return author;
	}

	public Date getCommentedOn() {
		return commentedOn;
	}

	public String getContent() {
		return content;
	}

}

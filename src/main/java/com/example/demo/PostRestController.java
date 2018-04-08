package com.example.demo;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/posts")
@Transactional
public class PostRestController {

	private final AccountRepository accountRepository;

	private final PostRepository postRepository;

	private final CommentRepository commentRepository;

	@Autowired
	public PostRestController(AccountRepository accountRepository,
			PostRepository postRepository,
			CommentRepository commentRepository) {
		super();
		this.accountRepository = accountRepository;
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
	}

	@RequestMapping(method = RequestMethod.GET)
	Collection<Post> readPosts(Principal principal) {
		this.validateUser(principal);
		return this.postRepository.findByAuthorUsername(principal.getName());
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	ResponseEntity<?> addPost(Principal principal,
			@RequestBody NewPost newPost) {
		this.validateUser(principal);
		return this.accountRepository
				.findByUsername(principal.getName())
				.map(author -> {

					Post post = this.postRepository
							.save(new Post(author, newPost.getSubject(), newPost.getContent()));

					URI location = ServletUriComponentsBuilder
							.fromCurrentRequestUri().path("/{id}")
							.buildAndExpand(post.getId()).toUri();

					return ResponseEntity.created(location).build();
				})
				.orElse(ResponseEntity.noContent().build());

	}

	@RequestMapping(path = "/{postId}", method = RequestMethod.GET)
	Post readPost(Principal principal, @PathVariable Long postId) {
		this.validateUser(principal);
		return this.postRepository.findById(postId).get();
	}

	@RequestMapping(path = "/{postId}/comments", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	ResponseEntity<?> addComment(Principal principal,
			@PathVariable Long postId,
			@RequestBody NewComment newComment) {
		this.validateUser(principal);
		this.validatePost(postId);
		return this.accountRepository
				.findByUsername(principal.getName())
				.map(author -> this.postRepository
						.findById(postId)

						.map(post -> {
							Comment comment = this.commentRepository
									.save(new Comment(post, author, newComment.getContent()));

							URI location = ServletUriComponentsBuilder
									.fromCurrentRequestUri().path("/{commentId}")
									.buildAndExpand(comment.getId()).toUri();

							return ResponseEntity.created(location).build();
						})
						.orElse(ResponseEntity.noContent().build()))

				.orElse(ResponseEntity.noContent().build());

	}

	@RequestMapping(path = "/{postId}/comments", method = RequestMethod.GET)
	Collection<Comment> readCommentsFromPost(Principal principal,
			@PathVariable Long postId) {
		this.validateUser(principal);
		this.validatePost(postId);
		return this.commentRepository.findByPostId(postId);
	}

	@RequestMapping(path = "/{postId}/comments/{commentId}", method = RequestMethod.GET)
	Comment readComment(Principal principal,
			@PathVariable Long postId,
			@PathVariable Long commentId) {
		this.validateUser(principal);
		this.validatePost(postId);
		this.validateComment(commentId);
		return this.commentRepository.findById(commentId).get();
	}

	@RequestMapping(path = "/{postId}/comments/{commentId}/repliedComments", method = RequestMethod.GET)
	Collection<Comment> readRepliedComments(Principal principal,
			@PathVariable Long postId,
			@PathVariable Long commentId) {
		this.validateUser(principal);
		this.validatePost(postId);
		this.validateComment(commentId);
		return this.commentRepository.findByParentCommentId(commentId);
	}

	@RequestMapping(path = "/{postId}/comments/{commentId}/repliedComments", method = RequestMethod.POST)
	@ResponseStatus(value = HttpStatus.CREATED)
	ResponseEntity<?> addRepliedComment(Principal principal,
			@PathVariable Long postId,
			@PathVariable Long commentId,
			@RequestBody NewComment newComment) {
		this.validateUser(principal);
		this.validatePost(postId);
		this.validateComment(commentId);
		return this.accountRepository
				.findByUsername(principal.getName())
				.map(author -> this.postRepository
						.findById(postId)
						.map(post -> this.commentRepository
								.findById(commentId)
								.map(parentComment -> {
									Comment comment = this.commentRepository
											.save(new Comment(parentComment, author, newComment.getContent()));
									URI location = ServletUriComponentsBuilder
											.fromCurrentRequestUri().path("/{commentId}")
											.buildAndExpand(comment.getId()).toUri();

									return ResponseEntity.created(location).build();
								}).orElse(ResponseEntity.noContent().build()))
						.orElse(ResponseEntity.noContent().build()))
				.orElse(ResponseEntity.noContent().build());
	}

	private void validateComment(Long commentId) {
		this.commentRepository
				.findById(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
	}

	private void validatePost(Long postId) {
		this.postRepository
				.findById(postId)
				.orElseThrow(() -> new PostNotFoundException(postId));
	}

	private void validateUser(Principal principal) {
		String userName = principal.getName();
		this.accountRepository
				.findByUsername(userName)
				.orElseThrow(() -> new UserNotFoundException(userName));
	}

}

class NewPost {

	private String subject;

	private String content;

	public NewPost() {
		// TODO Auto-generated constructor stub
	}

	public NewPost(String subject, String content) {
		super();
		this.subject = subject;
		this.content = content;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}

class NewComment {

	private String content;

	public NewComment() {
		// TODO Auto-generated constructor stub
	}

	public NewComment(String content) {
		super();
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}

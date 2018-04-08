package com.example.demo;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PocAuthApplication.class)
@WebAppConfiguration
public class PocAuthApplicationTests {

	private static final String CLIENT_ID = "pocauth";
	private static final String CLIENT_SECRET = "123456";

	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(),
			Charset.forName("utf8"));

	private MockMvc mockMvc;
	@SuppressWarnings("rawtypes")
	private HttpMessageConverter mappingJackson2HttpMessageConverter;
	@Autowired
	private FilterChainProxy springSecurityFilterChain;

	@Autowired
	void setConverters(HttpMessageConverter<?>[] converters) {

		this.mappingJackson2HttpMessageConverter = Arrays
				.asList(converters).stream()
				.filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
				.findAny()
				.orElse(null);

		assertNotNull("the JSON message converter must not be null",
				this.mappingJackson2HttpMessageConverter);
	}

	@Autowired
	private WebApplicationContext webApplicationContext;

	private String singleUsername = "teste1";

	private String userName = "dearrudam";
	private String password = "password";
	private Account account;
	private List<Post> posts = new ArrayList<>();
	private List<Comment> comments = new ArrayList<>();
	private Map<Long, List<Comment>> repliedComments = new HashMap<>();

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Before
	public void setup() throws Exception {
		this.mockMvc = webAppContextSetup(webApplicationContext).addFilter(springSecurityFilterChain).build();

		this.commentRepository.deleteAllInBatch();
		this.postRepository.deleteAllInBatch();
		this.accountRepository.deleteAllInBatch();

		accountRepository.save(new Account(singleUsername, password));

		Post post = null;
		Comment comment = null;
		List<Comment> repliedComments = null;
		this.account = accountRepository.save(new Account(userName, password));

		for (int p = 0; p < (new Random(3).nextInt() + 1); p++) {
			this.posts.add(post =
					postRepository.save(new Post(account, UUID.randomUUID().toString(), UUID.randomUUID().toString())));
			for (int c = 0; c < (new Random(3).nextInt() + 1); c++) {
				this.comments.add(
						comment = commentRepository.save(new Comment(post, account, UUID.randomUUID().toString())));
				this.repliedComments.put(comment.getId(), repliedComments = new ArrayList<>());
				for (int rc = 0; rc < (new Random(3).nextInt() + 1); rc++) {
					repliedComments.add(commentRepository
							.save(new Comment(comment, account, UUID.randomUUID().toString())));
				}
			}
		}

	}

	private Optional<OAuth2AccessToken> authenticate(String username, String password) throws IOException, Exception {

		final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "password");
		params.add("client_id", CLIENT_ID);
		params.add("username", username);
		params.add("password", password);

		MvcResult authResult = mockMvc
				.perform(post("/oauth/token")
						.params(params)
						.with(httpBasic(CLIENT_ID, CLIENT_SECRET))
						.accept(contentType))
				.andExpect(status().isOk())
				.andExpect(content().contentType(contentType))
				.andExpect(jsonPath("$.access_token", any(String.class)))
				.andReturn();

		return Optional.of(fromJson(OAuth2AccessToken.class, authResult.getResponse().getContentAsString()));

	}

	//	@Test
	//	public void userNotFound() throws Exception {
	//		mockMvc
	//				.perform(post("/george/posts/")
	//						.content(this.json(new NewPost(null, null)))
	//						.contentType(contentType))
	//				.andExpect(status().isNotFound());
	//	}

	@Test
	public void readBookmarks() throws Exception {

		authenticate(userName, password)
				.map(accessToken -> {
					try {
						ResultActions result = mockMvc
								.perform(get("/posts")
										.header(HttpHeaders.AUTHORIZATION,
												"Bearer " + accessToken.getValue()))
								.andExpect(status().isOk())
								.andExpect(content().contentType(contentType))
								.andExpect(jsonPath("$", hasSize(this.posts.size())));
						for (int p = 0; p < this.posts.size(); p++) {

							Post post = this.posts.get(p);

							result
									.andExpect(jsonPath("$[" + p + "].id", is(post.getId().intValue())))
									.andExpect(jsonPath("$[" + p + "].subject", is(post.getSubject())))
									.andExpect(jsonPath("$[" + p + "].content", is(post.getContent())))
									.andExpect(jsonPath("$[" + p + "].postedOn", is(post.getPostedOn())));

							ResultActions resultComments = mockMvc
									.perform(get("/posts/" + post.getId().intValue() + "/comments")
											.header(HttpHeaders.AUTHORIZATION,
													accessToken.getTokenType() + " " + accessToken.getValue()))
									.andExpect(status().isOk())
									.andExpect(content().contentType(contentType))
									.andExpect(jsonPath("$", hasSize(this.comments.size())));

							for (int c = 0; c < this.comments.size(); c++) {

								Comment comment = this.comments.get(c);

								resultComments
										.andExpect(jsonPath("$[" + c + "].id", is(comment.getId().intValue())))
										.andExpect(jsonPath("$[" + c + "].content", is(comment.getContent())))
										.andExpect(jsonPath("$[" + c + "].commentedOn", is(comment.getCommentedOn())));

								mockMvc
										.perform(get("/posts/"
												+ post.getId().intValue()
												+ "/comments/"
												+ comment.getId().intValue())
														.header(HttpHeaders.AUTHORIZATION,
																accessToken.getTokenType() + " "
																		+ accessToken.getValue()))
										.andExpect(status().isOk())
										.andExpect(content().contentType(contentType))
										.andExpect(jsonPath("$.id", is(comment.getId().intValue())))
										.andExpect(jsonPath("$.content", is(comment.getContent())))
										.andExpect(jsonPath("$.commentedOn", is(comment.getCommentedOn())));

								List<Comment> repliedComments = this.repliedComments.get(comment.getId());

								Comment parentComment = comment;

								ResultActions repliedCommentsResult = mockMvc
										.perform(get("/posts/"
												+ post.getId().intValue()
												+ "/comments/"
												+ parentComment.getId().intValue()
												+ "/repliedComments")
														.header(HttpHeaders.AUTHORIZATION,
																accessToken.getTokenType() + " "
																		+ accessToken.getValue()))
										.andExpect(status().isOk())
										.andExpect(content().contentType(contentType))
										.andExpect(jsonPath("$", hasSize(repliedComments.size())));

								for (int rc = 0; rc < repliedComments.size(); rc++) {

									repliedCommentsResult
											.andExpect(jsonPath("$[" + rc + "].id", is(comment.getId().intValue())))
											.andExpect(jsonPath("$[" + rc + "].content", is(comment.getContent())))
											.andExpect(jsonPath("$[" + rc + "].commentedOn",
													is(comment.getCommentedOn())));

									mockMvc
											.perform(get("/posts/"
													+ post.getId().intValue()
													+ "/comments/"
													+ comment.getId().intValue())
															.header(HttpHeaders.AUTHORIZATION,
																	accessToken.getTokenType() + " "
																			+ accessToken.getValue()))
											.andExpect(status().isOk())
											.andExpect(content().contentType(contentType))
											.andExpect(jsonPath("$.id", is(comment.getId().intValue())))
											.andExpect(jsonPath("$.content", is(comment.getContent())))
											.andExpect(jsonPath("$.commentedOn", is(comment.getCommentedOn())));
								}

							}
						}
					} catch (Exception e) {
						new RuntimeException(e);
					}
					return accessToken;
				})
				.orElseThrow(() -> new RuntimeException("authentication fail"));

	}

	@Test
	public void addPost() throws Exception {

		authenticate(singleUsername, password)
				.map(accessToken -> {
					try {
						NewPost newPost = new NewPost(UUID.randomUUID().toString(), UUID.randomUUID().toString());

						String postURL = mockMvc
								.perform(post("/posts")
										.contentType(contentType)
										.content(json(newPost))
										.header(HttpHeaders.AUTHORIZATION,
												"Bearer " + accessToken.getValue()))
								.andExpect(status().isCreated())
								.andExpect(header().string("Location", any(String.class)))
								.andReturn().getResponse().getHeader("Location");

						mockMvc
								.perform(get("/posts")
										.header(HttpHeaders.AUTHORIZATION,
												"Bearer " + accessToken.getValue()))
								.andExpect(status().isOk())
								.andExpect(content().contentType(contentType))
								.andExpect(jsonPath("$", hasSize(1)))
								.andExpect(jsonPath("$[0].id", any(Integer.class)))
								.andExpect(jsonPath("$[0].subject", is(newPost.getSubject())))
								.andExpect(jsonPath("$[0].content", is(newPost.getContent())))
								.andExpect(jsonPath("$[0].postedOn", any(String.class)));

						NewComment newComment = new NewComment(UUID.randomUUID().toString());

						String commentURL = mockMvc
								.perform(post(postURL + "/comments")
										.contentType(contentType)
										.content(json(newComment))
										.header(HttpHeaders.AUTHORIZATION,
												"Bearer " + accessToken.getValue()))
								.andExpect(status().isCreated())
								.andExpect(header().string("Location", any(String.class)))
								.andReturn().getResponse().getHeader("Location");

						NewComment repliedComment = new NewComment(UUID.randomUUID().toString());

						mockMvc
								.perform(post(commentURL + "/repliedComments")
										.contentType(contentType)
										.content(json(repliedComment))
										.header(HttpHeaders.AUTHORIZATION,
												"Bearer " + accessToken.getValue()))
								.andExpect(status().isCreated())
								.andExpect(header().string("Location", any(String.class)));
					} catch (Exception e) {
						new RuntimeException(e);
					}
					return accessToken;
				})
				.orElseThrow(() -> new RuntimeException("authentication fail"));
	}

	@SuppressWarnings("unchecked")
	protected String json(Object o) throws IOException {
		MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
		this.mappingJackson2HttpMessageConverter.write(
				o,
				MediaType.APPLICATION_JSON,
				mockHttpOutputMessage);
		return mockHttpOutputMessage.getBodyAsString();
	}

	protected <T> T fromJson(Class<T> type, String json) throws IOException {
		return new ObjectMapper().readValue(json, type);
	}
}

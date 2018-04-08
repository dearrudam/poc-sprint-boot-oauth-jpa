package com.example.demo;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

@SpringBootApplication
public class PocAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(PocAuthApplication.class, args);
	}

	@Bean
	CommandLineRunner init(AccountRepository accountRepository,
			PostRepository postRepository,
			CommentRepository commentRepository) {
		return (args) -> Arrays
				.asList("admin".split(","))
				.forEach(username -> {
					Account account = accountRepository.save(new Account(username, "password"));
					Post post = postRepository.save(new Post(account, "Fist post", "it's my first post!"));
					Comment parentComment =
							commentRepository.save(new Comment(post, account, "It's my first comment!"));
					commentRepository.save(new Comment(parentComment, account, "It's my first replied comment!"));
				});
	}

	// CORS
	@SuppressWarnings({
			"rawtypes", "unchecked"
	})
	@Bean
	FilterRegistrationBean corsFilter(
			@Value("${tagit.origin:*}") String origin) {
		return new FilterRegistrationBean(new Filter() {
			public void doFilter(ServletRequest req,
					ServletResponse res,
					FilterChain chain) throws IOException, ServletException {
				HttpServletRequest request = (HttpServletRequest) req;
				HttpServletResponse response = (HttpServletResponse) res;
				String method = request.getMethod();
				// this origin value could just as easily have come from a database
				response.setHeader("Access-Control-Allow-Origin", origin);
				response.setHeader("Access-Control-Allow-Methods",
						"POST,GET,OPTIONS,DELETE");
				response.setHeader("Access-Control-Max-Age", Long.toString(60 * 60));
				response.setHeader("Access-Control-Allow-Credentials", "true");
				response.setHeader(
						"Access-Control-Allow-Headers",
						"Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,Access-Control-Request-Headers,Authorization");
				if ("OPTIONS".equals(method)) {
					response.setStatus(HttpStatus.OK.value());
				} else {
					chain.doFilter(req, res);
				}
			}

			public void init(FilterConfig filterConfig) {
			}

			public void destroy() {
			}
		});
	}

}

package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class WebSecurityConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	AccountRepository accountRepository;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService());
	}

	// Workaround for https://github.com/spring-projects/spring-boot/issues/1801 There is no PasswordEncoder mapped for the id “null”
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new PasswordEncoder() {

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				return rawPassword.toString().equals(encodedPassword);
			}

			@Override
			public String encode(CharSequence rawPassword) {
				return rawPassword.toString();
			}
		};
	}

	@Bean
	UserDetailsService userDetailsService() {
		return (username) -> {
			return accountRepository
					.findByUsername(username)
					.map(a -> User
							.withUsername(a.getUsername())
							//.passwordEncoder(passwordEncoder()::encode)
							.password(a.getPassword())
							.accountExpired(false)
							.accountLocked(false)
							.disabled(false)
							.authorities(AuthorityUtils.createAuthorityList("USER", "write"))
							.build())
					.orElseThrow(
							() -> new UsernameNotFoundException("could not find the user '"
									+ username
									+ "'"));
		};
	}
}
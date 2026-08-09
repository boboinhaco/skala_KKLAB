package com.sk.skala.shopapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 비밀번호 해시 설정
@Configuration
public class SecurityConfig {

	/**
	 * BCrypt 는 솔트를 결과에 포함하므로 같은 비밀번호도 매번 다른 해시가 된다.
	 * 따라서 검증은 equals() 가 아니라 반드시 matches() 로 해야 한다.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

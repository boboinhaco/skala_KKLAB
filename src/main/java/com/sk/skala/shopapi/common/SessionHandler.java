package com.sk.skala.shopapi.common;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ResponseException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// JWT 토큰 발급과 쿠키 기반 사용자 식별을 담당
@Component
public class SessionHandler {

	@Value("${app.jwt.secret}")
	private String secret;

	@Value("${app.jwt.expire-minutes}")
	private long expireMinutes;

	@Value("${app.jwt.cookie-name}")
	private String cookieName;

	private static final String CLAIM_CUSTOMER_ID = "customerId";

	// 설정값 기반 서명 키 생성
	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	// 로그인 성공 시 JWT를 생성해 쿠키로 내려줌
	public void storeAccessToken(String customerId) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expireMinutes * 60 * 1000);

		String token = Jwts.builder()
				.claim(CLAIM_CUSTOMER_ID, customerId)
				.setIssuedAt(now)
				.setExpiration(expiry)
				.signWith(getSigningKey())
				.compact();

		Cookie cookie = new Cookie(cookieName, token);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge((int) (expireMinutes * 60));
		getCurrentResponse().addCookie(cookie);
	}

	// 요청 쿠키의 JWT에서 로그인한 고객 ID를 추출
	public String getCustomerId() {
		String token = extractToken(getCurrentRequest());
		if (token == null) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "로그인이 필요합니다.");
		}
		try {
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(getSigningKey())
					.build()
					.parseClaimsJws(token)
					.getBody();
			return claims.get(CLAIM_CUSTOMER_ID, String.class);
		} catch (Exception e) {
			throw new ResponseException(Error.NOT_AUTHENTICATED, "유효하지 않은 토큰입니다.");
		}
	}

	// 로그아웃 시 쿠키 제거
	public void removeAccessToken() {
		Cookie cookie = new Cookie(cookieName, null);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		getCurrentResponse().addCookie(cookie);
	}

	// 쿠키 목록에서 토큰 값 찾기
	private String extractToken(HttpServletRequest request) {
		if (request.getCookies() == null) {
			return null;
		}
		for (Cookie cookie : request.getCookies()) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private HttpServletRequest getCurrentRequest() {
		return getAttributes().getRequest();
	}

	private HttpServletResponse getCurrentResponse() {
		return getAttributes().getResponse();
	}

	private ServletRequestAttributes getAttributes() {
		ServletRequestAttributes attributes =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			throw new ResponseException(Error.SYSTEM_ERROR, "요청 컨텍스트를 찾을 수 없습니다.");
		}
		return attributes;
	}
}

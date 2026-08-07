package com.sk.skala.shopapi.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// 컨트롤러 진입/종료 시각과 소요 시간을 로그로 남김
@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

	@Around("execution(* com.sk.skala.shopapi.controller..*(..))")
	public Object logApi(ProceedingJoinPoint joinPoint) throws Throwable {
		String method = joinPoint.getSignature().toShortString();
		long start = System.currentTimeMillis();
		log.debug("[API 시작] {}", method);
		try {
			return joinPoint.proceed();
		} finally {
			log.debug("[API 종료] {} ({}ms)", method, System.currentTimeMillis() - start);
		}
	}
}

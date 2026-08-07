package com.sk.skala.shopapi.common;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

// 페이지 단위 조회 결과를 담는 공통 객체
@Getter
@Setter
public class PagedList {

	private long total;
	private int offset;
	private int count;
	private List<?> list;

	public PagedList() {
	}

	public PagedList(long total, int offset, int count, List<?> list) {
		this.total = total;
		this.offset = offset;
		this.count = count;
		this.list = list;
	}
}

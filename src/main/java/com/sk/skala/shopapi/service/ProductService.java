package com.sk.skala.shopapi.service;

import org.springframework.stereotype.Service;

import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

// 상품 관리 비즈니스 로직
@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;

	// 전체 상품 목록 조회 (페이지 단위)
	public Response getAllProducts(int offset, int count) {
		// TODO 1. PageRequest.of(offset, count)로 Pageable 생성
		// TODO 2. productRepository.findAll(pageable)로 페이지 조회
		// TODO 3. PagedList(총건수, offset, count, 목록)로 가공
		// TODO 4. Response.success(pagedList) 반환
		throw new UnsupportedOperationException("TODO: getAllProducts 구현");
	}

	// 개별 상품 상세 조회
	public Response getProductById(Long id) {
		// TODO 1. productRepository.findById(id)로 조회
		// TODO 2. 없으면 ResponseException(Error.DATA_NOT_FOUND)
		// TODO 3. Response.success(product) 반환
		throw new UnsupportedOperationException("TODO: getProductById 구현");
	}

	// 상품 등록
	public Response createProduct(Product product) {
		// TODO 1. productName 공백 또는 productPrice null/음수면 ParameterException
		// TODO 2. findByProductName으로 중복 체크, 있으면 Error.DATA_DUPLICATED
		// TODO 3. product.setId(0L)로 초기화 후 save
		// TODO 4. Response.success(saved) 반환
		throw new UnsupportedOperationException("TODO: createProduct 구현");
	}

	// 상품 정보 수정
	public Response updateProduct(Product product) {
		// TODO 1. productName, productPrice 검증 실패 시 ParameterException
		// TODO 2. 해당 ID 존재 확인, 없으면 Error.DATA_NOT_FOUND
		// TODO 3. 수정 후 save, Response.success(saved) 반환
		throw new UnsupportedOperationException("TODO: updateProduct 구현");
	}

	// 상품 삭제
	public Response deleteProduct(Product product) {
		// TODO 1. ID로 조회, 없으면 Error.DATA_NOT_FOUND
		// TODO 2. delete 후 Response.success() 반환
		throw new UnsupportedOperationException("TODO: deleteProduct 구현");
	}
}

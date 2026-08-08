package com.sk.skala.shopapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sk.skala.shopapi.common.PagedList;
import com.sk.skala.shopapi.common.Response;
import com.sk.skala.shopapi.data.dto.ProductDto;
import com.sk.skala.shopapi.data.table.Product;
import com.sk.skala.shopapi.exception.Error;
import com.sk.skala.shopapi.exception.ParameterException;
import com.sk.skala.shopapi.exception.ResponseException;
import com.sk.skala.shopapi.repository.OrderItemRepository;
import com.sk.skala.shopapi.repository.ProductRepository;
import com.sk.skala.shopapi.tools.StringUtil;

import lombok.RequiredArgsConstructor;

// 상품 관리 비즈니스 로직
@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final OrderItemRepository orderItemRepository;

	// 판매 상태 기본값
	private static final String ON_SALE = "ON_SALE";

	// 전체 상품 목록 조회 (페이지 단위)
	public Response getAllProducts(int offset, int count) {
		Pageable pageable = PageRequest.of(offset, count);
		Page<Product> page = productRepository.findAll(pageable);

		List<ProductDto> products = page.getContent().stream()
				.map(this::toProductDto)
				.toList();

		PagedList pagedList = new PagedList(page.getTotalElements(), offset, count, products);
		return Response.success(pagedList);
	}

	// 개별 상품 상세 조회
	public Response getProductById(Long id) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		return Response.success(toProductDto(product));
	}

	// 상품 등록
	@Transactional
	public Response createProduct(Product product) {
		validate(product);

		// 상품명 중복 확인
		if (productRepository.findByProductName(product.getProductName()).isPresent()) {
			throw new ResponseException(Error.DATA_DUPLICATED);
		}

		// 새 상품이므로 ID 는 DB 가 매기도록 비워둠
		product.setId(null);

		// 기본값 세팅 - 재고 0, 판매중
		if (product.getStockQuantity() == null) {
			product.setStockQuantity(0);
		}
		if (StringUtil.isAnyEmpty(product.getStatus())) {
			product.setStatus(ON_SALE);
		}

		Product saved = productRepository.save(product);
		return Response.success(toProductDto(saved));
	}

	// 상품 정보 수정
	@Transactional
	public Response updateProduct(Product product) {
		if (product.getId() == null) {
			throw new ParameterException("id");
		}
		validate(product);

		Product target = productRepository.findById(product.getId())
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		// 상품명이 바뀔 때만 중복 확인
		if (!product.getProductName().equals(target.getProductName())
				&& productRepository.findByProductName(product.getProductName()).isPresent()) {
			throw new ResponseException(Error.DATA_DUPLICATED);
		}

		target.setProductName(product.getProductName());
		target.setProductPrice(product.getProductPrice());

		// 값이 들어온 항목만 반영
		if (product.getCategoryId() != null) {
			target.setCategoryId(product.getCategoryId());
		}
		if (product.getDescription() != null) {
			target.setDescription(product.getDescription());
		}
		if (product.getTexture() != null) {
			target.setTexture(product.getTexture());
		}
		if (product.getSoundLevel() != null) {
			target.setSoundLevel(product.getSoundLevel());
		}
		if (product.getStretchLevel() != null) {
			target.setStretchLevel(product.getStretchLevel());
		}
		if (product.getScent() != null) {
			target.setScent(product.getScent());
		}
		if (product.getStockQuantity() != null) {
			target.setStockQuantity(product.getStockQuantity());
		}
		if (StringUtil.isNoneEmpty(product.getStatus())) {
			target.setStatus(product.getStatus());
		}

		return Response.success(toProductDto(target));
	}

	// 상품 삭제
	@Transactional
	public Response deleteProduct(Product product) {
		if (product.getId() == null) {
			throw new ParameterException("id");
		}

		Product target = productRepository.findById(product.getId())
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));

		// 주문 이력이 있으면 삭제 불가 - 주문 내역의 상품 참조가 끊어지는 것을 막음
		if (orderItemRepository.existsByProduct_Id(target.getId())) {
			throw new ResponseException(Error.DELETE_NOT_ALLOWED, "주문 이력이 있는 상품은 삭제할 수 없습니다.");
		}

		productRepository.delete(target);
		return Response.success();
	}

	// 필수 입력값 검증 - 상품명과 가격은 반드시 있어야 함
	private void validate(Product product) {
		if (StringUtil.isAnyEmpty(product.getProductName())) {
			throw new ParameterException("productName");
		}
		if (product.getProductPrice() == null || product.getProductPrice() < 0) {
			throw new ParameterException("productPrice");
		}
	}

	// 상품을 응답 DTO 로 변환
	private ProductDto toProductDto(Product product) {
		return ProductDto.builder()
				.id(product.getId())
				.productName(product.getProductName())
				.productPrice(product.getProductPrice())
				.description(product.getDescription())
				.texture(product.getTexture())
				.soundLevel(product.getSoundLevel())
				.stretchLevel(product.getStretchLevel())
				.scent(product.getScent())
				.stockQuantity(product.getStockQuantity())
				.status(product.getStatus())
				.build();
	}
}

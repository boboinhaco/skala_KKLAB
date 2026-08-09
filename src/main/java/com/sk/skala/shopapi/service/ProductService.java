package com.sk.skala.shopapi.service;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

	private static final String STATUS_ON_SALE = "ON_SALE";

	private final ProductRepository productRepository;
	private final OrderItemRepository orderItemRepository;

	@Transactional(readOnly = true)
	public Response getAllProducts(int offset, int count) {
		Page<Product> page = productRepository.findAll(PageRequest.of(offset, count));
		List<ProductDto> products = page.getContent().stream().map(ProductDto::from).toList();

		return Response.success(new PagedList(page.getTotalElements(), offset, count, products));
	}

	@Transactional(readOnly = true)
	public Response getProductById(Long id) {
		return Response.success(ProductDto.from(findOrThrow(id)));
	}

	@Transactional
	public Response createProduct(Product product) {
		validate(product);
		if (productRepository.findByProductName(product.getProductName()).isPresent()) {
			throw new ResponseException(Error.DATA_DUPLICATED, "이미 등록된 상품명입니다.");
		}

		// ID 가 남아 있으면 Spring Data 가 신규가 아닌 것으로 보고 merge 를 탄다.
		product.setId(null);
		if (product.getStockQuantity() == null) {
			product.setStockQuantity(0);
		}
		if (StringUtil.isAnyEmpty(product.getStatus())) {
			product.setStatus(STATUS_ON_SALE);
		}

		return Response.success(ProductDto.from(productRepository.save(product)));
	}

	// 값이 들어온 항목만 반영한다. (PUT 이지만 부분 수정을 허용)
	@Transactional
	public Response updateProduct(Product request) {
		if (request.getId() == null) {
			throw new ParameterException("id");
		}
		validate(request);

		Product product = findOrThrow(request.getId());
		if (!request.getProductName().equals(product.getProductName())
				&& productRepository.findByProductName(request.getProductName()).isPresent()) {
			throw new ResponseException(Error.DATA_DUPLICATED, "이미 등록된 상품명입니다.");
		}

		product.setProductName(request.getProductName());
		product.setProductPrice(request.getProductPrice());
		setIfPresent(request.getCategoryId(), product::setCategoryId);
		setIfPresent(request.getDescription(), product::setDescription);
		setIfPresent(request.getTexture(), product::setTexture);
		setIfPresent(request.getSoundLevel(), product::setSoundLevel);
		setIfPresent(request.getStretchLevel(), product::setStretchLevel);
		setIfPresent(request.getScent(), product::setScent);
		setIfPresent(request.getStockQuantity(), product::setStockQuantity);
		setIfPresent(request.getStatus(), product::setStatus);

		return Response.success(ProductDto.from(product));
	}

	@Transactional
	public Response deleteProduct(Product request) {
		if (request.getId() == null) {
			throw new ParameterException("id");
		}

		Product product = findOrThrow(request.getId());
		// 주문 상세가 참조하는 상품을 지우면 과거 주문의 상품 링크가 끊어진다.
		if (orderItemRepository.existsByProduct_Id(product.getId())) {
			throw new ResponseException(Error.DELETE_NOT_ALLOWED, "주문 이력이 있는 상품은 삭제할 수 없습니다.");
		}

		productRepository.delete(product);
		return Response.success();
	}

	private Product findOrThrow(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResponseException(Error.DATA_NOT_FOUND));
	}

	private void validate(Product product) {
		if (StringUtil.isAnyEmpty(product.getProductName())) {
			throw new ParameterException("productName");
		}
		if (product.getProductPrice() == null || product.getProductPrice() < 0) {
			throw new ParameterException("productPrice");
		}
	}

	private <T> void setIfPresent(T value, Consumer<T> setter) {
		if (value != null) {
			setter.accept(value);
		}
	}
}

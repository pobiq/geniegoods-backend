package com.example.geniegoods.service;

import com.example.geniegoods.dto.order.*;
import com.example.geniegoods.entity.*;
import com.example.geniegoods.enums.OrderStatus;
import com.example.geniegoods.enums.PaymentMethod;
import com.example.geniegoods.enums.PaymentStatus;
import com.example.geniegoods.repository.GoodsRepository;
import com.example.geniegoods.repository.OrderItemRepository;
import com.example.geniegoods.repository.OrderRepository;
import com.example.geniegoods.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final GoodsRepository goodsRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;

    private static final int SHIPPING_FEE = 3000; // 배송비
    private static final int PAGE_SIZE = 5; // 페이지 수

    /* 1. 주문 생성 */
    @Transactional
    public OrderEntity createOrder(UserEntity user, OrderRequestDTO dto) {
        // 검증
        validateOrderRequest(dto);

        String orderNumber = generateOrderNumber();

        OrderEntity order = OrderEntity.builder()
                .user(user)
                .orderNumber(orderNumber)
                .orderedAt(LocalDateTime.now())
                .status(OrderStatus.ORDERED)
                .zipcode(dto.getZipcode().trim())
                .address(dto.getAddress().trim())
                .detailAddress(dto.getDetailAddress() != null ? dto.getDetailAddress().trim() : null)
                .totalAmount(0)
                .build();

        orderRepository.save(order); // ID 생성

        int subtotal = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();

        for (OrderRequestDTO.OrderItemDto itemDto : dto.getItems()) {
            GoodsEntity goods = goodsRepository.findById(itemDto.getGoodsId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 굿즈입니다: " + itemDto.getGoodsId()));

            Integer price = goods.getGoodsCategoryEntity().getPrice();

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .goods(goods)
                    .quantity(itemDto.getQuantity())
                    .priceAtOrder(price)
                    .build();

            orderItemRepository.save(orderItem);
            
            // 소계 누적
            subtotal += price * itemDto.getQuantity();
        }

        // 총 금액 계산 (상품 합계 + 배송비)
        order.setTotalAmount(subtotal + SHIPPING_FEE);

        // 결제 생성
        PaymentEntity payment = PaymentEntity.builder()
                .amount(subtotal + SHIPPING_FEE)
                .method(PaymentMethod.from(dto.getMethod()))
                .status(PaymentStatus.PAID)
                .order(order)
                .build();

        paymentRepository.save(payment);

        return order;
    }

    private void validateOrderRequest(OrderRequestDTO dto) {
        if (dto == null) throw new IllegalArgumentException("주문 요청 데이터가 없습니다.");
        if (dto.getItems() == null || dto.getItems().isEmpty()) throw new IllegalArgumentException("주문할 굿즈를 하나 이상 선택해야 합니다.");
        if (dto.getZipcode() == null || dto.getZipcode().trim().isEmpty()) throw new IllegalArgumentException("우편번호는 필수입니다.");
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) throw new IllegalArgumentException("주소는 필수입니다.");

        for (OrderRequestDTO.OrderItemDto item : dto.getItems()) {
            if (item.getGoodsId() == null) throw new IllegalArgumentException("굿즈 ID는 필수입니다.");
            if (item.getQuantity() == null || item.getQuantity() < 1) throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
    }

    /* 2. 주문번호 생성 */
    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderRepository.countByOrderNumberStartingWith(datePrefix);
        return datePrefix + "-" + String.format("%03d", count + 1);
    }

    /* 3. 주문 목록 조회 */
    public List<OrderResponseDTO> getMyOrderList(Long userId) {
        List<OrderEntity> orders = orderRepository.findByUserUserIdOrderByOrderedAtDesc(userId);
        return orders.stream()
                .map(this::convertToResponseDto)
                .toList();
    }

    /* 4. 주문 상세 조회 */
    public OrderResponseDTO getOrderDetail(Long orderId, Long userId) {

        OrderEntity order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없거나 권한이 없습니다."));
        return convertToResponseDto(order);
    }

    /* 5. 공통 DTO 변환 */
    private OrderResponseDTO convertToResponseDto(OrderEntity order) {

        List<OrderResponseDTO.OrderItemResponseDto> items = new ArrayList<>();

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderNumber(order.getOrderNumber())
                .orderedAt(order.getOrderedAt())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().getDescription())
                .zipcode(order.getZipcode())
                .address(order.getAddress())
                .detailAddress(order.getDetailAddress())
                .subtotal(0)
                .shippingFee(SHIPPING_FEE)
                .items(items)
                .build();
    }

    /**
     * 기간별 + 페이징 주문 목록 조회 (PageRequest 사용)
     */
    public MyOrderResponseDTO getMyOrdersManualPaging(Long userId, Integer months, int page) {
        LocalDateTime cutoffDate = (months == null) ? null : LocalDateTime.now().minusMonths(months);

        // PageRequest 생성 (0-based page index)
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "orderedAt"));

        // Pageable을 사용한 페이징 쿼리
        Page<OrderEntity> orderPage = (cutoffDate == null) ?
                orderRepository.findByUserUserIdOrderByOrderedAtDesc(userId, pageable) :
                orderRepository.findByUserUserIdAndOrderedAtAfterOrderByOrderedAtDesc(userId, cutoffDate, pageable);

        List<AllOrderResponseDTO> orders = orderPage.getContent().stream()
                .map(this::convertToAllOrderResponseDto)
                .toList();

        int totalElements = (int) ((cutoffDate == null) ?
                orderRepository.countByUserUserId(userId) :
                orderRepository.countByUserUserIdAndOrderedAtAfter(userId, cutoffDate));

        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);

        return MyOrderResponseDTO.builder()
                .contents(orders)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    /**
     * OrderEntity를 AllOrderResponseDTO로 변환
     */
    private AllOrderResponseDTO convertToAllOrderResponseDto(OrderEntity order) {
        return AllOrderResponseDTO.of(order);
    }

    /**
     * 기간별 주문 총 개수 조회
     */
    public int getTotalOrderCount(Long userId, Integer months) {
        LocalDateTime cutoffDate = (months == null) ? null : LocalDateTime.now().minusMonths(months);
        return (int) ((cutoffDate == null) ?
                orderRepository.countByUserUserId(userId) :
                orderRepository.countByUserUserIdAndOrderedAtAfter(userId, cutoffDate));
    }

    /**
     * 마이페이지 -> 최근 주문 내역 2건 조회
     * @param user
     * @return
     */
    public List<RecentOrderResponseDTO> selectRecentOrder(UserEntity user) {
        // 사용자 정보가 없으면 빈 리스트 반환
        if (user == null) {
            return new ArrayList<>();
        }

        // 현재 로그인한 사용자의 최근 주문 내역 2건 조회
        PageRequest pageRequest =
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "orderedAt"));

        List<OrderEntity> orderList =
                orderRepository
                        .findByUserUserIdOrderByOrderedAtDesc(user.getUserId(), pageRequest)
                        .getContent();

        List<RecentOrderResponseDTO> response = new ArrayList<>();

        // 주문 상세 중 첫건 url 조회
        for(OrderEntity order : orderList) {

            RecentOrderResponseDTO recentOrder = RecentOrderResponseDTO.of(order);
            response.add(recentOrder);
        }

        return response;
    }

    
    /**
     * 주문 주소 수정
     */
    public void updateOrderAddress(Long orderId, OrderAddressRequestDTO addressRequest) {

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        order.setZipcode(addressRequest.getZipcode());
        order.setAddress(addressRequest.getAddress());
        order.setDetailAddress(addressRequest.getDetailAddress());
        orderRepository.save(order);

    }

    /**
     * 주문 취소
     * @param orderId
     * @param userId
     */
    public void cancelOrder(Long orderId, Long userId) {
        OrderEntity order = orderRepository.findByOrderIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없거나 권한이 없습니다."));

        // 이미 취소됐거나 배송 시작됐으면 불가
        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new IllegalArgumentException("이미 취소된 주문입니다.");
        }
        if (order.getStatus() == OrderStatus.DELIVERING || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("배송이 시작된 주문은 취소할 수 없습니다.");
        }

        // 취소 처리
        order.setStatus(OrderStatus.CANCELED);

        orderRepository.save(order);
    }
}
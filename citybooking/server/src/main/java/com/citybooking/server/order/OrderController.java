package com.citybooking.server.order;

import com.citybooking.server.common.ApiResponse;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.dto.OrderDto.CreateOrderReq;
import com.citybooking.server.dto.OrderDto.GrabReq;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.dto.OrderDto.PayResp;
import com.citybooking.server.dto.OrderDto.ReviewReq;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final DispatchService dispatchService;
    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ApiResponse<Long> create(@RequestBody @Valid CreateOrderReq req) {
        return ApiResponse.ok(orderService.createOrder(req, SecurityUtil.currentUserId()).getId());
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CONSUMER')")
    public ApiResponse<PayResp> pay(@PathVariable Long id) {
        return ApiResponse.ok(orderService.payOrder(id, SecurityUtil.currentUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderView> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id, SecurityUtil.currentUserId(), SecurityUtil.currentRole()));
    }

    @GetMapping("/grab-board")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<List<OrderView>> grabBoard() {
        return ApiResponse.ok(orderService.grabBoard(SecurityUtil.currentUserId()));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderView>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.myOrders(
                SecurityUtil.currentUserId(), SecurityUtil.currentRole(), status, page, size));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CONSUMER')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id, SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/grab")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<Void> grab(@PathVariable Long id, @RequestBody(required = false) GrabReq req) {
        dispatchService.grabOrder(id, SecurityUtil.currentUserId(), req);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('MERCHANT')")
    public ApiResponse<Void> accept(@PathVariable Long id) {
        dispatchService.acceptOrder(id, SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<Void> start(@PathVariable Long id) {
        dispatchService.startService(id, SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        dispatchService.completeService(id, SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('CONSUMER')")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody @Valid ReviewReq req) {
        reviewService.reviewOrder(id, SecurityUtil.currentUserId(), req);
        return ApiResponse.ok();
    }
}

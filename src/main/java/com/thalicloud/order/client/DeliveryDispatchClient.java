package com.thalicloud.order.client;

import com.thalicloud.order.entity.Customer;
import com.thalicloud.order.entity.Kitchen;
import com.thalicloud.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Notifies delivery-service that a vendor accepted an order, so it can offer
 * the delivery to an available partner (FR — "notification ... to assign
 * particular delivery boy on that area"). This is best-effort: delivery-
 * service has no real geo-matching engine yet (see its own docs — a caller
 * must otherwise supply an exact partnerId), so a dedicated dispatch endpoint
 * there picks the first available ONLINE partner instead. Any failure here
 * (no partner online, service down, etc.) is logged and swallowed — accepting
 * an order must never fail just because dispatch couldn't find a partner yet,
 * same "enrichment client" style as RatingClient/MealPlanClient in vendor-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryDispatchClient {

    private final RestTemplate restTemplate;

    @Value("${services.delivery-service.base-url}")
    private String deliveryServiceBaseUrl;

    @Value("${internal.dispatch-key}")
    private String dispatchKey;

    public void dispatchOrder(Order order, Kitchen kitchen, Customer customer) {
        try {
            DispatchOrderRequest body = new DispatchOrderRequest(
                    order.getOrderDisplayId(),
                    kitchen != null ? kitchen.getKitchenName() : "Kitchen",
                    // Phase 1 placeholder — order-service has no kitchen contact number
                    // field (vendor-service owns Kitchen.contactNumber; no enrichment
                    // call exists to fetch it yet).
                    "0000000000",
                    order.getDeliveryLatitude(),
                    order.getDeliveryLongitude(),
                    order.getDeliveryFullAddress(),
                    order.getCustomerName(),
                    customer != null ? customer.getPhone() : "0000000000",
                    order.getItems().size(),
                    order.getPaymentMethod() != null && "COD".equals(order.getPaymentMethod().name()) ? "COD" : "PREPAID",
                    order.getPaymentMethod() != null && "COD".equals(order.getPaymentMethod().name())
                            ? order.getAmountInPaise() : null,
                    order.getDeliveryChargeInPaise() != null ? order.getDeliveryChargeInPaise() : 0L
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Key", dispatchKey);
            headers.set("Content-Type", "application/json");

            restTemplate.postForEntity(
                    deliveryServiceBaseUrl + "/api/delivery/internal/dispatch",
                    new HttpEntity<>(body, headers),
                    Object.class);
        } catch (Exception e) {
            log.warn("Delivery dispatch notification failed for order {}: {}", order.getOrderDisplayId(), e.getMessage());
        }
    }

    private record DispatchOrderRequest(
            String orderId,
            String kitchenName,
            String kitchenContactNumber,
            double dropLatitude,
            double dropLongitude,
            String dropAddress,
            String customerName,
            String customerContactNumber,
            int itemCount,
            String paymentMethod,
            Long codAmountPaise,
            long deliveryChargePaise
    ) {}
}

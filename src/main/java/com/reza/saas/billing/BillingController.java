package com.reza.saas.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reza.saas.common.BadRequestException;

@RestController
@RequestMapping("/billing")
public class BillingController {

    public record CheckoutRequest(@NotNull Plan plan) {
    }

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/subscription")
    public BillingService.SubscriptionView subscription() {
        return billingService.getSubscription();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public BillingService.CheckoutSession checkout(@Valid @RequestBody CheckoutRequest request) {
        if (!request.plan().selfServeCheckout()) {
            throw new BadRequestException("Plan " + request.plan() + " cannot be purchased through checkout");
        }
        return billingService.createCheckoutSession(request.plan());
    }
}

package com.reza.saas.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.reza.saas.common.BadRequestException;

/**
 * Public route (no JWT) - it is authenticated by the Stripe-Signature header, which
 * the BillingService implementation must verify against the RAW body. That is why the
 * payload is taken as a String: re-serialising parsed JSON would break the signature.
 */
@RestController
public class StripeWebhookController {

    private final BillingService billingService;

    public StripeWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }
    
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (signature == null) {
            // No signature at all isn't "unauthenticated" in the JWT sense (this route has
            // no token), it's "not verifiably from Stripe" - reject before it reaches billing
            // logic rather than letting a real implementation NPE on a null signature.
        	throw new BadRequestException("Missing Stripe-Signature header");
        }
        billingService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}

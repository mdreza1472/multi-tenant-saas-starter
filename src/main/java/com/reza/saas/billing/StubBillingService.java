package com.reza.saas.billing;

import com.reza.saas.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Placeholder that lets every billing endpoint work end to end before Stripe is wired.
 * Active unless app.billing.provider is set to something else.
 *
 * TODO (StripeBillingService): create customers on tenant provisioning, create Checkout
 * Sessions with metadata.tenant_id, verify webhook signatures (Webhook.constructEvent),
 * and persist subscription state (pk = TENANT#<id>, sk = SUBSCRIPTION, plus a GSI on the
 * Stripe customer id so webhooks can find their tenant).
 */
@Service
@ConditionalOnProperty(name = "app.billing.provider", havingValue = "stub", matchIfMissing = true)
public class StubBillingService implements BillingService {

    private static final Logger LOG = LoggerFactory.getLogger(StubBillingService.class);

    @Override
    public SubscriptionView getSubscription() {
        return new SubscriptionView(Plan.FREE, "stub");
    }

    @Override
    public CheckoutSession createCheckoutSession(Plan plan) {
        String tenantId = TenantContext.requireTenantId();
        return new CheckoutSession("https://example.invalid/stub-checkout?plan=" + plan + "&tenant=" + tenantId);
    }

    @Override
    public void handleWebhook(String payload, String signatureHeader) {
        // Log the size only - real payloads contain customer data and shouldn't hit the logs.
        LOG.info("Stub billing: ignoring webhook ({} bytes, signature present: {})",
                payload == null ? 0 : payload.length(), signatureHeader != null);
    }
}

package com.reza.saas.billing;

/**
 * Billing boundary. The rest of the app only knows this interface, so swapping the
 * stub for Stripe (app.billing.provider=stripe) touches nothing outside this package.
 */
public interface BillingService {

    record SubscriptionView(Plan plan, String status) {
    }

    record CheckoutSession(String url) {
    }

    /** The CURRENT tenant's subscription. */
    SubscriptionView getSubscription();

    /**
     * Returns a URL the client redirects the tenant admin to in order to pay.
     *
     * @throws com.reza.saas.common.BadRequestException if {@code plan.selfServeCheckout()}
     *         is false (e.g. FREE, or a sales-led tier like ENTERPRISE). Callers should
     *         prefer checking {@link Plan#selfServeCheckout()} before invoking this, but
     *         an implementation must still enforce it itself rather than trust every caller to.
     */
    CheckoutSession createCheckoutSession(Plan plan);

    /**
     * Handles a raw webhook delivery. Runs with NO JWT and NO TenantContext, so an
     * implementation must (1) verify the signature header against the raw payload,
     * then (2) resolve the tenant from the event itself (e.g. metadata.tenant_id set
     * when the checkout session was created, or a GSI lookup by Stripe customer id).
     */
    void handleWebhook(String payload, String signatureHeader);
}
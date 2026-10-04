package com.reza.saas.billing;

/** Subscription tiers and the limits enforced per tier. Numbers are placeholders. */
public enum Plan {
    FREE(3, false),
    PRO(25, true),
    ENTERPRISE(Integer.MAX_VALUE, false);

    private final int maxUsers;
    private final boolean selfServeCheckout;

    Plan(int maxUsers, boolean selfServeCheckout) {
        this.maxUsers = maxUsers;
        this.selfServeCheckout = selfServeCheckout;
    }

    public int maxUsers() {
        return maxUsers;
    }

    /** False for plans that aren't bought via Stripe Checkout (free tier, sales-led enterprise). */
    public boolean selfServeCheckout() {
        return selfServeCheckout;
    }
}

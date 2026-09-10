package dev.ftathiago.ciir.fixtures.basic.domain;

/**
 * Authorizes payments against an external payment provider.
 */
public interface PaymentGateway {

    /**
     * Authorizes the given order.
     *
     * @param order the order to authorize.
     * @return the authorization result.
     */
    PaymentResult authorize(Order order);
}

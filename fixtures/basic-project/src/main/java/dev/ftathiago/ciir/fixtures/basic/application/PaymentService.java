package dev.ftathiago.ciir.fixtures.basic.application;

import dev.ftathiago.ciir.fixtures.basic.domain.InvalidOrderException;
import dev.ftathiago.ciir.fixtures.basic.domain.Order;
import dev.ftathiago.ciir.fixtures.basic.domain.PaymentGateway;
import dev.ftathiago.ciir.fixtures.basic.domain.PaymentResult;
import java.math.BigDecimal;

/**
 * Coordinates payment authorization for orders.
 */
public class PaymentService {

    private final PaymentGateway gateway;

    /**
     * Creates a service backed by the given gateway.
     *
     * @param gateway the payment gateway to delegate to.
     */
    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Authorizes the given order.
     *
     * @param order the order to authorize.
     * @return the authorization result.
     */
    public PaymentResult authorize(Order order) {
        if (order.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException();
        }
        return gateway.authorize(order);
    }
}

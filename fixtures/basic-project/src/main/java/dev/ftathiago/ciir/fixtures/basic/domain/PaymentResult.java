package dev.ftathiago.ciir.fixtures.basic.domain;

/**
 * The outcome of a payment authorization attempt.
 *
 * @param success whether the authorization succeeded.
 */
public record PaymentResult(boolean success) {
}

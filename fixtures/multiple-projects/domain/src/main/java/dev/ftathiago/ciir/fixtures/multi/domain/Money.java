package dev.ftathiago.ciir.fixtures.multi.domain;

import java.math.BigDecimal;

/**
 * An amount of money in a given currency.
 *
 * @param amount   the amount.
 * @param currency the ISO 4217 currency code.
 */
public record Money(BigDecimal amount, String currency) {
}

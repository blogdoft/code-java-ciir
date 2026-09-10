package dev.ftathiago.ciir.fixtures.multi.application;

import dev.ftathiago.ciir.fixtures.multi.domain.Money;
import java.math.BigDecimal;

/**
 * Applies discounts to prices.
 */
public class PriceCalculator {

    /**
     * Applies a percentage discount to the given price.
     *
     * @param basePrice          the price before discount.
     * @param discountPercentage the discount percentage, e.g. {@code 10} for 10%.
     * @return the discounted price, in the same currency as {@code basePrice}.
     */
    public Money applyDiscount(Money basePrice, BigDecimal discountPercentage) {
        var factor = BigDecimal.ONE.subtract(discountPercentage.divide(BigDecimal.valueOf(100)));
        var discounted = basePrice.amount().multiply(factor);
        return new Money(discounted, basePrice.currency());
    }
}

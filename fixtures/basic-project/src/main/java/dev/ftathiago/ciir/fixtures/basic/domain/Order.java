package dev.ftathiago.ciir.fixtures.basic.domain;

import java.math.BigDecimal;

/**
 * An order awaiting payment authorization.
 */
public class Order {

    private int id;
    private BigDecimal total;

    /**
     * Returns this order's identifier.
     *
     * @return the order id.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets this order's identifier.
     *
     * @param id the order id.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns this order's total amount.
     *
     * @return the total amount.
     */
    public BigDecimal getTotal() {
        return total;
    }

    /**
     * Sets this order's total amount.
     *
     * @param total the total amount.
     */
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}

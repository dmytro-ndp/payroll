package ua.payroll.model;

/**
 * Наказ директора — передумова додавання працівників і нарахування.
 * Наказ стає чинним одразу після створення; скасувати чинність можна лише викликом {@link #revoke()}.
 */
public class DirectorOrder {

    private final String orderId;
    private boolean active;

    public DirectorOrder(String orderId) {
        this.orderId = orderId;
        this.active = true;
    }

    public final boolean isActive() {
        return active;
    }

    public void revoke() {
        active = false;
    }
}

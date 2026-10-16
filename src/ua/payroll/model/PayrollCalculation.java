package ua.payroll.model;

import java.math.BigDecimal;

/**
 * Результат нарахування заробітної плати для одного працівника.
 * Створюється виключно всередині {@link PayrollSheet#calculate()}.
 */
public class PayrollCalculation {

    // Пакетна видимість: доступ лише з ua.payroll.model (PayrollSheet.findCalculation, Receipt),
    // без публічного getEmployee(), якого немає в docs/class.mmd.
    final Employee employee;
    private final BigDecimal amount;

    public PayrollCalculation(Employee employee, BigDecimal amount) {
        this.employee = employee;
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}

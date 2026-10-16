package ua.payroll.rule;

import java.math.BigDecimal;
import ua.payroll.model.Employee;

/**
 * Стандартне правило нарахування: повертає однакову фіксовану суму для кожного
 * працівника незалежно від його атрибутів.
 */
public class StandardPayrollRule extends BasePayrollRule {

    private static final BigDecimal FIXED_AMOUNT = new BigDecimal("10000.00");

    public StandardPayrollRule() {
        super();
    }

    @Override
    public BigDecimal calculate(Employee employee) {
        return FIXED_AMOUNT;
    }
}

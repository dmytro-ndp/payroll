package ua.payroll.rule;

import java.math.BigDecimal;
import ua.payroll.model.Employee;

/**
 * Контракт обчислення суми нарахування заробітної плати для одного працівника.
 */
public interface PayrollRule {

    BigDecimal calculate(Employee employee);
}

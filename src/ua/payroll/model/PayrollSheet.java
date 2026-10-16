package ua.payroll.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ua.payroll.rule.PayrollRule;

/**
 * Відомість нарахування — керує складом працівників, посилається на наказ,
 * утримує завершені нарахування. Додавання й нарахування вимагають чинного
 * {@link DirectorOrder} (FR-002–FR-004).
 */
public class PayrollSheet {

    private final DirectorOrder order;
    private PayrollRule payrollRule;
    private final Set<Employee> employees = new HashSet<>();
    private final List<PayrollCalculation> calculations = new ArrayList<>();

    public PayrollSheet(DirectorOrder order) {
        this.order = order;
    }

    public void setPayrollRule(PayrollRule payrollRule) {
        this.payrollRule = payrollRule;
    }

    /**
     * Додає працівника до відомості лише за чинного наказу й відсутності дубліката
     * за {@code employeeId}; інакше склад відомості не змінюється.
     */
    public boolean addEmployee(Employee employee) {
        if (!order.isActive()) {
            return false;
        }
        for (Employee existing : employees) {
            if (existing.getEmployeeId().equals(employee.getEmployeeId())) {
                return false;
            }
        }
        employees.add(employee);
        return true;
    }

    /**
     * Виконує нарахування для кожного працівника відомості за встановленим
     * {@link PayrollRule}; за нечинного наказу або порожньої відомості не створює
     * жодного {@link PayrollCalculation} і повертає порожній список.
     */
    public List<PayrollCalculation> calculate() {
        if (!order.isActive() || employees.isEmpty()) {
            return new ArrayList<>();
        }
        List<PayrollCalculation> created = new ArrayList<>();
        for (Employee employee : employees) {
            PayrollCalculation calculation =
                    new PayrollCalculation(employee, payrollRule.calculate(employee));
            calculations.add(calculation);
            created.add(calculation);
        }
        return created;
    }

    /**
     * Повертає завершене нарахування для працівника з тим самим {@code employeeId}
     * або {@code null}, якщо нарахування для нього ще немає.
     */
    public PayrollCalculation findCalculation(Employee employee) {
        for (PayrollCalculation calculation : calculations) {
            if (calculation.employee.getEmployeeId().equals(employee.getEmployeeId())) {
                return calculation;
            }
        }
        return null;
    }
}

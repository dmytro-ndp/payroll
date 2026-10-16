package ua.payroll.model;

/**
 * Квитанція, що подає працівникові підсумок завершеного нарахування.
 * Створюється лише якщо {@link PayrollSheet#findCalculation(Employee)} повернула
 * існуюче нарахування для цього працівника.
 */
public class Receipt {

    private final Employee employee;
    private final PayrollCalculation calculation;

    public Receipt(Employee employee, PayrollCalculation calculation) {
        this.employee = employee;
        this.calculation = calculation;
    }

    public void present() {
        System.out.println("Квитанція для працівника " + employee.getFullName()
                + " (" + employee.getEmployeeId() + "): нараховано "
                + calculation.getAmount() + " грн.");
    }
}

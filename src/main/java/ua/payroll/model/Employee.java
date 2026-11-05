package ua.payroll.model;

/**
 * Працівник підприємства, якого включають до відомості нарахування.
 * Рівність і унікальність у {@link PayrollSheet} визначаються за {@code employeeId}.
 */
public class Employee {

    private final String employeeId;
    private final String fullName;

    public Employee(String employeeId, String fullName) {
        this.employeeId = employeeId;
        this.fullName = fullName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }
}

package ua.payroll.model;

import java.util.List;

/**
 * Результат операції моделі: успіх або причина відмови з поясненням українською.
 * Для успішного читання містить список працівників.
 */
public class OperationResult {

    private final boolean success;
    private final FailureReason reason;
    private final String message;
    private final List<Employee> employees;

    private OperationResult(boolean success, FailureReason reason, String message,
            List<Employee> employees) {
        this.success = success;
        this.reason = reason;
        this.message = message;
        this.employees = employees;
    }

    public static OperationResult success(String message) {
        return new OperationResult(true, null, message, null);
    }

    public static OperationResult successWithEmployees(List<Employee> employees) {
        return new OperationResult(true, null, "", List.copyOf(employees));
    }

    public static OperationResult failure(FailureReason reason, String message) {
        return new OperationResult(false, reason, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public FailureReason getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    /** Список працівників; заповнений лише для успішного читання, інакше {@code null}. */
    public List<Employee> getEmployees() {
        return employees;
    }
}

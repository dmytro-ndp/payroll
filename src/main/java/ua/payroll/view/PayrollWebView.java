package ua.payroll.view;

import java.util.ArrayList;
import java.util.List;
import ua.payroll.model.Employee;
import ua.payroll.model.FailureReason;
import ua.payroll.model.OperationResult;

/**
 * Вигляд (MVC): перетворює результат моделі на відповідь із кодом стану й тілом.
 * Єдине місце відображення {@link FailureReason} на код; не залежить від вебфреймворку.
 */
public class PayrollWebView {

    /** Читання: 200 зі списком або код відмови. */
    public ApiResponse list(OperationResult result) {
        if (!result.isSuccess()) {
            return failure(result);
        }
        List<EmployeeItem> items = new ArrayList<>();
        for (Employee employee : result.getEmployees()) {
            items.add(new EmployeeItem(employee.getEmployeeId(), employee.getFullName()));
        }
        return new ApiResponse(200, new EmployeeListBody(items));
    }

    /** Додавання: 201 або код відмови. */
    public ApiResponse added(OperationResult result) {
        return message(result, 201);
    }

    /** Виправлення: 200 або код відмови. */
    public ApiResponse updated(OperationResult result) {
        return message(result, 200);
    }

    /** Видалення: 200 або код відмови. */
    public ApiResponse deleted(OperationResult result) {
        return message(result, 200);
    }

    private ApiResponse message(OperationResult result, int successStatus) {
        if (!result.isSuccess()) {
            return failure(result);
        }
        return new ApiResponse(successStatus, new MessageBody(result.getMessage()));
    }

    private ApiResponse failure(OperationResult result) {
        return new ApiResponse(statusOf(result.getReason()), new MessageBody(result.getMessage()));
    }

    private int statusOf(FailureReason reason) {
        return switch (reason) {
            case VALIDATION -> 400;
            case NOT_FOUND -> 404;
            case FORBIDDEN -> 409;
            case STORAGE_ERROR -> 500;
        };
    }
}

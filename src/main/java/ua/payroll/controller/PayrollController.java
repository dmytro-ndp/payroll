package ua.payroll.controller;

import ua.payroll.model.Employee;
import ua.payroll.model.PayrollSheet;
import ua.payroll.view.ApiResponse;
import ua.payroll.view.PayrollWebView;

/**
 * Контролер (MVC): приймає прості значення, викликає модель і передає результат
 * вигляду. Не містить SQL і не залежить від вебфреймворку. Перевірку даних (R1)
 * виконує модель, тому {@code null}-значення передаються їй без змін.
 */
public class PayrollController {

    private final PayrollSheet sheet;
    private final PayrollWebView view;

    public PayrollController(PayrollSheet sheet, PayrollWebView view) {
        this.sheet = sheet;
        this.view = view;
    }

    public ApiResponse list() {
        return view.list(sheet.listEmployees());
    }

    public ApiResponse add(String employeeId, String fullName) {
        return view.added(sheet.addEmployee(new Employee(employeeId, fullName)));
    }

    public ApiResponse update(String pathId, String bodyEmployeeId, String fullName) {
        return view.updated(sheet.updateEmployee(pathId, new Employee(bodyEmployeeId, fullName)));
    }

    public ApiResponse delete(String id) {
        return view.deleted(sheet.deleteEmployee(id));
    }
}

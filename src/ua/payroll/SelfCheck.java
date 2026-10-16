package ua.payroll;

import java.math.BigDecimal;
import java.util.List;
import ua.payroll.model.DirectorOrder;
import ua.payroll.model.Employee;
import ua.payroll.model.PayrollCalculation;
import ua.payroll.model.PayrollSheet;
import ua.payroll.model.Receipt;
import ua.payroll.rule.StandardPayrollRule;

/**
 * Автоматизована самоперевірка без зовнішніх залежностей (без JUnit).
 * Викликає публічний API доменних класів для сценаріїв успіху та відмови
 * (дублікат, нарахування без чинного наказу/на порожній відомості, передчасна
 * квитанція) і виводить підсумок у консоль українською (SC-003, SC-004, SC-005).
 */
public class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Самоперевірка прототипу системи нарахування заробітної плати ===");

        checkSuccessfulAddEmployee();
        checkDuplicateEmployeeRejected();
        checkCalculationRejectedWithoutActiveOrder();
        checkCalculationRejectedOnEmptySheet();
        checkSuccessfulCalculationAndReceipt();
        checkPrematureReceiptRejected();

        System.out.println("Підсумок: пройдено " + passed + ", провалено " + failed + ".");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkSuccessfulAddEmployee() {
        DirectorOrder order = new DirectorOrder("ORD-1");
        PayrollSheet sheet = new PayrollSheet(order);
        boolean added = sheet.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        check("Додавання нового працівника за чинного наказу повертає true", added);
    }

    private static void checkDuplicateEmployeeRejected() {
        DirectorOrder order = new DirectorOrder("ORD-2");
        PayrollSheet sheet = new PayrollSheet(order);
        sheet.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        boolean addedAgain = sheet.addEmployee(new Employee("EMP-1", "Інший Варіант"));
        check("Повторне додавання того самого employeeId відхиляється", !addedAgain);
    }

    private static void checkCalculationRejectedWithoutActiveOrder() {
        DirectorOrder order = new DirectorOrder("ORD-3");
        PayrollSheet sheet = new PayrollSheet(order);
        sheet.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        order.revoke();
        sheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> result = sheet.calculate();
        check("Нарахування без чинного наказу повертає порожній список", result.isEmpty());
    }

    private static void checkCalculationRejectedOnEmptySheet() {
        DirectorOrder order = new DirectorOrder("ORD-4");
        PayrollSheet sheet = new PayrollSheet(order);
        sheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> result = sheet.calculate();
        check("Нарахування на порожній відомості повертає порожній список", result.isEmpty());
    }

    private static void checkSuccessfulCalculationAndReceipt() {
        DirectorOrder order = new DirectorOrder("ORD-5");
        PayrollSheet sheet = new PayrollSheet(order);
        Employee employee = new Employee("EMP-1", "Іваненко Іван");
        sheet.addEmployee(employee);
        sheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> result = sheet.calculate();
        check("Нарахування для непорожньої відомості створює рівно один запис",
                result.size() == 1);

        PayrollCalculation calculation = sheet.findCalculation(employee);
        check("findCalculation повертає завершене нарахування для доданого працівника",
                calculation != null && calculation.getAmount().compareTo(BigDecimal.ZERO) > 0);

        Receipt receipt = new Receipt(employee, calculation);
        receipt.present();
        check("Receipt.present() виконується без винятків", true);
    }

    private static void checkPrematureReceiptRejected() {
        DirectorOrder order = new DirectorOrder("ORD-6");
        PayrollSheet sheet = new PayrollSheet(order);
        Employee employee = new Employee("EMP-1", "Іваненко Іван");
        sheet.addEmployee(employee);
        PayrollCalculation calculation = sheet.findCalculation(employee);
        check("Запит квитанції до нарахування не повертає результат", calculation == null);
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[OK] " + description);
        } else {
            failed++;
            System.out.println("[ПОМИЛКА] " + description);
        }
    }
}

package ua.payroll;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import ua.payroll.model.DirectorOrder;
import ua.payroll.model.Employee;
import ua.payroll.model.PayrollCalculation;
import ua.payroll.model.PayrollSheet;
import ua.payroll.model.Receipt;
import ua.payroll.rule.StandardPayrollRule;
import ua.payroll.storage.EmployeeStorageService;
import ua.payroll.storage.SqliteEmployeeStorageService;

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
        checkSaveEmployeeUpsertDoesNotDuplicate();
        checkFindEmployeesOfCurrentRunReturnsOnlyCurrentRun();

        System.out.println("Підсумок: пройдено " + passed + ", провалено " + failed + ".");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void checkSuccessfulAddEmployee() {
        DirectorOrder order = new DirectorOrder("ORD-1");
        PayrollSheet sheet = new PayrollSheet(order, newInMemoryStorageService());
        boolean added = sheet.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        check("Додавання нового працівника за чинного наказу повертає true", added);
    }

    private static void checkDuplicateEmployeeRejected() {
        DirectorOrder order = new DirectorOrder("ORD-2");
        PayrollSheet sheet = new PayrollSheet(order, newInMemoryStorageService());
        sheet.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        boolean addedAgain = sheet.addEmployee(new Employee("EMP-1", "Інший Варіант"));
        check("Повторне додавання того самого employeeId відхиляється", !addedAgain);
    }

    private static void checkCalculationRejectedWithoutActiveOrder() {
        DirectorOrder order = new DirectorOrder("ORD-3");
        PayrollSheet sheet = new PayrollSheet(order, newInMemoryStorageService());
        sheet.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        order.revoke();
        sheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> result = sheet.calculate();
        check("Нарахування без чинного наказу повертає порожній список", result.isEmpty());
    }

    private static void checkCalculationRejectedOnEmptySheet() {
        DirectorOrder order = new DirectorOrder("ORD-4");
        PayrollSheet sheet = new PayrollSheet(order, newInMemoryStorageService());
        sheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> result = sheet.calculate();
        check("Нарахування на порожній відомості повертає порожній список", result.isEmpty());
    }

    private static void checkSuccessfulCalculationAndReceipt() {
        DirectorOrder order = new DirectorOrder("ORD-5");
        PayrollSheet sheet = new PayrollSheet(order, newInMemoryStorageService());
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
        PayrollSheet sheet = new PayrollSheet(order, newInMemoryStorageService());
        Employee employee = new Employee("EMP-1", "Іваненко Іван");
        sheet.addEmployee(employee);
        PayrollCalculation calculation = sheet.findCalculation(employee);
        check("Запит квитанції до нарахування не повертає результат", calculation == null);
    }

    /**
     * Перевіряє поведінку upsert (FR-004a): повторне {@code saveEmployee} з тим самим
     * {@code employeeId} і новим {@code fullName} оновлює наявний рядок, а не створює
     * другий.
     */
    private static void checkSaveEmployeeUpsertDoesNotDuplicate() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            EmployeeStorageService storageService =
                    new SqliteEmployeeStorageService(connection, UUID.randomUUID().toString());
            storageService.saveEmployee(new Employee("EMP-UPSERT", "Перше Ім'я"));
            storageService.saveEmployee(new Employee("EMP-UPSERT", "Оновлене Ім'я"));

            int count = 0;
            String sql = "SELECT COUNT(*) AS cnt FROM employees WHERE employee_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "EMP-UPSERT");
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        count = rows.getInt("cnt");
                    }
                }
            }
            connection.close();
            check("Повторний saveEmployee з тим самим employeeId не створює другого рядка"
                    + " (upsert)", count == 1);
        } catch (SQLException e) {
            check("Повторний saveEmployee з тим самим employeeId не створює другого рядка"
                    + " (upsert)", false);
        }
    }

    /**
     * Перевіряє, що findEmployeesOfCurrentRun() повертає лише працівників поточного
     * запуску (FR-009), а не всі записи з попередніх run_id.
     */
    private static void checkFindEmployeesOfCurrentRunReturnsOnlyCurrentRun() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            EmployeeStorageService previousRunService =
                    new SqliteEmployeeStorageService(connection, "попередній-запуск");
            previousRunService.saveEmployee(new Employee("EMP-OLD", "Старий Працівник"));

            EmployeeStorageService currentRunService =
                    new SqliteEmployeeStorageService(connection, "поточний-запуск");
            currentRunService.saveEmployee(new Employee("EMP-NEW", "Новий Працівник"));

            List<Employee> currentRunEmployees = currentRunService.findEmployeesOfCurrentRun();
            connection.close();
            check("findEmployeesOfCurrentRun повертає лише працівників поточного запуску",
                    currentRunEmployees.size() == 1
                            && "EMP-NEW".equals(currentRunEmployees.get(0).getEmployeeId()));
        } catch (SQLException e) {
            check("findEmployeesOfCurrentRun повертає лише працівників поточного запуску", false);
        }
    }

    private static EmployeeStorageService newInMemoryStorageService() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            return new SqliteEmployeeStorageService(connection, UUID.randomUUID().toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Не вдалося відкрити тестове з'єднання SQLite"
                    + " у пам'яті — " + e.getMessage(), e);
        }
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

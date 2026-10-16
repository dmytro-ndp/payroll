package ua.payroll;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import ua.payroll.model.DirectorOrder;
import ua.payroll.model.Employee;
import ua.payroll.model.PayrollCalculation;
import ua.payroll.model.PayrollSheet;
import ua.payroll.model.Receipt;
import ua.payroll.rule.StandardPayrollRule;

/**
 * Точка входу консольного прототипу системи нарахування заробітної плати.
 * Координує єдиний інтерактивний сценарій (видання наказу, формування відомості,
 * нарахування, видача квитанції) без власної доменної логіки (SRP); увесь вивід —
 * українською мовою (FR-009, FR-016).
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Імітаційний прототип системи нарахування заробітної плати ===");

        demonstrateCalculationWithoutActiveOrder(scanner);

        System.out.print("Введіть ідентифікатор наказу директора: ");
        String orderId = scanner.nextLine();
        DirectorOrder order = new DirectorOrder(orderId);
        System.out.println("Наказ \"" + orderId + "\" видано та є чинним.");

        PayrollSheet sheet = new PayrollSheet(order);
        List<Employee> addedEmployees = formSheet(scanner, sheet);

        requestPrematureReceipt(scanner, sheet);

        calculatePayroll(scanner, sheet, addedEmployees);

        issueReceipts(scanner, sheet, addedEmployees);

        System.out.println("Розрахунковий цикл завершено. Дякуємо за використання прототипу.");
        scanner.close();
        System.exit(0);
    }

    /**
     * Крок 0 — демонстраційна перевірка відмови: нарахування без чинного наказу
     * (FR-004, FR-020, контракт «Крок 0»).
     */
    private static void demonstrateCalculationWithoutActiveOrder(Scanner scanner) {
        System.out.print("Виконати демонстраційну перевірку нарахування без чинного наказу? (так/ні): ");
        String answer = scanner.nextLine();
        if (!"так".equalsIgnoreCase(answer.trim())) {
            return;
        }
        DirectorOrder testOrder = new DirectorOrder("TEST-ORDER");
        testOrder.revoke();
        PayrollSheet testSheet = new PayrollSheet(testOrder);
        testSheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> result = testSheet.calculate();
        if (result.isEmpty()) {
            System.out.println("Відмова: нарахування неможливе без чинного наказу.");
        }
    }

    /**
     * Крок 2 — цикл формування відомості: запит кількості спроб додавання та
     * даних кожного працівника (FR-002, FR-003, FR-019, FR-020).
     */
    private static List<Employee> formSheet(Scanner scanner, PayrollSheet sheet) {
        List<Employee> added = new ArrayList<>();
        System.out.print("Скільки працівників спробувати додати до відомості? ");
        int attempts = readInt(scanner);
        for (int i = 1; i <= attempts; i++) {
            System.out.print("Ідентифікатор працівника #" + i + ": ");
            String employeeId = scanner.nextLine();
            System.out.print("Повне ім'я працівника #" + i + ": ");
            String fullName = scanner.nextLine();
            Employee employee = new Employee(employeeId, fullName);
            if (sheet.addEmployee(employee)) {
                added.add(employee);
                System.out.println("Працівника \"" + fullName + "\" (" + employeeId
                        + ") успішно додано до відомості.");
            } else {
                System.out.println("Відмова: не вдалося додати працівника \"" + fullName
                        + "\" (" + employeeId + ") — дублікат ідентифікатора або нечинний наказ.");
            }
        }
        return added;
    }

    /**
     * Крок 3 — передчасний запит квитанції до завершення нарахування (FR-007, FR-020).
     */
    private static void requestPrematureReceipt(Scanner scanner, PayrollSheet sheet) {
        System.out.print("Запитати квитанцію до нарахування? Введіть ідентифікатор працівника"
                + " або залиште порожнім, щоб пропустити: ");
        String employeeId = scanner.nextLine();
        if (employeeId.isBlank()) {
            return;
        }
        PayrollCalculation calculation = sheet.findCalculation(new Employee(employeeId, ""));
        if (calculation == null) {
            System.out.println("Відмова: нарахування для працівника \"" + employeeId
                    + "\" ще не завершене, квитанцію не видано.");
        }
    }

    /**
     * Крок 4 — нарахування заробітної плати за стандартним правилом (FR-004–FR-006).
     */
    private static void calculatePayroll(Scanner scanner, PayrollSheet sheet, List<Employee> addedEmployees) {
        System.out.print("Підтвердіть початок нарахування (Enter): ");
        scanner.nextLine();
        sheet.setPayrollRule(new StandardPayrollRule());
        List<PayrollCalculation> calculations = sheet.calculate();
        if (calculations.isEmpty()) {
            System.out.println("Відмова: нарахування неможливе — немає чинного наказу"
                    + " або відомість порожня.");
            return;
        }
        for (Employee employee : addedEmployees) {
            PayrollCalculation calculation = sheet.findCalculation(employee);
            if (calculation != null) {
                System.out.println("Нарахування завершено для " + employee.getFullName()
                        + " (" + employee.getEmployeeId() + "): " + calculation.getAmount() + " грн.");
            }
        }
    }

    /**
     * Крок 5 — видача квитанції працівникові за завершеним нарахуванням (FR-007, FR-008).
     */
    private static void issueReceipts(Scanner scanner, PayrollSheet sheet, List<Employee> addedEmployees) {
        while (true) {
            System.out.print("Введіть ідентифікатор працівника для видачі квитанції"
                    + " або залиште порожнім, щоб завершити: ");
            String employeeId = scanner.nextLine();
            if (employeeId.isBlank()) {
                return;
            }
            Employee employee = findEmployeeById(addedEmployees, employeeId);
            if (employee == null) {
                employee = new Employee(employeeId, "");
            }
            PayrollCalculation calculation = sheet.findCalculation(employee);
            if (calculation == null) {
                System.out.println("Відмова: нарахування для працівника \"" + employeeId
                        + "\" відсутнє, квитанцію не видано.");
                continue;
            }
            Receipt receipt = new Receipt(employee, calculation);
            receipt.present();
        }
    }

    private static Employee findEmployeeById(List<Employee> employees, String employeeId) {
        for (Employee employee : employees) {
            if (employee.getEmployeeId().equals(employeeId)) {
                return employee;
            }
        }
        return null;
    }

    private static int readInt(Scanner scanner) {
        while (true) {
            String line = scanner.nextLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.print("Некоректне число, спробуйте ще раз: ");
            }
        }
    }
}

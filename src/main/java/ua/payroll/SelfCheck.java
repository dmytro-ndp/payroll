package ua.payroll;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import ua.payroll.infrastructure.SqliteEmployeeStorageService;
import ua.payroll.model.DirectorOrder;
import ua.payroll.model.Employee;
import ua.payroll.model.EmployeeStorageService;
import ua.payroll.model.FailureReason;
import ua.payroll.model.OperationResult;
import ua.payroll.model.PayrollCalculation;
import ua.payroll.model.PayrollSheet;
import ua.payroll.model.Receipt;
import ua.payroll.model.StorageException;
import ua.payroll.rule.StandardPayrollRule;
import ua.payroll.view.ApiResponse;
import ua.payroll.view.EmployeeListBody;
import ua.payroll.view.PayrollWebView;

/**
 * Автоматизована самоперевірка без зовнішніх залежностей (без JUnit): модель
 * (правила R1–R6), сервіс сховища на SQLite у пам'яті та вигляд (коди відповідей).
 * Підсумок виводиться в консоль українською.
 */
public class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;

    /** Фейкове сховище в пам'яті; {@code broken = true} імітує збій бази даних. */
    private static class FakeStorage implements EmployeeStorageService {
        final List<Employee> data = new ArrayList<>();
        boolean broken = false;

        private void failIfBroken() {
            if (broken) {
                throw new StorageException("збій", new RuntimeException("імітація"));
            }
        }

        @Override
        public void saveEmployee(Employee employee) {
            failIfBroken();
            data.add(employee);
        }

        @Override
        public List<Employee> findAll() {
            failIfBroken();
            return new ArrayList<>(data);
        }

        @Override
        public boolean existsById(String employeeId) {
            failIfBroken();
            return data.stream().anyMatch(e -> e.getEmployeeId().equals(employeeId));
        }

        @Override
        public boolean update(Employee employee) {
            failIfBroken();
            for (int i = 0; i < data.size(); i++) {
                if (data.get(i).getEmployeeId().equals(employee.getEmployeeId())) {
                    data.set(i, employee);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean delete(String employeeId) {
            failIfBroken();
            return data.removeIf(e -> e.getEmployeeId().equals(employeeId));
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Самоперевірка системи нарахування заробітної плати ===");

        checkList();
        checkAdd();
        checkUpdate();
        checkDelete();
        checkCalculationAndReceipt();
        checkSqliteService();
        checkView();

        System.out.println("Підсумок: пройдено " + passed + ", провалено " + failed + ".");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---- US1: читання ----

    private static void checkList() {
        FakeStorage storage = new FakeStorage();
        PayrollSheet sheet = new PayrollSheet(new DirectorOrder("O"), storage);
        OperationResult empty = sheet.listEmployees();
        check("listEmployees: порожня БД дає успіх і порожній список",
                empty.isSuccess() && empty.getEmployees().isEmpty());

        storage.data.add(new Employee("E1", "Іван"));
        OperationResult full = sheet.listEmployees();
        check("listEmployees: непорожня БД дає список з 1 працівником",
                full.isSuccess() && full.getEmployees().size() == 1);

        storage.broken = true;
        check("listEmployees: збій БД дає STORAGE_ERROR",
                reason(sheet.listEmployees()) == FailureReason.STORAGE_ERROR);
    }

    // ---- US2: додавання ----

    private static void checkAdd() {
        FakeStorage storage = new FakeStorage();
        DirectorOrder order = new DirectorOrder("O");
        PayrollSheet sheet = new PayrollSheet(order, storage);

        check("addEmployee: коректні дані — успіх і запис у БД",
                sheet.addEmployee(new Employee("E1", "Іван")).isSuccess()
                        && storage.data.size() == 1);
        check("addEmployee: дублікат — VALIDATION (R2)",
                reason(sheet.addEmployee(new Employee("E1", "Інший"))) == FailureReason.VALIDATION);
        check("addEmployee: порожнє поле — VALIDATION (R1)",
                reason(sheet.addEmployee(new Employee("", "Петро"))) == FailureReason.VALIDATION);
        check("addEmployee: пробільне поле — VALIDATION (R1)",
                reason(sheet.addEmployee(new Employee("E2", "   "))) == FailureReason.VALIDATION);
        check("addEmployee: null-поля — VALIDATION (R1)",
                reason(sheet.addEmployee(new Employee(null, null))) == FailureReason.VALIDATION);
        check("addEmployee: відмови не змінили БД", storage.data.size() == 1);

        storage.broken = true;
        check("addEmployee: збій БД — STORAGE_ERROR",
                reason(sheet.addEmployee(new Employee("E3", "Олег"))) == FailureReason.STORAGE_ERROR);
        storage.broken = false;
        check("addEmployee: за збою БД колекція й БД не змінені", storage.data.size() == 1);

        order.revoke();
        check("addEmployee: нечинний наказ — FORBIDDEN (R4)",
                reason(sheet.addEmployee(new Employee("E4", "Ігор"))) == FailureReason.FORBIDDEN);
        check("addEmployee: R4 перевіряється раніше за R1",
                reason(sheet.addEmployee(new Employee("", ""))) == FailureReason.FORBIDDEN);
    }

    // ---- US3: виправлення ----

    private static void checkUpdate() {
        FakeStorage storage = new FakeStorage();
        DirectorOrder order = new DirectorOrder("O");
        PayrollSheet sheet = new PayrollSheet(order, storage);
        storage.data.add(new Employee("E1", "Іван"));

        check("updateEmployee: успіх змінює лише fullName",
                sheet.updateEmployee("E1", new Employee("E1", "Новий")).isSuccess()
                        && storage.data.get(0).getFullName().equals("Новий")
                        && storage.data.get(0).getEmployeeId().equals("E1"));
        check("updateEmployee: employeeId у тілі необов'язковий",
                sheet.updateEmployee("E1", new Employee(null, "Ще")).isSuccess());
        check("updateEmployee: неіснуючий — NOT_FOUND (R5)",
                reason(sheet.updateEmployee("X", new Employee("X", "Хтось"))) == FailureReason.NOT_FOUND);
        check("updateEmployee: порожнє ім'я — VALIDATION (R1)",
                reason(sheet.updateEmployee("E1", new Employee("E1", " "))) == FailureReason.VALIDATION);
        check("updateEmployee: розбіжність id — VALIDATION (R3)",
                reason(sheet.updateEmployee("E1", new Employee("E2", "Хтось"))) == FailureReason.VALIDATION);

        storage.broken = true;
        check("updateEmployee: збій БД — STORAGE_ERROR",
                reason(sheet.updateEmployee("E1", new Employee("E1", "Х"))) == FailureReason.STORAGE_ERROR);
        storage.broken = false;

        order.revoke();
        check("updateEmployee: нечинний наказ — FORBIDDEN (R4)",
                reason(sheet.updateEmployee("E1", new Employee("E1", "Х"))) == FailureReason.FORBIDDEN);
    }

    // ---- US4: видалення ----

    private static void checkDelete() {
        FakeStorage storage = new FakeStorage();
        DirectorOrder order = new DirectorOrder("O");
        PayrollSheet sheet = new PayrollSheet(order, storage);
        storage.data.add(new Employee("E1", "Іван"));

        check("deleteEmployee: неіснуючий — NOT_FOUND (R5)",
                reason(sheet.deleteEmployee("X")) == FailureReason.NOT_FOUND);

        storage.broken = true;
        check("deleteEmployee: збій БД — STORAGE_ERROR",
                reason(sheet.deleteEmployee("E1")) == FailureReason.STORAGE_ERROR);
        storage.broken = false;

        order.revoke();
        check("deleteEmployee: нечинний наказ — FORBIDDEN (R4)",
                reason(sheet.deleteEmployee("E1")) == FailureReason.FORBIDDEN);
        check("deleteEmployee: запис не видалено за відмов", storage.data.size() == 1);

        PayrollSheet active = new PayrollSheet(new DirectorOrder("O2"), storage);
        check("deleteEmployee: успіх — запис зникає з БД і списку",
                active.deleteEmployee("E1").isSuccess() && storage.data.isEmpty()
                        && active.listEmployees().getEmployees().isEmpty());
    }

    // ---- Наявні сценарії нарахування й квитанції ----

    private static void checkCalculationAndReceipt() {
        DirectorOrder order = new DirectorOrder("ORD-3");
        PayrollSheet sheet = new PayrollSheet(order, new FakeStorage());
        Employee employee = new Employee("EMP-1", "Іваненко Іван");
        sheet.addEmployee(employee);
        sheet.setPayrollRule(new StandardPayrollRule());
        check("Нарахування для непорожньої відомості створює рівно один запис",
                sheet.calculate().size() == 1);

        PayrollCalculation calculation = sheet.findCalculation(employee);
        check("findCalculation повертає завершене нарахування для доданого працівника",
                calculation != null && calculation.getAmount().compareTo(BigDecimal.ZERO) > 0);
        new Receipt(employee, calculation).present();
        check("Receipt.present() виконується без винятків", true);

        PayrollSheet empty = new PayrollSheet(new DirectorOrder("ORD-4"), new FakeStorage());
        empty.setPayrollRule(new StandardPayrollRule());
        check("Нарахування на порожній відомості повертає порожній список",
                empty.calculate().isEmpty());

        order.revoke();
        check("Нарахування без чинного наказу повертає порожній список",
                sheet.calculate().isEmpty());

        PayrollSheet premature = new PayrollSheet(new DirectorOrder("ORD-6"), new FakeStorage());
        premature.addEmployee(new Employee("EMP-1", "Іваненко Іван"));
        check("Запит квитанції до нарахування не повертає результат",
                premature.findCalculation(new Employee("EMP-1", "")) == null);
    }

    // ---- Сервіс сховища на SQLite у пам'яті ----

    private static void checkSqliteService() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            EmployeeStorageService service =
                    new SqliteEmployeeStorageService(connection, UUID.randomUUID().toString());
            check("SQLite: findAll на порожній БД повертає порожній список",
                    service.findAll().isEmpty());
            service.saveEmployee(new Employee("B", "Борис"));
            service.saveEmployee(new Employee("A", "Андрій"));
            List<Employee> all = service.findAll();
            check("SQLite: findAll повертає записи, упорядковані за employeeId",
                    all.size() == 2 && all.get(0).getEmployeeId().equals("A"));
            check("SQLite: existsById знаходить наявний і не знаходить відсутній",
                    service.existsById("A") && !service.existsById("Z"));
            check("SQLite: update змінює fullName і повертає false для відсутнього",
                    service.update(new Employee("A", "Анна"))
                            && service.findAll().get(0).getFullName().equals("Анна")
                            && !service.update(new Employee("Z", "Ніхто")));
            check("SQLite: delete видаляє запис і повертає false для відсутнього",
                    service.delete("A") && !service.delete("A") && service.findAll().size() == 1);
            boolean thrown = false;
            try {
                service.saveEmployee(new Employee("B", "Дублікат"));
            } catch (StorageException e) {
                thrown = true;
            }
            check("SQLite: вставка дубліката кидає StorageException", thrown);
        } catch (SQLException e) {
            check("SQLite: з'єднання у пам'яті відкрито", false);
        }
    }

    // ---- Вигляд ----

    private static void checkView() {
        PayrollWebView view = new PayrollWebView();
        OperationResult ok = OperationResult.success("Гаразд");

        ApiResponse list = view.list(OperationResult.successWithEmployees(
                List.of(new Employee("E1", "Іван"))));
        check("view.list: успіх — 200 з EmployeeListBody",
                list.status() == 200 && list.body() instanceof EmployeeListBody body
                        && body.employees().size() == 1);
        check("view.list: STORAGE_ERROR — 500", view.list(failure(FailureReason.STORAGE_ERROR))
                .status() == 500);

        check("view.added: успіх — 201", view.added(ok).status() == 201);
        check("view.added: VALIDATION — 400",
                view.added(failure(FailureReason.VALIDATION)).status() == 400);
        check("view.added: FORBIDDEN — 409",
                view.added(failure(FailureReason.FORBIDDEN)).status() == 409);
        check("view.added: STORAGE_ERROR — 500",
                view.added(failure(FailureReason.STORAGE_ERROR)).status() == 500);

        check("view.updated: успіх — 200", view.updated(ok).status() == 200);
        check("view.updated: VALIDATION — 400",
                view.updated(failure(FailureReason.VALIDATION)).status() == 400);
        check("view.updated: NOT_FOUND — 404",
                view.updated(failure(FailureReason.NOT_FOUND)).status() == 404);
        check("view.updated: FORBIDDEN — 409",
                view.updated(failure(FailureReason.FORBIDDEN)).status() == 409);
        check("view.updated: STORAGE_ERROR — 500",
                view.updated(failure(FailureReason.STORAGE_ERROR)).status() == 500);

        check("view.deleted: успіх — 200", view.deleted(ok).status() == 200);
        check("view.deleted: NOT_FOUND — 404",
                view.deleted(failure(FailureReason.NOT_FOUND)).status() == 404);
        check("view.deleted: FORBIDDEN — 409",
                view.deleted(failure(FailureReason.FORBIDDEN)).status() == 409);
        check("view.deleted: STORAGE_ERROR — 500",
                view.deleted(failure(FailureReason.STORAGE_ERROR)).status() == 500);
    }

    private static OperationResult failure(FailureReason reason) {
        return OperationResult.failure(reason, "Відмова");
    }

    private static FailureReason reason(OperationResult result) {
        return result.getReason();
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

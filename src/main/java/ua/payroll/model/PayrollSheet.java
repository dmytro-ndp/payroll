package ua.payroll.model;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import ua.payroll.rule.PayrollRule;

/**
 * Відомість нарахування — керує складом працівників, посилається на наказ,
 * утримує завершені нарахування. Додавання й нарахування вимагають чинного
 * {@link DirectorOrder} (FR-002–FR-004).
 */
public class PayrollSheet {

    private final DirectorOrder order;
    private final EmployeeStorageService storageService;
    private PayrollRule payrollRule;
    private final List<Employee> employees = new ArrayList<>();
    private final List<PayrollCalculation> calculations = new ArrayList<>();

    @Inject
    public PayrollSheet(DirectorOrder order, EmployeeStorageService storageService) {
        this.order = order;
        this.storageService = storageService;
    }

    public void setPayrollRule(PayrollRule payrollRule) {
        this.payrollRule = payrollRule;
    }

    /** Читає всіх працівників з бази даних і синхронізує з ними колекцію відомості. */
    public OperationResult listEmployees() {
        try {
            syncEmployees();
            return OperationResult.successWithEmployees(new ArrayList<>(employees));
        } catch (StorageException e) {
            return storageFailure();
        }
    }

    /**
     * Додає працівника: перевірки R4 (наказ) → R1 (обов'язкові поля) → R2 (унікальність);
     * за відмови база даних і колекція не змінюються.
     */
    public OperationResult addEmployee(Employee employee) {
        if (!order.isActive()) {
            return inactiveOrder();
        }
        if (isBlank(employee.getEmployeeId()) || isBlank(employee.getFullName())) {
            return requiredFieldsFailure();
        }
        try {
            if (storageService.existsById(employee.getEmployeeId())) {
                return OperationResult.failure(FailureReason.VALIDATION,
                        "Працівник з ідентифікатором \"" + employee.getEmployeeId()
                                + "\" уже існує.");
            }
            storageService.saveEmployee(employee);
            syncEmployees();
            return OperationResult.success("Працівника \"" + employee.getFullName()
                    + "\" успішно додано.");
        } catch (StorageException e) {
            return storageFailure();
        }
    }

    /**
     * Виправляє ПІБ працівника: R4 → R5 (існування) → R1 → R3 (збіг {@code employeeId}
     * у тілі зі шляхом). Змінюється лише {@code fullName}.
     */
    public OperationResult updateEmployee(String id, Employee employee) {
        if (!order.isActive()) {
            return inactiveOrder();
        }
        try {
            if (isBlank(id) || !storageService.existsById(id)) {
                return notFound(id);
            }
            if (isBlank(employee.getFullName())) {
                return requiredFieldsFailure();
            }
            if (!isBlank(employee.getEmployeeId()) && !employee.getEmployeeId().equals(id)) {
                return OperationResult.failure(FailureReason.VALIDATION,
                        "Ідентифікатор у тілі запиту не збігається з ідентифікатором у шляху.");
            }
            storageService.update(new Employee(id, employee.getFullName()));
            syncEmployees();
            return OperationResult.success("Дані працівника \"" + id + "\" успішно змінено.");
        } catch (StorageException e) {
            return storageFailure();
        }
    }

    /** Видаляє працівника: R4 → R5 (існування); R6 — видалення завжди дозволене за чинного наказу. */
    public OperationResult deleteEmployee(String id) {
        if (!order.isActive()) {
            return inactiveOrder();
        }
        try {
            if (isBlank(id) || !storageService.delete(id)) {
                return notFound(id);
            }
            syncEmployees();
            return OperationResult.success("Працівника \"" + id + "\" успішно видалено.");
        } catch (StorageException e) {
            return storageFailure();
        }
    }

    private void syncEmployees() {
        List<Employee> stored = storageService.findAll();
        employees.clear();
        employees.addAll(stored);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static OperationResult inactiveOrder() {
        return OperationResult.failure(FailureReason.FORBIDDEN,
                "Зміни заборонені: наказ директора нечинний.");
    }

    private static OperationResult requiredFieldsFailure() {
        return OperationResult.failure(FailureReason.VALIDATION,
                "Ідентифікатор і ПІБ працівника є обов'язковими та не можуть бути порожніми.");
    }

    private static OperationResult notFound(String id) {
        return OperationResult.failure(FailureReason.NOT_FOUND,
                "Працівника з ідентифікатором \"" + id + "\" не знайдено.");
    }

    private static OperationResult storageFailure() {
        return OperationResult.failure(FailureReason.STORAGE_ERROR,
                "Помилка сховища даних. Спробуйте пізніше.");
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

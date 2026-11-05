package ua.payroll.storage;

import java.util.List;
import ua.payroll.model.Employee;

/**
 * Сервіс збереження даних працівників у базі даних SQLite (єдине місце SQL-запитів,
 * принцип X конституції). Приховує деталі персистентності від доменного класу
 * {@link ua.payroll.model.PayrollSheet} (принцип IX — інверсія залежностей).
 */
public interface EmployeeStorageService {

    /**
     * Зберігає дані працівника: якщо запис із таким {@code employeeId} уже існує —
     * оновлює його атрибути (upsert), інакше створює новий запис.
     */
    void saveEmployee(Employee employee);

    /**
     * Повертає працівників, збережених або оновлених у поточному запуску застосунку,
     * упорядкованих за {@code employeeId}.
     */
    List<Employee> findEmployeesOfCurrentRun();
}

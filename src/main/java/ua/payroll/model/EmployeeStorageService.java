package ua.payroll.model;

import java.util.List;

/**
 * Сервіс збереження даних працівників (єдине місце SQL-запитів, принцип X конституції).
 * Приховує деталі персистентності від {@link PayrollSheet} (принцип IX).
 * Збій бази даних у будь-якому методі призводить до {@link StorageException}.
 */
public interface EmployeeStorageService {

    /** Вставляє новий запис працівника. */
    void saveEmployee(Employee employee);

    /** Повертає всіх працівників, упорядкованих за {@code employeeId}. */
    List<Employee> findAll();

    /** Перевіряє, чи існує працівник із таким {@code employeeId}. */
    boolean existsById(String employeeId);

    /** Змінює {@code full_name}; повертає {@code false}, якщо запису немає. */
    boolean update(Employee employee);

    /** Видаляє запис; повертає {@code false}, якщо запису немає. */
    boolean delete(String employeeId);
}

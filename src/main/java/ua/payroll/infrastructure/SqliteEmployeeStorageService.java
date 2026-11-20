package ua.payroll.infrastructure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import ua.payroll.model.Employee;
import ua.payroll.model.EmployeeStorageService;
import ua.payroll.model.StorageException;

/**
 * Реалізація {@link EmployeeStorageService} на основі SQLite. Єдиний клас проєкту,
 * що формує SQL-запити (принцип X конституції). З'єднання та ідентифікатор поточного
 * запуску впроваджуються через конструктор (Constructor Injection).
 */
public class SqliteEmployeeStorageService implements EmployeeStorageService {

    private final Connection connection;
    private final String runId;

    public SqliteEmployeeStorageService(Connection connection, String runId) {
        this.connection = connection;
        this.runId = runId;
        createTableIfMissing();
    }

    private void createTableIfMissing() {
        String ddl = "CREATE TABLE IF NOT EXISTS employees ("
                + "employee_id TEXT PRIMARY KEY, "
                + "full_name TEXT NOT NULL, "
                + "run_id TEXT NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        } catch (SQLException e) {
            throw new StorageException("Не вдалося створити таблицю `employees` у SQLite", e);
        }
    }

    @Override
    public void saveEmployee(Employee employee) {
        String insert = "INSERT INTO employees (employee_id, full_name, run_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getFullName());
            statement.setString(3, runId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Не вдалося зберегти дані працівника \""
                    + employee.getEmployeeId() + "\" у SQLite", e);
        }
    }

    @Override
    public List<Employee> findAll() {
        List<Employee> result = new ArrayList<>();
        String select = "SELECT employee_id, full_name FROM employees ORDER BY employee_id";
        try (PreparedStatement statement = connection.prepareStatement(select);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(new Employee(rows.getString("employee_id"),
                        rows.getString("full_name")));
            }
        } catch (SQLException e) {
            throw new StorageException("Не вдалося прочитати дані працівників з SQLite", e);
        }
        return result;
    }

    @Override
    public boolean existsById(String employeeId) {
        String select = "SELECT 1 FROM employees WHERE employee_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, employeeId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new StorageException("Не вдалося перевірити наявність працівника \""
                    + employeeId + "\" у SQLite", e);
        }
    }

    @Override
    public boolean update(Employee employee) {
        String update = "UPDATE employees SET full_name = ? WHERE employee_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getEmployeeId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StorageException("Не вдалося змінити дані працівника \""
                    + employee.getEmployeeId() + "\" у SQLite", e);
        }
    }

    @Override
    public boolean delete(String employeeId) {
        String delete = "DELETE FROM employees WHERE employee_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(delete)) {
            statement.setString(1, employeeId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StorageException("Не вдалося видалити працівника \""
                    + employeeId + "\" з SQLite", e);
        }
    }
}

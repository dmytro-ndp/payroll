package ua.payroll.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import ua.payroll.model.Employee;

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
            System.out.println("Відмова: не вдалося створити таблицю `employees` у SQLite — "
                    + e.getMessage());
        }
    }

    @Override
    public void saveEmployee(Employee employee) {
        String upsert = "INSERT INTO employees (employee_id, full_name, run_id) VALUES (?, ?, ?) "
                + "ON CONFLICT(employee_id) DO UPDATE SET full_name = excluded.full_name, "
                + "run_id = excluded.run_id";
        try (PreparedStatement statement = connection.prepareStatement(upsert)) {
            statement.setString(1, employee.getEmployeeId());
            statement.setString(2, employee.getFullName());
            statement.setString(3, runId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Відмова: не вдалося зберегти дані працівника \""
                    + employee.getEmployeeId() + "\" у SQLite (можливо, тека `target` недоступна"
                    + " для запису) — " + e.getMessage());
        }
    }

    @Override
    public List<Employee> findEmployeesOfCurrentRun() {
        List<Employee> result = new ArrayList<>();
        String select = "SELECT employee_id, full_name FROM employees WHERE run_id = ? "
                + "ORDER BY employee_id";
        try (PreparedStatement statement = connection.prepareStatement(select)) {
            statement.setString(1, runId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new Employee(rows.getString("employee_id"),
                            rows.getString("full_name")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Відмова: не вдалося прочитати дані працівників поточного запуску"
                    + " з SQLite — " + e.getMessage());
        }
        return result;
    }
}

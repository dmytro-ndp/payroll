package ua.payroll.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import ua.payroll.model.DirectorOrder;
import ua.payroll.storage.EmployeeStorageService;
import ua.payroll.storage.SqliteEmployeeStorageService;

/**
 * Guice-модуль складання застосунку (composition root). Прив'язує
 * {@link EmployeeStorageService} до {@link SqliteEmployeeStorageService} і надає
 * з'єднання з файловою базою даних SQLite у теці {@code target}.
 */
public class PayrollModule extends AbstractModule {

    private final DirectorOrder order;
    private final String runId;

    public PayrollModule(DirectorOrder order, String runId) {
        this.order = order;
        this.runId = runId;
    }

    @Override
    protected void configure() {
        bind(EmployeeStorageService.class).to(SqliteEmployeeStorageService.class);
        bind(DirectorOrder.class).toInstance(order);
    }

    @Provides
    @Singleton
    Connection provideConnection() {
        try {
            Path targetDir = Path.of("target");
            Files.createDirectories(targetDir);
            return DriverManager.getConnection("jdbc:sqlite:target/payroll.db");
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Відмова: не вдалося відкрити з'єднання з базою даних"
                    + " SQLite у теці `target` — " + e.getMessage(), e);
        }
    }

    @Provides
    @Singleton
    SqliteEmployeeStorageService provideStorageService(Connection connection) {
        return new SqliteEmployeeStorageService(connection, runId);
    }
}

package ua.payroll.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import ua.payroll.model.Employee;

/**
 * Формує статичну HTML-сторінку з таблицею працівників поточного запуску
 * (рядковою конкатенацією, без шаблонізаторів). Не залежить від Guice чи JDBC.
 */
public class EmployeeWebPageGenerator {

    public void generate(List<Employee> employees, Path targetFile) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"uk\">\n")
                .append("<head><meta charset=\"UTF-8\">")
                .append("<title>Працівники поточного запуску</title></head>\n")
                .append("<body>\n")
                .append("<h1>Працівники поточного запуску</h1>\n")
                .append("<table border=\"1\">\n")
                .append("<tr><th>Ідентифікатор</th><th>Повне ім'я</th></tr>\n");
        for (Employee employee : employees) {
            html.append("<tr><td>").append(employee.getEmployeeId()).append("</td><td>")
                    .append(employee.getFullName()).append("</td></tr>\n");
        }
        html.append("</table>\n</body>\n</html>\n");

        try {
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            Files.writeString(targetFile, html.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Відмова: не вдалося записати вебсторінку "
                    + targetFile + " — " + e.getMessage(), e);
        }
    }
}

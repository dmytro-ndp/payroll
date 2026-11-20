package ua.payroll.infrastructure;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import ua.payroll.controller.PayrollController;
import ua.payroll.view.ApiResponse;

/**
 * Реалізація {@link WebServer} на Javalin. Єдиний клас, що імпортує {@code io.javalin}
 * і Jackson (принцип XII): оголошує маршрути, читає тіло запиту, серіалізує відповідь
 * вигляду та віддає статичні файли з {@code src/main/resources/public}.
 */
public class JavalinWebServer implements WebServer {

    /** Тіло запиту додавання/виправлення; поля можуть бути {@code null}. */
    public static class EmployeeRequest {
        public String employeeId;
        public String fullName;
    }

    @Override
    public void start(PayrollController controller, int port) {
        Javalin app = Javalin.create(config ->
                config.staticFiles.add("/public", Location.CLASSPATH));

        app.get("/api/Payroll", ctx -> respond(ctx, controller.list()));
        app.post("/api/Payroll", ctx -> {
            EmployeeRequest request = readRequest(ctx);
            respond(ctx, controller.add(request.employeeId, request.fullName));
        });
        app.put("/api/Payroll/{id}", ctx -> {
            EmployeeRequest request = readRequest(ctx);
            respond(ctx, controller.update(ctx.pathParam("id"), request.employeeId,
                    request.fullName));
        });
        app.delete("/api/Payroll/{id}", ctx -> respond(ctx, controller.delete(ctx.pathParam("id"))));

        app.start(port);
    }

    /** Нечитабельне або порожнє тіло дає запит з {@code null}-полями (400 за R1). */
    private EmployeeRequest readRequest(Context ctx) {
        try {
            EmployeeRequest request = ctx.bodyAsClass(EmployeeRequest.class);
            return request != null ? request : new EmployeeRequest();
        } catch (Exception e) {
            return new EmployeeRequest();
        }
    }

    private void respond(Context ctx, ApiResponse response) {
        ctx.status(response.status()).json(response.body());
    }
}

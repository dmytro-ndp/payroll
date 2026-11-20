package ua.payroll;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.UUID;
import ua.payroll.controller.PayrollController;
import ua.payroll.infrastructure.PayrollModule;
import ua.payroll.infrastructure.WebServer;
import ua.payroll.model.DirectorOrder;
import ua.payroll.model.PayrollSheet;
import ua.payroll.view.PayrollWebView;

/**
 * Точка входу вебзастосунку системи нарахування заробітної плати (composition root):
 * складає об'єкти MVC, задає чинний наказ директора й запускає вебсервер на порту 8080.
 * Власної доменної логіки не містить (SRP).
 */
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        DirectorOrder order = new DirectorOrder("ORDER-001");
        String runId = UUID.randomUUID().toString();
        Injector injector = Guice.createInjector(new PayrollModule(order, runId));

        PayrollSheet sheet = injector.getInstance(PayrollSheet.class);
        PayrollController controller = new PayrollController(sheet, new PayrollWebView());
        WebServer server = injector.getInstance(WebServer.class);

        try {
            server.start(controller, PORT);
        } catch (RuntimeException e) {
            System.out.println("Відмова: не вдалося запустити вебсервер на порту " + PORT
                    + " (можливо, порт зайнятий іншою програмою) — " + e.getMessage());
            System.exit(1);
        }
        System.out.println("Вебсервер запущено: http://localhost:" + PORT + "/");
    }
}

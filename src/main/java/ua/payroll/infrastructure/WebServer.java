package ua.payroll.infrastructure;

import ua.payroll.controller.PayrollController;

/** Вебсервер, що передає запити контролеру; не залежить від типів вебфреймворку. */
public interface WebServer {

    void start(PayrollController controller, int port);
}

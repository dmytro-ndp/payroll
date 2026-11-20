package ua.payroll.view;

import java.util.List;

/** Тіло відповіді зі списком працівників. */
public record EmployeeListBody(List<EmployeeItem> employees) {
}

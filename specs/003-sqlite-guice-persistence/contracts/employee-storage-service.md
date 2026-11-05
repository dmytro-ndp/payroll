# Contract: EmployeeStorageService

**Пакет**: `ua.payroll.storage`

**Роль у системі**: Сервісний шар доступу до даних (принцип X конституції). Єдине місце, де
дозволено SQL-запити до SQLite. Приховує деталі персистентності від доменного класу
`PayrollSheet` (принцип IX — інверсія залежностей).

## Інтерфейс

```java
package ua.payroll.storage;

import java.util.List;
import ua.payroll.model.Employee;

/**
 * Сервіс збереження даних працівників у базі даних SQLite.
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
```

## Передумови та постумови

### `saveEmployee(Employee employee)`

- **Передумова**: `employee != null`, `employee.getEmployeeId()` не `null`.
- **Постумова (новий employeeId)**: у таблиці `employees` з'являється рівно один новий рядок з
  поточним `run_id`.
- **Постумова (наявний employeeId)**: наявний рядок оновлюється новими значеннями `full_name` і
  `run_id`; кількість рядків у таблиці для цього `employeeId` залишається 1 (FR-004a).
- **Помилка**: якщо з'єднання з базою даних недоступне або файл/тека `target` недоступні для
  запису — метод НЕ ПОВИНЕН аварійно завершити застосунок без пояснення; виняток обгортається й
  супроводжується зрозумілим повідомленням українською в консоль (Edge Case «тека недоступна для
  запису»).

### `findEmployeesOfCurrentRun()`

- **Передумова**: немає (може викликатися навіть якщо жодного працівника не збережено).
- **Постумова**: повертає список, що містить лише працівників із `run_id`, рівним ідентифікатору
  поточного запуску; якщо жодного такого запису немає — повертає порожній список (не `null`),
  Edge Case «вебсторінка з порожньою таблицею».

## Реалізація: SqliteEmployeeStorageService

- Конструктор: `SqliteEmployeeStorageService(java.sql.Connection connection, String runId)`
  (впровадження через конструктор; `Connection` і `runId` надаються Guice-модулем
  `PayrollModule`).
- Усі SQL-запити (DDL `CREATE TABLE IF NOT EXISTS`, upsert, SELECT за `run_id`) розташовані лише
  в цьому класі (принцип X); жодні інші класи проєкту не формують SQL-рядків.

## Контракт композиції (composition root)

```java
// ua.payroll.Main — єдине місце виклику Guice.createInjector(...) та injector.getInstance(...)
Injector injector = Guice.createInjector(new PayrollModule(order, runId));
PayrollSheet sheet = injector.getInstance(PayrollSheet.class); // замість `new PayrollSheet(order)`
```

- `PayrollModule` — єдиний клас, що успадковує `com.google.inject.AbstractModule` (FR-007).
- `PayrollSheet` — єдиний клас проєкту, що створюється через `injector.getInstance(...)` замість
  оператора `new` (SC-005).
- Заборона Service Locator (принцип IX) стосується доменного коду: жодний доменний клас (зокрема
  `PayrollSheet`) не звертається до `Injector` напряму. Композиційний корінь `Main` — не доменний
  код; йому ДОЗВОЛЕНО додатково викликати `injector.getInstance(EmployeeStorageService.class)`
  після виконання сценаріїв, щоб прочитати дані для вебсторінки (FR-009, `data-model.md`, п. 5,
  `tasks.md` T016). Це не суперечить SC-005, оскільки через DI-контейнер *створюється* лише один
  клас (`PayrollSheet`) — виклик для `EmployeeStorageService` лише повертає вже наявний
  singleton-екземпляр, а не створює новий клас-ціле.

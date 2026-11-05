# Data Model: Збереження даних працівників у SQLite через Google Guice

## Employee (без змін полів, нова поведінка зберігання)

| Поле | Тип | Опис | Правила |
|---|---|---|---|
| `employeeId` | `String` | Ідентифікатор працівника | Унікальний ключ у `PayrollSheet` (існуюче правило) і первинний ключ у таблиці SQLite |
| `fullName` | `String` | Повне ім'я працівника | Без змін |

Клас `Employee` не змінюється. Персистентність реалізована поза доменним класом — у
`SqliteEmployeeStorageService`.

## Таблиця SQLite `employees` (нова)

| Стовпець | Тип SQLite | Опис |
|---|---|---|
| `employee_id` | `TEXT PRIMARY KEY` | Відповідає `Employee.employeeId`; ключ upsert |
| `full_name` | `TEXT NOT NULL` | Відповідає `Employee.fullName` |
| `run_id` | `TEXT NOT NULL` | Ідентифікатор поточного запуску застосунку (UUID), встановлюється при кожному збереженні/оновленні запису |

DDL (виконується сервісом при ініціалізації, `CREATE TABLE IF NOT EXISTS`):

```sql
CREATE TABLE IF NOT EXISTS employees (
    employee_id TEXT PRIMARY KEY,
    full_name   TEXT NOT NULL,
    run_id      TEXT NOT NULL
);
```

Upsert-запит (FR-004a):

```sql
INSERT INTO employees (employee_id, full_name, run_id)
VALUES (?, ?, ?)
ON CONFLICT(employee_id) DO UPDATE SET
    full_name = excluded.full_name,
    run_id = excluded.run_id;
```

Запит для вебсторінки (лише поточний запуск, FR-009):

```sql
SELECT employee_id, full_name FROM employees WHERE run_id = ? ORDER BY employee_id;
```

## EmployeeStorageService (інтерфейс, пакет `ua.payroll.storage`)

| Метод | Призначення |
|---|---|
| `void saveEmployee(Employee employee)` | Зберегти (додати або оновити — upsert за `employeeId`) дані працівника |
| `List<Employee> findEmployeesOfCurrentRun()` | Повернути працівників, збережених/оновлених у поточному запуску (для вебсторінки) |

Назви методів — мовою предметної області («зберегти працівника», «знайти працівників поточного
запуску»), без технічних термінів на кшталт «insert»/«upsert» у публічному API.

## SqliteEmployeeStorageService (реалізація)

- Поля: `Connection connection`, `String runId` (впроваджуються через конструктор).
- При створенні виконує DDL `CREATE TABLE IF NOT EXISTS`.
- `saveEmployee` — виконує upsert-запит з параметром `runId`.
- `findEmployeesOfCurrentRun` — виконує SELECT з фільтром `run_id = runId`, мапує рядки у нові
  об'єкти `Employee`.

## PayrollSheet (зміна конструктора)

- Було: `PayrollSheet(DirectorOrder order)`.
- Стає: `PayrollSheet(DirectorOrder order, EmployeeStorageService storageService)` —
  **обидві залежності впроваджуються через конструктор** (Constructor Injection, FR-006).
- `addEmployee(Employee employee)`: після успішного додавання до внутрішнього `Set<Employee>`
  (наявна логіка дублікатів/чинності наказу не змінюється) викликає
  `storageService.saveEmployee(employee)`.
- Решта методів (`setPayrollRule`, `calculate`, `findCalculation`) — без змін.

## Guice composition root (зміна в Main)

1. `Main` читає `orderId` зі `Scanner` (як і раніше), створює `DirectorOrder order = new
   DirectorOrder(orderId)` (без змін — `DirectorOrder` не є Guice-залежністю).
2. Генерує `String runId` (наприклад, `UUID.randomUUID().toString()`) один раз на запуск.
3. Створює `Injector injector = Guice.createInjector(new PayrollModule(order, runId));`
   — модуль прив'язує `EmployeeStorageService` до `SqliteEmployeeStorageService` і надає
   `DirectorOrder` та `Connection` як інстанс-прив'язки/провайдери.
4. `PayrollSheet sheet = injector.getInstance(PayrollSheet.class);` — єдине місце створення
   об'єкта через DI-контейнер (SC-005); усі інші об'єкти (`Employee`, `Receipt`,
   `StandardPayrollRule` тощо) — як і раніше, через `new`.
5. Після виконання наявних сценаріїв: `List<Employee> current =
   injector.getInstance(EmployeeStorageService.class).findEmployeesOfCurrentRun();` і виклик
   `new EmployeeWebPageGenerator().generate(current, Path.of("target/employees.html"));`.

## PayrollModule (новий клас, пакет `ua.payroll.di`)

- Успадковує `com.google.inject.AbstractModule`.
- Конструктор приймає `DirectorOrder order, String runId`.
- `configure()`: `bind(EmployeeStorageService.class).to(SqliteEmployeeStorageService.class);`,
  `bind(DirectorOrder.class).toInstance(order);`.
- `@Provides @Singleton Connection provideConnection()`: відкриває
  `DriverManager.getConnection("jdbc:sqlite:target/payroll.db")`, створює теку `target`, якщо
  відсутня.
- `@Provides @Singleton SqliteEmployeeStorageService provideStorageService(Connection connection)`:
  `new SqliteEmployeeStorageService(connection, runId)` (`runId` — поле модуля, захоплене
  конструктором).

## EmployeeWebPageGenerator (новий клас, пакет `ua.payroll.report`)

| Метод | Призначення |
|---|---|
| `void generate(List<Employee> employees, Path targetFile)` | Сформувати HTML-файл із таблицею працівників (рядок на кожного, або порожня таблиця) |

Не залежить від Guice чи JDBC — отримує вже готовий список `Employee`.

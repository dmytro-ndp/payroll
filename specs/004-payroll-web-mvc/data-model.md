# Data Model: задача 004

## Сутності предметної області (model)

### Employee (клас-частина, без змін)
| Поле | Тип | Правила |
|------|-----|---------|
| `employeeId` | String | унікальний ідентифікатор; обов'язковий (R1), не дублюється (R2) |
| `fullName` | String | обов'язкове (R1); єдине поле, що змінюється при виправленні (R3) |

### PayrollSheet (клас-ціле)
- Залежності (конструктор): `DirectorOrder`, `EmployeeStorageService`.
- Колекція `employees` — кеш, синхронізований з БД після кожної зміни.
- Нові операції (повертають `OperationResult`, крім читання):

| Метод | Перевірки (порядок) | Успіх |
|-------|---------------------|-------|
| `listEmployees()` | — (збій БД → `STORAGE_ERROR`) | `OperationResult` зі списком усіх з БД |
| `addEmployee(Employee)` | R4, R1, R2 (`existsById`) | збереження в БД + колекція |
| `updateEmployee(String id, Employee)` | R4, R5, R1, R3 | `update` у БД + колекція |
| `deleteEmployee(String id)` | R4, R5 (R6 — завжди дозволено) | `delete` у БД + колекція |

Щоб не плодити класів, `OperationResult` має необов'язкове поле `employees` (див. нижче).

### DirectorOrder (без змін)
Чинність (`isActive`) дозволяє зміни (R4). У запуску — `ORDER-001`, чинний.

### OperationResult
| Поле | Тип | Опис |
|------|-----|------|
| `success` | boolean | ознака успіху |
| `reason` | FailureReason або null | причина відмови |
| `message` | String | пояснення українською |
| `employees` | List<Employee> | заповнюється лише успішним читанням |

### FailureReason (enum)
`VALIDATION` · `NOT_FOUND` · `FORBIDDEN` · `STORAGE_ERROR`.

### StorageException
Unchecked; кидається реалізаціями `EmployeeStorageService` за збою БД.

## Інтерфейс EmployeeStorageService (model)

| Метод | Призначення |
|-------|-------------|
| `void saveEmployee(Employee)` | вставка нового запису (наявний метод) |
| `List<Employee> findAll()` | усі працівники, за `employeeId` (замінює `findEmployeesOfCurrentRun`) |
| `boolean existsById(String)` | перевірка існування (R2, R5) |
| `boolean update(Employee)` | оновлення `full_name`; `false`, якщо запису немає |
| `boolean delete(String id)` | видалення; `false`, якщо запису немає |

## Сховище (SQLite, без змін схеми)

```text
employees(employee_id TEXT PRIMARY KEY, full_name TEXT NOT NULL, run_id TEXT NOT NULL)
```
`run_id` записується при вставці (UUID запуску), при читанні не використовується.

## Вигляд (view)

| Тип | Поля |
|-----|------|
| `ApiResponse` (record) | `int status`, `Object body` |
| `EmployeeItem` (record) | `employeeId`, `fullName` |
| `EmployeeListBody` (record) | `List<EmployeeItem> employees` |
| `MessageBody` (record) | `String message` |

Відображення `FailureReason` → код: VALIDATION 400 · NOT_FOUND 404 · FORBIDDEN 409 · STORAGE_ERROR 500;
успіх: читання/виправлення/видалення 200, додавання 201.

## Запит (infrastructure)
`EmployeeRequest` (клас із Jackson): `employeeId`, `fullName`, обидва допускають `null`. Лише в `JavalinWebServer`.

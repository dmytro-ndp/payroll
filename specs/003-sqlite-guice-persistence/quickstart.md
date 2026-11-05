# Quickstart: Перевірка збереження даних у SQLite й вебсторінки

Передумови: JDK 21 і Apache Maven в `PATH` (`java -version`, `mvn -v`).

## 1. Збирання проєкту

```powershell
mvn clean package
```

Очікуваний результат: збирання завершується `BUILD SUCCESS`; у теці `target` з'являється jar-файл
(`payroll-<version>-jar-with-dependencies.jar` або еквівалент, залежно від обраного плагіна
збирання — див. `research.md`, розділ 2).

## 2. Запуск застосовку та додавання працівників (User Story 1)

```powershell
mvn exec:java
```

або запуск зібраного jar:

```powershell
java -jar target\payroll-<version>-jar-with-dependencies.jar
```

Під час інтерактивного сценарію:
1. Пропустити демонстраційну перевірку (`ні`).
2. Ввести ідентифікатор наказу (наприклад, `ORD-001`).
3. На запит кількості спроб ввести `2`, додати двох працівників із різними `employeeId`.
4. Завершити решту сценарію (Enter для нарахування, порожній рядок для завершення видачі
   квитанцій).

**Перевірка**: відкрити `target/payroll.db` будь-яким SQLite-клієнтом (наприклад,
`sqlite3 target/payroll.db "SELECT * FROM employees;"`) і переконатися, що обидва працівники
присутні з коректними `employee_id`/`full_name` (відповідає Acceptance Scenario 1 User Story 1).

## 3. Перевірка збереження між запусками (Acceptance Scenario 2)

Повторно запустити застосунок (крок 2), ввести того самого `employeeId`, що й раніше, з новим
`fullName`. Перевірити через `sqlite3`, що рядок із цим `employeeId` оновився (нове ім'я), а не
задублювався — `SELECT COUNT(*) FROM employees WHERE employee_id = '...';` повертає `1`.

## 4. Перевірка вебсторінки (User Story 2)

Після завершення сценаріїв відкрити `target/employees.html` у браузері:

```powershell
start target\employees.html
```

**Перевірка**: таблиця містить рядок для кожного працівника, доданого саме в поточному запуску
(Acceptance Scenario 1). Якщо в поточному запуску не додавати жодного працівника (на запит
кількості спроб ввести `0`), сторінка все одно формується з порожньою таблицею, навіть якщо
`payroll.db` містить дані попередніх запусків (Acceptance Scenario 2).

## 5. Самоперевірка без JUnit

```powershell
mvn exec:java -Dexec.mainClass="ua.payroll.SelfCheck"
```

Очікуваний результат: усі перевірки виводять `[OK]`, підсумковий рядок — `пройдено N, провалено 0`.

## 6. Перевірка єдиної DI-залежності (SC-005)

Переглянути вихідний код `Main.java` — переконатися, що оператор `new` НЕ використовується для
`PayrollSheet` (лише `injector.getInstance(PayrollSheet.class)`), а `Guice.createInjector(...)`
викликається рівно один раз, у єдиному методі композиції застосунку.

## Довідково

- Контракт сервісу збереження: `contracts/employee-storage-service.md`.
- Модель даних і схема SQLite: `data-model.md`.
- Рішення щодо версій залежностей і підходу до складання: `research.md`.

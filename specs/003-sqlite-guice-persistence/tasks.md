---

description: "Task list template for feature implementation"
---

# Tasks: Збереження даних працівників у SQLite через Google Guice

**Input**: Design documents from `/specs/003-sqlite-guice-persistence/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/employee-storage-service.md, quickstart.md

**Tests**: Автоматизовані тести виконуються без JUnit, через наявний клас `ua.payroll.SelfCheck`
(стиль `check(description, condition)`); окремі тестові завдання не генеруються, перевірки
додаються до `SelfCheck.java` в межах відповідних завдань реалізації.

**Organization**: Завдання згруповано за користувацькими історіями (User Story 1/2/3) зі
spec.md для незалежної реалізації й перевірки кожної історії.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Можна виконувати паралельно (різні файли, без залежностей)
- **[Story]**: До якої користувацької історії належить завдання (US1, US2, US3)
- У кожному описі вказано точний шлях до файлу

## Path Conventions

- Один Maven-проєкт: `src/main/java/ua/payroll/...`, `src/test/java/` (порожній каталог)
- Корінь репозиторію: `pom.xml`, `README.md`, `docs/*.mmd`
- Згенеровані артефакти: `target/payroll.db`, `target/employees.html`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Перетворення наявного прототипу на стандартний Maven-проєкт

- [X] T001 Створити `pom.xml` у корені репозиторію: `artifactId` `payroll`, `groupId` `ua.payroll`,
  `maven-compiler-plugin` з `<release>21</release>`, залежності
  `com.google.inject:guice:7.0.0` та `org.xerial:sqlite-jdbc:3.51.1.0`, `maven-assembly-plugin`
  з дескриптором `jar-with-dependencies` і `mainClass` `ua.payroll.Main`, а також
  `exec-maven-plugin` (`mainClass` `ua.payroll.Main`) для команди `mvn exec:java`
- [X] T002 Перенести наявні джерела з `src/ua/payroll/**` до стандартної структури
  `src/main/java/ua/payroll/**` без зміни вмісту, імен класів і пакетів (`Main.java`,
  `SelfCheck.java`, `model/DirectorOrder.java`, `model/Employee.java`,
  `model/PayrollCalculation.java`, `model/PayrollSheet.java`, `model/Receipt.java`,
  `rule/PayrollRule.java`, `rule/BasePayrollRule.java`, `rule/StandardPayrollRule.java`); видалити
  стару теку `src/ua`
- [X] T003 [P] Створити порожній каталог `src/test/java/` відповідно до стандартної структури
  Maven (автотестування залишається через `SelfCheck`, не через JUnit)
- [X] T004 Виконати `mvn -q clean compile` і переконатися, що перенесені класи компілюються без
  змін поведінки (контрольна точка перед додаванням нової логіки)

**Checkpoint**: Проєкт має стандартну Maven-структуру й компілюється без змін бізнес-логіки

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Базова інфраструктура персистентності й DI, без якої неможлива жодна користувацька
історія

**⚠️ CRITICAL**: Жодна з історій US1/US2 не може бути реалізована до завершення цієї фази

- [X] T005 Створити інтерфейс `EmployeeStorageService` у
  `src/main/java/ua/payroll/storage/EmployeeStorageService.java` з методами
  `void saveEmployee(Employee employee)` і `List<Employee> findEmployeesOfCurrentRun()` (сигнатури
  й Javadoc — за контрактом `contracts/employee-storage-service.md`)
- [X] T006 Створити клас `SqliteEmployeeStorageService` у
  `src/main/java/ua/payroll/storage/SqliteEmployeeStorageService.java`, що реалізує
  `EmployeeStorageService`; конструктор `SqliteEmployeeStorageService(Connection connection,
  String runId)` виконує `CREATE TABLE IF NOT EXISTS employees (employee_id TEXT PRIMARY KEY,
  full_name TEXT NOT NULL, run_id TEXT NOT NULL)` (методи `saveEmployee`/
  `findEmployeesOfCurrentRun` поки без реалізації — додаються в US1/US2)
- [X] T007 Створити клас `PayrollModule` (успадковує `com.google.inject.AbstractModule`) у
  `src/main/java/ua/payroll/di/PayrollModule.java`: конструктор приймає `DirectorOrder order,
  String runId`; `configure()` виконує
  `bind(EmployeeStorageService.class).to(SqliteEmployeeStorageService.class)` і
  `bind(DirectorOrder.class).toInstance(order)`; метод `@Provides @Singleton Connection
  provideConnection()` створює теку `target` (якщо відсутня) і відкриває
  `DriverManager.getConnection("jdbc:sqlite:target/payroll.db")`; метод `@Provides @Singleton
  SqliteEmployeeStorageService provideStorageService(Connection connection)` повертає
  `new SqliteEmployeeStorageService(connection, runId)` (поле `runId` модуля)
- [X] T008 Змінити конструктор `PayrollSheet` у `src/main/java/ua/payroll/model/PayrollSheet.java`
  з `PayrollSheet(DirectorOrder order)` на
  `PayrollSheet(DirectorOrder order, EmployeeStorageService storageService)` (Constructor
  Injection, FR-006); зберегти поле `storageService`; решта методів (`setPayrollRule`,
  `calculate`, `findCalculation`) — без змін
- [X] T009 Змінити `src/main/java/ua/payroll/Main.java`: згенерувати `String runId =
  UUID.randomUUID().toString()` один раз на запуск; створити `Injector injector =
  Guice.createInjector(new PayrollModule(order, runId))`; замінити виклик
  `new PayrollSheet(order)` на `injector.getInstance(PayrollSheet.class)` (єдине місце
  `injector.getInstance(...)` у проєкті, SC-005)

**Checkpoint**: Фундамент готовий — DI-з'єднання працює, таблиця `employees` створюється, але
збереження/читання даних ще не реалізовано

---

## Phase 3: User Story 1 - Збереження даних працівників після роботи з відомістю (Priority: P1) 🎯 MVP

**Goal**: Кожен доданий до відомості працівник автоматично зберігається у файлі SQLite
`target/payroll.db` і не втрачається між запусками (upsert за `employeeId`)

**Independent Test**: Додати кількох працівників у сценарії, завершити програму, відкрити
`target/payroll.db` через `sqlite3` і переконатися, що записи присутні з коректними значеннями

### Implementation for User Story 1

- [X] T010 [US1] Реалізувати `saveEmployee(Employee employee)` у `SqliteEmployeeStorageService`
  (`src/main/java/ua/payroll/storage/SqliteEmployeeStorageService.java`) через upsert-запит
  `INSERT INTO employees (employee_id, full_name, run_id) VALUES (?, ?, ?) ON
  CONFLICT(employee_id) DO UPDATE SET full_name = excluded.full_name, run_id = excluded.run_id`
  (FR-004a: наявний запис оновлюється, дубліката не створюється)
- [X] T011 [US1] У методі `addEmployee(Employee employee)` класу `PayrollSheet`
  (`src/main/java/ua/payroll/model/PayrollSheet.java`) викликати
  `storageService.saveEmployee(employee)` одразу після успішного додавання працівника до
  внутрішнього `Set<Employee>` (наявна логіка перевірки дублікатів і чинності наказу — без змін)
- [X] T012 [US1] Додати обробку помилок запису в `PayrollModule.provideConnection()` і/або
  `SqliteEmployeeStorageService.saveEmployee` (`src/main/java/ua/payroll/di/PayrollModule.java`,
  `src/main/java/ua/payroll/storage/SqliteEmployeeStorageService.java`): перехоплювати
  `SQLException`/`IOException` при недоступності теки `target` для запису й виводити в консоль
  зрозуміле повідомлення українською без аварійного завершення застосунку (Edge Case)
- [X] T013 [US1] Додати до `src/main/java/ua/payroll/SelfCheck.java` перевірки
  (`check(description, condition)`) для поведінки збереження: повторне `saveEmployee` з тим самим
  `employeeId` і новим `fullName` не створює другого рядка, а оновлює наявний (запит `SELECT
  COUNT(*) FROM employees WHERE employee_id = ?` повертає `1`)

**Checkpoint**: User Story 1 повністю функціональна й перевіряється незалежно через `sqlite3`

---

## Phase 4: User Story 2 - Перегляд збережених даних на вебсторінці (Priority: P2)

**Goal**: Після виконання сценаріїв формується статична HTML-сторінка `target/employees.html` із
таблицею працівників, доданих/оновлених лише в поточному запуску

**Independent Test**: Після завершення сценаріїв відкрити `target/employees.html` у браузері й
переконатися, що таблиця містить рядок для кожного працівника поточного запуску (і лише їх)

### Implementation for User Story 2

- [X] T014 [P] [US2] Реалізувати `findEmployeesOfCurrentRun()` у `SqliteEmployeeStorageService`
  (`src/main/java/ua/payroll/storage/SqliteEmployeeStorageService.java`) через запит
  `SELECT employee_id, full_name FROM employees WHERE run_id = ? ORDER BY employee_id`, мапуючи
  рядки результату в нові об'єкти `Employee`; якщо записів немає — повертати порожній список (не
  `null`)
- [X] T015 [P] [US2] Створити клас `EmployeeWebPageGenerator` у
  `src/main/java/ua/payroll/report/EmployeeWebPageGenerator.java` з методом
  `void generate(List<Employee> employees, Path targetFile)`, що формує HTML-сторінку з таблицею
  (стовпці — ідентифікатор і повне ім'я працівника) рядковою конкатенацією/`StringBuilder` без
  шаблонізаторів і записує файл через `java.nio.file.Files`; порожній список формує таблицю без
  рядків даних (Edge Case)
- [X] T016 [US2] У `src/main/java/ua/payroll/Main.java` після завершення наявних сценаріїв
  додати виклик `injector.getInstance(EmployeeStorageService.class).findEmployeesOfCurrentRun()`
  і передати результат у `new EmployeeWebPageGenerator().generate(current,
  Path.of("target/employees.html"))` (залежить від T014, T015)

**Checkpoint**: User Story 1 і User Story 2 працюють незалежно; вебсторінка показує лише дані
поточного запуску

---

## Phase 5: User Story 3 - Збирання й запуск проєкту засобами Maven (Priority: P3)

**Goal**: Проєкт збирається й запускається стандартними командами Maven на чистому клоні, кроки
описані в README.md

**Independent Test**: На чистому клоні виконати описані в README.md команди `mvn clean package` і
запуск; переконатися, що сценарії виконуються без ручного керування classpath

### Implementation for User Story 3

- [X] T017 [US3] Створити/доповнити `README.md` у корені репозиторію (FR-011): опис
  функціональності фічі (збереження в SQLite, вебсторінка), команди збирання (`mvn clean
  package`), запуску (`mvn exec:java` і/або `java -jar
  target/payroll-<version>-jar-with-dependencies.jar`) та шлях до сформованої вебсторінки
  (`target/employees.html`)
- [X] T018 [US3] Виконати перевірку за `quickstart.md` (кроки 1–2) на чистому клоні: `mvn clean
  package` завершується `BUILD SUCCESS`, запуск через `mvn exec:java` виконує наявні сценарії й
  консольні повідомлення українською мовою залишаються незмінними по суті (SC-002, SC-004)

**Checkpoint**: Усі три користувацькі історії функціонують незалежно; проєкт відтворюваний на
чистому клоні лише командами Maven

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Документація UML і підсумкова перевірка відповідності принципам конституції

- [X] T019 [P] Оновити `docs/class.mmd` (FR-012): додати клас `PayrollModule` (успадковує
  `AbstractModule`), інтерфейс `EmployeeStorageService`, реалізацію
  `SqliteEmployeeStorageService`, клас `EmployeeWebPageGenerator` і зв'язок залежності
  `PayrollSheet ..> EmployeeStorageService` (використання інтерфейсу, впровадженого через
  конструктор)
- [X] T020 [P] Оновити `docs/sequence.mmd` (FR-013): показати створення `Injector` через
  `Guice.createInjector(new PayrollModule(order, runId))`, прив'язку
  `EmployeeStorageService -> SqliteEmployeeStorageService` в модулі та отримання `PayrollSheet`
  через `injector.getInstance(PayrollSheet.class)` із залежністю, впровадженою в конструктор
- [X] T021 Виконати `mvn exec:java -Dexec.mainClass="ua.payroll.SelfCheck"` (quickstart.md, крок
  5) і переконатися, що всі перевірки виводять `[OK]`, підсумковий рядок —
  `пройдено N, провалено 0`
- [X] T022 Переглянути `src/main/java/ua/payroll/Main.java` і підтвердити SC-005
  (quickstart.md, крок 6): оператор `new` НЕ використовується для `PayrollSheet` (лише
  `injector.getInstance(PayrollSheet.class)`), а `Guice.createInjector(...)` викликається рівно
  один раз

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: без залежностей — виконується першим
- **Foundational (Phase 2)**: залежить від Setup; БЛОКУЄ усі користувацькі історії
- **User Story 1 (Phase 3)**: залежить від Foundational; не залежить від US2/US3
- **User Story 2 (Phase 4)**: залежить від Foundational і логічно від даних, які зберігає US1
  (`findEmployeesOfCurrentRun` читає те, що зберігає `saveEmployee`), але реалізується в окремих
  файлах і може виконуватися паралельно з доробкою US1 після Foundational
- **User Story 3 (Phase 5)**: залежить від Foundational; документаційно описує результат
  US1/US2, тому практично виконується після них, хоча код не залежить від US1/US2 напряму
- **Polish (Phase 6)**: залежить від завершення US1, US2 і US3

### Within Each User Story

- US1: T010 (upsert SQL) → T011 (виклик з `PayrollSheet`) → T012 (обробка помилок) → T013
  (перевірки `SelfCheck`)
- US2: T014 і T015 — паралельно (різні файли) → T016 (інтеграція в `Main`, залежить від обох)
- US3: T017 → T018

### Parallel Opportunities

- T003 (Setup) можна виконувати паралельно з T002
- T014 і T015 (US2) — різні файли, без взаємних залежностей, можна виконувати паралельно
- T019 і T020 (Polish) — різні файли UML-діаграм, можна виконувати паралельно

---

## Parallel Example: User Story 2

```bash
# Запустити одночасно дві незалежні частини User Story 2:
Task: "Реалізувати findEmployeesOfCurrentRun() у SqliteEmployeeStorageService"
Task: "Створити клас EmployeeWebPageGenerator з методом generate(...)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Завершити Phase 1: Setup
2. Завершити Phase 2: Foundational (КРИТИЧНО — блокує всі історії)
3. Завершити Phase 3: User Story 1
4. **ЗУПИНИТИСЯ й ПЕРЕВІРИТИ**: перевірити User Story 1 незалежно через `sqlite3`
5. За потреби — продемонструвати MVP

### Incremental Delivery

1. Setup + Foundational → фундамент готовий
2. Додати User Story 1 → перевірити незалежно → MVP готовий
3. Додати User Story 2 → перевірити незалежно (вебсторінка)
4. Додати User Story 3 → перевірити незалежно (чистий клон + Maven)
5. Phase 6: UML-документація й підсумкова перевірка принципів конституції

---

## Notes

- [P] завдання = різні файли, без залежностей між ними
- Мітка історії ([US1]/[US2]/[US3]) відповідає за трасування завдання до конкретної користувацької
  історії зі spec.md
- Кожна користувацька історія має бути завершена й перевірена незалежно перед переходом до
  наступної (або паралельно після Foundational, якщо ресурси дозволяють)
- Тестування виконується через наявний `SelfCheck` (без JUnit), перевірки додаються в межах T013
- Коментарі в коді — українською мовою; імена класів/методів/пакетів — англійською (принцип VI
  конституції)

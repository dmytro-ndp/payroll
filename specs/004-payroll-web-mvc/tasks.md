---

description: "Список завдань для задачі 004: вебінтерфейс системи нарахування заробітної плати за MVC"
---

# Tasks: Вебінтерфейс системи нарахування заробітної плати за MVC

**Input**: Проєктні документи з `specs/004-payroll-web-mvc/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/rest-api.md](contracts/rest-api.md), [contracts/java-interfaces.md](contracts/java-interfaces.md), [quickstart.md](quickstart.md)

**Tests**: Окремих JUnit-тестів специфікація не вимагає (принцип I, `research.md` R-7). Перевірки моделі й вигляду
додаються до наявного `SelfCheck` (запуск: `mvn exec:java "-Dexec.mainClass=ua.payroll.SelfCheck"`), наскрізні
перевірки HTTP — `curl` за `quickstart.md`.

**Organization**: Завдання згруповано за історіями користувача, щоб кожну історію можна було реалізувати й перевірити окремо.

## Format: `[ID] [P?] [Story] Опис зі шляхом до файлу`

- **[P]**: можна виконувати паралельно (різні файли, немає залежностей від незавершених завдань)
- **[Story]**: історія користувача (US1–US5) зі `spec.md`
- Усі шляхи від кореня репозиторію; `J` = `src/main/java/ua/payroll`, `R` = `src/main/resources/public`
- Назви класів і методів — англійською, коментарі, повідомлення й тексти сторінки — українською (принцип VI)

## Phase 1: Setup (спільна інфраструктура)

**Purpose**: залежності Maven і пакети за ролями MVC

- [X] T001 Додати в `pom.xml` залежності `io.javalin:javalin:6.7.0`, `com.fasterxml.jackson.core:jackson-databind:2.18.2`, `org.slf4j:slf4j-simple:2.0.16` (наявні Guice 7.0.0 і sqlite-jdbc лишити); перевірити актуальні патч-версії на Maven Central
- [X] T002 Створити пакети й перенести наявні файли через `git mv`: `J/storage/EmployeeStorageService.java` → `J/model/`, `J/storage/SqliteEmployeeStorageService.java` → `J/infrastructure/`, `J/di/PayrollModule.java` → `J/infrastructure/`; виправити `package` та `import` у перенесених файлах, а також у `J/Main.java`, `J/model/PayrollSheet.java`, `J/SelfCheck.java`; видалити порожні теки `storage` і `di`; створити порожні теки `J/view`, `J/controller`, `src/main/resources/public`

---

## Phase 2: Foundational (блокуючі передумови)

**Purpose**: спільні типи моделі, сховища й вигляду, потрібні всім історіям

**⚠️ CRITICAL**: жодна історія не починається, доки ця фаза не завершена. Проєкт може не компілюватися до завершення T018 (Main залежить від нових класів) — збирання перевіряється на контрольній точці US1.

- [X] T003 [P] Створити enum `FailureReason` зі значеннями `VALIDATION`, `NOT_FOUND`, `FORBIDDEN`, `STORAGE_ERROR` у `J/model/FailureReason.java` (коментарі українською)
- [X] T004 [P] Створити unchecked-виняток `StorageException extends RuntimeException` (конструктор з повідомленням і причиною) у `J/model/StorageException.java`
- [X] T005 Створити клас `OperationResult` у `J/model/OperationResult.java` за `data-model.md`: поля `success`, `reason`, `message`, `employees` (`List<Employee>`, лише для успішного читання); статичні фабрики `success(String message)`, `successWithEmployees(List<Employee>)`, `failure(FailureReason reason, String message)` і геттери (залежить від T003)
- [X] T006 [P] Створити записи (`record`) у пакеті `view`: `J/view/ApiResponse.java` (`int status`, `Object body`), `J/view/EmployeeItem.java` (`employeeId`, `fullName`), `J/view/EmployeeListBody.java` (`List<EmployeeItem> employees`), `J/view/MessageBody.java` (`String message`)
- [X] T007 Оновити інтерфейс `EmployeeStorageService` у `J/model/EmployeeStorageService.java`: `saveEmployee(Employee)` (вставка нового запису), `List<Employee> findAll()` (за `employeeId`), `boolean existsById(String)`, `boolean update(Employee)` (змінює `full_name`, `false` якщо запису немає), `boolean delete(String)` (`false` якщо запису немає); видалити `findEmployeesOfCurrentRun`; описати в Javadoc, що збій БД кидає `StorageException`
- [X] T008 Реалізувати нові методи SQL у `J/infrastructure/SqliteEmployeeStorageService.java` (залежить від T004, T007): `findAll`, `existsById`, `update`, `delete` через `PreparedStatement`; `saveEmployee` — `INSERT` із `run_id` = ідентифікатор запуску; видалити `findEmployeesOfCurrentRun`; усі `catch (SQLException)` (разом із `createTableIfMissing`) замінити на `throw new StorageException(<повідомлення українською>, e)` — друку «Відмова…» у консоль більше немає
- [X] T009 [P] Створити інтерфейс `WebServer` з методом `void start(PayrollController controller, int port)` у `J/infrastructure/WebServer.java` (без типів вебфреймворку; клас `PayrollController` створюється в T012 — до того тимчасово може не компілюватися)

**Checkpoint**: типи моделі, сховище й записи вигляду готові — можна переходити до історій

---

## Phase 3: User Story 1 — Перегляд працівників на вебсторінці (Priority: P1) 🎯 MVP

**Goal**: на `http://localhost:8080/` показується таблиця всіх працівників з БД; `GET /api/Payroll` повертає 200.

**Independent Test**: запуск з порожньою БД → таблиця порожня, `GET /api/Payroll` → 200 і `{"employees":[]}`; після перезапуску дані зберігаються.

### Implementation for User Story 1

- [X] T010 [US1] Додати до `J/model/PayrollSheet.java` метод `OperationResult listEmployees()`: читає `storageService.findAll()`, синхронізує колекцію `employees` з БД, повертає `successWithEmployees(...)`; `StorageException` → `failure(STORAGE_ERROR, <повідомлення українською>)`
- [X] T011 [US1] Створити `J/view/PayrollWebView.java` (без імпортів Javalin/Jackson) з методом `ApiResponse list(OperationResult r)`: успіх → `ApiResponse(200, EmployeeListBody)` (відображення `Employee` → `EmployeeItem`); відмова → через приватний метод відображення `FailureReason` → код (VALIDATION 400, NOT_FOUND 404, FORBIDDEN 409, STORAGE_ERROR 500) із тілом `MessageBody(message)`; цей метод відображення далі використовують усі історії (залежить від T006, T010)
- [X] T012 [US1] Створити `J/controller/PayrollController.java` з конструктором `PayrollController(PayrollSheet sheet, PayrollWebView view)` і методом `ApiResponse list()` — викликає `sheet.listEmployees()` і передає результат у `view.list(...)`; без SQL, без залежності від вебфреймворку (залежить від T011)
- [X] T013 [US1] Створити `J/infrastructure/JavalinWebServer.java`, що реалізує `WebServer`: `Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH))`, маршрут `GET /api/Payroll` → `ctx.status(r.status()).json(r.body())`, `start(controller, port)` викликає `app.start(port)`; тільки цей клас імпортує `io.javalin.*` і `com.fasterxml.jackson.*` (залежить від T009, T012)
- [X] T014 [US1] Додати в `configure()` у `J/infrastructure/PayrollModule.java` прив'язку `bind(WebServer.class).to(JavalinWebServer.class).in(Singleton.class)`; решту прив'язок (`EmployeeStorageService`, `DirectorOrder`, Singleton `Connection`) зберегти (залежить від T013)
- [X] T015 [P] [US1] Створити `R/index.html` (`lang="uk"`): підключення Tailwind (`<script src="https://cdn.tailwindcss.com"></script>`), `style.css`, `script.js`; заголовок; таблиця `id="employee-table"` з тілом `id="employee-rows"` і колонками «Ідентифікатор», «ПІБ», «Дії»; порожній блок повідомлення `id="message"`; форма-заготовка `id="employee-form"` з полями `employeeId`, `fullName` і кнопками «Зберегти», «Скасувати» (кнопку «Скасувати» сховати класом `hidden`)
- [X] T016 [P] [US1] Створити `R/script.js`: функція `loadEmployees()` робить `fetch('/api/Payroll')` і малює рядки в `#employee-rows` (кнопки «Змінити» та «Видалити» з `data-id`), викликається при завантаженні сторінки; скрипт лише надсилає запити й показує відповіді — без перевірки даних, кольорів і розмірів
- [X] T017 [P] [US1] Створити `R/style.css` зі стилем «Офіційний»: стримана синьо-сіра палітра, заголовки шрифтом із зарубками (`serif`), чіткі рамки таблиці, прямі кути (`border-radius: 0`), без анімацій, контрастний текст
- [X] T018 [US1] Переписати `J/Main.java` за FR-014/FR-015: видалити весь консольний сценарій (Scanner, кроки 0–6, `readInt` тощо); створити `DirectorOrder("ORDER-001")`, `Injector` з `PayrollModule(order, runId)`, отримати `PayrollSheet`, створити `new PayrollWebView()` і `new PayrollController(sheet, view)`, отримати `WebServer` з `Injector` і викликати `start(controller, 8080)`; вивести рядок «Вебсервер запущено: http://localhost:8080/»; видалити файл `J/report/EmployeeWebPageGenerator.java` і теку `report` (залежить від T014)
- [X] T019 [US1] Оновити `J/SelfCheck.java` (залежить від T010, T011): прибрати перевірки, що спираються на видалені/змінені методи (`findEmployeesOfCurrentRun`, консольний вивід «Відмова…», генератор сторінки); додати перевірки `listEmployees()` на фейковому `EmployeeStorageService` (порожній і непорожній список, збій → `STORAGE_ERROR`) та `PayrollWebView.list` (успіх → 200 з `EmployeeListBody`, `STORAGE_ERROR` → 500)
- [X] T020 [US1] Перевірка: `mvn clean compile`, `mvn exec:java "-Dexec.mainClass=ua.payroll.SelfCheck"`, `mvn exec:java` і `curl -i localhost:8080/api/Payroll` (200 і порожній список), відкрити `http://localhost:8080/` — таблиця порожня

**Checkpoint**: US1 працює самостійно — це MVP (читання й перегляд даних)

---

## Phase 4: User Story 2 — Додавання працівника (Priority: P1)

**Goal**: `POST /api/Payroll` з перевіркою R4, R1, R2 і форма додавання на сторінці.

**Independent Test**: коректний POST → 201 і запис у БД; повтор того ж `employeeId` → 400; порожнє/пробільне поле або порожнє тіло → 400.

### Implementation for User Story 2

- [X] T021 [US2] Змінити в `J/model/PayrollSheet.java` метод `addEmployee(Employee)` на `OperationResult addEmployee(Employee)`: порядок перевірок R4 (нечинний наказ → `FORBIDDEN`) → R1 (відсутнє/порожнє/пробільне `employeeId` чи `fullName` → `VALIDATION`) → R2 (`existsById` → `VALIDATION`); успіх — `saveEmployee`, синхронізація колекції з `findAll`, `success(...)`; `StorageException` → `STORAGE_ERROR`; за відмови БД і колекція не змінюються; повідомлення українською
- [X] T022 [US2] Додати до `J/view/PayrollWebView.java` метод `ApiResponse added(OperationResult r)`: успіх → 201 з `MessageBody`, відмова → код за відображенням з T011 (залежить від T021)
- [X] T023 [US2] Додати до `J/controller/PayrollController.java` метод `ApiResponse add(String employeeId, String fullName)`: створює `Employee` (для `null`-значень — `Employee` із `null`/порожніми полями, щоб перевірка R1 лишилась у моделі), викликає `sheet.addEmployee`, передає результат у `view.added` (залежить від T022)
- [X] T024 [US2] У `J/infrastructure/JavalinWebServer.java` додати DTO `EmployeeRequest` (поля `employeeId`, `fullName`, допускають `null`) і маршрут `POST /api/Payroll`: тіло читається `ctx.bodyAsClass(EmployeeRequest.class)` у `try/catch` — за помилки розбору створюється `EmployeeRequest` з `null`-полями; результат `controller.add(...)` віддається як `ctx.status(r.status()).json(r.body())` (залежить від T023)
- [X] T025 [US2] Доповнити `R/script.js`: обробник `submit` форми в режимі додавання надсилає `POST /api/Payroll` з JSON `{employeeId, fullName}`, показує `message` із відповіді в `#message` (перемикає імена класів `message-success`/`message-error` за кодом відповіді, без кольорів у скрипті), після кожної зміни викликає `loadEmployees()`, очищує форму після успіху; повідомлення не зникає, доки його не замінить наступне (без таймерів)
- [X] T026 [P] [US2] Додати до `R/style.css` класи `message-success` і `message-error` — по-різному оформлені повідомлення (різний колір/рамка, контрастні) у синьо-сірій палітрі стилю «Офіційний»
- [X] T027 [US2] Розширити `J/SelfCheck.java` (залежить від T021, T022): сценарії `addEmployee` для R4 (`FORBIDDEN`), R1 (порожнє, пробіли, `null`), R2 (дублікат), успіху і `STORAGE_ERROR` на фейковому сервісі; перевірка, що за відмови БД не змінено; `PayrollWebView.added` (201/400/409/500)
- [X] T028 [US2] Перевірка за `quickstart.md` кроки 2–4 (`curl` POST: 201, дублікат 400, пробільне поле/порожнє тіло 400) і через форму на сторінці (рядок з'являється, повідомлення про успіх/відмову відрізняються)

**Checkpoint**: US1 і US2 працюють незалежно

---

## Phase 5: User Story 3 — Виправлення працівника (Priority: P2)

**Goal**: `PUT /api/Payroll/{id}` (R4, R5, R1, R3) і режим виправлення форми.

**Independent Test**: наявний id → 200 і нове `fullName` у БД; неіснуючий → 404; `employeeId` у тілі ≠ шляху → 400.

### Implementation for User Story 3

- [X] T029 [US3] Додати до `J/model/PayrollSheet.java` метод `OperationResult updateEmployee(String id, Employee employee)`: порядок R4 → R5 (`existsById(id)` → `NOT_FOUND`) → R1 → R3 (`employeeId` у тілі, якщо не порожній, має збігатися з `id`, інакше `VALIDATION`; змінюється лише `fullName`); успіх — `storageService.update(...)` з `employeeId` зі шляху, синхронізація колекції; `StorageException` → `STORAGE_ERROR`
- [X] T030 [US3] Додати до `J/view/PayrollWebView.java` метод `ApiResponse updated(OperationResult r)` (успіх 200, відмова — за відображенням) (залежить від T029)
- [X] T031 [US3] Додати до `J/controller/PayrollController.java` метод `ApiResponse update(String pathId, String bodyEmployeeId, String fullName)`: створює `Employee`, викликає `sheet.updateEmployee`, передає в `view.updated` (залежить від T030)
- [X] T032 [US3] У `J/infrastructure/JavalinWebServer.java` додати маршрут `PUT /api/Payroll/{id}` (`ctx.pathParam("id")`, тіло в `EmployeeRequest` з тим самим `try/catch` на `null`-значення) → `controller.update(...)` (залежить від T024, T031)
- [X] T033 [US3] Доповнити `R/script.js` та `R/index.html`: кнопка «Змінити» переносить `employeeId` і `fullName` рядка у форму, поле `employeeId` стає `readonly`, показується кнопка «Скасувати»; відправлення форми в цьому режимі — `PUT /api/Payroll/{id}`; «Скасувати» та успішне виправлення очищають форму й повертають режим додавання
- [X] T034 [US3] Розширити `J/SelfCheck.java`: `updateEmployee` — R4, R5 (неіснуючий → `NOT_FOUND`), R1, R3 (розбіжність id → `VALIDATION`), успіх змінює лише `fullName`; `PayrollWebView.updated` (200/400/404/409/500)
- [X] T035 [US3] Перевірка за `quickstart.md` кроки 5–7 (PUT 200 / 400 / 404) і режимом виправлення на сторінці

**Checkpoint**: US1–US3 працюють незалежно

---

## Phase 6: User Story 4 — Видалення працівника (Priority: P2)

**Goal**: `DELETE /api/Payroll/{id}` (R4, R5; R6) і кнопка «Видалити» з підтвердженням.

**Independent Test**: наявний id → 200 і запис зникає; неіснуючий → 404; за нечинного наказу → 409.

### Implementation for User Story 4

- [X] T036 [US4] Додати до `J/model/PayrollSheet.java` метод `OperationResult deleteEmployee(String id)`: R4 → R5 (`NOT_FOUND`); R6 — видалення дозволене завжди за чинного наказу; `storageService.delete(id)`, синхронізація колекції, `StorageException` → `STORAGE_ERROR`
- [X] T037 [US4] Додати до `J/view/PayrollWebView.java` метод `ApiResponse deleted(OperationResult r)` (успіх 200, відмова — за відображенням) (залежить від T036)
- [X] T038 [US4] Додати до `J/controller/PayrollController.java` метод `ApiResponse delete(String id)` → `sheet.deleteEmployee` → `view.deleted` (залежить від T037)
- [X] T039 [US4] У `J/infrastructure/JavalinWebServer.java` додати маршрут `DELETE /api/Payroll/{id}` → `controller.delete(ctx.pathParam("id"))` (залежить від T032, T038)
- [X] T040 [US4] Доповнити `R/script.js`: кнопка «Видалити» викликає `confirm(...)` (текст українською), за згоди — `DELETE /api/Payroll/{id}`, показує повідомлення, перечитує список
- [X] T041 [P] [US4] Додати до `R/style.css` клас візуально відмінної кнопки видалення (наприклад, `button-delete`) і застосувати його в `R/script.js`/`R/index.html` до кнопки «Видалити» (залежить від T040 лише за застосуванням класу)
- [X] T042 [US4] Розширити `J/SelfCheck.java`: `deleteEmployee` — R4, R5, успіх (запис зникає з БД і колекції), `STORAGE_ERROR`; `PayrollWebView.deleted` (200/404/409/500)
- [X] T043 [US4] Перевірка за `quickstart.md` кроки 7–8 (DELETE 200 / 404) і діалогом підтвердження на сторінці; перевірити 409 на будь-яку зміну за нечинного наказу (тимчасово відкликати `ORDER-001` у `Main`, потім повернути)

**Checkpoint**: усі чотири операції працюють незалежно

---

## Phase 7: User Story 5 — Запуск без консольних сценаріїв (Priority: P3)

**Goal**: програма одразу запускає вебсервер на 8080, працює до Ctrl+C; у консолі лише повідомлення про запуск; зайнятий порт обробляється зрозуміло.

**Independent Test**: запуск без введення в консолі → сторінка доступна; `target/employees.html` не створюється; за зайнятого порту — зрозуміле повідомлення.

### Implementation for User Story 5

- [X] T044 [US5] У `J/Main.java` обгорнути `server.start(controller, 8080)` у `try/catch`: за винятку старту (зайнятий порт, `JavalinException`/`BindException`) вивести в консоль зрозуміле повідомлення українською («Відмова: порт 8080 зайнятий…») і завершити процес `System.exit(1)` без стеку винятку
- [X] T045 [US5] Перевірка: `grep -rn "Scanner\|EmployeeWebPageGenerator\|employees.html" src` не знаходить залишків; запустити `mvn exec:java` без введення — сторінка відповідає, файл `target/employees.html` не створюється; повторний запуск за зайнятого 8080 дає повідомлення й код 1; Ctrl+C зупиняє роботу

**Checkpoint**: усі історії завершені

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: документація, діаграми, перевірка архітектурних обмежень

- [X] T046 [P] Оновити `README.md` (FR-017): класи моделі, вигляду й контролера, команди Maven (`mvn clean compile`, `mvn exec:java`, `SelfCheck`), адреса `http://localhost:8080/`, маршрути `/api/Payroll`, правила R1–R6, коди відповідей, стиль «Офіційний»
- [X] T047 [P] Оновити `docs/class.mmd` (FR-018): блоки `namespace model`, `view`, `controller`, `infrastructure`; розподіл — `EmployeeStorageService`, `OperationResult`, `FailureReason`, `StorageException` у model; `PayrollWebView`, `ApiResponse`, записи тіл відповіді — у view; `PayrollController` — у controller; `WebServer`, `JavalinWebServer`, `SqliteEmployeeStorageService`, `PayrollModule` — в infrastructure
- [X] T048 [P] Оновити `docs/sequence.mmd`: блоки `box` з тими самими назвами (model, view, controller, infrastructure); шлях запитів читання й зміни від вебсторінки до БД
- [X] T049 [P] Створити `docs/component.puml` (PlantUML): пакети view, controller, model, infrastructure; `actor` користувач, `node` вебсервер, `database` БД; інтерфейси `EmployeeStorageService` і `WebServer`, які реалізують класи інфраструктури
- [X] T050 Перевірка архітектурних обмежень: `grep -rn "io.javalin\|com.fasterxml" src/main/java` знаходить збіги лише в `J/infrastructure/JavalinWebServer.java`; `grep -rn "java.sql\|SELECT\|INSERT" src/main/java` — лише в `J/infrastructure/SqliteEmployeeStorageService.java` і `PayrollModule.java` (з'єднання); `J/model` не імпортує `view`, `controller`, `infrastructure`; `script.js` не містить правил і стилів (кольори, шрифти, розміри)
- [X] T051 Узгодити `specs/004-payroll-web-mvc/spec.md` з планом: переформулювати FR-011/FR-012 і розділ «Вибраний стиль вебсторінки» — розмітка й відступи через класи Tailwind CSS (CDN), тема «Офіційний» у `style.css` (примітка в `plan.md` розділ Constitution Check)
- [X] T052 Виконати повну перевірку `quickstart.md` (усі кроки API й сторінки, SelfCheck, перезапуск із збереженими даними, зайнятий порт)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: без залежностей
- **Foundational (Phase 2)**: залежить від Setup — блокує всі історії
- **User Stories (Phase 3–7)**: залежать від Foundational
  - US1 → US2 → US3 → US4 послідовно, бо спільні файли (`PayrollSheet`, `PayrollWebView`, `PayrollController`, `JavalinWebServer`, `script.js`, `SelfCheck`) змінюються в кожній історії; US1 створює скелет сервера й сторінки, на якому будуються решта
  - US5 залежить лише від US1 (Main і сервер), може виконуватись паралельно з US2–US4, але перевіряється наприкінці
- **Polish (Phase 8)**: після всіх бажаних історій

### Within Each User Story

- Модель (`PayrollSheet`) → вигляд (`PayrollWebView`) → контролер → маршрут `JavalinWebServer` → клієнт (`script.js`, стилі) → `SelfCheck` → перевірка
- Порядок перевірок моделі: R4 → R5 (PUT/DELETE) → R1, R3, R2

### Parallel Opportunities

- Phase 2: T003, T004, T006, T009 — паралельно (різні файли)
- US1: T015, T016, T017 (`index.html`, `script.js`, `style.css`) — паралельно після узгодження id елементів (див. T015) і незалежно від Java-завдань T010–T014
- US2: T026 (стилі) паралельно з Java-завданнями; US4: T041 (стилі) паралельно з T036–T039
- Polish: T046–T049 — паралельно

### Parallel Example: User Story 1

```text
# Клієнтська частина паралельно з Java-частиною:
Task: "T015 Створити index.html у src/main/resources/public/index.html"
Task: "T016 Створити script.js у src/main/resources/public/script.js"
Task: "T017 Створити style.css у src/main/resources/public/style.css"
Task: "T010 Додати listEmployees() у src/main/java/ua/payroll/model/PayrollSheet.java"
```

---

## Implementation Strategy

### MVP First (лише User Story 1)

1. Phase 1: Setup → Phase 2: Foundational
2. Phase 3: US1 (перегляд)
3. **STOP and VALIDATE**: `curl`/сторінка, `SelfCheck`
4. Демонстрація: вебсервер читає дані з БД

### Incremental Delivery

1. Setup + Foundational → фундамент
2. + US1 → читання (MVP)
3. + US2 → додавання
4. + US3 → виправлення
5. + US4 → видалення
6. + US5 → обробка зайнятого порту й підтвердження відсутності консольних сценаріїв
7. Polish → документація й діаграми

---

## Notes

- [P] — різні файли, без залежностей від незавершених завдань
- Мітка [Story] пов'язує завдання з історією для простежуваності
- Модель не повертає кодів HTTP; відображення причина → код живе лише в `PayrollWebView`
- Вебфреймворк і JSON — лише в `JavalinWebServer`; SQL — лише в `SqliteEmployeeStorageService`
- Коміт після кожного завдання або логічної групи; зупинятися на контрольних точках для перевірки історії

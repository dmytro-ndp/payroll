# Implementation Plan: Вебінтерфейс системи нарахування заробітної плати за MVC

**Branch**: `mvc` (тека специфікації `004-payroll-web-mvc`) | **Date**: 2026-11-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-payroll-web-mvc/spec.md`

## Summary

Консольний прототип замінюється вебінтерфейсом за шаблоном MVC. Модель (`PayrollSheet`, `Employee`,
`EmployeeStorageService`) отримує операції читання, додавання, виправлення й видалення з перевіркою
правил R1–R6 за даними БД та повертає `OperationResult` (успіх або причина відмови, без кодів HTTP).
`PayrollController` приймає прості значення (рядки), викликає модель і передає результат у
`PayrollWebView`, який будує `ApiResponse` (код стану + тіло). Інтерфейс `WebServer` та єдина його
реалізація `JavalinWebServer` (Javalin 6 + Jackson) оголошують маршрути `/api/Payroll`, лише
серіалізують відповідь вигляду й віддають статичні файли з `src/main/resources/public`. Вебсторінка —
HTML/JS + **Tailwind CSS з CDN** (вибір розробника) і `style.css` зі стилем «Офіційний».
Головний клас складає об'єкти, задає наказ `ORDER-001` і запускає сервер на порту 8080.

## Technical Context

**Language/Version**: Java 21 (JDK 21), Maven (`maven.compiler.release` = 21)

**Primary Dependencies**: Google Guice 7.0.0 (наявна); `io.javalin:javalin:6.7.0`;
`com.fasterxml.jackson.core:jackson-databind:2.18.2`; `org.slf4j:slf4j-simple:2.0.16`;
sqlite-jdbc 3.51.1.0 (наявна). Клієнт: HTML, JavaScript, Tailwind CSS (CDN, `https://cdn.tailwindcss.com`)

**Storage**: SQLite у файлі `target/payroll.db`, наявна таблиця `employees` (`employee_id` PK,
`full_name`, `run_id`); нових таблиць немає

**Testing**: наявний `SelfCheck` (без JUnit, запуск `mvn exec:java -Dexec.mainClass=ua.payroll.SelfCheck`) —
розширюється перевірками моделі й вигляду; наскрізна перевірка HTTP — `curl` за [quickstart.md](quickstart.md)

**Target Platform**: локальний комп'ютер розробника (Windows/Linux), командний рядок

**Project Type**: web-service з вебсторінкою (монолітний Maven-модуль, один JAR)

**Performance Goals**: не критично (навчальний проєкт, один користувач); відповідь на запит < 1 с

**Constraints**: порт 8080; вебфреймворк і JSON-бібліотека лише в `JavalinWebServer`; SQL лише в
`SqliteEmployeeStorageService`; модель не повертає кодів HTTP; `script.js` не містить правил і стилів;
сторінка потребує доступу до CDN Tailwind (без мережі — сторінка працює, але без оформлення)

**Scale/Scope**: 4 маршрути, 1 сторінка, 1 таблиця, ~10 нових класів

Усі раніше невизначені пункти (бібліотека CSS) закрито: Tailwind CSS з CDN (див. [research.md](research.md)).

## Constitution Check

*GATE: пройдено до Phase 0; повторно перевірено після Phase 1.*

| Принцип | Оцінка | Як дотримано |
|---------|--------|--------------|
| I. Мінімальна складність | ✅ | Нових таблиць і сервісів немає; один клас відповіді; без JUnit (використано `SelfCheck`); Tailwind з CDN без збирання |
| II. Єдина відповідальність | ✅ | Модель — правила; контролер — оркестрація; вигляд — коди й тіла; `JavalinWebServer` — маршрути/JSON; SQLite-сервіс — SQL |
| III. Консоль | ✅ | Консольне введення прибрано; у консоль лише повідомлення про запуск (допустимо як робота вебінтерфейсу) |
| IV. Локальний запуск | ✅ | Maven, порт 8080, SQLite-файл, Javalin дозволено |
| V. Явне впровадження | ✅ | Конструктори: `PayrollSheet(order, storage)`, `PayrollController(sheet, view)`, `JavalinWebServer()` |
| VI. Мова | ✅ | Назви англійською, коментарі й повідомлення українською |
| VII. UML у `docs` | ✅ | Оновлюються `class.mmd`, `sequence.mmd`, створюється `component.puml` |
| VIII. Українська специфікація | ✅ | Усі артефакти плану українською |
| IX. DIP через Guice | ✅ | `EmployeeStorageService` і `WebServer` прив'язуються в `PayrollModule`; `PayrollSheet` отримує лише інтерфейс; `Injector` використовується лише в `Main` |
| X. Сервісний шар | ✅ | Увесь SQL у `SqliteEmployeeStorageService` (нові `findAll`, `existsById`, `update`, `delete`) |
| XI. MVC | ✅ | Контролер і вигляд не залежать від Javalin; модель не залежить від вигляду/контролера; код HTTP формує лише вигляд |
| XII. Ізоляція вебфреймворку | ✅ | Імпорти `io.javalin.*` і `com.fasterxml.jackson.*` лише в `JavalinWebServer` (та DTO запиту поруч) |
| XIII. Тонкий клієнт | ✅ з приміткою | `script.js` лише `fetch` і показ відповідей; оформлення — класи Tailwind у `index.html` + `style.css`; скрипт лише перемикає імена класів, визначених у `style.css` (`message-success`, `message-error`), кольорів не задає |

Порушень немає; розділ Complexity Tracking не заповнюється.

**Примітка щодо суперечності зі специфікацією**: FR-011/FR-012 і «Вибраний стиль» вимагають, щоб
оформлення було лише в `style.css`. Розробник обрав Tailwind, а конституція (XIII) дозволяє класи
CSS-бібліотеки. Рішення: розмітка/відступи — класи Tailwind в `index.html`; тема «Офіційний» (палітра,
шрифт із зарубками, прямі кути, рамки таблиці, стилі повідомлень і кнопки видалення) — у `style.css`.
Рекомендовано оновити формулювання FR-011/FR-012 у специфікації (`/speckit-clarify`).

## Project Structure

### Documentation (this feature)

```text
specs/004-payroll-web-mvc/
├── plan.md              # цей файл
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   ├── rest-api.md      # HTTP-контракт /api/Payroll
│   └── java-interfaces.md  # контракти Java-інтерфейсів MVC
└── tasks.md             # Phase 2 (/speckit-tasks — не створюється тут)
```

### Source Code (repository root)

```text
pom.xml                                   # + javalin, jackson-databind, slf4j-simple
src/main/java/ua/payroll/
├── Main.java                             # змінюється: лише складання й запуск сервера
├── SelfCheck.java                        # змінюється: перевірки моделі/вигляду замість старих
├── model/
│   ├── Employee.java                     # без змін
│   ├── DirectorOrder.java                # без змін
│   ├── PayrollSheet.java                 # + listEmployees/add/update/delete → OperationResult
│   ├── PayrollCalculation.java, Receipt.java   # без змін
│   ├── EmployeeStorageService.java       # перенесено з storage; + findAll/existsById/update/delete
│   ├── StorageException.java             # нове: непередбачена помилка сховища
│   ├── OperationResult.java              # нове: успіх або причина відмови + пояснення
│   └── FailureReason.java                # нове: VALIDATION, NOT_FOUND, FORBIDDEN, STORAGE_ERROR
├── view/
│   ├── PayrollWebView.java               # нове: OperationResult/список → ApiResponse
│   ├── ApiResponse.java                  # нове: record (int status, Object body)
│   └── EmployeeItem.java, MessageBody.java, EmployeeListBody.java   # нове: записи тіл відповіді
├── controller/
│   └── PayrollController.java            # нове: list/add/update/delete (прості значення)
├── infrastructure/
│   ├── WebServer.java                    # нове: інтерфейс start(PayrollController, int port)
│   ├── JavalinWebServer.java             # нове: маршрути, JSON, статичні файли
│   ├── SqliteEmployeeStorageService.java # перенесено з storage; нові методи SQL
│   └── PayrollModule.java                # перенесено з di; + прив'язка WebServer
├── rule/                                 # без змін
└── (видалено) report/EmployeeWebPageGenerator.java
src/main/resources/public/
├── index.html                            # таблиця, форма, місце повідомлення, Tailwind CDN
├── script.js                             # fetch, показ відповідей, підтвердження видалення
└── style.css                             # стиль «Офіційний»
docs/
├── class.mmd                             # namespace model/view/controller/infrastructure
├── sequence.mmd                          # box model/view/controller/infrastructure
└── component.puml                        # нове
README.md                                 # оновлюється (FR-017)
```

**Structure Decision**: єдиний Maven-модуль із пакетами за ролями MVC (`model`, `view`,
`controller`, `infrastructure`), що збігається з блоками діаграм FR-018. Пакети `storage` і `di`
зникають: інтерфейс сервісу належить моделі, реалізація й модуль Guice — інфраструктурі.
`EmployeeWebPageGenerator` і пакет `report` видаляються (FR-015).

## Ключові проєктні рішення

1. **Результат моделі**: `OperationResult` (успіх / `FailureReason` + пояснення українською); `FailureReason`
   має чотири значення: `VALIDATION` → 400, `NOT_FOUND` → 404, `FORBIDDEN` → 409, `STORAGE_ERROR` → 500.
   Відображення причина→код живе лише у `PayrollWebView`.
2. **Порядок перевірок** `PayrollSheet`: R4 (наказ) → R5 (існування через `existsById`, для PUT/DELETE) →
   R1, R3, R2. Тіло `try/catch StorageException` → `STORAGE_ERROR`; за відмови нічого не змінюється ні в БД, ні в колекції.
3. **Колекція працівників**: після кожної успішної зміни й під час читання колекція `PayrollSheet`
   синхронізується з БД (`findAll`); `listEmployees()` читає з БД, а не з пам'яті (FR-006, SC-003).
4. **Сервіс і помилки**: `SqliteEmployeeStorageService` перестає друкувати «Відмова…» і кидає `StorageException`
   (обгортка `SQLException`); `saveEmployee` лишається вставкою (`INSERT`/upsert зберігає `run_id` = ідентифікатор запуску).
5. **Контролер без фреймворку**: методи приймають `String`, повертають `ApiResponse`:
   `list()`, `add(id, fullName)`, `update(pathId, bodyId, fullName)`, `delete(id)`. Контролер створює `Employee`.
6. **`JavalinWebServer`**: `Javalin.create(config -> config.staticFiles.add("/public", Location.CLASSPATH))`;
   маршрути `GET/POST /api/Payroll`, `PUT/DELETE /api/Payroll/{id}`; тіло читається в DTO `EmployeeRequest`
   (поля `employeeId`, `fullName`), за помилки розбору — `null`-значення; відповідь:
   `ctx.status(r.status()).json(r.body())`. Сервер — Singleton у Guice.
7. **Порт зайнятий**: виняток старту перехоплює `Main`, друкує зрозуміле повідомлення й завершує процес із кодом 1.
8. **Журнал**: `slf4j-simple` без конфігурації (рівень за замовчуванням); у консоль лише повідомлення
   Javalin про запуск і `Main` («Вебсервер запущено: http://localhost:8080/»).
9. **Сторінка**: після кожної зміни `script.js` викликає `GET /api/Payroll`; режим виправлення —
   поле `employeeId` `readonly`, кнопка «Скасувати»; повідомлення без таймерів; `confirm()` перед видаленням.
10. **Guice**: `PayrollSheet`, `PayrollController`, `PayrollWebView` не Singleton; Singleton лише
    `Connection` і `WebServer`. Контролер і вигляд створює `Main` через `new` за FR-014
    (`Main` — composition root; модель отримується з `Injector`).

## Complexity Tracking

Порушень конституції немає — не заповнюється.

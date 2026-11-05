# Implementation Plan: Збереження даних працівників у SQLite через Google Guice

**Branch**: `003-sqlite-guice-persistence` | **Date**: 2026-11-05 | **Spec**:
[spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-sqlite-guice-persistence/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Перетворити наявний імітаційний прототип на стандартний Maven-проєкт `payroll` (JDK 21), додавши
реальну персистентність працівників (клас-частина `Employee` обраного зв'язку агрегації
`PayrollSheet o-- Employee`) у файлову базу даних SQLite в теці `target` через окремий клас-сервіс
із SQL-запитами за інтерфейсом мовою предметної області. `PayrollSheet` залежатиме лише від цього
інтерфейсу, впровадженого через конструктор; Google Guice (модуль `AbstractModule`) прив'яже
інтерфейс до реалізації й надасть з'єднання з SQLite, а об'єкт `PayrollSheet` створюватиметься
винятково через `injector.getInstance(...)`. Після сценаріїв застосунок прочитає дані лише
поточного запуску й сформує статичну HTML-сторінку в `target`. Наявна бізнес-логіка, консольні
повідомлення українською та `SelfCheck` залишаються без змін по суті.

## Technical Context

**Language/Version**: Java 21 (JDK 21; `javac`/`java` з підтримкою Maven toolchain)

**Primary Dependencies**: Google Guice 7.0.0 (`com.google.inject:guice:7.0.0`) для інверсії
залежностей; `org.xerial:sqlite-jdbc:3.51.1.0` як JDBC-драйвер SQLite. Жодних інших зовнішніх
бібліотек (вебсторінка — рядковий HTML без шаблонізаторів).

**Storage**: Файлова база даних SQLite (`target/payroll.db`), створювана автоматично через JDBC
URL `jdbc:sqlite:target/payroll.db`, якщо файл або тека відсутні.

**Testing**: Наявний підхід без JUnit — ручний запуск класу `ua.payroll.SelfCheck` командами
Maven/`java -cp`; нові перевірки сервісу збереження додаються туди ж тим самим стилем
(`check(description, condition)`), без нового тестового фреймворку (принцип мінімальної
складності).

**Target Platform**: Локальний запуск з командного рядка (Windows/Unix), без сервера застосунків;
вебсторінка — статичний HTML-файл, що відкривається в будь-якому браузері без власного сервера.

**Project Type**: Один Maven-проєкт (single project), стандартна структура каталогів
`src/main/java`, `src/test/java` (не використовується для автотестів — залишено порожньою або
відсутньою, бо тестування виконує `SelfCheck`).

**Performance Goals**: Не є визначальними для навчального прототипу; достатньо синхронного
запису/читання невеликої кількості записів (одиниці-десятки працівників за запуск).

**Constraints**: Збирання й запуск — лише командами Maven з консолі (FR-002); лише одна залежність
створюється через `injector.getInstance(...)` (SC-005); SQL-запити — лише в класі-сервісі
(принцип X); `PayrollSheet` не повинен звертатися до JDBC чи Guice `Injector` напряму (принцип IX).

**Scale/Scope**: Розширення трьох існуючих класів (`PayrollSheet`, `Main`, `SelfCheck`), додавання
~4 нових класів/інтерфейсів (інтерфейс сервісу, реалізація, Guice-модуль, генератор вебсторінки) і
переміщення наявних 10 Java-файлів у Maven-структуру `src/main/java`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Принцип | Перевірка | Статус |
|---|---|---|
| I. Мінімальна складність | Один Guice-модуль, один сервіс-інтерфейс + одна реалізація, без зайвих шарів (DAO-фреймворків, ORM) | ✅ PASS |
| II. Єдина відповідальність | `EmployeeStorageService` — лише SQL; `PayrollSheet` — лише доменна логіка відомості; окремий клас для генерації HTML | ✅ PASS |
| III. Імітація зовнішніх операцій | Збереження в SQLite виконується реально (явний виняток принципу); інші дії — як і раніше, консоль | ✅ PASS |
| IV. Локальне виконання з CLI | Maven-збирання/запуск; SQLite — файлова БД без сервера | ✅ PASS |
| V. Явне впровадження залежностей | Інтерфейс сервісу впроваджується в `PayrollSheet` через конструктор (вимога FR-006) | ✅ PASS |
| VI. Мова та іменування | Класи/методи англійською, коментарі українською (зберігається стиль наявного коду) | ✅ PASS |
| VII. Текстова UML-документація | `docs/class.mmd` і `docs/sequence.mmd` оновлюються цим планом (FR-012, FR-013) | ✅ PASS |
| VIII. Українська мова специфікації | spec.md і цей план — українською | ✅ PASS |
| IX. Інверсія залежностей через Guice | Лише `PayrollSheet` отримує залежність через `Injector.getInstance`; решта об'єктів — через `new`, як і раніше | ✅ PASS |
| X. Сервісний шар доступу до даних | Усі SQL-запити — у класі `SqliteEmployeeStorageService` | ✅ PASS |

Порушень немає; Complexity Tracking не заповнюється.

## Project Structure

### Documentation (this feature)

```text
specs/003-sqlite-guice-persistence/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
pom.xml                               # Maven-дескриптор, artifactId "payroll", JDK 21

src/main/java/ua/payroll/
├── Main.java                        # Точка входу; injector.getInstance(PayrollSheet.class)
├── SelfCheck.java                   # Самоперевірка (доповнюється перевірками сервісу)
├── model/
│   ├── DirectorOrder.java
│   ├── Employee.java
│   ├── PayrollCalculation.java
│   ├── PayrollSheet.java            # Конструктор приймає EmployeeStorageService
│   └── Receipt.java
├── rule/
│   ├── PayrollRule.java
│   ├── BasePayrollRule.java
│   └── StandardPayrollRule.java
├── storage/                         # Новий пакет — сервісний шар персистентності
│   ├── EmployeeStorageService.java  # Інтерфейс мовою предметної області
│   └── SqliteEmployeeStorageService.java  # Реалізація з SQL-запитами (JDBC)
├── di/                              # Новий пакет — складання застосунку через Guice
│   └── PayrollModule.java           # AbstractModule: прив'язка інтерфейсу й Connection
└── report/                          # Новий пакет — формування статичної вебсторінки
    └── EmployeeWebPageGenerator.java

src/test/java/                       # Залишається порожнім каталогом (стандартна структура
                                      # Maven); автоперевірка — через SelfCheck, не JUnit

target/
├── payroll.db                      # Файл бази даних SQLite (створюється автоматично)
└── employees.html                  # Статична вебсторінка з таблицею працівників
```

**Structure Decision**: Один Maven-проєкт (`payroll`) зі стандартною структурою
`src/main/java`/`src/test/java`. Наявні пакети `model` і `rule` переносяться без зміни вмісту; нові
пакети `storage` (сервіс збереження), `di` (Guice-модуль) і `report` (генератор вебсторінки)
додаються поруч, зберігаючи SRP і межі відповідальності з Constitution Check.

## Constitution Check (повторна оцінка після Phase 1)

Після проєктування `data-model.md` і контракту `contracts/employee-storage-service.md` усі пункти
таблиці вище залишаються в статусі ✅ PASS: `PayrollSheet` отримує `EmployeeStorageService` лише
через конструктор; єдина SQL-логіка — в `SqliteEmployeeStorageService`; `PayrollModule` —
єдиний клас-нащадок `AbstractModule`; `Main` — єдине місце виклику `Guice.createInjector(...)` і
`injector.getInstance(...)`. Нових порушень не виявлено.

## Complexity Tracking

Порушень Constitution Check немає — розділ не заповнюється.

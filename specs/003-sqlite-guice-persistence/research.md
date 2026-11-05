# Phase 0 Research: Збереження даних працівників у SQLite через Google Guice

## 1. Версії залежностей Maven

- **Decision**: `com.google.inject:guice:7.0.0` та `org.xerial:sqlite-jdbc:3.51.1.0`.
- **Rationale**: Guice 7.0.0 — остання стабільна версія ядра Guice (без модуля `guice-assistedinject`
  чи інших розширень, які тут не потрібні), сумісна з JDK 21 і `jakarta.inject`. `sqlite-jdbc`
  3.51.1.0 — актуальна стабільна версія офіційного JDBC-драйвера Xerial SQLite з Maven Central,
  не вимагає окремого нативного інсталятора (включає нативні бібліотеки в jar).
- **Alternatives considered**: Hibernate/JPA з SQLite-діалектом — відхилено як надмірну складність
  для навчального прототипу (порушує принцип I); Guice 6.x — відхилено, бо специфікація explicitно
  вимагає Guice 7.

## 2. Структура Maven-проєкту

- **Decision**: Стандартний `pom.xml` у корені, artifactId `payroll`, `maven-compiler-plugin` з
  `<release>21</release>`, `maven-jar-plugin`/`maven-assembly-plugin` (shade/assembly) для
  складання fat-jar із залежностями (Guice, sqlite-jdbc) для запуску `java -jar`. Наявні пакети
  (`ua.payroll.*`) переносяться в `src/main/java` без зміни імен класів/пакетів.
- **Rationale**: Відповідає FR-001/FR-002 (стандартна структура, збирання й запуск лише Maven).
  Fat-jar (через `maven-assembly-plugin` з дескриптором `jar-with-dependencies`) спрощує запуск
  однією командою `java -jar target/payroll-....jar` без ручного складання classpath.
- **Alternatives considered**: `maven-dependency-plugin:copy-dependencies` + `java -cp
  target/classes:target/dependency/*` — відхилено, бо вимагає ручного керування classpath
  (суперечить SC-002 «без жодних додаткових ручних дій»).

## 3. Інтерфейс сервісу збереження даних

- **Decision**: Інтерфейс `EmployeeStorageService` у пакеті `ua.payroll.storage` з методом
  `void saveEmployee(Employee employee)` (мовою предметної області — «зберегти працівника»), що
  виконує upsert за `employeeId`. Реалізація `SqliteEmployeeStorageService` приймає `Connection`
  через конструктор, виконує `CREATE TABLE IF NOT EXISTS` при ініціалізації та
  `INSERT ... ON CONFLICT(employee_id) DO UPDATE SET full_name = excluded.full_name` для upsert.
- **Rationale**: Задовольняє FR-004/FR-004a/принцип X (SQL лише в сервісі) і принцип IX
  (`PayrollSheet` залежить лише від інтерфейсу). SQLite підтримує синтаксис
  `ON CONFLICT ... DO UPDATE` (upsert) з версії 3.24+, що покривається драйвером 3.51.1.0.
- **Alternatives considered**: `DELETE` + `INSERT` замість upsert — відхилено, менш ефективно й
  складніше атомарно; окремий метод `employeeExists` + `if/else` у сервісі — відхилено на користь
  одного SQL-виразу (простіше, атомарніше).

## 4. Позначення «поточного запуску» для вебсторінки (FR-009)

- **Decision**: Додати в таблицю SQLite стовпець `run_id TEXT NOT NULL`, що встановлюється одним
  значенням (наприклад, UUID або timestamp запуску), згенерованим один раз при старті застосунку
  й переданим у `SqliteEmployeeStorageService` через конструктор. Запит для вебсторінки:
  `SELECT employee_id, full_name FROM employees WHERE run_id = ?`.
- **Rationale**: Просте й надійне розрізнення «дані цього запуску» від «дані попередніх запусків»
  без видалення історичних даних (SC-001 вимагає збереження даних між запусками). Відповідає
  Edge Case «вебсторінка показує лише дані поточного запуску».
- **Alternatives considered**: Зберігати timestamp останнього оновлення й фільтрувати за
  «найновіший run» через `MAX(updated_at)` — відхилено, складніше за семантикою й крихкіше при
  швидких послідовних запусках програми в одну секунду.

## 5. Генерація статичної вебсторінки

- **Decision**: Окремий клас `EmployeeWebPageGenerator` (пакет `ua.payroll.report`) з методом
  `void generate(List<Employee> employees, Path targetFile)`, що формує HTML рядковою
  конкатенацією/`StringBuilder` (без шаблонізаторів) і записує файл через `java.nio.file.Files`.
  Виклик — з `Main` після завершення сценаріїв, дані отримуються через
  `EmployeeStorageService.findEmployeesByRun(...)` (додатковий метод читання).
- **Rationale**: Мінімальна складність (принцип I) — немає потреби в шаблонізаторі (Thymeleaf/
  FreeMarker) для простої таблиці. Генератор HTML — окремий клас (SRP), не SQL-клас.
- **Alternatives considered**: Генерація HTML безпосередньо в `SqliteEmployeeStorageService` —
  відхилено, порушує SRP (сервіс відповідає лише за персистентність, принцип II/X).

## 6. Спосіб впровадження та складання через Guice

- **Decision**: `PayrollModule extends AbstractModule` з методом `configure()`, що виконує
  `bind(EmployeeStorageService.class).to(SqliteEmployeeStorageService.class)`, і провайдер-методом
  `@Provides Connection provideConnection()` (або `@Singleton`), що відкриває
  `DriverManager.getConnection("jdbc:sqlite:target/payroll.db")`. У `Main`:
  `Injector injector = Guice.createInjector(new PayrollModule(runId));` і
  `PayrollSheet sheet = injector.getInstance(PayrollSheet.class);` замість `new PayrollSheet(order)`.
  `DirectorOrder` для `PayrollSheet` передається через `@Assisted`-подібний підхід не потрібен —
  простіше: `DirectorOrder` залишається параметром конструктора, що встановлюється одразу після
  отримання екземпляра через явний setter або другий конструктор; детальний вибір — у data-model.md
  та контракті `payroll-sheet-service.md`.
- **Rationale**: Відповідає FR-006/FR-007/FR-008 та принципу IX — єдина точка `new` для `Injector`
  відсутня в доменному коді, лише в `Main` (composition root).
- **Alternatives considered**: `@Inject`-анотація прямо на конструкторі `PayrollSheet` з
  JIT-зв'язуванням Guice без явного модуля — відхилено, бо специфікація вимагає явного модуля
  `AbstractModule`, що прив'язує саме інтерфейс сервісу (FR-007).

## 7. Встановлення Maven (попередня перевірка)

- **Decision**: Команда `mvn -v` у середовищі розробки підтвердила наявність Apache Maven 3.9.10 і
  JDK 21 у `PATH`; додаткове встановлення Maven не знадобилося.
- **Rationale**: Відповідає умові користувача «якщо команда mvn недоступна, встанови сам» — умова
  не настала.

## Підсумок

Усі пункти NEEDS CLARIFICATION із шаблону технічного контексту вирішено; явних прогалин специфікації
немає (сесія `/speckit-clarify` вже відпрацювала два відкритих питання).

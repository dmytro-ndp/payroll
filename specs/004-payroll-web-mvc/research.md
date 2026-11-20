# Research: Вебінтерфейс за MVC (задача 004)

## R-1. CSS-бібліотека для вебсторінки
- **Decision**: Tailwind CSS через CDN (`<script src="https://cdn.tailwindcss.com"></script>`) + власний `style.css`.
- **Rationale**: обрано розробником (відповідь на запит плану). Конституція XIII дозволяє класи CSS-бібліотеки; збирання
  (npm/PostCSS) не потрібне, що зберігає чисте Maven-збирання.
- **Alternatives considered**: без бібліотеки (відповідало б тексту FR-011 дослівно); Bootstrap із CDN; Tailwind зі
  статичних файлів (потребує збирання або завантаження ~300 КБ скрипта).
- **Наслідок**: Play CDN Tailwind потребує мережі й призначений для розробки; стиль «Офіційний» (шрифт із зарубками,
  прямі кути, палітра, повідомлення, кнопка видалення) задається в `style.css`, щоб сторінка лишалась
  читабельною навіть без CDN.

## R-2. Javalin 6 і статичні файли
- **Decision**: `config.staticFiles.add("/public", Location.CLASSPATH)`; `index.html` віддається за `/` автоматично.
- **Rationale**: штатний спосіб Javalin 6, файли беруться з `src/main/resources/public`.
- **Alternatives**: `config.staticFiles.add(s -> {...})` з розширеною конфігурацією — зайве.

## R-3. Jackson у Javalin 6
- **Decision**: додати `jackson-databind` як окрему залежність; Javalin 6 автоматично використовує `JavalinJackson`
  за наявності бібліотеки на classpath. Тіло запиту читається `ctx.bodyAsClass(EmployeeRequest.class)` у `try/catch`;
  за будь-якого винятку створюється `EmployeeRequest` із `null`-полями (вимога специфікації: вебсервер не відхиляє запит).
- **Rationale**: `javalin` (не `javalin-bundle`) + явні `jackson-databind`, `slf4j-simple` — відповідає переліку технологій.
- **Alternatives**: `javalin-bundle` (приносить зайве); Gson (не за завданням).
- **Примітка**: відповіді — Java `record`; Jackson 2.18 серіалізує записи без додаткової конфігурації.

## R-4. Версії
- Javalin 6.7.0 (потребує Java 17+, Jetty 11/12 підтягується транзитивно), jackson-databind 2.18.2, slf4j-simple 2.0.16,
  Guice 7.0.0 (чинна, `jakarta.inject`). Перед реалізацією перевірити останні патч-версії на Maven Central.

## R-5. Життєвий цикл сервера й порт
- **Decision**: `Javalin.start(port)` не блокує, але потоки Jetty не-daemon, тож процес працює до Ctrl+C (`mvn exec:java`
  чекає на не-daemon потоки). Виняток зайнятого порту (`JavalinException`/`BindException`) перехоплює `Main`.
- **Alternatives**: `Thread.currentThread().join()` — зайве.

## R-6. Помилки сховища
- **Decision**: нове unchecked `StorageException` у `model`; `SqliteEmployeeStorageService` обгортає `SQLException`.
  `PayrollSheet` перехоплює його й повертає `FailureReason.STORAGE_ERROR`.
- **Rationale**: поточний сервіс ковтає помилки (друкує в консоль) — модель не змогла б повідомити про збій; специфікація
  вимагає причину «помилка сховища» (500).
- **Alternatives**: checked-виняток (засмічує інтерфейс); повернення булевих прапорців (втрата причини).

## R-7. Стратегія перевірки
- **Decision**: розширити наявний `SelfCheck` (без JUnit): сценарії R1–R6 на `PayrollSheet` із тимчасовою БД/фейковим
  сервісом, відображення причини→код у `PayrollWebView`; наскрізні перевірки — `curl` за `quickstart.md`.
- **Rationale**: принцип I; JUnit у проєкті немає, `src/test` порожній.
- **Alternatives**: JUnit 5 + Javalin TestTools — відхилено як зайве для навчального обсягу.

## R-8. Колонка `run_id`
- **Decision**: лишається; `PayrollModule` передає ідентифікатор запуску (UUID) у SQLite-сервіс для вставки; читання
  `findAll` повертає всіх працівників незалежно від запуску; `findEmployeesOfCurrentRun` видаляється.

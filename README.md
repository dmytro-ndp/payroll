# payroll

Навчальний вебзастосунок системи нарахування заробітної плати на Java 21 (Maven-проєкт, шаблон
MVC). Вебсторінка `http://localhost:8080/` показує таблицю працівників з бази даних SQLite
(`target/payroll.db`) і дозволяє додавати, виправляти та видаляти працівників через REST-маршрути
`/api/Payroll`. Дані зберігаються між запусками.

## Архітектура (MVC)

| Роль | Пакет | Класи |
|------|-------|-------|
| Модель | `ua.payroll.model` | `Employee`, `DirectorOrder`, `PayrollSheet`, `PayrollCalculation`, `Receipt`, `EmployeeStorageService`, `OperationResult`, `FailureReason`, `StorageException` |
| Вигляд | `ua.payroll.view` | `PayrollWebView`, `ApiResponse`, `EmployeeItem`, `EmployeeListBody`, `MessageBody` |
| Контролер | `ua.payroll.controller` | `PayrollController` |
| Інфраструктура | `ua.payroll.infrastructure` | `WebServer`, `JavalinWebServer`, `SqliteEmployeeStorageService`, `PayrollModule` |

- Модель перевіряє правила й повертає `OperationResult` (без кодів HTTP).
- Контролер приймає прості значення, викликає модель і передає результат вигляду.
- Вигляд — єдине місце, що перетворює результат моделі на код відповіді й тіло.
- Javalin і Jackson використовує лише `JavalinWebServer`; SQL — лише `SqliteEmployeeStorageService`.
- `Main` складає об'єкти (Guice для моделі й сервера), задає чинний наказ `ORDER-001`.
- Сторінка (`src/main/resources/public`): `index.html` (розмітка, Tailwind CSS з CDN),
  `script.js` (лише запити й показ відповідей), `style.css` (стиль «Офіційний»).

## Вимоги

- JDK 21+ і Apache Maven (`mvn` у `PATH`).
- Для оформлення сторінки потрібен доступ до CDN Tailwind (без мережі сторінка працює без оформлення).

## Збирання й запуск

```powershell
mvn clean compile
mvn exec:java
```

Сервер виводить «Вебсервер запущено: http://localhost:8080/» і працює до `Ctrl+C`. Якщо порт 8080
зайнятий, виводиться повідомлення про відмову й процес завершується з кодом 1.

Fat-jar: `mvn clean package`, потім `java -jar target\payroll-1.0.0-jar-with-dependencies.jar`.

## Маршрути `/api/Payroll`

| Метод | Шлях | Успіх |
|-------|------|-------|
| GET | `/api/Payroll` | 200 `{"employees":[{"employeeId":"E1","fullName":"…"}]}` |
| POST | `/api/Payroll` | 201 `{"message":"…"}` |
| PUT | `/api/Payroll/{id}` | 200 `{"message":"…"}` |
| DELETE | `/api/Payroll/{id}` | 200 `{"message":"…"}` |

Відмови: 400 (дані не пройшли перевірку), 404 (працівника не знайдено), 409 (наказ нечинний),
500 (помилка сховища); тіло — `{"message":"…"}` українською.

## Правила

- **R1** — `employeeId` і `fullName` обов'язкові, не порожні й не з самих пробілів → 400.
- **R2** — `employeeId` унікальний → 400 за дубліката.
- **R3** — під час виправлення змінюється лише `fullName`; `employeeId` у тілі, якщо є, має збігатися зі шляхом → 400.
- **R4** — зміни дозволені лише за чинного наказу директора → 409.
- **R5** — виправлення й видалення неіснуючого працівника → 404.
- **R6** — видалення дозволене завжди за чинного наказу.

Порядок перевірок: R4 → R5 → R1, R3, R2.

## Автоматизована самоперевірка (без JUnit)

```powershell
mvn exec:java "-Dexec.mainClass=ua.payroll.SelfCheck"
```

Перевіряє модель (R1–R6), сервіс SQLite у пам'яті та відображення результатів на коди у вигляді.
Наскрізні перевірки HTTP — `curl` за `specs/004-payroll-web-mvc/quickstart.md`.

## Документація

Специфікації фіч — `specs/` (поточна: `specs/004-payroll-web-mvc/`); UML — `docs/`:
`class.mmd`, `sequence.mmd` (блоки model, view, controller, infrastructure), `component.puml`,
`state.mmd`, `usecase.puml`, `crc.xlsx`.

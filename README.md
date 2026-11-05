# payroll

Імітаційний навчальний прототип консольної системи нарахування заробітної плати на Java 21
(Maven-проєкт). Директор видає наказ, бухгалтер інтерактивно формує відомість і запускає
нарахування за фіксованим правилом, працівник отримує квитанцію. Більшість операцій, що імітують
зовнішню взаємодію, виводяться повідомленнями українською мовою в консоль.

Дані кожного працівника, доданого до відомості, реально зберігаються у файлову базу даних SQLite
(`target/payroll.db`) через сервіс `EmployeeStorageService`, впроваджений у `PayrollSheet` за
допомогою Google Guice (єдиний клас, що створюється через `injector.getInstance(...)`). Після
завершення сценаріїв формується статична HTML-сторінка `target/employees.html` із таблицею
працівників, доданих або оновлених саме в поточному запуску.

Детальніше: специфікація, план і завдання фічі — у `specs/003-sqlite-guice-persistence/`; попередня
фіча (базовий прототип без персистентності) — у `specs/002-payroll-prototype/`; UML-документація —
у `docs/` (`class.mmd`, `sequence.mmd`, `state.mmd`, `usecase.puml`, `crc.xlsx`).

## Вимоги

- Встановлений JDK 21+ (команди `javac`, `java` доступні в `PATH`).
- Встановлений Apache Maven (команда `mvn` доступна в `PATH`).

## Збирання

```powershell
mvn clean package
```

Результат: `BUILD SUCCESS`; у теці `target` з'являється `payroll-1.0.0-jar-with-dependencies.jar`
(fat-jar з усіма залежностями — Guice, SQLite JDBC-драйвер).

## Запуск

```powershell
mvn exec:java
```

або запуск зібраного jar-файла:

```powershell
java -jar target\payroll-1.0.0-jar-with-dependencies.jar
```

Після завершення інтерактивного сценарію:

- дані доданих/оновлених працівників збережено у `target/payroll.db` (SQLite, зберігається між
  запусками, upsert за ідентифікатором працівника);
- сформовано вебсторінку `target/employees.html` із таблицею працівників поточного запуску (її
  можна відкрити в будь-якому браузері, наприклад `start target\employees.html`).

## Автоматизована самоперевірка (без JUnit)

```powershell
mvn exec:java "-Dexec.mainClass=ua.payroll.SelfCheck"
```

Детальний сценарій ручної перевірки — у `specs/003-sqlite-guice-persistence/quickstart.md`.
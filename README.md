# payroll

Імітаційний навчальний прототип консольної системи нарахування заробітної плати на Java 17.
Директор видає наказ, бухгалтер інтерактивно формує відомість і запускає нарахування за
фіксованим правилом, працівник отримує квитанцію. Без зовнішніх бібліотек, без бази даних,
без мережі — усі операції, що імітують зовнішню взаємодію, виводяться повідомленнями
українською мовою в консоль.

Детальніше: специфікація, план і завдання — у `specs/002-payroll-prototype/`; UML-документація —
у `docs/` (`class.mmd`, `sequence.mmd`, `state.mmd`, `usecase.puml`, `crc.xlsx`).

## Вимоги

- Встановлений JDK 17+ (команди `javac`, `java` доступні в `PATH`).

## Компіляція

Unix-подібні оболонки:

```sh
javac -d bin $(find src -name "*.java")
```

Windows PowerShell:

```powershell
javac -d bin (Get-ChildItem -Recurse -Filter *.java src).FullName
```

## Запуск

```sh
java -cp bin ua.payroll.Main
```

## Автоматизована самоперевірка (без JUnit)

```sh
java -cp bin ua.payroll.SelfCheck
```

Детальний сценарій ручної перевірки — у `specs/002-payroll-prototype/quickstart.md`.
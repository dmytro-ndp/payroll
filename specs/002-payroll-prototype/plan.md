# Implementation Plan: Імітаційний прототип системи нарахування заробітної плати

**Branch**: `002-payroll-prototype` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-payroll-prototype/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Реалізувати консольний Java-прототип системи нарахування заробітної плати без зовнішніх
залежностей і без бази даних: директор видає наказ, бухгалтер інтерактивно (через консольний
ввід) формує відомість і запускає нарахування за фіксованим правилом, працівник отримує
квитанцію. Усі операції, що імітують зовнішню взаємодію, та всі відмови виводяться повідомленнями
українською мовою в консоль. Структура класів, послідовність взаємодій і переходи станів точно
відтворюють `docs/class.mmd`, `docs/sequence.mmd` та `docs/state.mmd`; `README.md` у корені
проєкту документує компіляцію й запуск лише засобами JDK командного рядка.

## Technical Context

**Language/Version**: Java 17 (LTS), компіляція й запуск виключно через `javac`/`java` без
build-інструментів (Maven/Gradle)

**Primary Dependencies**: Відсутні — лише стандартна бібліотека Java SE (`java.util.Scanner` для
консольного вводу, `java.math.BigDecimal` для сум)

**Storage**: Відсутнє — стан (наказ, відомість, нарахування) існує лише в пам'яті одного процесу
протягом виконання

**Testing**: Без зовнішніх фреймворків (без JUnit); перевірка — компактний самостійний клас
`SelfCheck`, що виконує сценарії через публічний API доменних класів і звіряє результат простими
`assert`/порівняннями, виводячи підсумок у консоль українською; доповнюється ручним прогоном за
`quickstart.md`

**Target Platform**: Локальна консоль/термінал з установленим JDK 17+ (Windows/Linux/macOS)

**Project Type**: Один консольний Java-проєкт (single project) без клієнт-серверного поділу

**Performance Goals**: Не застосовно для навчального консольного прототипу з одним користувачем
і нарахуванням для кількох працівників за один запуск; немає вимог до пропускної здатності

**Constraints**: Без зовнішніх бібліотек, без БД, без мережі й без файлового вводу/виводу; уся
імітація зовнішніх дій — лише повідомлення в консоль українською мовою; залежності — лише через
конструктор/setter

**Scale/Scope**: Один розрахунковий цикл за запуск; невелика кількість працівників, яку визначає
користувач інтерактивним вводом під час сеансу (орієнтовно одиниці — десятки записів)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Принцип | Рішення | Стан |
|---|---|---|
| I. Мінімальна складність | Жодних фреймворків/build-інструментів; прості POJO-класи, компіляція `javac`, точка входу `Main` | Пройдено |
| II. Єдина відповідальність | Кожен клас (`Employee`, `DirectorOrder`, `PayrollSheet`, `PayrollCalculation`, `Receipt`, `PayrollRule`-ієрархія) відповідає одній CRC-картці з `docs/crc.xlsx` | Пройдено |
| III. Імітація зовнішніх операцій у консолі | Видання наказу, додавання, нарахування, видача квитанції й усі відмови — лише `System.out.println` українською | Пройдено |
| IV. Локальне виконання з командного рядка | Без БД/мережі; компіляція й запуск через `javac`/`java`; стан лише в пам'яті процесу | Пройдено |
| V. Явне впровадження залежностей | `PayrollSheet` отримує `DirectorOrder` і `PayrollRule` через конструктор/setter; `Receipt` — через конструктор | Пройдено |
| VI. Мова та іменування | Ідентифікатори англійською; коментарі й консольні повідомлення українською | Пройдено |
| VII. Текстова UML-документація | Реалізація відтворює вже наявні `docs/class.mmd`, `docs/sequence.mmd`, `docs/state.mmd`, `docs/usecase.puml`; зміни структури діаграм не плануються | Пройдено |
| VIII. Українська мова специфікації | `spec.md`, `plan.md` та супутні артефакти — українською | Пройдено |

Порушень немає; розділ «Відстеження складності» не заповнюється.

**Повторна перевірка після Phase 1**: `research.md`, `data-model.md`, `contracts/console-cli.md`
і `quickstart.md` не вводять нових класів, залежностей чи зовнішніх інтеграцій поза межами
`docs/class.mmd`; усі вісім принципів лишаються пройденими без змін.

## Project Structure

### Documentation (this feature)

```text
specs/002-payroll-prototype/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── console-cli.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
src/
└── ua/payroll/
    ├── Main.java                     # Точка входу: консольний сценарій, Scanner-ввід, демонстрація відмов
    ├── model/
    │   ├── Employee.java
    │   ├── DirectorOrder.java
    │   ├── PayrollSheet.java
    │   ├── PayrollCalculation.java
    │   └── Receipt.java
    └── rule/
        ├── PayrollRule.java          # interface
        ├── BasePayrollRule.java      # abstract
        └── StandardPayrollRule.java

bin/                                   # Каталог скомпільованих .class (створюється командою компіляції)

README.md                              # Опис прототипу та команди компіляції/запуску
```

**Structure Decision**: Один консольний Java-проєкт без build-інструментів. Вихідний код — у
`src/` за пакетами `ua.payroll`, `ua.payroll.model`, `ua.payroll.rule`, що безпосередньо
відображають класи й зв'язки з `docs/class.mmd`. Скомпільовані класи потрапляють у `bin/`
(каталог компіляції, не версіюється). `Main` відповідає лише за сценарій консольної взаємодії
(SRP), не змішуючи доменну логіку з виведенням.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Немає порушень конституції, що потребують обґрунтування.

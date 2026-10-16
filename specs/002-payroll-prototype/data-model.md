# Модель даних: Імітаційний прототип системи нарахування заробітної плати

**Функціональність**: `specs/002-payroll-prototype/spec.md` | **Джерело класів**: `docs/class.mmd`
**CRC-відповідальності**: `docs/crc.xlsx`, аркуш `CRC`

Усі атрибути та методи нижче — лише ті, що задокументовані в `docs/class.mmd`; нові атрибути чи
класи поза межами цієї діаграми не додаються (принцип I конституції, мінімальна складність).

## Employee

Працівник підприємства, якого включають до відомості.

| Атрибут | Тип | Опис |
|---|---|---|
| `employeeId` | `String` | Унікальний ідентифікатор працівника (ключ рівності/унікальності у відомості) |
| `fullName` | `String` | Повне ім'я працівника |

**Методи**: конструктор `Employee(String employeeId, String fullName)`, `String getEmployeeId()`,
`String getFullName()`.

**Інваріанти**: `employeeId` не може бути порожнім; рівність і унікальність у `PayrollSheet`
визначаються за `employeeId`.

## DirectorOrder

Наказ директора — передумова додавання працівників і нарахування.

| Атрибут | Тип | Опис |
|---|---|---|
| `orderId` | `String` | Ідентифікатор наказу |
| `active` | `boolean` | Чинність наказу |

**Методи**: конструктор `DirectorOrder(String orderId)` (наказ чинний одразу після створення),
`boolean isActive() final`, `void revoke()` (скасовує чинність).

**Переходи станів**: `active = true` → (за викликом `revoke()`) → `active = false`; зворотного
переходу немає — новий чинний наказ вимагає нового екземпляра `DirectorOrder`.

## PayrollSheet

Відомість нарахування — керує складом працівників, посилається на наказ, утримує нарахування.

| Атрибут | Тип | Опис |
|---|---|---|
| `order` | `DirectorOrder` | Наказ, якому підпорядкована відомість (впроваджується через конструктор) |
| `payrollRule` | `PayrollRule` | Правило обчислення суми (впроваджується через setter до нарахування) |
| `employees` | `Set<Employee>` | Унікальний склад працівників відомості |
| `calculations` | `List<PayrollCalculation>` | Завершені нарахування |

**Методи**:
- конструктор `PayrollSheet(DirectorOrder order)` — явне впровадження залежності наказу;
- `void setPayrollRule(PayrollRule payrollRule)` — явне впровадження правила через setter;
- `boolean addEmployee(Employee employee)` — повертає `true`/додає лише за `order.isActive()` і
  відсутності дубліката за `employeeId`; інакше повертає `false` без зміни `employees`;
- `List<PayrollCalculation> calculate()` — повертає порожній список (без
  побічних ефектів) якщо `!order.isActive()` або `employees` порожній; інакше для кожного
  `Employee` обчислює суму через `payrollRule.calculate(employee)`, створює `PayrollCalculation`,
  додає до `calculations`;
- `PayrollCalculation findCalculation(Employee employee)` — повертає відповідне завершене
  нарахування або `null`/відсутність результату, якщо нарахування для цього працівника ще
  немає.

**Інваріанти**:
- Додавання й нарахування вимагають `order.isActive() == true` (FR-002–FR-004).
- `employees` не містить дублікатів за `employeeId` (FR-003).
- `calculate()` на порожній відомості не створює жодного `PayrollCalculation` (FR-004).
- До виклику `calculate()` для відомості встановлено ненульове `payrollRule` через
  `setPayrollRule(...)`.

## PayrollCalculation

Результат нарахування заробітної плати для одного працівника.

| Атрибут | Тип | Опис |
|---|---|---|
| `employee` | `Employee` | Працівник, для якого виконано нарахування |
| `amount` | `BigDecimal` (`decimal` на діаграмі) | Нарахована сума |

**Методи**: конструктор `PayrollCalculation(Employee employee, BigDecimal amount)`,
`BigDecimal getAmount()`.

**Інваріанти**: створюється виключно всередині `PayrollSheet.calculate()`; одне нарахування
відповідає рівно одному `Employee` в межах одного циклу (FR-006).

## Receipt

Квитанція, що подає працівникові підсумок завершеного нарахування.

| Атрибут | Тип | Опис |
|---|---|---|
| `employee` | `Employee` | Адресат квитанції |
| `calculation` | `PayrollCalculation` | Завершене нарахування, що подається |

**Методи**: конструктор `Receipt(Employee employee, PayrollCalculation calculation)`,
`void present()` — виводить у консоль повідомлення українською з іменем працівника та сумою
(FR-008).

**Інваріанти**: створюється лише якщо `PayrollSheet.findCalculation(employee)` повернула
існуюче нарахування для цього працівника (FR-007); до цього моменту запит завершується відмовою
без створення `Receipt`.

## PayrollRule (interface) / BasePayrollRule (abstract) / StandardPayrollRule

Контракт, спільна основа та конкретна реалізація обчислення суми нарахування.

- `PayrollRule`: `BigDecimal calculate(Employee employee)`.
- `BasePayrollRule`: абстрактний клас, що реалізує `PayrollRule` і надає спільну (наразі
  порожню/розширювану) основу для конкретних правил; конструктор `protected BasePayrollRule()`.
- `StandardPayrollRule extends BasePayrollRule`: повертає фіксовану константу суми, однакову
  для кожного `Employee` незалежно від його атрибутів (FR-005, Рішення #4 у `research.md`).

**Інваріанти**: `PayrollSheet` отримує конкретний екземпляр `PayrollRule` через
`setPayrollRule(PayrollRule payrollRule)`, а не через статичне поле чи створення всередині
`PayrollSheet` (явне впровадження залежності, принцип V).

## Зв'язки між класами (відповідно до `docs/class.mmd`)

| Зв'язок | Тип | Опис |
|---|---|---|
| `PayrollSheet` → `DirectorOrder` | Асоціація (1) | Відомість посилається на один наказ |
| `PayrollSheet` o-- `Employee` | Агрегація (0..*) | Відомість утримує, але не володіє життєвим циклом працівників |
| `PayrollSheet` *-- `PayrollCalculation` | Композиція (0..*) | Нарахування існують лише в межах відомості |
| `PayrollCalculation` → `Employee` | Асоціація (1) | Нарахування належить одному працівнику |
| `Receipt` → `Employee`, `Receipt` → `PayrollCalculation` | Асоціація (1, 1) | Квитанція посилається на адресата й завершене нарахування |
| `PayrollSheet` → `PayrollRule` | Асоціація (1) | Відомість отримує правило через setter і використовує його під час `calculate()` |
| `BasePayrollRule` ..|> `PayrollRule` | Реалізація | Абстрактна основа реалізує контракт |
| `StandardPayrollRule` --|> `BasePayrollRule` | Успадкування | Конкретне правило успадковує спільну основу |

## Відповідність станів (`docs/state.mmd`)

| Стан | Умова на рівні моделі |
|---|---|
| Очікування наказу | `PayrollSheet` ще не створено або поточний `order.isActive() == false` |
| Формування відомості | `order.isActive() == true`, `employees` може бути порожнім або непорожнім, `calculations` порожній |
| Готовність до нарахування | `order.isActive() == true`, `employees` непорожній, `calculations` порожній |
| Нарахування | Виконується `calculate()` (миттєвий перехід у межах виклику методу) |
| Нарахування завершено | `calculations` непорожній після успішного `calculate()` |
| Видача квитанції | Виконується `Receipt.present()` для працівника з існуючим `PayrollCalculation` |

Переходи відмови (нечинний наказ, дублікат, порожня відомість, передчасна квитанція) не змінюють
жодного з наведених полів і повертають цикл у той самий стан (FR-002, FR-003, FR-004, FR-007).

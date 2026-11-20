# Контракти Java-інтерфейсів MVC

Сигнатури орієнтовні (імена англійською за принципом VI); тіла — у фазі реалізації.

```text
model
  EmployeeStorageService
    void saveEmployee(Employee e)            // throws StorageException
    List<Employee> findAll()
    boolean existsById(String id)
    boolean update(Employee e)
    boolean delete(String id)
  PayrollSheet
    OperationResult listEmployees()
    OperationResult addEmployee(Employee e)
    OperationResult updateEmployee(String id, Employee e)
    OperationResult deleteEmployee(String id)

view
  PayrollWebView
    ApiResponse list(OperationResult r)
    ApiResponse added(OperationResult r)     // 201 / відмова
    ApiResponse updated(OperationResult r)   // 200 / відмова
    ApiResponse deleted(OperationResult r)   // 200 / відмова
  ApiResponse(int status, Object body)

controller
  PayrollController(PayrollSheet sheet, PayrollWebView view)
    ApiResponse list()
    ApiResponse add(String employeeId, String fullName)
    ApiResponse update(String pathId, String bodyEmployeeId, String fullName)
    ApiResponse delete(String id)

infrastructure
  WebServer
    void start(PayrollController controller, int port)
  JavalinWebServer implements WebServer         // єдиний клас з io.javalin / Jackson
  SqliteEmployeeStorageService implements EmployeeStorageService
  PayrollModule extends AbstractModule
    bind(EmployeeStorageService) -> SqliteEmployeeStorageService
    bind(WebServer) -> JavalinWebServer (Singleton)
```

Залежності: view → model (читає `OperationResult`); controller → model, view; infrastructure → controller;
model → нічого з view/controller/infrastructure; контролер і вигляд не імпортують `io.javalin`/Jackson.

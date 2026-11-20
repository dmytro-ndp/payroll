package ua.payroll.model;

/**
 * Непередбачена помилка сховища даних; кидається реалізаціями
 * {@link EmployeeStorageService} замість друку повідомлення в консоль.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

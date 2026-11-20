package ua.payroll.view;

/** Відповідь вигляду: код стану та тіло, яке серіалізує вебсервер. */
public record ApiResponse(int status, Object body) {
}

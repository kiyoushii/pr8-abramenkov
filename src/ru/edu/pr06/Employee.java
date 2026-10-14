package ru.edu.pr06;

/** Сотрудник-командированный. Неизменяемый объект. */
public class Employee {
    private final String id;
    private final String fullName;

    public Employee(String id, String fullName) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: id сотрудника не задан");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: ФИО сотрудника не задано");
        }
        this.id = id.trim();
        this.fullName = fullName.trim();
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }
}

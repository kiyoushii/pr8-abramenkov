package ru.edu.pr06;

import java.time.LocalDate;

/**
 * Заявка на командировку. Статус нельзя установить извне: он меняется только доменными методами
 * submit, approve, reject, cancel, complete. Неудачная операция бросает исключение и не меняет объект.
 */
public class BusinessTrip {
    /** Вариант 1: минимальная длина причины отклонения. Единственное место этого порога. */
    public static final int MIN_REASON_LENGTH = 10;

    private final String id;
    private final Employee employee;
    private final String destination;
    private final LocalDate start;
    private final LocalDate end;
    private TripStatus status = TripStatus.DRAFT;
    private String rejectionReason;

    public BusinessTrip(String id, Employee employee, String destination, LocalDate start, LocalDate end) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: id заявки не задан");
        }
        if (employee == null) {
            throw new IllegalArgumentException("Ошибка: сотрудник не задан");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: пункт назначения не задан");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Ошибка: даты начала и окончания обязательны");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Ошибка: дата окончания " + end + " раньше даты начала " + start);
        }
        this.id = id.trim();
        this.employee = employee;
        this.destination = destination.trim();
        this.start = start;
        this.end = end;
    }

    public void submit() {
        moveTo(TripStatus.SUBMITTED);
    }

    public void approve() {
        moveTo(TripStatus.APPROVED);
    }

    public void reject(String reason) {
        requireTransition(TripStatus.REJECTED);
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            throw new IllegalArgumentException("Ошибка: причина отклонения обязательна и должна содержать минимум "
                    + MIN_REASON_LENGTH + " символов");
        }
        rejectionReason = reason.trim();
        status = TripStatus.REJECTED;
    }

    public void cancel() {
        moveTo(TripStatus.CANCELLED);
    }

    /** Завершение возможно только из APPROVED и строго после даты окончания командировки. */
    public void complete(LocalDate today) {
        requireTransition(TripStatus.COMPLETED);
        if (today == null) {
            throw new IllegalArgumentException("Ошибка: текущая дата не задана");
        }
        if (!today.isAfter(end)) {
            throw new IllegalStateException("Ошибка: завершить можно только после даты окончания " + end
                    + ", получено " + today);
        }
        status = TripStatus.COMPLETED;
    }

    private void moveTo(TripStatus target) {
        requireTransition(target);
        status = target;
    }

    private void requireTransition(TripStatus target) {
        if (!status.canMoveTo(target)) {
            throw new IllegalStateException("Ошибка: переход " + status + " -> " + target + " недопустим");
        }
    }

    public String getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public TripStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}

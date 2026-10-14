package ru.edu.pr06;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервисный слой: хранит заявки, создаёт их и выполняет текстовые команды.
 * Команды: create/id/пункт/начало/конец (даты ГГГГ-ММ-ДД), submit, approve, reject/причина, cancel, complete/дата.
 */
public class BusinessTripService {
    private final List<BusinessTrip> trips = new ArrayList<BusinessTrip>();

    public BusinessTrip create(String id, Employee employee, String destination, LocalDate start, LocalDate end) {
        for (int i = 0; i < trips.size(); i++) {
            if (trips.get(i).getId().equals(id == null ? null : id.trim())) {
                throw new IllegalArgumentException("Ошибка: заявка " + id + " уже существует");
            }
        }
        BusinessTrip trip = new BusinessTrip(id, employee, destination, start, end);
        trips.add(trip);
        return trip;
    }

    public List<BusinessTrip> findByStatus(TripStatus status) {
        List<BusinessTrip> result = new ArrayList<BusinessTrip>();
        for (int i = 0; i < trips.size(); i++) {
            if (trips.get(i).getStatus() == status) {
                result.add(trips.get(i));
            }
        }
        return result;
    }

    public BusinessTrip find(String id) {
        for (int i = 0; i < trips.size(); i++) {
            if (trips.get(i).getId().equals(id)) {
                return trips.get(i);
            }
        }
        return null;
    }

    /** Выполняет команду над заявкой; для create заявка создаётся и возвращается. */
    public BusinessTrip execute(BusinessTrip current, String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: команда не задана");
        }
        String[] p = command.trim().split("/", -1);
        String name = p[0].trim().toLowerCase();
        if (name.equals("create")) {
            if (p.length != 5) {
                throw new IllegalArgumentException("Ошибка: формат create/id/пункт/начало/конец");
            }
            return create(p[1], new Employee("E1", "Сотрудник"), p[2], parseDate(p[3]), parseDate(p[4]));
        }
        if (current == null) {
            throw new IllegalStateException("Ошибка: сначала создайте заявку командой create");
        }
        if (name.equals("submit")) {
            current.submit();
        } else if (name.equals("approve")) {
            current.approve();
        } else if (name.equals("cancel")) {
            current.cancel();
        } else if (name.equals("reject")) {
            current.reject(p.length > 1 ? p[1] : null);
        } else if (name.equals("complete")) {
            current.complete(p.length > 1 ? parseDate(p[1]) : null);
        } else {
            throw new IllegalArgumentException("Ошибка: неизвестная команда \"" + p[0] + "\"");
        }
        return current;
    }

    static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: дата не задана");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ошибка: дата должна быть в формате ГГГГ-ММ-ДД, получено \"" + text.trim() + "\"");
        }
    }
}

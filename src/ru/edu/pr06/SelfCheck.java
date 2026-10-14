package ru.edu.pr06;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

/** Точка входа: unit-проверки, data/sample.csv и интерактивный режим. */
public class SelfCheck {
    private static int passed = 0;
    private static int failed = 0;

    private static final LocalDate START = LocalDate.of(2026, 11, 10);
    private static final LocalDate END = LocalDate.of(2026, 11, 14);

    public static void main(String[] args) throws IOException {
        String csv = args.length > 0 ? args[0] : "data/sample.csv";
        System.out.println("=== Unit-проверки ===");
        unitChecks();
        System.out.println("=== Data-driven проверки: " + csv + " ===");
        dataChecks(csv);
        System.out.println("Итого: PASS=" + passed + ", FAIL=" + failed);
        System.out.println(failed == 0 ? "ALL PASS" : "ЕСТЬ ОШИБКИ");
        interactive();
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    private static RuntimeException thrown(Runnable r) {
        try {
            r.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static boolean iae(Runnable r) {
        return thrown(r) instanceof IllegalArgumentException;
    }

    private static boolean ise(Runnable r) {
        return thrown(r) instanceof IllegalStateException;
    }

    private static BusinessTrip trip() {
        return new BusinessTrip("T1", new Employee("E1", "Иванов Иван"), "Казань", START, END);
    }

    private static BusinessTrip inState(TripStatus s) {
        BusinessTrip t = trip();
        if (s == TripStatus.DRAFT) return t;
        if (s == TripStatus.CANCELLED) { t.cancel(); return t; }
        t.submit();
        if (s == TripStatus.SUBMITTED) return t;
        if (s == TripStatus.REJECTED) { t.reject("Нет бюджета на поездку"); return t; }
        t.approve();
        if (s == TripStatus.APPROVED) return t;
        t.complete(END.plusDays(1));
        return t;
    }

    private static void unitChecks() {
        // --- конструктор и инварианты
        check("Новая заявка в статусе DRAFT", trip().getStatus() == TripStatus.DRAFT);
        check("Конструктор: окончание раньше начала — ошибка", iae(new Runnable() { public void run() {
            new BusinessTrip("T", new Employee("E", "x"), "Казань", END, START); } }));
        check("Конструктор: один день (начало = конец) допустим", new BusinessTrip("T", new Employee("E", "x"), "Казань", START, START) != null);
        check("Конструктор: null-дата — ошибка", iae(new Runnable() { public void run() {
            new BusinessTrip("T", new Employee("E", "x"), "Казань", null, END); } }));
        check("Конструктор: пустой пункт назначения — ошибка", iae(new Runnable() { public void run() {
            new BusinessTrip("T", new Employee("E", "x"), " ", START, END); } }));
        check("Конструктор: null-сотрудник — ошибка", iae(new Runnable() { public void run() {
            new BusinessTrip("T", null, "Казань", START, END); } }));
        check("Employee: пустое ФИО — ошибка", iae(new Runnable() { public void run() { new Employee("E", ""); } }));

        // --- допустимые переходы
        BusinessTrip a = trip();
        a.submit();
        check("DRAFT -> SUBMITTED (submit)", a.getStatus() == TripStatus.SUBMITTED);
        a.approve();
        check("SUBMITTED -> APPROVED (approve)", a.getStatus() == TripStatus.APPROVED);
        a.complete(END.plusDays(1));
        check("APPROVED -> COMPLETED после даты окончания", a.getStatus() == TripStatus.COMPLETED);
        BusinessTrip r = inState(TripStatus.SUBMITTED);
        r.reject("Недостаточно обоснования");
        check("SUBMITTED -> REJECTED (reject) с причиной", r.getStatus() == TripStatus.REJECTED
                && r.getRejectionReason().equals("Недостаточно обоснования"));
        check("cancel из DRAFT", cancelFrom(TripStatus.DRAFT));
        check("cancel из SUBMITTED", cancelFrom(TripStatus.SUBMITTED));
        check("cancel из APPROVED", cancelFrom(TripStatus.APPROVED));

        // --- недопустимые переходы (ISE) и сохранность состояния
        check("approve из DRAFT запрещён", ise(new Runnable() { public void run() { trip().approve(); } }));
        check("reject из DRAFT запрещён", ise(new Runnable() { public void run() { trip().reject("Достаточно длинная причина"); } }));
        check("complete из SUBMITTED запрещён", ise(new Runnable() { public void run() { inState(TripStatus.SUBMITTED).complete(END.plusDays(5)); } }));
        check("повторный submit запрещён", ise(new Runnable() { public void run() { inState(TripStatus.SUBMITTED).submit(); } }));
        check("cancel из COMPLETED запрещён", ise(new Runnable() { public void run() { inState(TripStatus.COMPLETED).cancel(); } }));
        check("cancel из REJECTED запрещён", ise(new Runnable() { public void run() { inState(TripStatus.REJECTED).cancel(); } }));
        check("cancel из CANCELLED запрещён", ise(new Runnable() { public void run() { inState(TripStatus.CANCELLED).cancel(); } }));
        check("approve после CANCELLED запрещён", ise(new Runnable() { public void run() { inState(TripStatus.CANCELLED).approve(); } }));
        check("submit после REJECTED запрещён", ise(new Runnable() { public void run() { inState(TripStatus.REJECTED).submit(); } }));
        final BusinessTrip keep = inState(TripStatus.SUBMITTED);
        thrown(new Runnable() { public void run() { keep.complete(END.plusDays(3)); } });
        check("Неудачная операция не меняет статус", keep.getStatus() == TripStatus.SUBMITTED);

        // --- таблица переходов целиком: 6 x 6
        int allowed = 0;
        TripStatus[] all = TripStatus.values();
        for (int i = 0; i < all.length; i++) {
            for (int j = 0; j < all.length; j++) {
                if (all[i].canMoveTo(all[j])) {
                    allowed++;
                }
            }
        }
        check("В таблице переходов ровно 7 допустимых пар", allowed == 7);

        // --- причина отклонения: граница 10 символов (вариант 1)
        check("Причина из 10 символов принимается", rejectOk("1234567890"));
        check("Причина из 9 символов отклоняется", rejectBad("123456789"));
        check("Причина из пробелов отклоняется", rejectBad("            "));
        check("Причина null отклоняется", rejectBad(null));
        check("Причина с пробелами по краям считается без них", rejectBad("   123456789   "));
        check("Причина без обязательных символов не меняет статус", rejectKeepsStatus());

        // --- дата завершения
        check("complete в день окончания запрещён", ise(new Runnable() { public void run() { inState(TripStatus.APPROVED).complete(END); } }));
        check("complete на следующий день разрешён", completeOk(END.plusDays(1)));
        check("complete до начала запрещён", ise(new Runnable() { public void run() { inState(TripStatus.APPROVED).complete(START.minusDays(1)); } }));
        check("complete с null-датой — ошибка", iae(new Runnable() { public void run() { inState(TripStatus.APPROVED).complete(null); } }));

        // --- сервис
        BusinessTripService s = new BusinessTripService();
        BusinessTrip t = s.create("T9", new Employee("E2", "Петров Пётр"), "Москва", START, END);
        check("Сервис: findByStatus(DRAFT) находит заявку", s.findByStatus(TripStatus.DRAFT).size() == 1);
        t.submit();
        check("Сервис: после submit DRAFT пуст, SUBMITTED = 1", s.findByStatus(TripStatus.DRAFT).isEmpty()
                && s.findByStatus(TripStatus.SUBMITTED).size() == 1);
        final BusinessTripService fs = s;
        check("Сервис: дубликат id — ошибка", iae(new Runnable() { public void run() {
            fs.create("T9", new Employee("E2", "x"), "Москва", START, END); } }));
        check("Сервис: дата в неверном формате — ошибка", iae(new Runnable() { public void run() {
            fs.execute(null, "create/T10/Казань/10.11.2026/2026-11-14"); } }));
        check("Сервис: команда без заявки — ошибка состояния", ise(new Runnable() { public void run() {
            fs.execute(null, "submit"); } }));
    }

    private static boolean cancelFrom(TripStatus s) {
        BusinessTrip t = inState(s);
        t.cancel();
        return t.getStatus() == TripStatus.CANCELLED;
    }

    private static boolean rejectOk(String reason) {
        BusinessTrip t = inState(TripStatus.SUBMITTED);
        t.reject(reason);
        return t.getStatus() == TripStatus.REJECTED;
    }

    private static boolean rejectBad(final String reason) {
        final BusinessTrip t = inState(TripStatus.SUBMITTED);
        return iae(new Runnable() { public void run() { t.reject(reason); } });
    }

    private static boolean rejectKeepsStatus() {
        final BusinessTrip t = inState(TripStatus.SUBMITTED);
        thrown(new Runnable() { public void run() { t.reject("коротко"); } });
        return t.getStatus() == TripStatus.SUBMITTED && t.getRejectionReason() == null;
    }

    private static boolean completeOk(LocalDate d) {
        BusinessTrip t = inState(TripStatus.APPROVED);
        t.complete(d);
        return t.getStatus() == TripStatus.COMPLETED;
    }

    /** Строка: команды через «|» ; ожидание OK:status=X или ERROR:фрагмент сообщения. */
    private static void dataChecks(String path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            check("Файл " + path + " доступен", false);
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int cut = line.lastIndexOf(';');
            if (cut < 0) {
                check("Строка " + (i + 1) + ": неверный формат строки данных", false);
                continue;
            }
            String commands = line.substring(0, cut);
            String expected = line.substring(cut + 1).trim();
            check("Строка " + (i + 1) + ": " + commands + " -> " + expected, matches(commands, expected));
        }
    }

    private static boolean matches(String commands, String expected) {
        BusinessTripService service = new BusinessTripService();
        BusinessTrip trip = null;
        String error = null;
        String[] list = commands.split("\\|", -1);
        for (int i = 0; i < list.length; i++) {
            try {
                trip = service.execute(trip, list[i]);
            } catch (IllegalArgumentException e) {
                error = e.getMessage();
                break;
            } catch (IllegalStateException e) {
                error = e.getMessage();
                break;
            }
        }
        if (expected.startsWith("ERROR:")) {
            return error != null && error.contains(expected.substring(6));
        }
        return error == null && trip != null && expected.equals("OK:status=" + trip.getStatus());
    }

    private static void interactive() throws IOException {
        System.out.println("=== Интерактивный режим ===");
        System.out.println("Команды: create/id/пункт/ГГГГ-ММ-ДД/ГГГГ-ММ-ДД, submit, approve, reject/причина, cancel, complete/дата, show, exit");
        BusinessTripService service = new BusinessTripService();
        BusinessTrip trip = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.equalsIgnoreCase("exit")) {
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (line.equalsIgnoreCase("show")) {
                    System.out.println(trip == null ? "Заявки нет" : "Заявка " + trip.getId() + ": " + trip.getStatus());
                } else {
                    trip = service.execute(trip, line);
                    System.out.println("OK, статус: " + trip.getStatus());
                }
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

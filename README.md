# Практическая работа №8. Инкапсуляция, enum и управление состоянием

Студент: Абраменков Андрей, группа ЭФБО-12-25, вариант 1 (минимальная длина причины отклонения — 10 символов).

Классы (пакет `ru.edu.pr06`): `TripStatus` (enum), `Employee`, `BusinessTrip`, `BusinessTripService`, `SelfCheck`.

Запуск: `run.bat` (Windows) или `sh run.sh`. Вручную: `javac -encoding UTF-8 -d out src/ru/edu/pr06/*.java`,
затем `java -Dfile.encoding=UTF-8 -cp out ru.edu.pr06.SelfCheck`.

SelfCheck выполняет 63 проверки (unit и строки `data/sample.csv`), затем интерактивный режим.
Команды: `create/id/пункт/ГГГГ-ММ-ДД/ГГГГ-ММ-ДД`, `submit`, `approve`, `reject/причина`, `cancel`, `complete/дата`, `show`, `exit`.

Диаграммы: `diagrams/`. Таблица переходов и решения: `DECISIONS.md`.

package ru.edu.pr06;

/** Состояния командировки. Допустимые переходы записаны здесь, в одном месте (таблица переходов). */
public enum TripStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED, CANCELLED, COMPLETED;

    /** Можно ли перейти из текущего состояния в целевое. */
    public boolean canMoveTo(TripStatus target) {
        switch (this) {
            case DRAFT:
                return target == SUBMITTED || target == CANCELLED;
            case SUBMITTED:
                return target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED:
                return target == COMPLETED || target == CANCELLED;
            default: // REJECTED, CANCELLED, COMPLETED — конечные состояния
                return false;
        }
    }
}

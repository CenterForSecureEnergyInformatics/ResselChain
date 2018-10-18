/*
* This file is part of ResselChain.
* Copyright Center for Secure Energy Informatics 2018
* Fabian Knirsch, Andreas Unterweger, Clemens Brunner
* This code is licensed under a modified 3-Clause BSD License. See LICENSE file for details.
*/

package at.entrust.resselchain.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;

public class TimeSlots {
    public static int getSlotsPerDay(long date) {
        LocalDate localDate = LocalDate.ofEpochDay(date);
        int month = localDate.getMonthValue();
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.SUNDAY && localDate.plusWeeks(1).getMonthValue() != month) {
            if (month == 2) return 92;
            else if (month == 9) return 100;
        }
        return 96;
    }
}

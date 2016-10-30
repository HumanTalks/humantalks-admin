package com.humantalks.common.services

import org.joda.time.{ LocalTime, LocalDate, DateTime }

object DateSrv {
  def getNthDayOfMonth(start: DateTime, nth: Int, dayOfWeek: Int): LocalDate =
    new LocalDate(start.getYear, start.getMonthOfYear, 1).withDayOfWeek(dayOfWeek).plusWeeks(nth)

  def nextDate(start: DateTime, nthOfMonth: Int, dayOfWeek: Int, time: LocalTime): DateTime = {
    val thisMonth = getNthDayOfMonth(start, nthOfMonth, dayOfWeek).toDateTime(time)
    val nextMonth = getNthDayOfMonth(start.plusMonths(1), nthOfMonth, dayOfWeek).toDateTime(time)
    if (thisMonth.isAfter(start)) thisMonth else nextMonth
  }
}

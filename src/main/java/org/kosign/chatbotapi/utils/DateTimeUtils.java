package org.kosign.chatbotapi.utils;

import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

public class DateTimeUtils {

    private static final String VA_PATTERN_YYYYMM = "yyyyMM";
    private static final String VA_PATTERN_TIME6 = "HHmmss";
    private static final String VA_PATTERN_MONTH6 = "yyyyMM";
    private static final String VA_PATTERN_DATE8 = "yyyyMMdd";
    private static final String VA_PATTERN_DTM14 = "yyyyMMddHHmmss";
    private static final String VA_PATTERN_DTM_MINUTE = "yyyyMMddHHmm";
    private static final String TIME_FORMAT = "HH:mm";

    private static final String DATE_FORMAT = "yyyyMMddHHmmss";

    public static final DateTimeFormatter VA_FORMATTER_TIME6 = DateTimeFormatter.ofPattern(VA_PATTERN_TIME6);
    public static final DateTimeFormatter VA_FORMATTER_DATE8 = DateTimeFormatter.ofPattern(VA_PATTERN_DATE8);
    public static final DateTimeFormatter VA_FORMATTER_DTM14 = DateTimeFormatter.ofPattern(VA_PATTERN_DTM14);
    public static final DateTimeFormatter VA_FORMATTER_MONTH6= DateTimeFormatter.ofPattern(VA_PATTERN_MONTH6);
    public static final DateTimeFormatter VA_FORMATTER_YYYYMM = DateTimeFormatter.ofPattern(VA_PATTERN_YYYYMM);

    public static final DateTimeFormatter VA_FORMATTER_DTM_MINUTE = DateTimeFormatter.ofPattern(VA_PATTERN_DTM_MINUTE);

    public static final Clock clock = Clock.system(TimeZone.getDefault().toZoneId());
    public static final Duration clockSkew = Duration.ofSeconds(60);

    public static LocalDateTime ictNow() {
        return LocalDateTime.now(clock);
    }

    public static String getDateNow(){
        return ictNow().format(VA_FORMATTER_DATE8);
    }

    public static String getTimeNow(){
        return ictNow().format(VA_FORMATTER_TIME6);
    }

    public static String getEndOfTime(){
        return LocalTime.MAX.format(DateTimeFormatter.ofPattern(VA_PATTERN_TIME6));
    }

    public static String getDateTimeNow(){
        return ictNow().format(VA_FORMATTER_DTM14);
    }

    public static LocalDateTime atEndOfDay(){
        return ictNow().toLocalDate().atTime(LocalTime.MAX);
    }

    public static LocalDateTime prevNow() {
        var localDateTime = ictNow();
        return localDateTime.minusDays ( 1 );
    }

    public static String calculateDueDate(String issueDateStr, String paymentTermDays) {
        // Parse the issue date string to LocalDateTime
        LocalDateTime issueDate = LocalDateTime.parse(issueDateStr, VA_FORMATTER_DTM14);

        // Add payment term days
        LocalDateTime dueDate = issueDate.plusDays(Long.parseLong(paymentTermDays));

        // Set time to end of the day (23:59:59)
        dueDate = dueDate.with(LocalTime.of(23, 59, 59));

        // Format due date to string
        return dueDate.format(VA_FORMATTER_DTM14);
    }
    //00:02 to 0000
    public static String formatTimeToHHmmss(String time) {
        if (time == null || time.isEmpty()) {
            return "000000"; // Default to midnight if input is null or empty
        }
        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("HHmmss");
            LocalTime localTime = LocalTime.parse(time, inputFormatter);
            return localTime.format(outputFormatter);
        } catch (DateTimeParseException e) {
            return "000000"; // Return default if parsing fails
        }
    }

    public static String getDueDateTime(Long term) {
        return ictNow().plusDays(term).format(VA_FORMATTER_DTM14);
    }

    public static LocalDateTime parseDateTime(@NonNull String dateTime) {
        return LocalDateTime.parse(dateTime, VA_FORMATTER_DTM14);
    }

    public static LocalDateTime formatDateTimeMinute(@NonNull String dateTime) {
        return LocalDateTime.parse(dateTime , VA_FORMATTER_DTM_MINUTE);
    }

    public static LocalDate parseDate(String date) {
        if(date == null || date.length() != 8) {
            return null;
        }
        return LocalDate.parse(date, VA_FORMATTER_DATE8);
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = DayOfWeek.of(date.get(ChronoField.DAY_OF_WEEK));
        return day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
    }

    //get time yyyyMMddHHmmss to hh:mm
    public static String getTime(String dateTime) {
        return dateTime.substring(8, 14);
    }

    public static void main(String[] args) {
        System.err.println(parseDateTime("20230227125500").isAfter(LocalDateTime.now()));
        System.err.println(DateTimeUtils.ictNow().format(VA_FORMATTER_YYYYMM).equals(parseDateTime("20220627125500").format(VA_FORMATTER_YYYYMM)));
        //ex: getTime("20230227125500") will return 12:55
        System.err.println(getTime("20230227125500"));
    }

    public static String stringToDate (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("dd/MMMM/yyyy"));
    }

    public static String stringToDateWithFormat (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("dd, MMMM yyyy"));
    }

    public static String stringToDateYYYY_MM_DD (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
    }

    public static String stringToDateYYYY_MM_DD_HH_MM_SS(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
        return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a"));
    }

    public static String stringToDateDD_MM_YYYY (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("dd/MM/YYYY"));
    }

    public static String stringToDateYYYYMMDD (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern(VA_PATTERN_DATE8));
    }

    public static String stringToDateMMMM_YYYY (String value) {
        String fullDate = value + "01";
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(fullDate, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        return date.format(outputFormatter);
    }

    public static String date_YYYYMMDD_add_HHmmss(String value) {
        // Get the current time in HHmmss format
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        // Combine the date and time
        return value + currentTime;
    }

    public static boolean isMoreThanOneMinuteInPast(String dateTime) {
        // Define the formatter for yyyyMMddHHmmss
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // Parse the input date-time string to LocalDateTime
        LocalDateTime inputDateTime = LocalDateTime.parse(dateTime, formatter);

        // Get the current date-time
        LocalDateTime currentDateTime = LocalDateTime.now();

        // Check if the input date-time is more than one minute in the past
        return Duration.between(inputDateTime, currentDateTime).toMinutes() > 1;
    }

    public static boolean isDateInFuture(String date) {
        // Define the formatter for yyyyMMdd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // Parse the input date string to LocalDate
        LocalDate inputDate = LocalDate.parse(date, formatter);

        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Check if the input date is after the current date
        return inputDate.isAfter(currentDate);
    }

    public static String daysBetween(String issueDate, String dueDate) {

        String startDateStr = issueDate.substring(0, 8);
        String endDateStr = dueDate.substring(0, 8);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate startDate = LocalDate.parse(startDateStr, formatter);
        LocalDate endDate = LocalDate.parse(endDateStr, formatter);

        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return String.valueOf(days);
    }

    public static String stringToDateDD_MM_YY (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("dd-MM-YY"));
    }

    public static String stringToDateDD_MMM_YY (String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDate.parse(value, formatter).format(DateTimeFormatter.ofPattern("dd-MMM-YY"));
    }
    public static String getOrdinalSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default:
                return "th";
        }
    }

    public static Integer calculateDaysStartEnd_yyyyMMdd(String startDate, String endDate) {
        try {
            // Define formatter for 'yyyyMMdd' format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(VA_PATTERN_DATE8);

            // Parse the date strings
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);

            // Calculate days between (inclusive)
            return Math.toIntExact(ChronoUnit.DAYS.between(start, end));

        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                    "Invalid date format. Please use 'yyyyMMdd' format.",
                    e.getParsedString(),
                    e.getErrorIndex()
            );
        }
    }

    public static String getEndDateByDays_yyyyMMdd(int days) {
        // Get current date
        LocalDate currentDate = LocalDate.now();

        // Add specified number of days
        LocalDate endDate = currentDate.plusDays(days);

        // Format the result as 'yyyyMMdd'
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(VA_PATTERN_DATE8);
        return endDate.format(formatter);
    }

    public static String getNextDay_yyyyMMdd(String dateStr) {
        try {
            // Define formatter for 'yyyyMMdd' format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(VA_PATTERN_DATE8);

            // Parse the input date string
            LocalDate date = LocalDate.parse(dateStr, formatter);

            // Get next day
            LocalDate nextDay = date.plusDays(1);

            // Format result as 'yyyyMMdd'
            return nextDay.format(formatter);

        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                    "Invalid date format. Please use 'yyyyMMdd' format.",
                    e.getParsedString(),
                    e.getErrorIndex()
            );
        }
    }


    public static String getEndDateYear_yyyyMMdd(BigDecimal years, String startDateStr) {
        try {
            // Define formatter for 'yyyyMMdd' format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            // Parse the start date string
            LocalDate startDate = LocalDate.parse(startDateStr, formatter);

            // Convert years to days (considering average of 365.25 days per year)
            long days = years == null ? 0 : years.multiply(BigDecimal.valueOf(365.25)).longValue();

            // Add calculated days
            LocalDate endDate = startDate.plusDays(days);

            // Format result as 'yyyyMMdd'
            return endDate.format(formatter);

        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                    "Invalid date format. Please use 'yyyyMMdd' format.",
                    e.getParsedString(),
                    e.getErrorIndex()
            );
        }
    }









    public static String convertMonthToOrdinal(String month) {
        switch (month) {
            case "January":   return "1";
            case "February":  return "2";
            case "March":     return "3";
            case "April":     return "4";
            case "May":       return "5";
            case "June":      return "6";
            case "July":      return "7";
            case "August":    return "8";
            case "September": return "9";
            case "October":   return "10";
            case "November":  return "11";
            case "December":  return "12";
            default:          throw new IllegalArgumentException("Invalid month name: " + month);
        }
    }

//    String value = "20241228124300";
//    The output will be: "28 Dec 2024"
    public static String formatDateTimeMonthChar(String value) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value,
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    public static String format3DateTimeMonthChar(String value) {
        try {
            LocalDate date = LocalDate.parse(value,
                    DateTimeFormatter.ofPattern("yyyyMMdd"));
            return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } catch (DateTimeParseException e) {
            return "";
        }
    }


    public static String getDayFromDate(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value,
                    DateTimeFormatter.ofPattern(DATE_FORMAT));
            return String.format("%02d", dateTime.getDayOfMonth());
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    public static String getDayOfWeekFromDate(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value,
                    DateTimeFormatter.ofPattern(DATE_FORMAT));
            return String.valueOf(dateTime.getDayOfWeek().getValue());
        } catch (DateTimeParseException e) {
            return "";
        }
    }

    public static String getMonthFromDate(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(value,
                    DateTimeFormatter.ofPattern(DATE_FORMAT));
            return String.format("%02d", dateTime.getMonthValue());
        } catch (DateTimeParseException e) {
            return "";
        }
    }


}

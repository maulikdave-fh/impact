package in.foresthut.impact.utils.datetime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
	public static LocalDateTime parse(String s) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		if (s==null || s.isBlank())
			return LocalDateTime.parse("2020-01-01 00:00:00", formatter);
					
		s = s.replaceAll("Z|z", "");
		if (!s.contains("T"))
			s = s + " 00:00:00";

		s = s.replaceAll("T", " ");

		LocalDateTime result = null;
		if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}")) {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			result = LocalDateTime.parse(s, formatter);
		} else if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}")) {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
			result = LocalDateTime.parse(s, formatter);
		} else if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}\\.\\d{3}\\+\\d{2}:\\d{2}")) {
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			result = LocalDateTime.parse(s.substring(0, s.indexOf('.')), formatter);
		}
		return result;
	}
}

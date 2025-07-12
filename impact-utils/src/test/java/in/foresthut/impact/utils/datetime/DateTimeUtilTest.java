package in.foresthut.impact.utils.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

public class DateTimeUtilTest {
	@Test
	void test_whenNoTimeGiven_shouldReturnDateWithHHmmyyAsZeros() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		assertEquals(LocalDateTime.parse("2024-12-01 00:00:00", formatter), DateTimeUtil.parse("2024-12-01"));
	}
	
	@Test
	void test_whenTimeIsGiven_shouldReturnCorrectDateTime() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		assertEquals(LocalDateTime.parse("2024-12-01 23:34:05", formatter), DateTimeUtil.parse("2024-12-01T23:34:05"));
	}
	
	@Test
	void test_whenTimeIsWithHHmm_shouldReturnCorrectDateTime() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		assertEquals(LocalDateTime.parse("2024-12-01 23:34:00", formatter), DateTimeUtil.parse("2024-12-01T23:34"));
	}
	
	@Test
	void test_whenStringIsNull_shouldReturnADefaultDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		assertEquals(LocalDateTime.parse("2020-01-01 00:00:00", formatter), DateTimeUtil.parse(null));
	}
	
	@Test
	void test_whenStringWithMilliSeconds_shouldReturnDate() {
		// 2025-06-14T02:34:29.000+00:00
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		assertEquals(LocalDateTime.parse("2025-06-14 02:34:29", formatter), DateTimeUtil.parse("2025-06-14T02:34:29.000+00:00"));
	}
}

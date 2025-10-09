package nlob.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    /**
     * 构建时间过滤条件，获取从指定天数前的UTC时间。
     *
     * @param daysBack 从多少天前开始同步（相对于运行此代码的时刻，在GitHub Actions中即UTC时间）
     */
    public static String getUTCBeforeDays(int daysBack) {
        // 在GitHub Actions中，Instant.now()获取的是UTC时间
        Instant nowUtc = Instant.now();
        // 计算起始时间点
        Instant sinceUtc = nowUtc.minus(Duration.ofDays(daysBack));

        // 格式化日期为Notion API要求的ISO 8601格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
        String sinceTimeString = formatter.format(sinceUtc);

        return sinceTimeString;
    }
    /**
     * 解析 Notion API 的 UTC 时间并转换为北京时间
     */
    public static LocalDateTime parseUTCDateTime2Beijing(String utcDateTimeStr) {
        try {
            // Notion API 返回的是 UTC 时间
            LocalDateTime utcTime = LocalDateTime.parse(utcDateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

            // 转换为北京时间 (UTC+8)
            ZonedDateTime utcZoned = utcTime.atZone(ZoneOffset.UTC);
            ZonedDateTime beijingTime = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Shanghai"));

            return beijingTime.toLocalDateTime();

        } catch (Exception e) {
            System.err.println("日期解析失败: " + utcDateTimeStr + " - " + e.getMessage());
            return LocalDateTime.now(); // 返回当前北京时间作为备用
        }
    }
}

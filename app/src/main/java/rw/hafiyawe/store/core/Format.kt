package rw.hafiyawe.store.core

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** ISO-8601 helpers. java.time needs API 26, so SimpleDateFormat keeps minSdk 24 working. */
object Iso {

    private val full = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val short = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val day = SimpleDateFormat("d MMM yyyy", Locale.US)
    private val dayTime = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US)

    fun now(): String = synchronized(full) { full.format(Date()) }

    fun hoursAgo(hours: Int): String =
        synchronized(full) { full.format(Date(System.currentTimeMillis() - hours * 3_600_000L)) }

    fun daysAhead(days: Int): String =
        synchronized(full) { full.format(Date(System.currentTimeMillis() + days * 86_400_000L)) }

    fun parse(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        return runCatching { synchronized(full) { full.parse(value) } }.getOrNull()
            ?: runCatching { synchronized(short) { short.parse(value) } }.getOrNull()
    }

    /** "just now", "12m", "3h", "2d", then a plain date. */
    fun relative(value: String?): String {
        val date = parse(value) ?: return ""
        val diff = System.currentTimeMillis() - date.time
        return when {
            diff < 60_000L -> "just now"
            diff < 3_600_000L -> "${diff / 60_000L}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
            diff < 7 * 86_400_000L -> "${diff / 86_400_000L}d ago"
            else -> day.format(date)
        }
    }

    fun date(value: String?): String = parse(value)?.let { day.format(it) } ?: ""

    fun dateTime(value: String?): String = parse(value)?.let { dayTime.format(it) } ?: ""

    /** Days left on a monthly plan; 0 when not active. */
    fun daysLeft(value: String?): Int {
        val date = parse(value) ?: return 0
        val diff = date.time - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / 86_400_000L).toInt() + 1
    }
}

fun daysLeft(planExpiresAt: String?): Int = Iso.daysLeft(planExpiresAt)

fun rwf(amount: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(amount) + " RWF"

fun compactRwf(amount: Long): String =
    if (amount >= 1_000_000L) {
        String.format(Locale.US, "%.1fM RWF", amount / 1_000_000.0)
    } else {
        rwf(amount)
    }

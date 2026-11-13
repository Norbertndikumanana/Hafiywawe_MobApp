package rw.hafiyawe.store.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.AppConfig
import rw.hafiyawe.store.data.AppNotification
import rw.hafiyawe.store.data.AuditEntry
import rw.hafiyawe.store.data.ChatRoom
import rw.hafiyawe.store.data.HelpArticle
import rw.hafiyawe.store.data.Message
import rw.hafiyawe.store.data.Payment
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.SavedEntity
import rw.hafiyawe.store.data.SavedProduct
import rw.hafiyawe.store.data.SupportMessage
import rw.hafiyawe.store.data.SupportTicket
import rw.hafiyawe.store.data.ViewEvent

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    isLenient = true
}

class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>?): String = AppJson.encodeToString(value ?: emptyList())

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { AppJson.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun messagesToJson(value: List<SupportMessage>?): String = AppJson.encodeToString(value ?: emptyList())

    @TypeConverter
    fun jsonToMessages(value: String?): List<SupportMessage> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { AppJson.decodeFromString<List<SupportMessage>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun providersToJson(value: rw.hafiyawe.store.data.SocialProviders?): String =
        AppJson.encodeToString(value ?: rw.hafiyawe.store.data.SocialProviders())

    @TypeConverter
    fun jsonToProviders(value: String?): rw.hafiyawe.store.data.SocialProviders =
        if (value.isNullOrBlank()) rw.hafiyawe.store.data.SocialProviders()
        else runCatching {
            AppJson.decodeFromString<rw.hafiyawe.store.data.SocialProviders>(value)
        }.getOrDefault(rw.hafiyawe.store.data.SocialProviders())

    @TypeConverter
    fun elementToJson(value: JsonElement?): String? = value?.toString()

    @TypeConverter
    fun jsonToElement(value: String?): JsonElement? =
        value?.let { runCatching { AppJson.parseToJsonElement(it) }.getOrNull() }
}

@Dao
interface ProductDao {
    @Query(
        "SELECT * FROM products WHERE status = 'active' AND (isDemo = 0 OR :allowDemo = 1) " +
            "ORDER BY createdAt DESC"
    )
    fun observeActive(allowDemo: Boolean): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun observeMine(sellerId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: String): Flow<Product?>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: String): Product?

    @Query("SELECT * FROM products")
    suspend fun all(): List<Product>

    @Upsert
    suspend fun upsert(product: Product)

    @Upsert
    suspend fun upsertAll(products: List<Product>)

    @Query("DELETE FROM products WHERE status = 'active' AND id NOT IN (:ids)")
    suspend fun deleteActiveNotIn(ids: List<String>)

    @Query("DELETE FROM products WHERE status = 'active'")
    suspend fun deleteActive()

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: String)

    @Transaction
    suspend fun replaceActive(list: List<Product>) {
        if (list.isEmpty()) deleteActive() else deleteActiveNotIn(list.map { it.id })
        upsertAll(list)
    }
}

@Dao
interface SavedDao {
    @Query(
        "SELECT p.*, s.available AS savedAvailable, s.savedAt AS savedAt FROM products p " +
            "INNER JOIN saved s ON s.productId = p.id WHERE s.kind = :kind ORDER BY s.savedAt DESC"
    )
    fun observe(kind: String): Flow<List<SavedProduct>>

    @Query("SELECT COUNT(*) FROM saved WHERE kind = :kind AND productId = :productId")
    fun observeIsSaved(kind: String, productId: String): Flow<Int>

    @Upsert
    suspend fun save(row: SavedEntity)

    @Query("DELETE FROM saved WHERE kind = :kind AND productId = :productId")
    suspend fun remove(kind: String, productId: String)

    @Query("DELETE FROM saved WHERE kind = :kind")
    suspend fun clearKind(kind: String)

    @Query("SELECT * FROM saved WHERE kind = :kind ORDER BY savedAt DESC")
    suspend fun rows(kind: String): List<SavedEntity>
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_rooms ORDER BY updatedAt DESC")
    fun observeRooms(): Flow<List<ChatRoom>>

    @Query("SELECT * FROM chat_rooms WHERE id = :id")
    fun observeRoom(id: String): Flow<ChatRoom?>

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY createdAt ASC")
    fun observeMessages(roomId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY createdAt ASC")
    suspend fun messagesOnce(roomId: String): List<Message>

    @Query("SELECT * FROM chat_rooms WHERE id = :id")
    suspend fun room(id: String): ChatRoom?

    @Query("SELECT * FROM chat_rooms ORDER BY updatedAt DESC")
    suspend fun roomsOnce(): List<ChatRoom>

    @Upsert
    suspend fun upsertRoom(room: ChatRoom)

    @Upsert
    suspend fun upsertRooms(rooms: List<ChatRoom>)

    @Upsert
    suspend fun upsertMessage(message: Message)

    @Upsert
    suspend fun upsertMessages(messages: List<Message>)

    @Query("DELETE FROM chat_rooms")
    suspend fun clearRooms()

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    @Transaction
    suspend fun replaceRooms(rooms: List<ChatRoom>) {
        clearRooms()
        upsertRooms(rooms)
    }
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE readAt IS NULL")
    fun observeUnread(): Flow<Int>

    @Upsert
    suspend fun upsert(list: List<AppNotification>)

    @Query("UPDATE notifications SET readAt = :readAt WHERE id = :id")
    suspend fun markRead(id: String, readAt: String)

    @Query("UPDATE notifications SET readAt = :readAt WHERE readAt IS NULL")
    suspend fun markAllRead(readAt: String)

    @Query("DELETE FROM notifications")
    suspend fun clear()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Payment>>

    @Upsert
    suspend fun upsert(payment: Payment)

    @Upsert
    suspend fun upsertAll(list: List<Payment>)

    @Query("DELETE FROM payments")
    suspend fun clear()

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    suspend fun allOnce(): List<Payment>
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    fun observeConfig(): Flow<AppConfig?>

    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun config(): AppConfig?

    @Upsert
    suspend fun upsertConfig(config: AppConfig)

    @Query("SELECT * FROM help_articles ORDER BY title ASC")
    fun observeHelp(): Flow<List<HelpArticle>>

    @Upsert
    suspend fun upsertHelp(list: List<HelpArticle>)

    @Query("DELETE FROM help_articles")
    suspend fun clearHelp()

    @Query("SELECT * FROM announcements ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAnnouncements(): Flow<List<Announcement>>

    @Upsert
    suspend fun upsertAnnouncements(list: List<Announcement>)

    @Query("DELETE FROM announcements")
    suspend fun clearAnnouncements()

    @Query("SELECT * FROM support_tickets ORDER BY updatedAt DESC")
    fun observeTickets(): Flow<List<SupportTicket>>

    @Query("SELECT * FROM support_tickets WHERE id = :id")
    fun observeTicket(id: String): Flow<SupportTicket?>

    @Query("SELECT * FROM support_tickets WHERE id = :id")
    suspend fun ticketOnce(id: String): SupportTicket?

    @Query("SELECT * FROM support_tickets ORDER BY updatedAt DESC")
    suspend fun ticketsOnce(): List<SupportTicket>

    @Query("SELECT * FROM help_articles ORDER BY title ASC")
    suspend fun helpOnce(): List<HelpArticle>

    @Query("SELECT * FROM announcements ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun announcementsOnce(): List<Announcement>

    @Upsert
    suspend fun upsertTicket(ticket: SupportTicket)

    @Upsert
    suspend fun upsertTickets(list: List<SupportTicket>)

    @Query("DELETE FROM support_tickets")
    suspend fun clearTickets()

    @Upsert
    suspend fun upsertAudit(list: List<AuditEntry>)

    @Query("SELECT * FROM audit_entries ORDER BY createdAt DESC LIMIT 200")
    fun observeAudit(): Flow<List<AuditEntry>>

    @Query("DELETE FROM audit_entries")
    suspend fun clearAudit()
}

@Dao
interface LearningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(event: ViewEvent)

    @Query(
        "SELECT category AS category, SUM(weight) AS weight FROM view_events " +
            "GROUP BY category ORDER BY weight DESC LIMIT :limit"
    )
    suspend fun topCategories(limit: Int): List<CategoryWeight>

    @Query(
        "SELECT category AS category, SUM(weight) AS weight FROM view_events " +
            "GROUP BY category ORDER BY weight DESC LIMIT :limit"
    )
    fun observeTopCategories(limit: Int = 6): Flow<List<CategoryWeight>>

    @Query("DELETE FROM view_events")
    suspend fun clear()
}

data class CategoryWeight(val category: String = "", val weight: Int = 0)

@Database(
    entities = [
        Product::class,
        SavedEntity::class,
        ChatRoom::class,
        Message::class,
        AppNotification::class,
        Payment::class,
        Announcement::class,
        HelpArticle::class,
        SupportTicket::class,
        AuditEntry::class,
        ViewEvent::class,
        AppConfig::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HafiYaweDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun saved(): SavedDao
    abstract fun chats(): ChatDao
    abstract fun notifications(): NotificationDao
    abstract fun payments(): PaymentDao
    abstract fun meta(): MetaDao
    abstract fun learning(): LearningDao

    companion object {
        const val NAME = "hafiyawe.db"
    }
}

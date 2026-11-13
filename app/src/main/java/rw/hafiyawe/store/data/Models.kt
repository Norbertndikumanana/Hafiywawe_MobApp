package rw.hafiyawe.store.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ---------------------------------------------------------------------------
// Shared vocabulary. These strings are the exact values the API accepts.
// ---------------------------------------------------------------------------

object Market {
    val CATEGORIES = listOf(
        "Phones", "Electronics", "Clothing", "Furniture", "Vehicles",
        "Appliances", "Books", "Sports", "Toys", "Other"
    )
    val CONDITIONS = listOf("Like New", "Good", "Fair")
    val PROVINCES = listOf("Kigali", "Northern", "Southern", "Eastern", "Western")
    val SORTS = listOf("newest", "price_asc", "price_desc")

    const val STATUS_PENDING = "pending"
    const val STATUS_ACTIVE = "active"
    const val STATUS_REJECTED = "rejected"
    const val STATUS_OFF_MARKET = "off_market"

    val DISTRICTS = mapOf(
        "Kigali" to listOf("Gasabo", "Kicukiro", "Nyarugenge"),
        "Northern" to listOf("Musanze", "Gicumbi", "Burera", "Gakenke", "Nyabihu"),
        "Southern" to listOf("Huye", "Muhanga", "Nyanza", "Ruhango", "Kamonyi", "Gisagara", "Nyamagabe", "Nyaruguru"),
        "Eastern" to listOf("Rwamagana", "Kayonza", "Kirehe", "Ngoma", "Bugesera", "Gatsibo", "Nyagatare"),
        "Western" to listOf("Rubavu", "Karongi", "Rusizi", "Nyamasheke", "Ngororero", "Rutsiro", "Nyabihu")
    )
}

// ---------------------------------------------------------------------------
// JSON models. Every model is also a Room entity unless noted, which keeps the
// offline cache and the API in one shape.
// ---------------------------------------------------------------------------

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",
    val emailVerified: Boolean = false,
    val identityVerified: Boolean = false,
    val phone: String = "",
    val province: String = "",
    val district: String = "",
    val nationalIdLast4: String = "",
    val freePostsRemaining: Int = 0,
    val postCredits: Int = 0,
    val planExpiresAt: String? = null,
    val isBanned: Boolean = false,
    val bannedAt: String? = null,
    val banReason: String = ""
)

val User.isAdmin: Boolean get() = role == "admin"

val User.hasCompleteProfile: Boolean
    get() = phone.isNotBlank() && province.isNotBlank() &&
        district.isNotBlank() && nationalIdLast4.isNotBlank()

val User.initials: String
    get() = name.trim().split(" ", limit = 2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

@Serializable
@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Long = 0L,
    val category: String = "Other",
    val condition: String = "Good",
    val province: String = "",
    val district: String = "",
    val images: List<String> = emptyList(),
    val status: String = Market.STATUS_PENDING,
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerVerified: Boolean = false,
    val createdAt: String = "",
    val isDemo: Boolean = false,
    val rejectionReason: String? = null,
    val available: Boolean = true
)

val Product.isActive: Boolean get() = status == Market.STATUS_ACTIVE

val Product.place: String
    get() = listOf(district, province).filter { it.isNotBlank() }.joinToString(", ")

val Product.cover: String? get() = images.firstOrNull()

@Serializable
@Entity(tableName = "chat_rooms")
data class ChatRoom(
    @PrimaryKey val id: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productImage: String? = null,
    val otherName: String = "",
    val side: String = "buying",
    val lastMessage: String? = null,
    val updatedAt: String = "",
    val unreadCount: Int = 0
)

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val body: String = "",
    val createdAt: String = ""
)

@Serializable
@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val href: String? = null,
    @SerialName("data") val data: JsonElement? = null,
    val readAt: String? = null,
    val createdAt: String = ""
)

val AppNotification.isUnread: Boolean get() = readAt == null

@Serializable
@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val kind: String = "single",
    val amount: Long = 0L,
    val reference: String = "",
    val status: String = "pending",
    val createdAt: String = "",
    val reviewedAt: String? = null,
    val proofId: String? = null,
    val proofUrl: String? = null,
    val rejectionReason: String? = null
)

@Serializable
@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val body: String = "",
    val kind: String = "info",
    val ctaLabel: String? = null,
    val href: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val enabled: Boolean = true,
    val sortOrder: Int = 100,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
@Entity(tableName = "help_articles")
data class HelpArticle(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "",
    val updatedAt: String = ""
)

@Serializable
data class SupportMessage(
    val id: String = "",
    val body: String = "",
    val authorRole: String = "user",
    val createdAt: String = ""
)

@Serializable
@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey val id: String = "",
    val userId: String? = null,
    val email: String = "",
    val subject: String? = null,
    val description: String = "",
    val status: String = "open",
    val createdAt: String = "",
    val updatedAt: String = "",
    val messages: List<SupportMessage> = emptyList()
)

@Serializable
@Entity(tableName = "audit_entries")
data class AuditEntry(
    @PrimaryKey val id: String = "",
    val actorId: String? = null,
    val action: String = "",
    val targetId: String? = null,
    val createdAt: String = "",
    val details: JsonElement? = null,
    val actorName: String? = null,
    val actorEmail: String? = null,
    val userName: String? = null,
    val userEmail: String? = null
)

/** A saved row: "favorite" or "cart". Product details live in the products table. */
@Entity(tableName = "saved", primaryKeys = ["kind", "productId"])
data class SavedEntity(
    val kind: String,
    val productId: String,
    val available: Boolean = true,
    val savedAt: Long = System.currentTimeMillis()
)

/** Result of joining a saved row with its cached product. */
data class SavedProduct(
    @Embedded val product: Product,
    @ColumnInfo(name = "savedAvailable") val available: Boolean = true,
    val savedAt: Long = 0L
)

/**
 * On-device learning signal. Every listing view, favourite and conversation is
 * recorded, and the home feed ranks categories the person actually looks at.
 * Nothing leaves the phone unless it is a normal API write.
 */
@Entity(tableName = "view_events", primaryKeys = ["id"])
data class ViewEvent(
    val id: String,
    val productId: String,
    val category: String,
    val weight: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val singlePostPrice: Long = 100,
    val monthlyPlanPrice: Long = 2000,
    val freePostLimit: Int = 2,
    val paymentInstructions: String = "",
    val paymentsConfigured: Boolean = false,
    val merchantCode: String = "",
    val merchantName: String = "",
    val emailDeliveryConfigured: Boolean = false,
    val development: Boolean = false,
    val showDemoData: Boolean = false,
    val supportEmail: String = "",
    val socialProviders: SocialProviders = SocialProviders()
)

@Serializable
data class SocialProviders(
    val google: Boolean = false,
    val apple: Boolean = false
)

// ---------------------------------------------------------------------------
// Request / response envelopes.
// ---------------------------------------------------------------------------

@Serializable data class MeResponse(val user: User? = null)
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class LoginResponse(val user: User = User())
@Serializable data class RegisterRequest(val name: String, val email: String, val password: String)
@Serializable data class RegisterResponse(
    val user: User = User(),
    val verificationDelivery: String = "not_configured",
    val retryAfterSeconds: Int = 60,
    val verificationUrl: String? = null
)
@Serializable data class VerifyEmailRequest(val token: String)
@Serializable data class ResendResponse(
    val ok: Boolean = true,
    val alreadyVerified: Boolean = false,
    val verificationDelivery: String = "not_configured",
    val retryAfterSeconds: Int = 60,
    val verificationUrl: String? = null
)
@Serializable data class ForgotRequest(val email: String)
@Serializable data class ForgotResponse(val ok: Boolean = true, val resetUrl: String? = null)
@Serializable data class ResetRequest(val token: String, val password: String)
@Serializable data class OkResponse(val ok: Boolean = true)
@Serializable data class IdResponse(val id: String = "")

@Serializable data class ProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val nationalId: String? = null,
    val province: String? = null,
    val district: String? = null
)

@Serializable data class ProductRequest(
    val title: String,
    val description: String,
    val price: Long,
    val category: String,
    val condition: String,
    val province: String,
    val district: String,
    val images: List<String> = emptyList()
)

@Serializable data class ProductStatusRequest(val status: String, val rejectionReason: String? = null)
@Serializable data class UploadsResponse(val urls: List<String> = emptyList())
@Serializable data class ProofResponse(val id: String = "", val url: String = "")
@Serializable data class ChatRequest(val productId: String)
@Serializable data class MessageRequest(val body: String)
@Serializable data class PaymentRequest(val kind: String, val reference: String, val proofId: String)
@Serializable data class NotificationsPage(
    val items: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val nextCursor: String? = null
)
@Serializable data class UnreadCount(val count: Int = 0)
@Serializable data class UpdateReadRequest(val read: Boolean = true)
@Serializable data class AuditSearchPage(
    val items: List<AuditEntry> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 50
)
@Serializable data class AdminSupportPage(val items: List<SupportTicket> = emptyList())
@Serializable data class AdminStats(
    val pendingProducts: Int = 0,
    val pendingPayments: Int = 0,
    val users: Int = 0,
    val activeProducts: Int = 0
)
@Serializable data class TicketRequest(val email: String, val subject: String? = null, val description: String)
@Serializable data class TicketResponse(val ticket: SupportTicket = SupportTicket(), val accessToken: String? = null)
@Serializable data class ReplyRequest(val body: String)
@Serializable data class IdentityRequest(val identityVerified: Boolean)
@Serializable data class AccessRequest(val banned: Boolean, val reason: String? = null)
@Serializable data class AnnouncementRequest(
    val title: String,
    val body: String,
    val kind: String? = null,
    val ctaLabel: String? = null,
    val href: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val enabled: Boolean? = null,
    val sortOrder: Int? = null
)
@Serializable data class HelpRequest(val title: String, val body: String, val category: String)
@Serializable data class HealthResponse(val ok: Boolean = false, val database: String = "")

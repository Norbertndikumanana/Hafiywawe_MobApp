package rw.hafiyawe.store.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.core.SessionStore
import rw.hafiyawe.store.data.AccessRequest
import rw.hafiyawe.store.data.AdminStats
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.AnnouncementRequest
import rw.hafiyawe.store.data.AppNotification
import rw.hafiyawe.store.data.AuditSearchPage
import rw.hafiyawe.store.data.ChatRequest
import rw.hafiyawe.store.data.ChatRoom
import rw.hafiyawe.store.data.HelpArticle
import rw.hafiyawe.store.data.HelpRequest
import rw.hafiyawe.store.data.IdentityRequest
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.Message
import rw.hafiyawe.store.data.MessageRequest
import rw.hafiyawe.store.data.Payment
import rw.hafiyawe.store.data.PaymentRequest
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.ProductStatusRequest
import rw.hafiyawe.store.data.ReplyRequest
import rw.hafiyawe.store.data.SupportTicket
import rw.hafiyawe.store.data.TicketRequest
import rw.hafiyawe.store.data.TicketResponse
import rw.hafiyawe.store.data.User
import rw.hafiyawe.store.data.cover
import rw.hafiyawe.store.data.local.DemoSeed
import rw.hafiyawe.store.data.local.HafiYaweDatabase
import rw.hafiyawe.store.data.remote.ApiException
import rw.hafiyawe.store.data.remote.ApiService
import rw.hafiyawe.store.data.remote.apiCall
import java.util.UUID

// ---------------------------------------------------------------------------
// Conversations
// ---------------------------------------------------------------------------

class ChatRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase
) {
    val rooms: Flow<List<ChatRoom>> = db.chats().observeRooms()
    val unread: Flow<Int> = rooms.map { list -> list.sumOf { it.unreadCount } }

    fun messages(roomId: String): Flow<List<Message>> = db.chats().observeMessages(roomId)
    fun room(roomId: String): Flow<ChatRoom?> = db.chats().observeRoom(roomId)

    suspend fun refreshRooms() {
        if (session.demoMode.value || session.user.value == null) return
        val list = apiCall { api.chats() }
        db.chats().replaceRooms(list)
    }

    suspend fun loadMessages(roomId: String, before: String? = null) {
        if (session.demoMode.value) {
            db.chats().room(roomId)?.let { db.chats().upsertRoom(it.copy(unreadCount = 0)) }
            return
        }
        val list = apiCall { api.messages(roomId, before) }
        db.chats().upsertMessages(list)
        db.chats().room(roomId)?.let { db.chats().upsertRoom(it.copy(unreadCount = 0)) }
    }

    suspend fun send(roomId: String, body: String): Message {
        val text = body.trim()
        if (text.isEmpty()) throw ApiException(400, "Write a message first.")
        if (session.demoMode.value) {
            val me = session.user.value ?: throw ApiException(401, "Please sign in to send a message.")
            val message = Message(
                id = "local-${UUID.randomUUID()}",
                roomId = roomId,
                senderId = me.id,
                body = text,
                createdAt = Iso.now()
            )
            db.chats().upsertMessage(message)
            db.chats().room(roomId)?.let {
                db.chats().upsertRoom(it.copy(lastMessage = text, updatedAt = message.createdAt))
            }
            return message
        }
        val message = apiCall { api.sendMessage(roomId, MessageRequest(text)) }
        db.chats().upsertMessage(message)
        db.chats().room(roomId)?.let {
            db.chats().upsertRoom(it.copy(lastMessage = text, updatedAt = message.createdAt))
        }
        return message
    }

    /** Opens (or reuses) the conversation about one listing and returns its room id. */
    suspend fun openFor(product: Product): String {
        val me = session.user.value ?: throw ApiException(401, "Please sign in to contact the seller.")
        if (session.demoMode.value) {
            val existing = db.chats().roomsOnce().firstOrNull { it.productId == product.id }
            if (existing != null) return existing.id
            val room = ChatRoom(
                id = "local-room-${UUID.randomUUID()}",
                productId = product.id,
                productTitle = product.title,
                productImage = product.cover,
                otherName = product.sellerName.ifBlank { "Seller" },
                side = if (product.sellerId == me.id) "selling" else "buying",
                lastMessage = null,
                updatedAt = Iso.now()
            )
            db.chats().upsertRoom(room)
            return room.id
        }
        val response = apiCall { api.openChat(ChatRequest(product.id)) }
        refreshRooms()
        return response.id
    }
}

// ---------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------

class NotificationsRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase
) {
    val items: Flow<List<AppNotification>> = db.notifications().observeAll()
    val unread: Flow<Int> = db.notifications().observeUnread()

    suspend fun refresh() {
        if (session.demoMode.value || session.user.value == null) return
        var cursor: String? = null
        var page = 0
        do {
            val result = apiCall { api.notifications(cursor) }
            db.notifications().upsert(result.items)
            cursor = result.nextCursor
            page++
        } while (cursor != null && page < 5)
    }

    suspend fun markRead(id: String) {
        if (!session.demoMode.value) apiCall { api.markNotificationRead(id) }
        db.notifications().markRead(id, Iso.now())
    }

    suspend fun markAllRead() {
        if (!session.demoMode.value) apiCall { api.markAllNotificationsRead() }
        db.notifications().markAllRead(Iso.now())
    }
}

// ---------------------------------------------------------------------------
// Posting payments
// ---------------------------------------------------------------------------

class PaymentsRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase,
    private val catalog: CatalogRepository
) {
    val payments: Flow<List<Payment>> = db.payments().observeAll()

    suspend fun refresh() {
        if (session.demoMode.value || session.user.value == null) return
        val list = apiCall { api.payments() }
        db.payments().clear()
        db.payments().upsertAll(list)
    }

    suspend fun submit(kind: String, reference: String, proofUri: android.net.Uri?): Payment {
        val cleanReference = reference.trim().uppercase().replace(" ", "")
        if (cleanReference.length < 6) {
            throw ApiException(400, "Enter the mobile-money transaction reference (at least 6 characters).")
        }
        if (proofUri == null) {
            throw ApiException(400, "Attach a photo of the payment receipt.")
        }
        val me = session.user.value ?: throw ApiException(401, "Please sign in to submit a payment.")

        if (session.demoMode.value) {
            val amount = if (kind == "monthly") 2000L else 100L
            val payment = Payment(
                id = "local-payment-${UUID.randomUUID()}",
                userId = me.id,
                userName = me.name,
                kind = kind,
                amount = amount,
                reference = cleanReference,
                status = "pending",
                createdAt = Iso.now(),
                proofUrl = proofUri.toString()
            )
            db.payments().upsert(payment)
            return payment
        }

        val proof = catalog.uploadProof(proofUri)
        val payment = apiCall {
            api.submitPayment(PaymentRequest(kind, cleanReference, proof.id))
        }
        db.payments().upsert(payment)
        return payment
    }
}

// ---------------------------------------------------------------------------
// Support
// ---------------------------------------------------------------------------

class SupportRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase
) {
    val tickets: Flow<List<SupportTicket>> = db.meta().observeTickets()

    fun ticket(id: String): Flow<SupportTicket?> = db.meta().observeTicket(id)

    suspend fun refresh() {
        if (session.demoMode.value) return
        if (session.user.value == null) return
        val list = apiCall { api.tickets() }
        db.meta().clearTickets()
        db.meta().upsertTickets(list)
    }

    suspend fun refreshTicket(id: String): SupportTicket? {
        if (session.demoMode.value) return null
        val token = session.supportToken(id)
        val fresh = apiCall { api.ticket(id, token) }
        db.meta().upsertTicket(fresh)
        return fresh
    }

    suspend fun create(email: String, subject: String?, description: String): TicketResponse {
        val trimmed = description.trim()
        if (trimmed.length < 10) {
            throw ApiException(400, "Describe the problem in at least 10 characters.")
        }
        if (session.demoMode.value) {
            val ticket = SupportTicket(
                id = "local-ticket-${UUID.randomUUID()}",
                userId = session.user.value?.id,
                email = email.ifBlank { session.user.value?.email ?: "demo@hafiyawe.local" },
                subject = subject?.takeIf { it.isNotBlank() },
                description = trimmed,
                status = "open",
                createdAt = Iso.now(),
                updatedAt = Iso.now()
            )
            db.meta().upsertTicket(ticket)
            return TicketResponse(ticket = ticket)
        }
        val result = apiCall { api.createTicket(TicketRequest(email, subject, trimmed)) }
        result.accessToken?.let { session.saveSupportToken(result.ticket.id, it) }
        db.meta().upsertTicket(result.ticket)
        return result
    }

    suspend fun reply(ticketId: String, body: String): SupportTicket {
        val text = body.trim()
        if (text.isEmpty()) throw ApiException(400, "Write a reply first.")
        if (session.demoMode.value) {
            val ticket = db.meta().ticketOnce(ticketId)
                ?: throw ApiException(404, "That request no longer exists.")
            val updated = ticket.copy(
                status = "open",
                updatedAt = Iso.now(),
                messages = ticket.messages + rw.hafiyawe.store.data.SupportMessage(
                    id = "local-${UUID.randomUUID()}",
                    body = text,
                    authorRole = "user",
                    createdAt = Iso.now()
                )
            )
            db.meta().upsertTicket(updated)
            return updated
        }
        val token = session.supportToken(ticketId)
        val updated = apiCall { api.replyTicket(ticketId, ReplyRequest(text), token) }
        db.meta().upsertTicket(updated)
        return updated
    }
}

// ---------------------------------------------------------------------------
// Administrator workspace
// ---------------------------------------------------------------------------

class AdminRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase
) {
    private val demo: Boolean get() = session.demoMode.value

    suspend fun stats(): AdminStats {
        if (demo) {
            val all = db.products().all()
            return AdminStats(
                pendingProducts = all.count { it.status == Market.STATUS_PENDING },
                pendingPayments = db.payments().allOnce().count { it.status == "pending" },
                users = 3,
                activeProducts = all.count { it.status == Market.STATUS_ACTIVE }
            )
        }
        return apiCall { api.adminStats() }
    }

    suspend fun products(status: String): List<Product> {
        if (demo) {
            val all = db.products().all()
            return if (status == "all") all else all.filter { it.status == status }
        }
        return apiCall { api.adminProducts(status) }.also { db.products().upsertAll(it) }
    }

    suspend fun decideProduct(id: String, status: String, reason: String?) {
        if (demo) {
            val current = db.products().byId(id) ?: return
            db.products().upsert(current.copy(status = status, rejectionReason = reason))
            return
        }
        val updated = apiCall { api.decideProduct(id, ProductStatusRequest(status, reason)) }
        db.products().upsert(updated)
    }

    suspend fun payments(status: String): List<Payment> {
        if (demo) {
            val all = db.payments().allOnce()
            return if (status == "all") all else all.filter { it.status == status }
        }
        return apiCall { api.adminPayments(status) }.also { db.payments().upsertAll(it) }
    }

    suspend fun decidePayment(id: String, status: String, reason: String?): Payment {
        if (demo) {
            val all = db.payments().allOnce()
            val current = all.firstOrNull { it.id == id } ?: throw ApiException(404, "That payment is gone.")
            val updated = current.copy(
                status = status,
                reviewedAt = Iso.now(),
                rejectionReason = reason
            )
            db.payments().upsert(updated)
            if (status == "approved") {
                val me = session.user.value
                if (me != null && me.id == current.userId) {
                    session.setUser(
                        if (current.kind == "monthly") me.copy(
                            planExpiresAt = Iso.daysAhead(30)
                        ) else me.copy(postCredits = me.postCredits + 1)
                    )
                }
            }
            return updated
        }
        val updated = apiCall { api.decidePayment(id, ProductStatusRequest(status, reason)) }
        db.payments().upsert(updated)
        runCatching { session.user.value?.let { /* balances changed server-side */ } }
        return updated
    }

    suspend fun users(): List<User> {
        if (demo) {
            return listOf(
                DemoSeed.demoUser.copy(name = "Demo Admin (you)"),
                User(
                    id = DemoSeed.DEMO_SELLER_ID, name = "Aline Demo",
                    email = "seller@hafiyawe.test", role = "user",
                    emailVerified = true, identityVerified = true,
                    phone = "+250788111222", province = "Kigali", district = "Gasabo",
                    nationalIdLast4 = "9931", freePostsRemaining = 0, postCredits = 1
                ),
                User(
                    id = "demo-buyer", name = "Eric Demo",
                    email = "buyer@hafiyawe.test", role = "user",
                    emailVerified = true, identityVerified = false,
                    phone = "+250722333444", province = "Kigali", district = "Kicukiro",
                    nationalIdLast4 = "1177", isBanned = true,
                    bannedAt = Iso.hoursAgo(30),
                    banReason = "Repeatedly asked buyers to pay before inspection."
                )
            )
        }
        return apiCall { api.adminUsers() }
    }

    suspend fun verifyIdentity(id: String, verified: Boolean): User {
        if (demo) return demoUserFromDb(id).copy(identityVerified = verified)
        return apiCall { api.verifyIdentity(id, IdentityRequest(verified)) }
    }

    suspend fun setAccess(id: String, banned: Boolean, reason: String?): User {
        if (demo) {
            return demoUserFromDb(id).copy(
                isBanned = banned,
                bannedAt = if (banned) Iso.now() else null,
                banReason = if (banned) reason.orEmpty() else ""
            )
        }
        return apiCall { api.setAccess(id, AccessRequest(banned, reason)) }
    }

    private fun demoUserFromDb(id: String): User =
        if (id == DemoSeed.DEMO_SELLER_ID) {
            User(
                id = id, name = "Aline Demo", email = "seller@hafiyawe.test",
                emailVerified = true, identityVerified = true, phone = "+250788111222",
                province = "Kigali", district = "Gasabo", nationalIdLast4 = "9931"
            )
        } else {
            session.user.value ?: DemoSeed.demoUser
        }

    suspend fun support(status: String, query: String): List<SupportTicket> {
        if (demo) {
            val all = db.meta().ticketsOnce()
            return all.filter {
                (status == "all" || it.status == status) &&
                    (query.isBlank() || it.subject.orEmpty().contains(query, true) ||
                        it.email.contains(query, true) || it.description.contains(query, true))
            }
        }
        return apiCall { api.adminSupport(status, query.ifBlank { null }) }.items
    }

    suspend fun replySupport(id: String, body: String): SupportTicket {
        if (demo) {
            val ticket = db.meta().ticketOnce(id)
                ?: throw ApiException(404, "That request is gone.")
            val updated = ticket.copy(
                updatedAt = Iso.now(),
                messages = ticket.messages + rw.hafiyawe.store.data.SupportMessage(
                    id = "local-${UUID.randomUUID()}",
                    body = body,
                    authorRole = "admin",
                    createdAt = Iso.now()
                )
            )
            db.meta().upsertTicket(updated)
            return updated
        }
        val updated = apiCall { api.adminReply(id, ReplyRequest(body)) }
        db.meta().upsertTicket(updated)
        return updated
    }

    suspend fun setSupportStatus(id: String, status: String): SupportTicket {
        if (demo) {
            val ticket = db.meta().ticketOnce(id)
                ?: throw ApiException(404, "That request is gone.")
            val updated = ticket.copy(status = status, updatedAt = Iso.now())
            db.meta().upsertTicket(updated)
            return updated
        }
        val updated = apiCall { api.setSupportStatus(id, ProductStatusRequest(status)) }
        db.meta().upsertTicket(updated)
        return updated
    }

    suspend fun auditSearch(
        query: String,
        action: String,
        from: String,
        to: String,
        page: Int = 1
    ): AuditSearchPage {
        if (demo) {
            return AuditSearchPage(items = emptyList(), total = 0, page = page, pageSize = 50)
        }
        val result = apiCall {
            api.auditSearch(
                q = query.ifBlank { null },
                action = action.ifBlank { null },
                from = from.ifBlank { null },
                to = to.ifBlank { null },
                page = page,
                pageSize = 50
            )
        }
        db.meta().upsertAudit(result.items)
        return result
    }

    suspend fun announcements(): List<Announcement> {
        if (demo) return db.meta().announcementsOnce()
        return apiCall { api.adminAnnouncements() }.also {
            db.meta().clearAnnouncements()
            db.meta().upsertAnnouncements(it)
        }
    }

    suspend fun saveAnnouncement(existing: Announcement?, body: AnnouncementRequest): Announcement {
        if (demo) {
            val now = Iso.now()
            val saved = Announcement(
                id = existing?.id ?: "local-announcement-${UUID.randomUUID()}",
                title = body.title,
                body = body.body,
                kind = body.kind ?: "info",
                ctaLabel = body.ctaLabel,
                href = body.href,
                startsAt = body.startsAt,
                endsAt = body.endsAt,
                enabled = body.enabled ?: true,
                sortOrder = body.sortOrder ?: 100,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            db.meta().upsertAnnouncements(listOf(saved))
            return saved
        }
        val saved = if (existing == null) apiCall { api.createAnnouncement(body) }
        else apiCall { api.updateAnnouncement(existing.id, body) }
        db.meta().upsertAnnouncements(listOf(saved))
        return saved
    }

    suspend fun deleteAnnouncement(id: String) {
        if (!demo) apiCall { api.deleteAnnouncement(id) }
        db.meta().clearAnnouncements()
    }

    suspend fun helpArticles(): List<HelpArticle> {
        if (demo) return db.meta().helpOnce()
        return apiCall { api.help() }
    }

    suspend fun saveHelp(existing: HelpArticle?, body: HelpRequest): HelpArticle {
        if (demo) {
            val saved = HelpArticle(
                id = existing?.id ?: "local-help-${UUID.randomUUID()}",
                title = body.title,
                body = body.body,
                category = body.category,
                updatedAt = Iso.now()
            )
            db.meta().upsertHelp(listOf(saved))
            return saved
        }
        val saved = if (existing == null) apiCall { api.createHelp(body) }
        else apiCall { api.updateHelp(existing.id, body) }
        db.meta().upsertHelp(listOf(saved))
        return saved
    }

    suspend fun deleteHelp(id: String) {
        if (!demo) apiCall { api.deleteHelp(id) }
    }
}

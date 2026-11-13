package rw.hafiyawe.store.data.repo

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.core.SessionStore
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.AppConfig
import rw.hafiyawe.store.data.AppNotification
import rw.hafiyawe.store.data.ForgotResponse
import rw.hafiyawe.store.data.HelpArticle
import rw.hafiyawe.store.data.LoginRequest
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.ProductRequest
import rw.hafiyawe.store.data.ProductStatusRequest
import rw.hafiyawe.store.data.ProfileRequest
import rw.hafiyawe.store.data.RegisterRequest
import rw.hafiyawe.store.data.RegisterResponse
import rw.hafiyawe.store.data.ResendResponse
import rw.hafiyawe.store.data.ResetRequest
import rw.hafiyawe.store.data.SavedEntity
import rw.hafiyawe.store.data.SavedProduct
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.User
import rw.hafiyawe.store.data.ViewEvent
import rw.hafiyawe.store.data.local.CategoryWeight
import rw.hafiyawe.store.data.local.DemoSeed
import rw.hafiyawe.store.data.local.HafiYaweDatabase
import rw.hafiyawe.store.data.remote.ApiException
import rw.hafiyawe.store.data.remote.ApiService
import rw.hafiyawe.store.data.remote.PersistentCookieJar
import rw.hafiyawe.store.data.remote.apiCall
import java.io.IOException
import java.util.UUID

// ---------------------------------------------------------------------------
// Account
// ---------------------------------------------------------------------------

class AuthRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase,
    private val cookies: PersistentCookieJar
) {
    val user: StateFlow<User?> = session.user
    val demoMode: StateFlow<Boolean> = session.demoMode

    suspend fun signIn(email: String, password: String): User {
        if (session.demoMode.value) return enterDemo()
        val response = apiCall { api.login(LoginRequest(email.trim(), password)) }
        session.lastEmail = email.trim()
        session.setUser(response.user)
        return response.user
    }

    suspend fun signUp(name: String, email: String, password: String): RegisterResponse {
        val response = apiCall {
            api.register(RegisterRequest(name.trim(), email.trim(), password))
        }
        session.lastEmail = email.trim()
        session.setUser(response.user)
        return response
    }

    suspend fun refreshMe(): User? {
        if (session.demoMode.value) return session.user.value ?: enterDemo()
        val response = apiCall { api.me() }
        session.setUser(response.user)
        return response.user
    }

    suspend fun signOut() {
        if (!session.demoMode.value) {
            runCatching { apiCall { api.logout() } }
        }
        cookies.clear()
        session.setUser(null)
        wipeLocalAccount()
    }

    suspend fun verifyEmail(token: String): User {
        val response = apiCall { api.verifyEmail(rw.hafiyawe.store.data.VerifyEmailRequest(token)) }
        session.setUser(response.user)
        return response.user
    }

    suspend fun resendVerification(): ResendResponse = apiCall { api.resendVerification() }

    suspend fun forgotPassword(email: String): ForgotResponse =
        apiCall { api.forgotPassword(rw.hafiyawe.store.data.ForgotRequest(email.trim())) }

    suspend fun resetPassword(token: String, password: String) {
        apiCall { api.resetPassword(ResetRequest(token, password)) }
        cookies.clear()
        session.setUser(null)
    }

    suspend fun updateProfile(
        name: String? = null,
        phone: String? = null,
        nationalId: String? = null,
        province: String? = null,
        district: String? = null
    ): User {
        val current = session.user.value
        if (session.demoMode.value) {
            val updated = (current ?: DemoSeed.demoUser).copy(
                name = name ?: current?.name ?: "",
                phone = phone ?: current?.phone ?: "",
                province = province ?: current?.province ?: "",
                district = district ?: current?.district ?: "",
                nationalIdLast4 = nationalId?.takeLast(4) ?: current?.nationalIdLast4 ?: ""
            )
            session.setUser(updated)
            return updated
        }
        val response = apiCall {
            api.updateProfile(ProfileRequest(name, phone, nationalId, province, district))
        }
        session.setUser(response.user)
        return response.user
    }

    suspend fun enterDemo(): User {
        session.setDemoMode(true)
        DemoSeed.ensure(db)
        session.setUser(DemoSeed.demoUser)
        return DemoSeed.demoUser
    }

    suspend fun leaveDemo() {
        session.setDemoMode(false)
        session.setUser(null)
        wipeLocalAccount()
    }

    private suspend fun wipeLocalAccount() {
        db.chats().clearRooms()
        db.chats().clearMessages()
        db.notifications().clear()
        db.payments().clear()
        db.meta().clearTickets()
        db.saved().clearKind(KIND_FAVORITE)
        db.saved().clearKind(KIND_CART)
    }

    companion object {
        const val KIND_FAVORITE = "favorite"
        const val KIND_CART = "cart"
    }
}

// ---------------------------------------------------------------------------
// Config, help and announcements
// ---------------------------------------------------------------------------

class MetaRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase
) {
    val config: Flow<AppConfig?> = db.meta().observeConfig()
    val help: Flow<List<HelpArticle>> = db.meta().observeHelp()
    val announcements: Flow<List<Announcement>> = db.meta().observeAnnouncements()
    val audit: Flow<List<rw.hafiyawe.store.data.AuditEntry>> = db.meta().observeAudit()

    suspend fun refreshConfig() {
        if (session.demoMode.value) return
        val value = apiCall { api.config() }
        db.meta().upsertConfig(value.copy(id = 1))
    }

    suspend fun refreshHelp() {
        if (session.demoMode.value) return
        val list = apiCall { api.help() }
        db.meta().clearHelp()
        db.meta().upsertHelp(list)
    }

    suspend fun refreshAnnouncements() {
        if (session.demoMode.value) return
        val list = apiCall { api.announcements() }
        db.meta().clearAnnouncements()
        db.meta().upsertAnnouncements(list)
    }

    suspend fun refreshAll() {
        if (session.demoMode.value) {
            DemoSeed.ensure(db)
            return
        }
        runCatching { refreshConfig() }
        runCatching { refreshHelp() }
        runCatching { refreshAnnouncements() }
    }

    suspend fun configOnce(): AppConfig = db.meta().config() ?: AppConfig()
}

// ---------------------------------------------------------------------------
// Listings and saved collections
// ---------------------------------------------------------------------------

data class ProductFilters(
    val query: String = "",
    val category: String? = null,
    val condition: String? = null,
    val province: String? = null,
    val sort: String = "newest"
) {
    val isActive: Boolean
        get() = query.isNotBlank() || category != null || condition != null ||
            province != null || sort != "newest"
}

class CatalogRepository(
    private val api: ApiService,
    private val session: SessionStore,
    private val db: HafiYaweDatabase,
    private val resolver: ContentResolver
) {
    /**
     * Everything cached and currently public. Marked demo rows are only shown when the
     * device is in demo mode or the server itself opted in with showDemoData.
     */
    val products: Flow<List<Product>> = combine(
        db.products().observeActive(true),
        session.demoMode,
        db.meta().observeConfig()
    ) { list, demo, config ->
        if (demo || config?.showDemoData == true) list else list.filter { !it.isDemo }
    }

    /**
     * The on-device learning part: listings in categories this person opens most
     * float to the top of the home feed, newest first inside each category.
     */
    val learnedFeed: Flow<List<Product>> = combine(
        products,
        db.learning().observeTopCategories(6)
    ) { list, weights -> rank(list, weights) }

    val myListings: Flow<List<Product>> = combine(
        session.user,
        db.products().observeAll()
    ) { user, all ->
        if (user == null) emptyList() else all.filter { it.sellerId == user.id }
    }

    fun browse(filters: ProductFilters): Flow<List<Product>> =
        products.map { applyFilters(it, filters) }

    fun observeProduct(id: String): Flow<Product?> = db.products().observeById(id)

    fun saved(kind: String): Flow<List<SavedProduct>> = db.saved().observe(kind)

    fun isSaved(kind: String, productId: String): Flow<Boolean> =
        db.saved().observeIsSaved(kind, productId).map { it > 0 }

    suspend fun refresh() {
        if (session.demoMode.value) return
        val list = apiCall { api.products() }
        db.products().replaceActive(list)
    }

    suspend fun refreshMine() {
        if (session.demoMode.value || session.user.value == null) return
        val list = apiCall { api.myProducts() }
        db.products().upsertAll(list)
    }

    suspend fun refreshSaved(kind: String) {
        if (session.demoMode.value || session.user.value == null) return
        val list = apiCall {
            if (kind == AuthRepository.KIND_FAVORITE) api.favorites() else api.cart()
        }
        list.forEach { db.products().upsert(it) }
        db.saved().clearKind(kind)
        list.forEach {
            db.saved().save(
                SavedEntity(kind = kind, productId = it.id, available = it.available)
            )
        }
    }

    /** Loads one listing, preferring the cache and refreshing it when possible. */
    suspend fun product(id: String, force: Boolean = false): Product? {
        val cached = db.products().byId(id)
        if (session.demoMode.value) return cached
        if (cached != null && !force) {
            runCatching {
                apiCall { api.product(id) }.let { db.products().upsert(it) }
            }
            return db.products().byId(id) ?: cached
        }
        val fresh = runCatching { apiCall { api.product(id) } }.getOrNull() ?: return cached
        db.products().upsert(fresh)
        return fresh
    }

    suspend fun createListing(request: ProductRequest): Product {
        val user = session.user.value
            ?: throw ApiException(401, "Please sign in to post a listing.")

        if (session.demoMode.value) {
            val planActive = Iso.daysLeft(user.planExpiresAt) > 0
            if (!planActive && user.freePostsRemaining <= 0 && user.postCredits <= 0) {
                throw ApiException(
                    402,
                    "Your two free submissions have been used. Add a posting credit or activate a monthly plan."
                )
            }
            val listing = Product(
                id = "local-${UUID.randomUUID()}",
                title = request.title,
                description = request.description,
                price = request.price,
                category = request.category,
                condition = request.condition,
                province = request.province,
                district = request.district,
                images = request.images,
                status = Market.STATUS_PENDING,
                sellerId = user.id,
                sellerName = user.name,
                sellerVerified = user.identityVerified,
                createdAt = Iso.now(),
                isDemo = true
            )
            db.products().upsert(listing)
            session.setUser(
                if (planActive) user
                else if (user.freePostsRemaining > 0) user.copy(freePostsRemaining = user.freePostsRemaining - 1)
                else user.copy(postCredits = user.postCredits - 1)
            )
            db.notifications().upsert(
                listOf(
                    AppNotification(
                        id = "local-n-${listing.id}",
                        type = "listing_approved",
                        title = "Listing submitted for review",
                        body = "${listing.title} is waiting for an administrator to review it.",
                        href = "/my-listings",
                        createdAt = Iso.now()
                    )
                )
            )
            return listing
        }

        val listing = apiCall { api.createProduct(request) }
        db.products().upsert(listing)
        return listing
    }

    suspend fun setStatus(id: String, status: String) {
        if (session.demoMode.value) {
            val current = db.products().byId(id) ?: return
            db.products().upsert(current.copy(status = status))
            return
        }
        val updated = apiCall { api.updateProductStatus(id, ProductStatusRequest(status)) }
        db.products().upsert(updated)
    }

    suspend fun deleteListing(id: String) {
        if (!session.demoMode.value) {
            apiCall { api.deleteProduct(id) }
        }
        db.products().delete(id)
        db.saved().remove(AuthRepository.KIND_FAVORITE, id)
        db.saved().remove(AuthRepository.KIND_CART, id)
    }

    suspend fun toggleSaved(kind: String, product: Product) {
        val alreadySaved = db.saved().rows(kind).any { it.productId == product.id }
        val canSync = !session.demoMode.value && session.user.value != null

        if (canSync) {
            apiCall {
                if (alreadySaved) {
                    if (kind == AuthRepository.KIND_FAVORITE) api.removeFavorite(product.id)
                    else api.removeFromCart(product.id)
                } else {
                    if (kind == AuthRepository.KIND_FAVORITE) api.addFavorite(product.id)
                    else api.addToCart(product.id)
                }
            }
        }

        if (alreadySaved) {
            db.saved().remove(kind, product.id)
        } else {
            db.products().upsert(product)
            db.saved().save(SavedEntity(kind = kind, productId = product.id))
            recordView(product, weight = 2)
        }
    }

    /** Records what the person looks at; used only on this device for the home feed. */
    suspend fun recordView(product: Product, weight: Int = 1) {
        db.learning().record(
            ViewEvent(
                id = UUID.randomUUID().toString(),
                productId = product.id,
                category = product.category,
                weight = weight
            )
        )
    }

    suspend fun uploadImages(uris: List<Uri>): List<String> {
        if (uris.isEmpty()) return emptyList()
        if (session.demoMode.value) {
            // Photos stay on the device in demo mode — nothing is uploaded.
            return uris.map { it.toString() }
        }
        val parts = uris.map { partFor(it, "images") }
        return apiCall { api.uploadImages(parts) }.urls
    }

    suspend fun uploadProof(uri: Uri): rw.hafiyawe.store.data.ProofResponse {
        val part = partFor(uri, "proof")
        return apiCall { api.uploadProof(part) }
    }

    private fun partFor(uri: Uri, field: String): MultipartBody.Part {
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: IOException) {
            null
        } ?: throw ApiException(-1, "That file could not be read. Please pick it again.")

        if (bytes.size > 5 * 1024 * 1024) {
            throw ApiException(413, "Each photo must be 5 MB or smaller.")
        }
        val extension = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/avif" -> "avif"
            else -> "jpg"
        }
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(field, "upload.$extension", body)
    }

    private fun rank(list: List<Product>, weights: List<CategoryWeight>): List<Product> {
        if (weights.isEmpty()) return list
        val scores = weights.associate { it.category to it.weight }
        return list.sortedWith(
            compareByDescending<Product> { scores[it.category] ?: 0 }
                .thenByDescending { it.createdAt }
        )
    }

    private fun applyFilters(list: List<Product>, filters: ProductFilters): List<Product> {
        val query = filters.query.trim().lowercase()
        val filtered = list.filter { product ->
            (query.isBlank() ||
                product.title.lowercase().contains(query) ||
                product.description.lowercase().contains(query) ||
                product.category.lowercase().contains(query)) &&
                (filters.category == null || product.category == filters.category) &&
                (filters.condition == null || product.condition == filters.condition) &&
                (filters.province == null || product.province == filters.province)
        }
        return when (filters.sort) {
            "price_asc" -> filtered.sortedBy { it.price }
            "price_desc" -> filtered.sortedByDescending { it.price }
            else -> filtered.sortedByDescending { it.createdAt }
        }
    }
}

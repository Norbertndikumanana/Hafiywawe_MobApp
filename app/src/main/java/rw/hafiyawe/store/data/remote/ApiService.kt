package rw.hafiyawe.store.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import rw.hafiyawe.store.data.AccessRequest
import rw.hafiyawe.store.data.AdminStats
import rw.hafiyawe.store.data.AdminSupportPage
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.AnnouncementRequest
import rw.hafiyawe.store.data.AppConfig
import rw.hafiyawe.store.data.AppNotification
import rw.hafiyawe.store.data.AuditEntry
import rw.hafiyawe.store.data.AuditSearchPage
import rw.hafiyawe.store.data.ChatRequest
import rw.hafiyawe.store.data.ChatRoom
import rw.hafiyawe.store.data.ForgotRequest
import rw.hafiyawe.store.data.ForgotResponse
import rw.hafiyawe.store.data.HelpArticle
import rw.hafiyawe.store.data.HelpRequest
import rw.hafiyawe.store.data.IdResponse
import rw.hafiyawe.store.data.IdentityRequest
import rw.hafiyawe.store.data.LoginRequest
import rw.hafiyawe.store.data.LoginResponse
import rw.hafiyawe.store.data.MeResponse
import rw.hafiyawe.store.data.Message
import rw.hafiyawe.store.data.MessageRequest
import rw.hafiyawe.store.data.NotificationsPage
import rw.hafiyawe.store.data.OkResponse
import rw.hafiyawe.store.data.Payment
import rw.hafiyawe.store.data.PaymentRequest
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.ProductRequest
import rw.hafiyawe.store.data.ProductStatusRequest
import rw.hafiyawe.store.data.ProfileRequest
import rw.hafiyawe.store.data.ProofResponse
import rw.hafiyawe.store.data.RegisterRequest
import rw.hafiyawe.store.data.RegisterResponse
import rw.hafiyawe.store.data.ReplyRequest
import rw.hafiyawe.store.data.ResendResponse
import rw.hafiyawe.store.data.ResetRequest
import rw.hafiyawe.store.data.SupportTicket
import rw.hafiyawe.store.data.TicketRequest
import rw.hafiyawe.store.data.TicketResponse
import rw.hafiyawe.store.data.UnreadCount
import rw.hafiyawe.store.data.UploadsResponse
import rw.hafiyawe.store.data.User
import rw.hafiyawe.store.data.VerifyEmailRequest

interface ApiService {

    // --- session -------------------------------------------------------------

    @GET("api/auth/me")
    suspend fun me(): MeResponse

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/logout")
    suspend fun logout(): OkResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): LoginResponse

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(): ResendResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotRequest): ForgotResponse

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetRequest): OkResponse

    @PATCH("api/profile")
    suspend fun updateProfile(@Body body: ProfileRequest): LoginResponse

    // --- public metadata -----------------------------------------------------

    @GET("api/config")
    suspend fun config(): AppConfig

    @GET("api/help")
    suspend fun help(): List<HelpArticle>

    @GET("api/announcements")
    suspend fun announcements(): List<Announcement>

    // --- listings ------------------------------------------------------------

    @GET("api/products")
    suspend fun products(
        @Query("q") q: String? = null,
        @Query("category") category: String? = null,
        @Query("condition") condition: String? = null,
        @Query("province") province: String? = null,
        @Query("sort") sort: String? = null
    ): List<Product>

    @GET("api/products/{id}")
    suspend fun product(@Path("id") id: String): Product

    @GET("api/my/products")
    suspend fun myProducts(): List<Product>

    @POST("api/products")
    suspend fun createProduct(@Body body: ProductRequest): Product

    @PATCH("api/products/{id}")
    suspend fun updateProductStatus(
        @Path("id") id: String,
        @Body body: ProductStatusRequest
    ): Product

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): OkResponse

    @Multipart
    @POST("api/uploads")
    suspend fun uploadImages(@Part images: List<MultipartBody.Part>): UploadsResponse

    // --- saved collections ---------------------------------------------------

    @GET("api/favorites")
    suspend fun favorites(): List<Product>

    @PUT("api/favorites/{id}")
    suspend fun addFavorite(@Path("id") id: String): OkResponse

    @DELETE("api/favorites/{id}")
    suspend fun removeFavorite(@Path("id") id: String): OkResponse

    @GET("api/cart")
    suspend fun cart(): List<Product>

    @PUT("api/cart/{id}")
    suspend fun addToCart(@Path("id") id: String): OkResponse

    @DELETE("api/cart/{id}")
    suspend fun removeFromCart(@Path("id") id: String): OkResponse

    // --- messaging -----------------------------------------------------------

    @GET("api/chats")
    suspend fun chats(): List<ChatRoom>

    @GET("api/chats/unread")
    suspend fun unreadChats(): UnreadCount

    @POST("api/chats")
    suspend fun openChat(@Body body: ChatRequest): IdResponse

    @GET("api/chats/{id}/messages")
    suspend fun messages(
        @Path("id") roomId: String,
        @Query("before") before: String? = null
    ): List<Message>

    @POST("api/chats/{id}/messages")
    suspend fun sendMessage(@Path("id") roomId: String, @Body body: MessageRequest): Message

    // --- notifications -------------------------------------------------------

    @GET("api/notifications")
    suspend fun notifications(@Query("before") before: String? = null): NotificationsPage

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): OkResponse

    @POST("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): OkResponse

    // --- posting payments ----------------------------------------------------

    @Multipart
    @POST("api/payment-proofs")
    suspend fun uploadProof(@Part proof: MultipartBody.Part): ProofResponse

    @GET("api/payments")
    suspend fun payments(): List<Payment>

    @POST("api/payments")
    suspend fun submitPayment(@Body body: PaymentRequest): Payment

    // --- support -------------------------------------------------------------

    @POST("api/support")
    suspend fun createTicket(@Body body: TicketRequest): TicketResponse

    @GET("api/support")
    suspend fun tickets(): List<SupportTicket>

    @GET("api/support/{id}")
    suspend fun ticket(
        @Path("id") id: String,
        @Header("X-Support-Token") token: String? = null
    ): SupportTicket

    @POST("api/support/{id}/messages")
    suspend fun replyTicket(
        @Path("id") id: String,
        @Body body: ReplyRequest,
        @Header("X-Support-Token") token: String? = null
    ): SupportTicket

    // --- administrator -------------------------------------------------------

    @GET("api/admin/stats")
    suspend fun adminStats(): AdminStats

    @GET("api/admin/products")
    suspend fun adminProducts(@Query("status") status: String = "pending"): List<Product>

    @PATCH("api/admin/products/{id}")
    suspend fun decideProduct(
        @Path("id") id: String,
        @Body body: ProductStatusRequest
    ): Product

    @GET("api/admin/payments")
    suspend fun adminPayments(@Query("status") status: String = "pending"): List<Payment>

    @PATCH("api/admin/payments/{id}")
    suspend fun decidePayment(
        @Path("id") id: String,
        @Body body: ProductStatusRequest
    ): Payment

    @GET("api/admin/users")
    suspend fun adminUsers(): List<User>

    @PATCH("api/admin/users/{id}")
    suspend fun verifyIdentity(
        @Path("id") id: String,
        @Body body: IdentityRequest
    ): User

    @PATCH("api/admin/users/{id}/access")
    suspend fun setAccess(@Path("id") id: String, @Body body: AccessRequest): User

    @GET("api/admin/support")
    suspend fun adminSupport(
        @Query("status") status: String = "open",
        @Query("q") q: String? = null
    ): AdminSupportPage

    @PATCH("api/admin/support/{id}")
    suspend fun setSupportStatus(
        @Path("id") id: String,
        @Body body: ProductStatusRequest
    ): SupportTicket

    @POST("api/admin/support/{id}/messages")
    suspend fun adminReply(@Path("id") id: String, @Body body: ReplyRequest): SupportTicket

    @GET("api/admin/audit")
    suspend fun audit(
        @Query("paymentId") paymentId: String? = null,
        @Query("userId") userId: String? = null
    ): List<AuditEntry>

    @GET("api/admin/audit/search")
    suspend fun auditSearch(
        @Query("q") q: String? = null,
        @Query("action") action: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): AuditSearchPage

    @GET("api/admin/announcements")
    suspend fun adminAnnouncements(): List<Announcement>

    @POST("api/admin/announcements")
    suspend fun createAnnouncement(@Body body: AnnouncementRequest): Announcement

    @PATCH("api/admin/announcements/{id}")
    suspend fun updateAnnouncement(
        @Path("id") id: String,
        @Body body: AnnouncementRequest
    ): Announcement

    @DELETE("api/admin/announcements/{id}")
    suspend fun deleteAnnouncement(@Path("id") id: String): OkResponse

    @POST("api/admin/help")
    suspend fun createHelp(@Body body: HelpRequest): HelpArticle

    @PATCH("api/admin/help/{id}")
    suspend fun updateHelp(@Path("id") id: String, @Body body: HelpRequest): HelpArticle

    @DELETE("api/admin/help/{id}")
    suspend fun deleteHelp(@Path("id") id: String): OkResponse
}

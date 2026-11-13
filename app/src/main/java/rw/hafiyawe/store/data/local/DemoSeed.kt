package rw.hafiyawe.store.data.local

import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.AppConfig
import rw.hafiyawe.store.data.AppNotification
import rw.hafiyawe.store.data.ChatRoom
import rw.hafiyawe.store.data.HelpArticle
import rw.hafiyawe.store.data.Message
import rw.hafiyawe.store.data.Payment
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.SupportMessage
import rw.hafiyawe.store.data.SupportTicket
import rw.hafiyawe.store.data.User

/**
 * Demo mode fills the embedded database with a realistic marketplace so the app can be
 * explored (and the admin workspace tested) with no server running. It is local only:
 * nothing is uploaded and no payment instruction is invented.
 */
object DemoSeed {

    const val DEMO_USER_ID = "demo-me"
    const val DEMO_SELLER_ID = "demo-seller"

    val demoUser = User(
        id = DEMO_USER_ID,
        name = "Demo Admin",
        email = "demo@hafiyawe.local",
        role = "admin",
        emailVerified = true,
        identityVerified = true,
        phone = "+250788000111",
        province = "Kigali",
        district = "Gasabo",
        nationalIdLast4 = "4821",
        freePostsRemaining = 2,
        postCredits = 1,
        planExpiresAt = null,
        isBanned = false
    )

    suspend fun ensure(db: HafiYaweDatabase) {
        val meta = db.meta()
        if (meta.config() != null) return

        meta.upsertConfig(
            AppConfig(
                id = 1,
                singlePostPrice = 100,
                monthlyPlanPrice = 2000,
                freePostLimit = 2,
                paymentInstructions = "",
                paymentsConfigured = false,
                merchantCode = "",
                merchantName = "",
                emailDeliveryConfigured = false,
                development = true,
                showDemoData = true,
                supportEmail = "hafiyawestore@gmail.com"
            )
        )

        db.products().upsertAll(products())
        meta.upsertHelp(help())
        meta.upsertAnnouncements(listOf(announcement()))
        db.payments().upsertAll(payments())
        db.chats().upsertRooms(rooms())
        db.chats().upsertMessages(messages())
        db.notifications().upsert(notifications())
        meta.upsertTickets(tickets())
    }

    private fun products(): List<Product> = listOf(
        Product(
            id = "demo-phone", title = "Samsung Galaxy S22",
            description = "A well-kept Galaxy S22 with a bright display and dependable battery. " +
                "Includes its charging cable and original box. Light signs of everyday use on the frame. " +
                "Available to inspect in Kigali.",
            price = 285_000, category = "Phones", condition = "Like New",
            province = "Kigali", district = "Gasabo",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            sellerVerified = true, createdAt = Iso.hoursAgo(2), isDemo = true
        ),
        Product(
            id = "demo-chair", title = "Lounge chair",
            description = "A comfortable accent chair with warm woven upholstery and solid wooden legs. " +
                "Clean, sturdy and ready for a new home.",
            price = 45_000, category = "Furniture", condition = "Good",
            province = "Kigali", district = "Kicukiro",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(3), isDemo = true
        ),
        Product(
            id = "demo-laptop", title = "MacBook Air M2",
            description = "Lightweight MacBook Air with the M2 chip, 8 GB memory and 256 GB storage. " +
                "Well cared for, battery still holds a full working day.",
            price = 720_000, category = "Electronics", condition = "Like New",
            province = "Kigali", district = "Nyarugenge",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            sellerVerified = true, createdAt = Iso.hoursAgo(5), isDemo = true
        ),
        Product(
            id = "demo-fridge", title = "Hisense double-door fridge",
            description = "Two-door fridge that cools quickly and runs quietly. Moving abroad, so it must go this month.",
            price = 230_000, category = "Appliances", condition = "Good",
            province = "Kigali", district = "Gasabo",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(8), isDemo = true
        ),
        Product(
            id = "demo-bike", title = "Mountain bike, 27.5 inch",
            description = "Aluminium frame, front suspension and working gears. New tyres fitted last month.",
            price = 145_000, category = "Sports", condition = "Good",
            province = "Southern", district = "Huye",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(11), isDemo = true
        ),
        Product(
            id = "demo-books", title = "Secondary school book bundle",
            description = "Twenty books covering maths, physics and biology. Useful for S4 to S6 revision.",
            price = 18_000, category = "Books", condition = "Fair",
            province = "Northern", district = "Musanze",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(16), isDemo = true
        ),
        Product(
            id = "demo-jacket", title = "Leather jacket, size M",
            description = "Genuine leather, warm lining, worn only a few times. Fits a medium frame.",
            price = 60_000, category = "Clothing", condition = "Like New",
            province = "Kigali", district = "Gasabo",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(20), isDemo = true
        ),
        Product(
            id = "demo-toys", title = "Wooden building blocks",
            description = "A full set of smooth wooden blocks in a cotton bag. Complete and clean.",
            price = 12_000, category = "Toys", condition = "Good",
            province = "Eastern", district = "Rwamagana",
            status = "active", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(26), isDemo = true
        ),
        Product(
            id = "demo-phone-pending", title = "Tecno Camon 20",
            description = "Bought last year, screen protector fitted from day one. Charger included.",
            price = 165_000, category = "Phones", condition = "Good",
            province = "Kigali", district = "Kicukiro",
            status = "pending", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(1), isDemo = true
        ),
        Product(
            id = "demo-sofa-pending", title = "Three-seat fabric sofa",
            description = "Grey three-seat sofa, no tears, cushions included. Pick up in Kimironko.",
            price = 180_000, category = "Furniture", condition = "Good",
            province = "Kigali", district = "Gasabo",
            status = "pending", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(4), isDemo = true
        ),
        Product(
            id = "demo-mattress-rejected", title = "Single mattress",
            description = "Used mattress.",
            price = 25_000, category = "Furniture", condition = "Fair",
            province = "Kigali", district = "Nyarugenge",
            status = "rejected", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(30), isDemo = true,
            rejectionReason = "Please add a clearer description and photos showing the actual condition."
        ),
        Product(
            id = "demo-car-off", title = "Toyota RAV4, 2008",
            description = "Well maintained, service history available. Taken off market while I renew the papers.",
            price = 9_500_000, category = "Vehicles", condition = "Good",
            province = "Kigali", district = "Gasabo",
            status = "off_market", sellerId = DEMO_SELLER_ID, sellerName = "Aline Demo",
            createdAt = Iso.hoursAgo(40), isDemo = true
        )
    )

    private fun help(): List<HelpArticle> = listOf(
        HelpArticle(
            id = "help-buy",
            title = "How to buy your first find",
            category = "Buying",
            body = "Browse categories or search for an item, then open its page and select Message seller. " +
                "Ask about condition, arrange a public meeting place, and inspect the item before paying. " +
                "HAFIYAWE connects buyers and sellers; it never holds or processes the purchase price.",
            updatedAt = Iso.now()
        ),
        HelpArticle(
            id = "help-sell",
            title = "Posting a listing",
            category = "Selling",
            body = "Every account gets two free listings forever. After that a posting credit costs 100 RWF, " +
                "or 2,000 RWF unlocks unlimited listings for 30 days. Your email must be verified and your " +
                "profile needs a phone number, national ID and location before you can submit.",
            updatedAt = Iso.now()
        ),
        HelpArticle(
            id = "help-review",
            title = "Why is my listing still pending?",
            category = "Selling",
            body = "A person reviews every listing before it becomes public. Approved items appear in Browse " +
                "immediately; rejected items show the reason under My listings. Review decisions never " +
                "refund a used posting slot.",
            updatedAt = Iso.now()
        ),
        HelpArticle(
            id = "help-safe",
            title = "Staying safe when you meet",
            category = "Safety",
            body = "Meet in a busy public place during daylight, bring someone with you, and inspect the item " +
                "carefully before paying. Never send money before you have seen the item. Report suspicious " +
                "behaviour to support.",
            updatedAt = Iso.now()
        )
    )

    private fun announcement(): Announcement = Announcement(
        id = "demo-announcement",
        title = "Karibu HAFIYAWE",
        body = "Buy and sell second-hand items across Rwanda. Two listings are always free.",
        kind = "info",
        enabled = true,
        sortOrder = 10,
        createdAt = Iso.now(),
        updatedAt = Iso.now()
    )

    private fun payments(): List<Payment> = listOf(
        Payment(
            id = "demo-payment-approved", userId = DEMO_USER_ID, userName = "Demo Admin",
            kind = "single", amount = 100, reference = "DEMO100CREDIT",
            status = "approved", createdAt = Iso.hoursAgo(72), reviewedAt = Iso.hoursAgo(70)
        ),
        Payment(
            id = "demo-payment-pending", userId = DEMO_SELLER_ID, userName = "Aline Demo",
            kind = "monthly", amount = 2000, reference = "MM2409181234",
            status = "pending", createdAt = Iso.hoursAgo(3)
        )
    )

    private fun rooms(): List<ChatRoom> = listOf(
        ChatRoom(
            id = "demo-room-1", productId = "demo-phone", productTitle = "Samsung Galaxy S22",
            otherName = "Aline Demo", side = "buying",
            lastMessage = "Yes, you can see it tomorrow morning in Remera.",
            updatedAt = Iso.hoursAgo(1), unreadCount = 1
        ),
        ChatRoom(
            id = "demo-room-2", productId = "demo-chair", productTitle = "Lounge chair",
            otherName = "Aline Demo", side = "buying",
            lastMessage = "Thank you, see you then.",
            updatedAt = Iso.hoursAgo(20), unreadCount = 0
        )
    )

    private fun messages(): List<Message> = listOf(
        Message(
            id = "demo-m1", roomId = "demo-room-1", senderId = DEMO_USER_ID,
            body = "Hello, is the Galaxy S22 still available?", createdAt = Iso.hoursAgo(3)
        ),
        Message(
            id = "demo-m2", roomId = "demo-room-1", senderId = DEMO_SELLER_ID,
            body = "Yes it is. The battery is still strong and the screen has no scratches.",
            createdAt = Iso.hoursAgo(2)
        ),
        Message(
            id = "demo-m3", roomId = "demo-room-1", senderId = DEMO_USER_ID,
            body = "Can I see it tomorrow?", createdAt = Iso.hoursAgo(2)
        ),
        Message(
            id = "demo-m4", roomId = "demo-room-1", senderId = DEMO_SELLER_ID,
            body = "Yes, you can see it tomorrow morning in Remera.", createdAt = Iso.hoursAgo(1)
        ),
        Message(
            id = "demo-m5", roomId = "demo-room-2", senderId = DEMO_USER_ID,
            body = "Is the chair still available? I can collect on Saturday.", createdAt = Iso.hoursAgo(22)
        ),
        Message(
            id = "demo-m6", roomId = "demo-room-2", senderId = DEMO_SELLER_ID,
            body = "Thank you, see you then.", createdAt = Iso.hoursAgo(20)
        )
    )

    private fun notifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "demo-n1", type = "message", title = "New message from Aline Demo",
            body = "Yes, you can see it tomorrow morning in Remera.",
            href = "/chats/demo-room-1", createdAt = Iso.hoursAgo(1)
        ),
        AppNotification(
            id = "demo-n2", type = "listing_approved", title = "Listing approved",
            body = "MacBook Air M2 is now visible to everyone.",
            href = "/my-listings", createdAt = Iso.hoursAgo(5)
        ),
        AppNotification(
            id = "demo-n3", type = "payment_approved", title = "Posting credit added",
            body = "Your 100 RWF payment was approved. One posting credit is ready to use.",
            href = "/payments", readAt = Iso.hoursAgo(60), createdAt = Iso.hoursAgo(70)
        )
    )

    private fun tickets(): List<SupportTicket> = listOf(
        SupportTicket(
            id = "demo-ticket-1", userId = DEMO_SELLER_ID, email = "seller@hafiyawe.test",
            subject = "Listing rejected without photos",
            description = "My mattress listing was rejected but I cannot see what to change.",
            status = "open", createdAt = Iso.hoursAgo(8), updatedAt = Iso.hoursAgo(6),
            messages = listOf(
                SupportMessage(
                    id = "demo-sm1", authorRole = "user",
                    body = "My mattress listing was rejected but I cannot see what to change.",
                    createdAt = Iso.hoursAgo(8)
                ),
                SupportMessage(
                    id = "demo-sm2", authorRole = "admin",
                    body = "Please add photos that show the real condition and a fuller description.",
                    createdAt = Iso.hoursAgo(6)
                )
            )
        )
    )
}

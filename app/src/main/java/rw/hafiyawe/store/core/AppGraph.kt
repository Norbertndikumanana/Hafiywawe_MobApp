package rw.hafiyawe.store.core

import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.decode.SvgDecoder
import rw.hafiyawe.store.data.local.HafiYaweDatabase
import rw.hafiyawe.store.data.remote.PersistentCookieJar
import rw.hafiyawe.store.data.remote.buildApi
import rw.hafiyawe.store.data.remote.buildHttpClient
import rw.hafiyawe.store.data.repo.AdminRepository
import rw.hafiyawe.store.data.repo.AuthRepository
import rw.hafiyawe.store.data.repo.CatalogRepository
import rw.hafiyawe.store.data.repo.ChatRepository
import rw.hafiyawe.store.data.repo.MetaRepository
import rw.hafiyawe.store.data.repo.NotificationsRepository
import rw.hafiyawe.store.data.repo.PaymentsRepository
import rw.hafiyawe.store.data.repo.SupportRepository

/**
 * One object graph for the whole app. Small enough that a DI framework would only
 * add build complexity: everything is created once in [HafiYaweApp].
 */
class AppGraph(context: Context) {

    val session = SessionStore(context)

    val db: HafiYaweDatabase = Room.databaseBuilder(
        context,
        HafiYaweDatabase::class.java,
        HafiYaweDatabase.NAME
    )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val cookies = PersistentCookieJar(session.cookiePrefs)
    private val httpClient = buildHttpClient(cookies, session)
    private val api = buildApi(httpClient)

    val auth = AuthRepository(api, session, db, cookies)
    val meta = MetaRepository(api, session, db)
    val catalog = CatalogRepository(api, session, db, context.contentResolver)
    val chats = ChatRepository(api, session, db)
    val notifications = NotificationsRepository(api, session, db)
    val payments = PaymentsRepository(api, session, db, catalog)
    val support = SupportRepository(api, session, db)
    val admin = AdminRepository(api, session, db)

    /** Product photos are public; payment receipts need the session cookie, so the
     *  same OkHttp client (with its cookie jar) is reused for image loading. */
    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .okHttpClient(httpClient)
        .crossfade(true)
        .build()
}

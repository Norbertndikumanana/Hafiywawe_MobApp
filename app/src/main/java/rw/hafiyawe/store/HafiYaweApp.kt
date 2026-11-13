package rw.hafiyawe.store

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import rw.hafiyawe.store.core.AppGraph

class HafiYaweApp : Application(), ImageLoaderFactory {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }

    override fun newImageLoader(): ImageLoader = graph.imageLoader

    companion object {
        fun graph(context: Context): AppGraph =
            (context.applicationContext as HafiYaweApp).graph
    }
}

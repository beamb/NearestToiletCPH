package beamb.nearesttoiletcph.api

import beamb.nearesttoiletcph.data.ToiletResult
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ToiletRetriever {
    private val service: OpenDataService

    companion object {
        private const val BASE_URL = "https://wfs-kbhkort.kk.dk/k101/"
    }

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(OpenDataService::class.java)
    }

    fun getToilets(callback: Callback<ToiletResult>) {
        service.retrieveToilets().enqueue(callback)
    }
}
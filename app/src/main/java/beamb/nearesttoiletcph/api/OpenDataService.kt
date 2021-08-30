package beamb.nearesttoiletcph.api

import beamb.nearesttoiletcph.data.ToiletResult
import retrofit2.Call
import retrofit2.http.GET

interface OpenDataService {
    @GET("/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=k101:toilet_tmf_kk&outputFormat=json&SRSNAME=EPSG:4326")
    fun retrieveToilets(): Call<ToiletResult>
}
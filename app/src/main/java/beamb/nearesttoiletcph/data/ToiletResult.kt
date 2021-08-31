package beamb.nearesttoiletcph.data

import com.google.gson.annotations.SerializedName

data class ToiletResult(@SerializedName("features") val features : List<Features>)

data class Features (
    @SerializedName("properties") val properties : Properties
)

data class Properties (
    @SerializedName("toilet_lokalitet") val toiletLocation : String,
    @SerializedName("helaarsaabent") val yearRoundHours : String,
    @SerializedName("mandag") val monday : String,
    @SerializedName("tirsdag") val tuesday : String,
    @SerializedName("onsdag") val wednesday : String,
    @SerializedName("torsdag") val thursday : String,
    @SerializedName("fredag") val friday : String,
    @SerializedName("loerdag") val saturday : String,
    @SerializedName("soendag") val sunday : String,
    @SerializedName("bemaerkning") val notes : String,
    @SerializedName("longitude") val longitude : Double,
    @SerializedName("latitude") val latitude : Double,
    @SerializedName("data_opdatering") val lastUpdate : String
)
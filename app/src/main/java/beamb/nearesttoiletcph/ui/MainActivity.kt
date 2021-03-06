package beamb.nearesttoiletcph.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import beamb.nearesttoiletcph.R
import beamb.nearesttoiletcph.api.ToiletRetriever
import beamb.nearesttoiletcph.data.Properties
import beamb.nearesttoiletcph.data.ToiletResult
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.ArrayList

/** Nearest Toilet CPH is inspired by (jst) Jørgen Staunstrup's Nearest Toilet App - presented
 * in the Mobile App Development course at IT University of Copenhagen (Spring 2020 & Spring 2021).*/

class MainActivity : AppCompatActivity() {
    // Finding nearest toilet in Copenhagen based on KK open data

    private lateinit var mCurrentLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mClosestToilet: Properties
    private lateinit var mBuilder: AlertDialog.Builder
    private lateinit var mWeb: WebView
    private lateinit var toilets: ArrayList<Properties>


    private val toiletRetriever = ToiletRetriever()

    private val callback = object : Callback<ToiletResult> {
        override fun onFailure(call: Call<ToiletResult>?, t: Throwable?) {
            Log.e("MainActivity", "Problem calling Open Data API ${t?.message}")
        }

        override fun onResponse(call: Call<ToiletResult>?, response: Response<ToiletResult>?) {
            response?.isSuccessful.let {
                val resultList = ToiletResult(response?.body()?.features ?: emptyList())
                toilets = ArrayList()
                resultList.features.forEach {
                    toilets.add(it.properties)
                }
                mClosestToilet = findClosestToilet(toilets)!!
                displayClosestToilet()
                Log.i("MainActivity", resultList.toString())
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setTitle(R.string.app_title)

        mBuilder = AlertDialog.Builder(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        mWeb = findViewById(R.id.webpage)
        mWeb.settings.javaScriptEnabled = true
        mWeb.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }
        if (isNetworkConnected() && isLocationEnabled()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        mCurrentLocation = location
                        if (!this::toilets.isInitialized) {
                            toiletRetriever.getToilets(callback)
                        } else {
                            mClosestToilet = findClosestToilet(toilets)!!
                            displayClosestToilet()
                        }
                        Log.i("MainActivity", mCurrentLocation.toString())
                    } else {
                        displayDialog(
                            getString(R.string.refresh_location_title),
                            getString(R.string.refresh_location)
                        )
                    }
                }
        } else {
            displayDialog(
                getString(R.string.share_location_title),
                getString(R.string.share_location)
            )
        }
    }

    private fun findClosestToilet(b: ArrayList<Properties>?): Properties? {
        var temp: Double
        if (b != null && b.isNotEmpty()) {
            var i = b.size - 1
            var closest = b[i]
            val location = getToiletLocation(closest.latitude, closest.longitude)
            var min = distance(location, mCurrentLocation)
            while (i > 0) {
                i -= 1
                val tempLoc = getToiletLocation(b[i].latitude, b[i].longitude)
                temp = distance(tempLoc, mCurrentLocation)
                if (temp < min) {
                    closest = b[i]
                    min = temp
                }
            }
            return closest
        }
        return null
    }

    private fun distance(p1: Location, p2: Location): Double {
        return (p1.distanceTo(p2)).toDouble()
    }

    private fun displayClosestToilet() {
        val closestToiletLocation =
            getToiletLocation(mClosestToilet.latitude, mClosestToilet.longitude)
        startBrowser(mCurrentLocation, closestToiletLocation)
    }

    private fun getToiletLocation(lat: Double, long: Double): Location {
        val location = Location("Toilet")
        location.latitude = lat
        location.longitude = long
        return location
    }

    private fun startBrowser(
        start: Location,
        dest: Location
    ) {
        val url =
            "https://maps.google.com?saddr=" + start.latitude + "," + start.longitude +
                    "&daddr=" + dest.latitude + "," + dest.longitude + "&dirflg=w"
        mWeb.loadUrl(url)
    }

    private fun displayDialog(title: String, message: CharSequence) {
        mBuilder.setMessage(message)
            .setCancelable(false)
            .setPositiveButton(
                "Ok"
            ) { dialog, _ ->
                dialog.cancel()
            }
            .setTitle(title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_info -> {
                displayDialog(
                    mClosestToilet.toiletLocation + "\n" + "Opening hours:",
                    Html.fromHtml(getToiletInfo(), Html.FROM_HTML_MODE_LEGACY)
                )
                true
            }
            R.id.menu_sync -> {
                getLocation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nCap = cm.getNetworkCapabilities(cm.activeNetwork)
        return nCap != null && nCap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gpsEnabled && networkEnabled
    }

    private fun getToiletInfo(): String {
        return "<small>" + getString(R.string.toilet_info) + "</small>" +
                "<br/>" +
                "<small>" + "Latest update: " + mClosestToilet.lastUpdate + "</small>" +
                "<br/>" + "<br/>" +
                "<b>" + "Monday:" + "</b>" + "<br/>" +
                mClosestToilet.monday + "<br/>" + "<br/>" +
                "<b>" + "Tuesday:" + "</b>" + "<br/>" +
                mClosestToilet.tuesday + "<br/>" + "<br/>" +
                "<b>" + "Wednesday:" + "</b>" + "<br/>" +
                mClosestToilet.wednesday + "<br/>" + "<br/>" +
                "<b>" + "Thursday:" + "</b>" + "<br/>" +
                mClosestToilet.thursday + "<br/>" + "<br/>" +
                "<b>" + "Friday:" + "</b>" + "<br/>" +
                mClosestToilet.friday + "<br/>" + "<br/>" +
                "<b>" + "Saturday:" + "</b>" + "<br/>" +
                mClosestToilet.saturday + "<br/>" + "<br/>" +
                "<b>" + "Sunday:" + "</b>" + "<br/>" +
                mClosestToilet.sunday + "<br/>" + "<br/>" +
                "<br/>" +
                "<b>" + "Open year-round: " + "</b>" + mClosestToilet.yearRoundHours + "<br/>" +
                if (mClosestToilet.notes != "0") {
                    "<u>" + "Notes:" + "</u>" + "<br/>" + mClosestToilet.notes
                } else {
                    ""
                }
    }
}
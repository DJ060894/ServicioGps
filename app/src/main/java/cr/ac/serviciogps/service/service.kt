package cr.ac.serviciogps.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.os.Looper
import com.google.android.gms.location.*

import cr.ac.serviciogps.db.LocationDatabase
import cr.ac.serviciogps.entity.Location


class service : IntentService("GpsService") {

    lateinit var mLocationCallback: LocationCallback
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationDatabase: LocationDatabase

    companion object {
        val GPS = "cr.ac.service.GPS_EVENT"

    }

    override fun onHandleIntent(intent: Intent?) {
        locationDatabase = LocationDatabase.getInstance(this)
        getLocation()
    }

    @SuppressLint("MissingPermission")
            /**Inicializa call y fused, coloca intervalo, recibe la ubicación, y envia el brodcast*/
    fun getLocation(){

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.create()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationCallback = object : LocationCallback(){
            override fun onLocationResult( locationResult : LocationResult) {
                if (locationResult == null){
                    return
                }
                for (location in locationResult.locations){

                    val bcIntent = Intent()
                    val location = Location (null, location.latitude, location.longitude)
                    bcIntent.action = service.GPS
                    //bcIntent.putExtra("localizacion", location)


                    sendBroadcast(bcIntent)

                    locationDatabase.locationDao.insert(Location(null, location.latitude, location.longitude))


                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            null
        )
        Looper.loop()
    }

}
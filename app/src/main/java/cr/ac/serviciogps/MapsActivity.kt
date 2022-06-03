package cr.ac.serviciogps

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import androidx.constraintlayout.motion.widget.Debug.getLocation

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPolygon
import cr.ac.serviciogps.databinding.ActivityMapsBinding
import cr.ac.serviciogps.db.LocationDatabase
import cr.ac.serviciogps.entity.Location
import cr.ac.serviciogps.service.service
import org.json.JSONObject

private lateinit var mMap: GoogleMap
private lateinit var layer : GeoJsonLayer

@Suppress("SAFE_CALL_WILL_CHANGE_NULLABILITY")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapsBinding
    private val SOLICITAR_GPS = 1
    private lateinit var locationDatabase : LocationDatabase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

// Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationDatabase = LocationDatabase.getInstance(this)
        validaPermisos()
        //getLocation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        iniciaServicio()
        recuperarPuntos()
        definePoligono(googleMap)
    }

    fun definePoligono(googleMap: GoogleMap){
        val geoJsonData= JSONObject("{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {},\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"LineString\",\n" +
                "        \"coordinates\": [\n" +
                "          [\n" +
                "            -85.97900390625,\n" +
                "            11.221510260010541\n" +
                "          ],\n" +
                "          [\n" +
                "            -83.551025390625,\n" +
                "            11.232286352218692\n" +
                "          ],\n" +
                "          [\n" +
                "            -82.254638671875,\n" +
                "            9.514079262770904\n" +
                "          ],\n" +
                "          [\n" +
                "            -82.77099609375,\n" +
                "            8.080984688871991\n" +
                "          ],\n" +
                "          [\n" +
                "            -86.12182617187499,\n" +
                "            10.163560279490476\n" +
                "          ],\n" +
                "          [\n" +
                "            -86.0009765625,\n" +
                "            11.14606637590688\n" +
                "          ]\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}")

        layer = GeoJsonLayer(googleMap, geoJsonData)
        layer.addLayerToMap()

    }


    /*
    Obteniene los puntos almacenados en la BD y los muestra en el mapa
    */
    fun recuperarPuntos(){
        for(location in  locationDatabase.locationDao.query()) {
            val lugar = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(lugar).title("Marker in Map"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lugar))

        }
    }


    /*
    Hace un filtro del broadcast/acción GPS (cr.ac.apservice.GPS_EVENT)
    E inicia el servicio (startService) GpsService
    */
    fun iniciaServicio(){
        val filter = IntentFilter()
        filter.addAction(service.GPS)
        val rev = ProgressReceiver()
        registerReceiver(rev, filter)
        startService(Intent(this, service::class.java))
    }
    /*
    valida si la app tiene permisos de ACCESS_FINE_LOCATION y de ACCESS_COARSE_LOCATION
    Si no tiene permisos solicita al usuario permisos (requestPermissions)
    */
    fun validaPermisos(){
        if(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
//No tengo permisos
            ActivityCompat.requestPermissions(this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                SOLICITAR_GPS
            )
        }
    }

    /*
    Validar que se le dieron los permisos a la app, en caso contrario salir
    */
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ){
        when(requestCode){
            SOLICITAR_GPS ->{
//Usuario no dio permisos
                if(grantResults.isEmpty() ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    System.exit(1)
                }
            }
        }
    }

    /*
    Es la clase para recibir los mensajes de broadcast
    */
    class ProgressReceiver : BroadcastReceiver() {
        /*
        Se obtiene el parametro eviado por el servicio (location)
        Coloca en el mapa la localización
        Mueve la cámara a la localización
        */
        fun getPolygon(layer: GeoJsonLayer): GeoJsonPolygon? {
            for (feature in layer.features) {
                return feature.geometry as GeoJsonPolygon
            }
            return null
        }

        override fun onReceive(context: Context?, intent: Intent?){
            if(intent?.action == service.GPS){
                val location : Location =
                    intent?.getSerializableExtra("localizacion") as Location

                val punto = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(punto).title("marker in Map "))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(punto))

                if(PolyUtil.containsLocation(location.latitude, location.longitude, getPolygon(layer)!!.outerBoundaryCoordinates, false))
                    Toast.makeText(context,"Punto dentro del Poligono ",Toast.LENGTH_SHORT).show()
        }
        else
                Toast.makeText(context,"Punto fuera del Poligono",Toast.LENGTH_SHORT).show()
    }
}
}
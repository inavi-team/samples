package us.inavi.sample.routing

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.basarsoft.inavi.libs.helper.BuildingDataType
import com.basarsoft.inavi.libs.helper.CoordType
import com.basarsoft.inavi.libs.helper.Point
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.BuildingLicenseRequest
import com.basarsoft.inavi.libs.licensemanager.licenserequest.ModuleLicenseRequest
import com.basarsoft.inavi.libs.packagemanager.DownloadListener
import com.basarsoft.inavi.libs.packagemanager.PackageManager
import com.basarsoft.inavi.libs.routing.Routing
import com.basarsoft.inavi.libs.manifest.*
import kotlin.concurrent.schedule
import java.util.Timer


private const val TAG = "iNavi"
private const val BUILDING_ID = "ENTER_YOUR_BUILDING_ID"
private const val BUILDING_VERSION_MAJOR = 2
private const val ROUTING_MODULE = "inavi_routing"
private const val ROUTING_VERSION = 1
private const val INAVI_API_KEY = "ENTER_YOUR_API_KEY"

class MainActivity : AppCompatActivity(), LicenseListener, DownloadListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            ModuleLicenseRequest(ROUTING_MODULE, ROUTING_VERSION.toString()),
            BuildingLicenseRequest(
                BUILDING_ID,
                BUILDING_VERSION_MAJOR.toString(),
                BuildingDataType.MAP
            ),
            BuildingLicenseRequest(
                BUILDING_ID,
                BUILDING_VERSION_MAJOR.toString(),
                BuildingDataType.POSITIONING
            ),
            BuildingLicenseRequest(
                BUILDING_ID,
                BUILDING_VERSION_MAJOR.toString(),
                BuildingDataType.ROUTING
            ),
        )
        licenseRequestMap.put(INAVI_API_KEY, licenseRequests)
        LicenseManager.requestLicense(licenseRequestMap, this)
    }

    override fun onLicenseSuccess() {
        Log.e(TAG, "LicenseListener::onLicenseSuccess")
        val buildingList = PackageManager.getBuildingList()
        buildingList.forEach { buildingInfo ->
            Log.d(TAG, "Building info: $buildingInfo")
        }
        PackageManager.downloadBuildingData(
            BUILDING_ID,
            BUILDING_VERSION_MAJOR,
            arrayListOf(
                BuildingDataType.ROUTING,
                BuildingDataType.MAP,
                BuildingDataType.POSITIONING
            ),
            this
        )
    }

    override fun onLicenseError(message: String) {
        Log.e(TAG, "LicenseListener::onLicenseError: $message")
    }

    //This listener is using for download states of building data
    //When downloading building data is completed, this callback is called
    override fun onCompleted(buildingId: String) {
        Log.e(TAG, "DownloadListener::onCompleted: building id: $buildingId")
        //Selecting downlaoded building data
        PackageManager.selectBuildingData(BUILDING_ID, BUILDING_VERSION_MAJOR)

        /*Creating routing object
         configJson: is generally using constant. This field is for creating profiles. Routing will calculate special routes for every profiles
        */
        val routingConfigJson =
            """
                {
                    "tesselationdistance": 5.0,
                    "pathstraightening": true,
                    "zsensitivity": 1000.0,
                    "min_turnangle": 5.0,
                    "min_updownangle": 45.0,
                    "rules": [
                        {
                            "speed": 1.0
                        },
                        {
                            "profiles": ["visitor"],
                            "filters": [{"visitor": false}],
                            "speed": 0.0
                        },
                        {
                            "profiles": ["wheelchair"],
                            "filters": [{"wheelchair": false}],
                            "speed": 0.0
                        },
                        {
                            "profiles": ["staff"],
                            "filters": [{"staff": false}],
                            "speed": 0.0
                        },
                        {
                            "profiles": ["vehicle"],
                            "filters": [{"vehicle": false}],
                            "speed": 0.0
                        }
                    ]
                }
            """.trimIndent()
        val routing = Routing(routingConfigJson)
        //Start point for routing
        //End point for routing
        val points = arrayOf(
            Point(5190037.868829081, 2840442.240174248, 0.0, CoordType.EPSG3857),
            Point(5190014.364326005, 2840565.9084912036, 0.0, CoordType.EPSG3857)
        )

        /*Calculating route with start point, end point, coordType and profile
         If you want to draw this route on map, you can take a look at "Full Sample App"
         */
        val routeGeoJSON = routing.calculateRouteAsGeoJSON(points, CoordType.EPSG3857)
        Log.e(TAG, "Routing::calculateRouteAsGeoJSON result: $routeGeoJSON")

        val routeSimpleJSON = routing.calculateRouteAsSimpleJSON(points, CoordType.EPSG3857)
        Log.e(TAG, "Routing::calculateRouteAsSimpleJSON result: $routeSimpleJSON")

        //Giving route to manifest object for calculating direction commands
        val manifest = iNaviManifest(routeGeoJSON, 1.0)

        /*
        Android's tts library needs context, because of that you need to create an TTSManifestCommandNotifier
         */
        val ttsCommandNotifier = TTSManifestCommandNotifier(this)
        //If you want to play sentences of
        manifest.setManifestCommandNotifier(ttsCommandNotifier)

        //Getting static direction commands
        val staticManifest = manifest.getStaticManifestResult(this)
        staticManifest.manifestResults.forEach {
            Log.e(TAG, it.stringCommand)
        }

        //These points are fake points for simulating user's walk path on route
        val simulatedPoints = arrayOf(
            Point( 5190032.588103076, 2840443.798932999, 0.0, CoordType.EPSG3857),
            Point( 5190027.3953997, 2840443.470657682, 0.0, CoordType.EPSG3857),
            Point( 5190020.667865119, 2840443.6001404086, 0.0, CoordType.EPSG3857),
            Point( 5190013.446393871, 2840443.3963252148, 0.0, CoordType.EPSG3857),
            Point( 5190007.821400579, 2840443.483692567, 0.0, CoordType.EPSG3857),
            Point( 5190007.766795883, 2840449.3840900213, 0.0, CoordType.EPSG3857),
            Point( 5190004.383723546, 2840452.852275449, 0.0, CoordType.EPSG3857),
            Point( 5190004.667162687, 2840460.0892926096, 0.0, CoordType.EPSG3857),
            Point( 5190004.896545145, 2840469.292683911, 0.0, CoordType.EPSG3857),
            Point( 5190005.259701587, 2840484.328055435, 0.0, CoordType.EPSG3857),
            Point( 5190006.241182455, 2840521.0756734167, 0.0, CoordType.EPSG3857),
            Point( 5190006.122716145, 2840536.891660028, 0.0, CoordType.EPSG3857),
            Point( 5190007.055151018, 2840549.4880041545, 0.0, CoordType.EPSG3857),
            Point( 5190003.113400955, 2840556.956291116, 0.0, CoordType.EPSG3857),
            Point( 5190003.048757326, 2840563.632493267, 0.0, CoordType.EPSG3857),
            Point( 5190008.624765145, 2840563.3927463153, 0.0, CoordType.EPSG3857),
        )
        var time = 0L

        /*These codes simulating the navigation. It simulates as if the user is walking.
         Simulating the fake location for manifest step by step. Every point is selected manually.
         If you want to draw this fake location o the map, you can take a look at "Full Sample App".
         */
        for(point in simulatedPoints){
            Timer("SettingUp", false).schedule(time) {
                simulateManifest(manifest, point)
            }
            //This is for simulating user's walking, in every 5 seconds simulating the fake location.
            time += 5000L
        }

    }

    //When an error occured while downloading building data, this callback is called
    override fun onError(buildingId: String, errorMessage: String) {
        Log.e(
            TAG,
            "DownloadListener::onError: building id: $buildingId error message: $errorMessage"
        )
    }

    //While a building data is downloading, this callback is called
    override fun onProgress(buildingId: String, percentage: Int) {
        Log.e(TAG, "DownloadListener::onProgress: building id: $buildingId percentage: $percentage")
    }

    //Simulating the manifest with given point
    fun simulateManifest(manifest: iNaviManifest, point: Point){
        //This is a fake location of user.
        val dynamicManifest = manifest.queryManifest(point)
        //Enum of a current command
        Log.e(TAG, "CommandEnum:" +dynamicManifest.commandEnum)
        //Enum of a current commandtype
        Log.e(TAG, "CommandType:" +dynamicManifest.commandEnum)
    }
}
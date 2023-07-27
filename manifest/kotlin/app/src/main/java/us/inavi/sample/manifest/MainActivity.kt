package us.inavi.sample.manifest

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.PackageManagerCompat.LOG_TAG
import com.basarsoft.inavi.libs.helper.BuildingDataType
import com.basarsoft.inavi.libs.helper.CoordType
import com.basarsoft.inavi.libs.helper.Point
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.BuildingLicenseRequest
import com.basarsoft.inavi.libs.licensemanager.licenserequest.ModuleLicenseRequest
import com.basarsoft.inavi.libs.manifest.TTSEngine
import com.basarsoft.inavi.libs.manifest.TTSManifestCommandNotifier
import com.basarsoft.inavi.libs.manifest.iNaviManifest
import com.basarsoft.inavi.libs.packagemanager.DownloadListener
import com.basarsoft.inavi.libs.packagemanager.PackageManager
import com.basarsoft.inavi.libs.routing.Routing
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity(), LicenseListener, DownloadListener {
    val LOG_TAG: String = "iNavi"
    val API_KEY: String = ""
    val BUILDING_ID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            ModuleLicenseRequest("yolbil_map", "1"),
            ModuleLicenseRequest("inavi_positioner", "1"),
            ModuleLicenseRequest("inavi_routing", "1"),
            ModuleLicenseRequest("inavi_manifest", "1"),
            BuildingLicenseRequest(BUILDING_ID, "3", BuildingDataType.MAP),
            BuildingLicenseRequest(BUILDING_ID, "3", BuildingDataType.POSITIONING),
            BuildingLicenseRequest(BUILDING_ID, "3", BuildingDataType.ROUTING),
        )
        licenseRequestMap.put(API_KEY, licenseRequests)
        LicenseManager.requestLicense(licenseRequestMap, this)

    }
    override fun onLicenseSuccess() {
        Log.d("inavi-license", "onLicenseSuccess")
        val buildingList = PackageManager.getBuildingList()
        buildingList.forEach { buildingInfo ->
            Log.d("buildingInfo", buildingInfo.id)
        }
        PackageManager.downloadBuildingData(
            BUILDING_ID,
            3,
            arrayListOf(BuildingDataType.MAP, BuildingDataType.ROUTING, BuildingDataType.POSITIONING),
            this
        )
    }

    override fun onLicenseError(message: String) {
        Log.e(LOG_TAG, "License failed")
    }

    override fun onCompleted(buildingId: String) {
        PackageManager.selectBuildingData(BUILDING_ID, 3)
        val points = arrayOf<Point>(Point(32.77547737170958, 39.90906469768344, 0.0, CoordType.WGS84), Point(32.77711621248441, 39.91072501467675, 2.0, CoordType.WGS84))
        val routing = Routing("{\"tesselationdistance\": 5.0, \"pathstraightening\": true, \"zsensitivity\": 1000.0, \"min_turnangle\": 20.0, \"min_updownangle\": 45.0, \"rules\": [{ \"speed\": 1.0 } ] }")
        Log.e("result",routing.calculateRouteAsGeoJSON(points, CoordType.WGS84))
        Log.e("result",routing.calculateRouteAsSimpleJSON(points, CoordType.WGS84))
        val manifest = iNaviManifest(routing.calculateRouteAsGeoJSON(points, CoordType.WGS84),1.0)
        val staticManifest = manifest.getStaticManifestResult(this@MainActivity, "en")

        val manifestCommandNotifier =  TTSManifestCommandNotifier(this@MainActivity);
        manifest.setManifestCommandNotifier(manifestCommandNotifier)


        val locale = Locale("en")
        Locale.setDefault(locale)
        val config = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            config.setLocale(locale)
        else
            config.locale = locale

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        val tts = TTSEngine.getInstance(this@MainActivity)
        tts.setLanguage("en")

        Timer().schedule(2000) {
            manifest.queryManifest(Point(32.775935995593386, 39.90962099669135, 0.0, CoordType.WGS84))
            manifest.queryManifest(Point(32.776568002874754, 39.90981099869898, 0.0, CoordType.WGS84))
        }

    }

    override fun onError(buildingId: String, errorMessage: String) {

    }

    override fun onProgress(buildingId: String, percentage: Int) {

    }
}

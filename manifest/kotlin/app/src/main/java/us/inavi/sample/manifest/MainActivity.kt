package us.inavi.sample.manifest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.PackageManagerCompat.LOG_TAG
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.ModuleLicenseRequest
import com.basarsoft.inavi.libs.manifest.iNaviManifest

class MainActivity : AppCompatActivity(), LicenseListener {
    val LOG_TAG: String = "iNavi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            ModuleLicenseRequest("inavi_manifest", "1"),
        )
        licenseRequestMap.put("ENTER_YOUR_API_KEY", licenseRequests)
        LicenseManager.requestLicense(licenseRequestMap, this)

        val manifest = iNaviManifest(getRoutingGeoJSONResult(), 1.0)
        val staticManifest = manifest.getStaticManifestResult(this)
        staticManifest.manifestResults.forEach {
            Log.d(LOG_TAG, it.stringCommand)
        }
    }
    override fun onLicenseSuccess() {
        Log.e(LOG_TAG, "License successful")
    }

    override fun onLicenseError(message: String) {
        Log.e(LOG_TAG, "License failed")
    }

    fun getRoutingGeoJSONResult(): String {
        return """
            {
                "features": [
                    {
                        "geometry": {
                            "coordinates": [
                                [32.775970996978366, 39.909616003754515,0],
                                [32.775970996978366, 39.909616003754515,0]
                            ],
                            "type": "LineString"
                        },
                        "properties": {
                            "floor": 0,
                            "fromto": true,
                            "id": "cirwUpBWkx",
                            "tofrom": true,
                            "type": 0
                        },
                        "type": "Feature"
                    }
                ],
                "type": "FeatureCollection"
            }
        """.trimIndent()
    }
}

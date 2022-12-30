package us.inavi.sample.positioner

import android.Manifest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.basarsoft.inavi.libs.helper.BuildingDataType
import com.basarsoft.inavi.libs.helper.CoordType
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.BuildingLicenseRequest
import com.basarsoft.inavi.libs.licensemanager.licenserequest.ModuleLicenseRequest
import com.basarsoft.inavi.libs.packagemanager.DownloadListener
import com.basarsoft.inavi.libs.packagemanager.PackageManager
import com.basarsoft.inavi.libs.positioner.Location
import com.basarsoft.inavi.libs.positioner.PositionListener
import com.basarsoft.inavi.libs.positioner.Positioner
import com.basarsoft.inavi.libs.sensormanager.SensorManager

class MainActivity : AppCompatActivity(), DownloadListener, LicenseListener {
    lateinit var positioner: Positioner
    val LOG_TAG: String = "iNavi"
    val BUILDING_ID: String = "ENTER_YOUR_BUILDING_ID"
    val BUILDING_VERSION_MAJOR_STRING: String = "2"
    val BUILDING_VERSION_MAJOR: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissionList = arrayListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            SensorManager.startBeaconScan(this)
        }

        checkPermission(permissionList)

        positioner = Positioner(CoordType.EPSG3857)
        positioner.registerListener(object : PositionListener {
            override fun onLocationChanged(location: Location) {
                Log.e(LOG_TAG, "Location x: " + location.x + ", y: " + location.y + ", f: " + location.f)
            }
        })

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            ModuleLicenseRequest("inavi_positioner", "1"),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.MAP),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.POSITIONING),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.ROUTING),
        )
        licenseRequestMap.put("ENTER_YOUR_API_KEY", licenseRequests)
        LicenseManager.requestLicense(licenseRequestMap, this)
    }

    override fun onLicenseSuccess() {
        Log.e(LOG_TAG, "License successful")
        Log.d(LOG_TAG, "Building Ids:")
        val buildingList = PackageManager.getBuildingList()
        buildingList.forEach { buildingInfo ->
            Log.d(LOG_TAG, "Building Id: " + buildingInfo.id + ", version: " + buildingInfo.version)
        }
        PackageManager.downloadBuildingData(
            BUILDING_ID,
            BUILDING_VERSION_MAJOR,
            arrayListOf(BuildingDataType.MAP, BuildingDataType.ROUTING, BuildingDataType.POSITIONING),
            this
        )
    }

    override fun onLicenseError(message: String) {
        Log.e(LOG_TAG, "License failed")
    }

    override fun onProgress(buildingId: String, percentage: Int) {
        Log.e(LOG_TAG, "Download progress: $percentage")
    }

    override fun onCompleted(buildingId: String) {
        Log.e(LOG_TAG,"Download completed")
        PackageManager.selectBuildingData(BUILDING_ID, BUILDING_VERSION_MAJOR)
        positioner.enable()
    }

    override fun onError(buildingId: String, errorMessage: String) {
        Log.e(LOG_TAG, errorMessage)
    }

    private fun checkPermission(permissions: List<String>, requestCode: Int = 100) {
        val requestPermissions = arrayListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this@MainActivity, it) == android.content.pm.PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(it)
            } else {
                if (it == Manifest.permission.BLUETOOTH_SCAN) {
                    SensorManager.startBeaconScan(this)
                } else if (it == Manifest.permission.ACCESS_FINE_LOCATION) {
                    SensorManager.startWifiScan(this)
                }
            }
        }
        if (requestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this@MainActivity, requestPermissions.toTypedArray(), requestCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissions.forEachIndexed { i, s ->
            if (grantResults[i] != android.content.pm.PackageManager.PERMISSION_GRANTED) return
            if (s == Manifest.permission.BLUETOOTH_SCAN) {
                SensorManager.startBeaconScan(this)
            } else if (s == Manifest.permission.ACCESS_FINE_LOCATION) {
                SensorManager.startWifiScan(this)
            }
        }
    }
}

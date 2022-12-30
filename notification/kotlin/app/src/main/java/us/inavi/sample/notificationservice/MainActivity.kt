package us.inavi.sample.notificationservice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.basarsoft.inavi.libs.helper.BuildingDataType
import com.basarsoft.inavi.libs.helper.CoordType
import com.basarsoft.inavi.libs.helper.NotificationResult
import com.basarsoft.inavi.libs.helper.Point
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.BuildingLicenseRequest
import com.basarsoft.inavi.libs.notificationservice.NotificationListener
import com.basarsoft.inavi.libs.notificationservice.NotificationService
import com.basarsoft.inavi.libs.packagemanager.DownloadListener
import com.basarsoft.inavi.libs.packagemanager.PackageManager

class MainActivity : AppCompatActivity(), NotificationListener, DownloadListener, LicenseListener {
    val LOG_TAG: String = "iNavi"
    val BUILDING_ID: String = "ENTER_YOUR_BUILDING_ID"
    val BUILDING_VERSION_MAJOR_STRING: String = "3"
    val BUILDING_VERSION_MAJOR: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.MAP),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.POSITIONING),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.ROUTING),
        )
        licenseRequestMap.put("ENTER_YOUR_API_KEY", licenseRequests)
        LicenseManager.requestLicense(licenseRequestMap, this)
    }

    override fun onEnterArea(notificationResult: NotificationResult) {
        Log.d(LOG_TAG,"on enter area\n")
        Log.d(LOG_TAG,"notifaction result feature: ${notificationResult.feature.points} ${notificationResult.feature.featureType}")
        Log.d(LOG_TAG,"notifaction result distance: ${notificationResult.distance}")
        Log.d(LOG_TAG,"notifaction result closestPoint: ${notificationResult.closestPoint.x} ${notificationResult.closestPoint.y} ${notificationResult.closestPoint.f} ${notificationResult.closestPoint.coordType}")
        Log.d(LOG_TAG,"notifaction result description: ${notificationResult.desc}")
        Log.d(LOG_TAG,"notifaction result id: ${notificationResult.objectId}")
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

        //dataType 7 is required for setting notificationData to notificationService
        //we are trying to get notification of areas which notificationType is 1. This value is setted from "iNavi-Web"
        val notificationService = NotificationService(7, arrayOf("notificationType"), arrayListOf(1).toIntArray())
        Log.d(LOG_TAG, notificationService.registerListener(this).toString())

        notificationService.sendLocation(Point(5190002.199046696, 2840437.0460758056, 1.0, CoordType.EPSG3857))
    }

    override fun onError(buildingId: String, errorMessage: String) {
        Log.e(LOG_TAG, errorMessage)
    }
}
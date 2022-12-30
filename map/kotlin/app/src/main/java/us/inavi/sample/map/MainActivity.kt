package us.inavi.sample.map

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.basarsoft.inavi.libs.helper.BuildingDataType
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.BuildingLicenseRequest
import com.basarsoft.inavi.libs.licensemanager.licenserequest.ModuleLicenseRequest
import com.basarsoft.inavi.libs.packagemanager.BuildingInfo
import com.basarsoft.inavi.libs.packagemanager.DownloadListener
import com.basarsoft.inavi.libs.packagemanager.PackageManager
import com.basarsoft.yolbil.core.MapPos
import com.basarsoft.yolbil.core.StringVector
import com.basarsoft.yolbil.datasources.BlueDotDataSource
import com.basarsoft.yolbil.datasources.HTTPTileDataSource
import com.basarsoft.yolbil.datasources.MBTilesTileDataSource
import com.basarsoft.yolbil.datasources.MemoryCacheTileDataSource
import com.basarsoft.yolbil.geometry.PointGeometry
import com.basarsoft.yolbil.layers.*
import com.basarsoft.yolbil.location.LocationBuilder
import com.basarsoft.yolbil.location.LocationSource
import com.basarsoft.yolbil.projections.EPSG3857
import com.basarsoft.yolbil.search.SearchRequest
import com.basarsoft.yolbil.search.VectorTileSearchService
import com.basarsoft.yolbil.styles.CompiledStyleSet
import com.basarsoft.yolbil.ui.MapView
import com.basarsoft.yolbil.ui.VectorTileClickInfo
import com.basarsoft.yolbil.utils.AssetUtils
import com.basarsoft.yolbil.utils.ZippedAssetPackage
import com.basarsoft.yolbil.vectortiles.MBVectorTileDecoder

class MyVectorTileEventListener (var tileDecoder: MBVectorTileDecoder) : VectorTileEventListener() {
    val LOG_TAG: String = "iNavi"
    override fun onVectorTileClicked(clickInfo: VectorTileClickInfo?): Boolean {
        clickInfo?.let { info ->
            val id = info.feature.properties.getObjectElement("id").string
            val name = info.feature.properties.getObjectElement("name").string
            Log.d(LOG_TAG, "onVectorTileClicked -> name: $name, id: $id")
            tileDecoder.setStyleParameter("selectedObjectId", id)
        }
        return super.onVectorTileClicked(clickInfo)
    }
}

class MainActivity : AppCompatActivity(), DownloadListener, LicenseListener {
    val LOG_TAG: String = "iNavi"
    val BUILDING_ID: String = "ENTER_YOUR_BUILDING_ID"
    val BUILDING_VERSION_MAJOR_STRING: String = "2"
    val BUILDING_VERSION_MAJOR: Int = 2
    lateinit var mapView: MapView
    lateinit var searchService: VectorTileSearchService
    lateinit var buildingInfo: BuildingInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = this.findViewById(R.id.mapView) as MapView

        addGoogleTileLayer()

        checkPermission(arrayListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        ))

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            ModuleLicenseRequest("yolbil_map", "1"),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.MAP),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.POSITIONING),
            BuildingLicenseRequest(BUILDING_ID, BUILDING_VERSION_MAJOR_STRING, BuildingDataType.ROUTING),
        )
        licenseRequestMap.put("ENTER_YOUR_API_KEY", licenseRequests)
        LicenseManager.requestLicense(licenseRequestMap, this)
    }

    fun addGoogleTileLayer() {
        val httpTileDataSource = HTTPTileDataSource(
            0,
            20,
            "https://mt0.google.com/vt/lyrs=m&hl=tr&scale=4&apistyle=s.e%3Ag%7Cp.c%3A%23ff242f3e%2Cs.e%3Al.t.f%7Cp.c%3A%23ff746855%2Cs.e%3Al.t.s%7Cp.c%3A%23ff242f3e%2Cs.t%3A1%7Cs.e%3Ag%7Cp.v%3Aoff%2Cs.t%3A19%7Cs.e%3Al.t.f%7Cp.c%3A%23ffd59563%2Cs.t%3A2%7Cp.v%3Aoff%2Cs.t%3A2%7Cs.e%3Al.t.f%7Cp.c%3A%23ffd59563%2Cs.t%3A40%7Cs.e%3Ag%7Cp.c%3A%23ff263c3f%2Cs.t%3A40%7Cs.e%3Al.t.f%7Cp.c%3A%23ff6b9a76%2Cs.t%3A3%7Cs.e%3Ag%7Cp.c%3A%23ff38414e%2Cs.t%3A3%7Cs.e%3Ag.s%7Cp.c%3A%23ff212a37%2Cs.t%3A3%7Cs.e%3Al.i%7Cp.v%3Aoff%2Cs.t%3A3%7Cs.e%3Al.t.f%7Cp.c%3A%23ff9ca5b3%2Cs.t%3A49%7Cs.e%3Ag%7Cp.c%3A%23ff746855%2Cs.t%3A49%7Cs.e%3Ag.s%7Cp.c%3A%23ff1f2835%2Cs.t%3A49%7Cs.e%3Al.t.f%7Cp.c%3A%23fff3d19c%2Cs.t%3A4%7Cp.v%3Aoff%2Cs.t%3A4%7Cs.e%3Ag%7Cp.c%3A%23ff2f3948%2Cs.t%3A66%7Cs.e%3Al.t.f%7Cp.c%3A%23ffd59563%2Cs.t%3A6%7Cs.e%3Ag%7Cp.c%3A%23ff17263c%2Cs.t%3A6%7Cs.e%3Al.t.f%7Cp.c%3A%23ff515c6d%2Cs.t%3A6%7Cs.e%3Al.t.s%7Cp.c%3A%23ff17263c&x={x}&y={y}&z={zoom}"
        )
        val subdomains = StringVector()
        subdomains.add("1")
        subdomains.add("2")
        subdomains.add("3")
        httpTileDataSource.subdomains = subdomains
        val memorySource = MemoryCacheTileDataSource(httpTileDataSource)
        val rasterlayer = RasterTileLayer(memorySource)
        mapView.layers.add(rasterlayer)
    }

    fun addBuildingMap() {
        val styleAsset = AssetUtils.loadAsset("inavistyle.zip")
        val assetPackage = ZippedAssetPackage(styleAsset)
        val styleSet = CompiledStyleSet(assetPackage, "inavistyle")
        val tileDecoder = MBVectorTileDecoder(styleSet)

        val mbDataSource = MBTilesTileDataSource(filesDir.absolutePath + "/buildings/" + BUILDING_ID + "/" + BUILDING_VERSION_MAJOR_STRING + "/" + BUILDING_ID + ".inavi0")
        val mbTilesLayer = VectorTileLayer(mbDataSource, tileDecoder)
        mbTilesLayer.labelRenderOrder = VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST
        mbTilesLayer.vectorTileEventListener = MyVectorTileEventListener(tileDecoder)
        mapView.layers.add(mbTilesLayer)

        searchService = VectorTileSearchService(mbDataSource, tileDecoder)
        searchService.minZoom = 14
        searchService.maxZoom = 14

        val pos = MapPos(3646477.0, 4851624.0)
        mapView.setFocusPos(pos, 0f)
        mapView.setZoom(17f, 0f)
    }

    fun addBlueDot() {
        val locationSource = LocationSource()
        val blueDotDataSource = BlueDotDataSource(EPSG3857(), locationSource)
        val blueDotVectorLayer = VectorLayer(blueDotDataSource)
        mapView.layers.add(blueDotVectorLayer)

        val locationBuilder = LocationBuilder()
        locationBuilder.setCoordinate(MapPos(3646477.0, 4851624.0))
        locationBuilder.setHorizontalAccuracy(20.0)
        locationSource.updateLocation(locationBuilder.build())
    }

    override fun onLicenseSuccess() {
        Log.e(LOG_TAG, "License successful")
        Log.d(LOG_TAG, "Building Ids:")
        val buildingList = PackageManager.getBuildingList()
        buildingInfo = buildingList.filter { it.id == BUILDING_ID }[0]
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
        runOnUiThread {
            addBuildingMap()
            addBlueDot()
            search("pol")
        }
    }

    override fun onError(buildingId: String, errorMessage: String) {
        Log.e(LOG_TAG, errorMessage)
    }

    private fun checkPermission(permissions: List<String>, requestCode: Int = 100) {
        val requestPermissions = arrayListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this@MainActivity, it) == android.content.pm.PackageManager.PERMISSION_DENIED) {
                requestPermissions.add(it)
            }
        }
        if (requestPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this@MainActivity, requestPermissions.toTypedArray(), requestCode)
        }
    }

    fun search(keyword: String) {
        if (keyword.length < 2) return

        val searchRequest = SearchRequest()
        searchRequest.projection = EPSG3857()
        searchRequest.searchRadius = 1000f
        val center = buildingInfo.geoSpatial.center
        searchRequest.geometry = PointGeometry( EPSG3857().fromWgs84(MapPos(center[0], center[1])))
        searchRequest.filterExpression = "REGEXP_ILIKE(name,'(.*)${keyword}(.*)') AND categories IS NOT NULL"
        val result = searchService.findFeatures(searchRequest)
        result?.let {
            for (i in 0 until it.featureCount) {
                val feature = it.getFeature(i)
                val name = feature.properties.getObjectElement("name").string
                val floor = feature.properties.getObjectElement("floor").long
                val coords = feature.geometry.centerPos
                Log.d(LOG_TAG, "Area name: $name, name: $floor, coords: $coords")
            }
        }
    }
}

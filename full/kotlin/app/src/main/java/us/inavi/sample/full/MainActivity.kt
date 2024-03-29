package us.inavi.sample.full

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.basarsoft.inavi.libs.helper.BuildingDataType
import com.basarsoft.inavi.libs.helper.CoordType
import com.basarsoft.inavi.libs.helper.Point
import com.basarsoft.inavi.libs.licensemanager.LicenseListener
import com.basarsoft.inavi.libs.licensemanager.LicenseManager
import com.basarsoft.inavi.libs.licensemanager.LicenseRequestMap
import com.basarsoft.inavi.libs.licensemanager.licenserequest.BuildingLicenseRequest
import com.basarsoft.inavi.libs.licensemanager.licenserequest.ModuleLicenseRequest
import com.basarsoft.inavi.libs.manifest.NavigationResult
import com.basarsoft.inavi.libs.manifest.iNaviManifest
import com.basarsoft.inavi.libs.packagemanager.BuildingInfo
import com.basarsoft.inavi.libs.packagemanager.DownloadListener
import com.basarsoft.inavi.libs.packagemanager.PackageManager
import com.basarsoft.inavi.libs.positioner.Location
import com.basarsoft.inavi.libs.positioner.PositionListener
import com.basarsoft.inavi.libs.positioner.Positioner
import com.basarsoft.inavi.libs.routing.Routing
import com.basarsoft.inavi.libs.sensormanager.SensorManager
import com.basarsoft.yolbil.core.*
import com.basarsoft.yolbil.datasources.*
import com.basarsoft.yolbil.geometry.PointGeometry
import com.basarsoft.yolbil.graphics.Color
import com.basarsoft.yolbil.layers.*
import com.basarsoft.yolbil.location.LocationBuilder
import com.basarsoft.yolbil.location.LocationSource
import com.basarsoft.yolbil.projections.EPSG3857
import com.basarsoft.yolbil.search.SearchRequest
import com.basarsoft.yolbil.search.VectorTileSearchService
import com.basarsoft.yolbil.styles.CompiledStyleSet
import com.basarsoft.yolbil.styles.LineJoinType
import com.basarsoft.yolbil.styles.LineStyleBuilder
import com.basarsoft.yolbil.ui.MapView
import com.basarsoft.yolbil.ui.VectorTileClickInfo
import com.basarsoft.yolbil.utils.AssetUtils
import com.basarsoft.yolbil.utils.ZippedAssetPackage
import com.basarsoft.yolbil.vectorelements.Line
import com.basarsoft.yolbil.vectortiles.MBVectorTileDecoder
import org.json.JSONObject
import kotlin.math.sqrt

class MyVectorTileEventListener (var tileDecoder: MBVectorTileDecoder) : VectorTileEventListener() {
    val LOG_TAG: String = "iNavi"
    override fun onVectorTileClicked(clickInfo: VectorTileClickInfo?): Boolean {
        clickInfo?.let { info ->
            val id = info.feature.properties.getObjectElement("id").string
            val name = JSONObject(info.feature.properties.getObjectElement("name").string).getString("en")
            Log.d(LOG_TAG, "onVectorTileClicked -> name: $name, id: $id")
            tileDecoder.setStyleParameter("selectedObjectId", id)
        }
        return super.onVectorTileClicked(clickInfo)
    }
}

class MainActivity : AppCompatActivity(), DownloadListener, LicenseListener {
    lateinit var positioner: Positioner
    lateinit var routing: Routing
    lateinit var locationSource: LocationSource
    lateinit var manifest: iNaviManifest
    lateinit var tileDecoder: MBVectorTileDecoder
    lateinit var routeJson: JSONObject
    lateinit var completedLine: Line
    lateinit var incompletedLine: Line
    lateinit var searchService: VectorTileSearchService
    lateinit var buildingInfo: BuildingInfo
    var floor: Int = 0

    val LOG_TAG: String = "iNavi"
    val BUILDING_ID: String = "ENTER_YOUR_BUILDING_ID"
    val BUILDING_VERSION_MAJOR_STRING: String = "2"
    val BUILDING_VERSION_MAJOR: Int = 2
    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = this.findViewById(R.id.mapView) as MapView

        addGoogleTileLayer()
        mapView.isDeviceOrientationFocused = true

        val permissionList = arrayListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            SensorManager.startBeaconScan(this)
        }
        checkPermission(permissionList)

        routing = Routing(getRoutingOptions())

        positioner = Positioner(CoordType.EPSG3857)
        positioner.registerListener(object : PositionListener {
            override fun onLocationChanged(location: Location) {
                Log.e(LOG_TAG, "Location x: " + location.x + ", y: " + location.y + ", f: " + location.f)

                if (floor != location.f.toInt()) {
                    floor = location.f.toInt()
                    tileDecoder.setStyleParameter("floorIndex", floor.toString())
                }

                val locationBuilder = LocationBuilder()
                locationBuilder.setCoordinate(MapPos(location.x, location.y))
                locationBuilder.setHorizontalAccuracy(location.accuracy.toDouble())
                locationSource.updateLocation(locationBuilder.build())

                if (location.accuracy < 13) {
                    mapView.setZoom(21f, 1f)
                    mapView.setFocusPos(MapPos(location.x, location.y), 1f)
                } else {
                    val minScreen = ScreenPos()
                    val maxScreen = ScreenPos(mapView.width.toFloat(), mapView.height.toFloat())
                    val screenPos = ScreenBounds(minScreen, maxScreen)
                    val crossDist = location.accuracy * sqrt(2f)
                    val minMapPos = MapPos(
                        location.x - crossDist,
                        location.y - crossDist,
                    )
                    val maxMapPos = MapPos(
                        location.x + crossDist,
                        location.y + crossDist,
                    )
                    val mapBound = MapBounds(minMapPos, maxMapPos)
                    mapView.moveToFitBounds(mapBound, screenPos, false, false, false, 1f)
                }

                val navResult = manifest.queryManifest(Point(location.x, location.y, location.f, CoordType.EPSG3857))
                Log.d(LOG_TAG, "distance: " + navResult.distance)
                Log.d(LOG_TAG, "command enum: " + navResult.commandEnum.name)
                Log.d(LOG_TAG, "command type: " + navResult.commandType.name)
                Log.d(LOG_TAG, "edge type: " + navResult.edgeType.name)
                Log.d(LOG_TAG, "next command enum: " + navResult.nextCommandEnum.name)
                Log.d(LOG_TAG, "next edge type: " + navResult.nextEdgeType.name)
                drawRoute(navResult)
            }
        })

        val licenseRequestMap = LicenseRequestMap()
        val licenseRequests = arrayListOf(
            ModuleLicenseRequest("yolbil_map", "1"),
            ModuleLicenseRequest("inavi_positioner", "1"),
            ModuleLicenseRequest("inavi_sensormanager", "1"),
            ModuleLicenseRequest("inavi_routing", "1"),
            ModuleLicenseRequest("inavi_manifest", "1"),
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
        tileDecoder = MBVectorTileDecoder(styleSet)

        val mbDataSource = MBTilesTileDataSource(filesDir.absolutePath + "/buildings/" + BUILDING_ID + "/" + BUILDING_VERSION_MAJOR_STRING + "/" + BUILDING_ID + ".inavi0")
        val mbTilesLayer = VectorTileLayer(mbDataSource, tileDecoder)
        mbTilesLayer.labelRenderOrder = VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST
        mbTilesLayer.vectorTileEventListener = MyVectorTileEventListener(tileDecoder)
        mapView.layers.add(mbTilesLayer)

        searchService = VectorTileSearchService(mbDataSource, tileDecoder)
        searchService.minZoom = 14
        searchService.maxZoom = 14

        val localVectorDataSource = LocalVectorDataSource(EPSG3857())
        val completedMapPosVector = MapPosVector()
        val incompletedMapPosVector = MapPosVector()
        val lineStyleBuilderIncompleted = LineStyleBuilder()
        lineStyleBuilderIncompleted.color = Color(-0xff5031)
        lineStyleBuilderIncompleted.width = 15f
        lineStyleBuilderIncompleted.lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND
        incompletedLine = Line(incompletedMapPosVector, lineStyleBuilderIncompleted.buildStyle())
        localVectorDataSource.add(incompletedLine)
        val lineStyleBuilderCompleted = LineStyleBuilder()
        lineStyleBuilderCompleted.color = Color((-0x80ff5031).toInt())
        lineStyleBuilderCompleted.width = 15f
        lineStyleBuilderCompleted.lineJoinType = LineJoinType.LINE_JOIN_TYPE_ROUND
        completedLine = Line(completedMapPosVector, lineStyleBuilderCompleted.buildStyle())
        localVectorDataSource.add(completedLine)
        val routingLayer = VectorLayer(localVectorDataSource);
        mapView.layers.add(routingLayer)

        val pos = MapPos(3646477.0, 4851624.0)
        mapView.setFocusPos(pos, 0f)
        mapView.setZoom(17f, 0f)

        tileDecoder.setStyleParameter("language", "tr")
    }

    fun addBlueDot() {
        locationSource = LocationSource()
        val blueDotDataSource = BlueDotDataSource(EPSG3857(), locationSource)
        val blueDotVectorLayer = VectorLayer(blueDotDataSource)
        mapView.layers.add(blueDotVectorLayer)
    }

    fun createRouteAndManifest() {
        val points = arrayOf(
            Point(3646252.1,4851479.9, -1.0, CoordType.EPSG3857),
            Point(3646369.9, 4851579.1, 0.0, CoordType.EPSG3857),
        )

        val routeGeoJSON = routing.calculateRouteAsGeoJSON(points, CoordType.EPSG3857)
        routeJson = JSONObject(routeGeoJSON)
        manifest = iNaviManifest(routeGeoJSON, 1.0)
        val staticManifest = manifest.getStaticManifestResult(this,"en")
        staticManifest.manifestResults.forEach {
            Log.d(LOG_TAG, it.stringCommand)
        }
    }

    override fun onLicenseSuccess() {
        Log.e(LOG_TAG, "License successful")
        Log.d(LOG_TAG, "Building Ids:")
        val buildingList = PackageManager.getBuildingList()
        buildingInfo = buildingList.filter { it.id == BUILDING_ID }[0]
        buildingList.forEach { buildingInfo ->
            Log.d(LOG_TAG, "Building Id: " + buildingInfo.id + ", version: " + buildingInfo.version +", name: " + buildingInfo.languageMap.getValue("en").name)
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
            PackageManager.selectBuildingData(BUILDING_ID, BUILDING_VERSION_MAJOR)
            createRouteAndManifest()
            positioner.enable()
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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

    fun getRoutingOptions(): String {
        return """
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
    }

    fun drawRoute(navResult: NavigationResult) {
        val completedMapPosVector = MapPosVector()
        val incompletedMapPosVector = MapPosVector()

        val edgeIndex = navResult.realIndexOfEdge
        val snappedPoint = navResult.endPoint

        var snappedPointX = snappedPoint.x;
        var snappedPointY = snappedPoint.y;
        var snappedPointF = snappedPoint.f;

        for (i in 0 .. edgeIndex) {
            val coords = routeJson.getJSONArray("features").getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates")
            if (floor == coords.getJSONArray(0).getDouble(2).toInt()) {
                completedMapPosVector.add(
                    MapPos(coords.getJSONArray(0).getDouble(0), coords.getJSONArray(0).getDouble(1)),
                )
                break;
            }
        }

        for (i in 0 until edgeIndex) {
            val coords = routeJson.getJSONArray("features").getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates")
            if (floor == coords.getJSONArray(1).getDouble(2).toInt()) {
                completedMapPosVector.add(
                    MapPos(coords.getJSONArray(1).getDouble(0), coords.getJSONArray(1).getDouble(1)),
                )
            }
        }

        if (floor == snappedPointF.toInt()) {
            completedMapPosVector.add(MapPos(snappedPointX, snappedPointY));

            incompletedMapPosVector.add(MapPos(snappedPointX, snappedPointY));
        }

        for (i in edgeIndex until routeJson.getJSONArray("features").length()) {
            val coords = routeJson.getJSONArray("features").getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates")
            if (floor == coords.getJSONArray(1).getDouble(2).toInt()) {
                incompletedMapPosVector.add(
                    MapPos(coords.getJSONArray(1).getDouble(0), coords.getJSONArray(1).getDouble(1)),
                )
            }
        }

        incompletedLine.setPoses(incompletedMapPosVector);
        completedLine.setPoses(completedMapPosVector);
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

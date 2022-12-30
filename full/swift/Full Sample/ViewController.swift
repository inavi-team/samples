import UIKit
import YolbilMobileSDK
import INVSensorManager
import INVPositioner
import INVRouting
import INVManifest
import INVNotificationService
import INVLicenseManager
import INVPackageManager

class ViewController: UIViewController {
    var mapView: YBMapView!
    var positioner: INVPositioner!
    var routing: INVRouting!
    var locationSource: YBLocationSource!
    var manifest: INVManifest!
    var tileDecoder: YBMBVectorTileDecoder!
    var routeJson: [String:AnyObject]!
    var completedLine: YBLine!
    var incompletedLine: YBLine!
    var licenseListener: LicenseListener!
    var positionListener: PositionListener!
    var vectorTileEventListener: VectorTileEventListener!
    var searchService: YBVectorTileSearchService!
    var buildingInfo: INVBuildingInfo!
    var floor: Int = 0
    

    override func viewDidLoad() {
        super.viewDidLoad()
        mapView = YBMapView()
        view = mapView
        
        addGoogleTileLayer()
        mapView.setDeviceOrientationFocused(true)
        
        // INVSensorManager.startBeaconScan(self)
        
        routing = INVRouting(configJson: getRoutingOptions())
        
        positioner = INVPositioner(coordType: EPSG3857)
        positionListener = PositionListener(vc: self)
        positioner.registerListener(listener: positionListener)
        
        let licenseRequestMap = INVLicenseRequestMap()
        let licenseRequests: [INVLicenseRequest] = [
            INVModuleLicenseRequest(type: "yolbil_map", version: "1"),
            INVModuleLicenseRequest(type: "inavi_positioner", version: "1"),
            INVModuleLicenseRequest(type: "inavi_sensormanager", version: "1"),
            INVModuleLicenseRequest(type: "inavi_routing", version: "1"),
            INVModuleLicenseRequest(type: "inavi_manifest", version: "1"),
            INVBuildingLicenseRequest(id: Const.BUILDING_ID, version: Const.BUILDING_VERSION_MAJOR_STRING, type: MAP),
            INVBuildingLicenseRequest(id: Const.BUILDING_ID, version: Const.BUILDING_VERSION_MAJOR_STRING, type: POSITIONING),
            INVBuildingLicenseRequest(id: Const.BUILDING_ID, version: Const.BUILDING_VERSION_MAJOR_STRING, type: ROUTING),
        ]
        licenseRequestMap.put(apiKey: "ENTER_YOUR_API_KEY", licenseRequests: licenseRequests)
        licenseListener = LicenseListener(vc: self)
        INVLicenseManager.requestLicense(licenseRequestMap: licenseRequestMap, licenseListener: licenseListener)
    }
    
    func addGoogleTileLayer() {
        let httpTileDataSource: YBHTTPTileDataSource = YBHTTPTileDataSource(
            minZoom: 0,
            maxZoom: 20,
            baseURL: "https://mt0.google.com/vt/lyrs=m&hl=tr&scale=4&apistyle=s.e%3Ag%7Cp.c%3A%23ff242f3e%2Cs.e%3Al.t.f%7Cp.c%3A%23ff746855%2Cs.e%3Al.t.s%7Cp.c%3A%23ff242f3e%2Cs.t%3A1%7Cs.e%3Ag%7Cp.v%3Aoff%2Cs.t%3A19%7Cs.e%3Al.t.f%7Cp.c%3A%23ffd59563%2Cs.t%3A2%7Cp.v%3Aoff%2Cs.t%3A2%7Cs.e%3Al.t.f%7Cp.c%3A%23ffd59563%2Cs.t%3A40%7Cs.e%3Ag%7Cp.c%3A%23ff263c3f%2Cs.t%3A40%7Cs.e%3Al.t.f%7Cp.c%3A%23ff6b9a76%2Cs.t%3A3%7Cs.e%3Ag%7Cp.c%3A%23ff38414e%2Cs.t%3A3%7Cs.e%3Ag.s%7Cp.c%3A%23ff212a37%2Cs.t%3A3%7Cs.e%3Al.i%7Cp.v%3Aoff%2Cs.t%3A3%7Cs.e%3Al.t.f%7Cp.c%3A%23ff9ca5b3%2Cs.t%3A49%7Cs.e%3Ag%7Cp.c%3A%23ff746855%2Cs.t%3A49%7Cs.e%3Ag.s%7Cp.c%3A%23ff1f2835%2Cs.t%3A49%7Cs.e%3Al.t.f%7Cp.c%3A%23fff3d19c%2Cs.t%3A4%7Cp.v%3Aoff%2Cs.t%3A4%7Cs.e%3Ag%7Cp.c%3A%23ff2f3948%2Cs.t%3A66%7Cs.e%3Al.t.f%7Cp.c%3A%23ffd59563%2Cs.t%3A6%7Cs.e%3Ag%7Cp.c%3A%23ff17263c%2Cs.t%3A6%7Cs.e%3Al.t.f%7Cp.c%3A%23ff515c6d%2Cs.t%3A6%7Cs.e%3Al.t.s%7Cp.c%3A%23ff17263c&x={x}&y={y}&z={zoom}"
        )
        let subdomains: YBStringVector = YBStringVector()
        subdomains.add("1")
        subdomains.add("2")
        subdomains.add("3")
        httpTileDataSource.setSubdomains(subdomains)
        let memorySource: YBMemoryCacheTileDataSource = YBMemoryCacheTileDataSource(dataSource: httpTileDataSource)
        let rasterlayer: YBRasterTileLayer = YBRasterTileLayer(dataSource: memorySource)
        mapView.getLayers().add(rasterlayer)
    }
    
    func addBuildingMap() {
        let styleAsset = YBAssetUtils.loadAsset("inavistyle.zip")
        let assetPackage = YBZippedAssetPackage(zip: styleAsset)
        let styleSet = YBCompiledStyleSet(assetPackage: assetPackage, styleName: "inavistyle")
        tileDecoder = YBMBVectorTileDecoder(compiledStyleSet: styleSet)

        let documentDir = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
        let buildingPath = "/buildings/" + Const.BUILDING_ID + "/" + Const.BUILDING_VERSION_MAJOR_STRING + "/" + Const.BUILDING_ID + ".inavi0"
        let mbDataSource = YBMBTilesTileDataSource(path: documentDir + buildingPath)
        let mbTilesLayer: YBVectorTileLayer = YBVectorTileLayer(dataSource: mbDataSource, decoder: tileDecoder)
        mbTilesLayer.setLabel(YBVectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST)
        vectorTileEventListener = VectorTileEventListener(tileDecoder: tileDecoder)
        mbTilesLayer.setVectorTileEventListener(vectorTileEventListener)
        mapView.getLayers().add(mbTilesLayer)
        
        searchService = YBVectorTileSearchService(dataSource: mbDataSource, tileDecoder: tileDecoder)
        searchService.setMinZoom(14)
        searchService.setMaxZoom(14)

        let localVectorDataSource: YBLocalVectorDataSource = YBLocalVectorDataSource(projection: YBEPSG3857())
        let completedMapPosVector = YBMapPosVector()
        let incompletedMapPosVector = YBMapPosVector()
        let lineStyleBuilderIncompleted: YBLineStyleBuilder = YBLineStyleBuilder()
        lineStyleBuilderIncompleted.setColor(YBColor(color: -0xff5031))
        lineStyleBuilderIncompleted.setWidth(15)
        lineStyleBuilderIncompleted.setLineJoinType(YBLineJoinType.LINE_JOIN_TYPE_ROUND)
        incompletedLine = YBLine(poses: incompletedMapPosVector, style: lineStyleBuilderIncompleted.buildStyle())
        localVectorDataSource.add(incompletedLine)
        let lineStyleBuilderCompleted: YBLineStyleBuilder = YBLineStyleBuilder()
        lineStyleBuilderCompleted.setColor(YBColor(color: Int32(truncatingIfNeeded: Int(-0x80ff5031))))
        lineStyleBuilderCompleted.setWidth(15)
        lineStyleBuilderCompleted.setLineJoinType(YBLineJoinType.LINE_JOIN_TYPE_ROUND)
        completedLine = YBLine(poses: completedMapPosVector, style: lineStyleBuilderCompleted.buildStyle())
        localVectorDataSource.add(completedLine)
        let routingLayer = YBVectorLayer(dataSource: localVectorDataSource);
        mapView.getLayers().add(routingLayer)

        let pos = YBMapPos(x: 3646477.0, y: 4851624.0)
        mapView.setFocus(pos, durationSeconds: 0)
        mapView.setZoom(17, durationSeconds: 0)
    }

    func addBlueDot() {
        locationSource = YBLocationSource()
        let blueDotDataSource = YBBlueDotDataSource(projection: YBEPSG3857(), locationSource: locationSource)
        let blueDotVectorLayer = YBVectorLayer(dataSource: blueDotDataSource)
        mapView.getLayers().add(blueDotVectorLayer)
    }

    func createRouteAndManifest() {
        let points: [INVPoint] = [
            INVPoint(coordX: 3646252.1,coordY: 4851479.9, coordZ: -1.0, coordType: EPSG3857),
            INVPoint(coordX: 3646369.9, coordY: 4851579.1, coordZ: 0.0, coordType: EPSG3857),
        ]

        let routeGeoJSON = routing.calculateRouteAsGeoJSON(points: points, coordType: EPSG3857)
        routeJson = convertStringToDictionary(text: routeGeoJSON)
        manifest = INVManifest(routeJson: routeGeoJSON, cellLength: 1.0)
        let staticManifest = manifest.getStaticManifestResult()
        staticManifest.manifestResults.forEach { result in
            print(Const.LOG_TAG, result.stringCommand)
        }
    }

    func getRoutingOptions() -> String {
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
            """
    }
    
    func drawRoute(navResult: INVNavigationResult) {
        let completedMapPosVector: YBMapPosVector = YBMapPosVector()
        let incompletedMapPosVector: YBMapPosVector = YBMapPosVector()

        let edgeIndex = navResult.realIndexOfEdge
        let snappedPoint = navResult.endPoint!

        let snappedPointX = snappedPoint.coordX;
        let snappedPointY = snappedPoint.coordY;
        let snappedPointF = snappedPoint.coordZ;

        for index in 0...edgeIndex {
            let i: Int = Int(index)
            let coords = ((routeJson["features"] as! [AnyObject])[i]["geometry"] as! [String : AnyObject])["coordinates"] as! [[Double]]
            if (floor == Int(coords[0][2])) {
                completedMapPosVector.add(
                    YBMapPos(x: coords[0][0], y: coords[0][1])
                )
                break;
            }
        }

        if (edgeIndex > 0) {
            for index in 0...edgeIndex-1 {
                let i: Int = Int(index)
                let coords = ((routeJson["features"] as! [AnyObject])[i]["geometry"] as! [String : AnyObject])["coordinates"] as! [[Double]]
                if (floor == Int(coords[1][2])) {
                    completedMapPosVector.add(
                        YBMapPos(x: coords[1][0], y: coords[1][1])
                    )
                }
            }
        }

        if (floor == Int(snappedPointF)) {
            completedMapPosVector.add(YBMapPos(x: snappedPointX, y: snappedPointY));

            incompletedMapPosVector.add(YBMapPos(x: snappedPointX, y: snappedPointY));
        }

        let featureCount = Int32((routeJson["features"] as! [AnyObject]).count)
        for index in edgeIndex...featureCount - 1 {
            let i: Int = Int(index)
            let coords = ((routeJson["features"] as! [AnyObject])[i]["geometry"] as! [String : AnyObject])["coordinates"] as! [[Double]]
            if (floor == Int(coords[1][2])) {
                incompletedMapPosVector.add(
                    YBMapPos(x: coords[1][0], y: coords[1][1])
                )
            }
        }

        incompletedLine.setPoses(incompletedMapPosVector);
        completedLine.setPoses(completedMapPosVector);
    }
    
    func search(keyword: String) {
        if (keyword.count < 2) { return }

        let searchRequest = YBSearchRequest()!
        searchRequest.setProjection(YBEPSG3857())
        searchRequest.setSearchRadius(1000)
        let center = buildingInfo.geoSpatial.center as! [Double]
        searchRequest.setGeometry(YBPointGeometry(pos: YBEPSG3857().fromWgs84(YBMapPos(x: center[0], y: center[1]))))
        searchRequest.setFilterExpression("REGEXP_ILIKE(name,'(.*)\(keyword)(.*)') AND categories IS NOT NULL")
        let result = searchService.findFeatures(searchRequest)
        if let it = result {
            for i in 0...it.getFeatureCount() - 1 {
                let feature = it.getFeature(i)!
                let name = feature.getProperties().getObjectElement("name").getString()!
                let floor = feature.getProperties().getObjectElement("floor").getLong()
                let coords = feature.getGeometry().getCenterPos()!
                print("Area name: \(name), floor: \(floor), coords: \(coords)")
            }
        }
    }

    
    func convertStringToDictionary(text: String) -> [String:AnyObject]? {
        if let data = text.data(using: .utf8) {
            do {
                let json = try JSONSerialization.jsonObject(with: data, options: .mutableContainers) as? [String:AnyObject]
                return json
            } catch {
                print("Something went wrong")
            }
        }
        return nil
    }
}


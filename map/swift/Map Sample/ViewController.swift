import UIKit
import YolbilMobileSDK
import INVLicenseManager
import INVPackageManager

class ViewController: UIViewController {
    var mapView: YBMapView!
    var tileDecoder: YBMBVectorTileDecoder!
    var licenseListener: LicenseListener!
    var vectorTileEventListener: VectorTileEventListener!
    var searchService: YBVectorTileSearchService!
    var buildingInfo: INVBuildingInfo!

    override func viewDidLoad() {
        super.viewDidLoad()
        mapView = YBMapView()
        view = mapView
        
        addGoogleTileLayer()
        
        let licenseRequestMap = INVLicenseRequestMap()
        let licenseRequests: [INVLicenseRequest] = [
            INVModuleLicenseRequest(type: "yolbil_map", version: "1"),
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

        let pos = YBMapPos(x: 3646477.0, y: 4851624.0)
        mapView.setFocus(pos, durationSeconds: 0)
        mapView.setZoom(17, durationSeconds: 0)
    }
    
    func addBlueDot() {
        let locationSource = YBLocationSource()!
        let blueDotDataSource = YBBlueDotDataSource(projection: YBEPSG3857(), locationSource: locationSource)
        let blueDotVectorLayer = YBVectorLayer(dataSource: blueDotDataSource)
        mapView.getLayers().add(blueDotVectorLayer)
        
        let locationBuilder = YBLocationBuilder()!
        locationBuilder.setCoordinate(YBMapPos(x: 3646477.0, y: 4851624.0))
        locationBuilder.setHorizontalAccuracy(20.0)
        locationSource.update(locationBuilder.build())
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
}

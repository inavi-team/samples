import UIKit
import INVLicenseManager
import INVManifest

class ViewController: UIViewController {
    var licenseListener: LicenseListener!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let licenseRequestMap = INVLicenseRequestMap()
        let licenseRequests: [INVLicenseRequest] = [
            INVModuleLicenseRequest(type: "inavi_manifest", version: "1")
        ]
        licenseRequestMap.put(apiKey: "ENTER_YOUR_API_KEY", licenseRequests: licenseRequests)
        licenseListener = LicenseListener()
        INVLicenseManager.requestLicense(licenseRequestMap: licenseRequestMap, licenseListener: licenseListener)
        
        let manifest = INVManifest(routeJson: getRoutingGeoJSONResult(), cellLength: 1.0)
        let staticManifest = manifest.getStaticManifestResult()
        staticManifest.manifestResults.forEach { result in
            print(result.stringCommand)
        }
    }

    func getRoutingGeoJSONResult() -> String {
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
        """
    }
}


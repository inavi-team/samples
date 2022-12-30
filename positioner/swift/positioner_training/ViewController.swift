import INVPositioner
import INVLicenseManager
import INVPackageManager
import INVSensorManager
import UIKit


var sensorListener: IOSSensorListener!
var licenceListener: INVLicenseListener!
var downloadListener: INVDownloadListener!

class ViewController: UIViewController {

    class MyDownloadListener : INVDownloadListener {
        var positioner: INVPositioner!
        var positionerListener: IOSPositionListener!
        override func onProgress(buildingId: String, percentage: Int) {
            print("onProgress: \(buildingId), percentage: \(percentage)")
        }
        
        override func onCompleted(buildingId: String) {
            print("onCompleted:", buildingId)
            
            INVPackageManager.selectBuildingData(buildingId: "ENTER_YOUR_BUILDING_ID", versionMajor: 2)
            
            positioner = INVPositioner(coordType: EPSG3857)
            positionerListener = IOSPositionListener()
            positioner.registerListener(listener: positionerListener)
            positioner.enable()

        }
        
        override func onError(buildingId: String, errorMessage: String) {
            print("onError: \(buildingId), message: \(errorMessage)")
        }
    }
    
    class MyLicenseListener : INVLicenseListener {

        var downloadListener: INVDownloadListener!
        override func onLicenseSuccess() {
            print("playground onLicenseSuccess")
            let buildingList = INVPackageManager.getBuildingList()
            
            for buildingInfo in buildingList {
                print("buildingInfo: " + buildingInfo.id)
            }
            
            downloadListener = MyDownloadListener()

            INVPackageManager.downloadBuildingData(
                buildingId: "ENTER_YOUR_BUILDING_ID",
                versionMajor: 2,
                buildingDataTypes: [MAP, ROUTING, POSITIONING],
                listener: downloadListener
            )
            
        }

        override func onLicenseError(message: String) {
            print("playground onLicenseError: " + message)
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        sensorListener = IOSSensorListener()
        licenceListener = MyLicenseListener()


        let licenseRequestMap = INVLicenseRequestMap()
        let licenseRequests: [INVLicenseRequest] = [
            INVModuleLicenseRequest(type: "yolbil_map", version: "1"),
            INVModuleLicenseRequest(type: "inavi_positioner", version: "1"),
            INVModuleLicenseRequest(type: "inavi_routing", version: "1"),
            INVModuleLicenseRequest(type: "inavi_manifest", version: "1"),
            
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "2", type: MAP),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "2", type: POSITIONING),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "2", type: ROUTING),
            ]
        licenseRequestMap.put(apiKey: "ENTER_YOUR_API_KEY", licenseRequests: licenseRequests)
        INVLicenseManager.requestLicense(licenseRequestMap: licenseRequestMap, licenseListener: licenceListener)

        
    }


}


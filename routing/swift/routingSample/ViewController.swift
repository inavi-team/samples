import UIKit
import INVPackageManager
import INVLicenseManager
import INVRouting
import INVObjects
import INVManifest

var downloadListener: MyDownloadListener!
var licenseListener: MyLicenseListener!
var routing: INVRouting!

//This listener is using for download states of building data
class MyDownloadListener : INVDownloadListener {
    //While a building data is downloading, this callback is called
    override func onProgress(buildingId: String, percentage: Int) {
        print("onProgress: \(buildingId), percentage: \(percentage)")
    }
    //When downloading building data is completed, this callback is called
    override func onCompleted(buildingId: String) {
        print("onCompleted:", buildingId)
        //Selecting downlaoded building data
        INVPackageManager.selectBuildingData(buildingId: "ENTER_YOUR_BUILDING_ID", versionMajor: 2)
        
        /*Creating routing object
         configJson: is generally using constant. This field is for creating profiles. Routing will calculate special routes for every profiles
        */
        routing = INVRouting(configJson: """
{
                             "tesselationdistance": 5,
                             "pathstraightening": true,
                             "zsensitivity": 1000,
                             "min_turnangle": 5,
                             "min_updownangle": 45,
                             "rules": [
                               {
                                 "speed": 1
                               },
                               {
                                 "profiles": [
                                   "visitor"
                                 ],
                                 "filters": [
                                   {
                                     "visitor": false
                                   }
                                 ],
                                 "speed": 0
                               },
                               {
                                 "profiles": [
                                   "wheelchair"
                                 ],
                                 "filters": [
                                   {
                                     "wheelchair": false
                                   }
                                 ],
                                 "speed": 0
                               },
                               {
                                 "profiles": [
                                   "staff"
                                 ],
                                 "filters": [
                                   {
                                     "staff": false
                                   }
                                 ],
                                 "speed": 0
                               },
                               {
                                 "profiles": [
                                   "vehicle"
                                 ],
                                 "filters": [
                                   {
                                     "vehicle": false
                                   }
                                 ],
                                 "speed": 0
                               }
                             ]
                           }
""")
        
        var points = [INVPoint]()
        //Start point for routing
        points.append(INVPoint(coordX: 5190037.868829081, coordY: 2840442.240174248, coordZ: 0.0))
        //End point for routing
        points.append(INVPoint(coordX: 5190014.364326005, coordY: 2840565.9084912036, coordZ: 0.0))
        
        /*Calculating route with start point, end point, coordType and profile
         If you want to draw this route on map, you can take a look at "Full Sample App"
         */
        let routingRes = routing.calculateRouteAsGeoJSON(points: points, coordType: EPSG3857, profile: "visitor")
        
        print("Routing Result = ", routingRes);
        
        //Giving route to manifest object for calculating direction commands
        let manifest = INVManifest(routeJson: routingRes, cellLength: 1.0)

        //Getting static direction commands
        let staticManifest = manifest.getStaticManifestResult()
        let arr = staticManifest.manifestResults!
        
        //The total distance of route
        print("Distance(meter): ", staticManifest.distance, "\n")
        
        for val in arr {
            //printing command sentences and distance of performing command
            print("\n" + val.stringCommand + ": " + String(format: "%d", val.distance))
            //Basic sentences of command
            print(val.stringBasicCommand)
            //Enum of command
            print(val.navigationCommandEnum)
        }
        
        var time = 0.0;
        
        //These points are fake points for simulating user's walk path on route
        let simulatedPoints:[INVPoint] = [
            INVPoint(coordX: 5190032.588103076, coordY: 2840443.798932999, coordZ: 0.0),
            INVPoint(coordX: 5190027.3953997, coordY: 2840443.470657682, coordZ: 0.0),
            INVPoint(coordX: 5190020.667865119, coordY: 2840443.6001404086, coordZ: 0.0),
            INVPoint(coordX: 5190013.446393871, coordY: 2840443.3963252148, coordZ: 0.0),
            INVPoint(coordX: 5190007.821400579, coordY: 2840443.483692567, coordZ: 0.0),
            INVPoint(coordX: 5190007.766795883, coordY: 2840449.3840900213, coordZ: 0.0),
            INVPoint(coordX: 5190004.383723546, coordY: 2840452.852275449, coordZ: 0.0),
            INVPoint(coordX: 5190004.667162687, coordY: 2840460.0892926096, coordZ: 0.0),
            INVPoint(coordX: 5190004.896545145, coordY: 2840469.292683911, coordZ: 0.0),
            INVPoint(coordX: 5190005.259701587, coordY: 2840484.328055435, coordZ: 0.0),
            INVPoint(coordX: 5190006.241182455, coordY: 2840521.0756734167, coordZ: 0.0),
            INVPoint(coordX: 5190006.122716145, coordY: 2840536.891660028, coordZ: 0.0),
            INVPoint(coordX: 5190007.055151018, coordY: 2840549.4880041545, coordZ: 0.0),
            INVPoint(coordX: 5190003.113400955, coordY: 2840556.956291116, coordZ: 0.0),
            INVPoint(coordX: 5190003.048757326, coordY: 2840563.632493267, coordZ: 0.0),
            INVPoint(coordX: 5190008.624765145, coordY: 2840563.3927463153, coordZ: 0.0),
        ]
        
         /*These codes simulating the navigation. It simulates as if the user is walking.
         Simulating the fake location for manifest step by step. Every point is selected manually.
         If you want to draw this fake location o the map, you can take a look at "Full Sample App".
         */
        for point in simulatedPoints {
            DispatchQueue.main.asyncAfter(deadline: .now() + time) {
                //This is a fake location of user.
                let dynamic = manifest.queryDynamicManifest(point: point)
                //The current command sentence to say.
                print("\n" + dynamic.generateTTSString())
                print("\n ttstring", dynamic.getTTSString()!)
                //Enum of a current command
                print("\n commandEnum", dynamic.commandEnum)
                //Enum of a current commandtype
                print("\n commandType", dynamic.commandType)
             }
            //This is for simulating user's walking, in every 5 seconds simulating the fake location.
            time += 5.0;
        }
        
    }
    
    //When an error occured while downloading building data, this callback is called
    override func onError(buildingId: String, errorMessage: String) {
    }
}
//This listener is using for states of license request
class MyLicenseListener : INVLicenseListener {
    //When a licence request succeeded, this callback is called
    override func onLicenseSuccess() {
        print("playground onLicenseSuccess")
        let buildingList = INVPackageManager.getBuildingList()

        for buildingInfo in buildingList {
            print("buildingInfo: " + buildingInfo.id)
        }
        

        INVPackageManager.downloadBuildingData(
            buildingId: "ENTER_YOUR_BUILDING_ID",
            versionMajor: 2,
            buildingDataTypes: [MAP, ROUTING, POSITIONING],
            listener: downloadListener
        )
    }

    //When an error occured while getting license, this callback is called
    override func onLicenseError(message: String) {
        print("playground onLicenseError: " + message)
    }
}


class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        
        downloadListener = MyDownloadListener()
        licenseListener = MyLicenseListener()

        //Making license requests for modules and building data
        let licenseRequestMap = INVLicenseRequestMap()
        let licenseRequests: [INVLicenseRequest] = [
            INVModuleLicenseRequest(type: "yolbil_map", version: "1"),
            INVModuleLicenseRequest(type: "inavi_routing", version: "1"),
            INVModuleLicenseRequest(type: "inavi_manifest", version: "1"),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "2", type: MAP),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "2", type: POSITIONING),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "2", type: ROUTING),
            ]
        
        licenseRequestMap.put(apiKey: "ENTER_YOUR_API_KEY", licenseRequests: licenseRequests)
        INVLicenseManager.requestLicense(licenseRequestMap: licenseRequestMap, licenseListener: licenseListener)
    }


}


import Foundation
import INVLicenseManager
import INVPackageManager

class LicenseListener: INVLicenseListener {
    var vc: ViewController!
    var downloadListener: DownloadListener!
    
    init(vc: ViewController) {
        super.init()
        self.vc = vc
        self.downloadListener = DownloadListener(vc: vc)
    }

    override func onLicenseSuccess() {
        print("License successful")
        print("Building Ids:")
        let buildingList: [INVBuildingInfo] = INVPackageManager.getBuildingList()
        self.vc.buildingInfo = buildingList.filter { $0.id == Const.BUILDING_ID }[0]
        buildingList.forEach { buildingInfo in
            print("Building Id: " + buildingInfo.id + ", version: " + buildingInfo.version)
        }
        INVPackageManager.downloadBuildingData(
            buildingId: Const.BUILDING_ID,
            versionMajor: Int32(Const.BUILDING_VERSION_MAJOR),
            buildingDataTypes: [MAP, ROUTING, POSITIONING],
            listener: downloadListener
        )
    }

    override func onLicenseError(message: String) {
        print("License failed")
    }

}

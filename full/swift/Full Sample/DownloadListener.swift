import Foundation
import INVPackageManager

class DownloadListener: INVDownloadListener {
    var vc: ViewController!
    
    init(vc: ViewController) {
        super.init()
        self.vc = vc
    }
    override func onProgress(buildingId: String, percentage: Int) {
        print("Download progress: \(percentage)")
    }
    override func onCompleted(buildingId: String) {
        print("Download completed")
        DispatchQueue.main.async() {
            self.vc.addBuildingMap()
            self.vc.addBlueDot()
            INVPackageManager.selectBuildingData(buildingId: Const.BUILDING_ID, versionMajor: Int32(Const.BUILDING_VERSION_MAJOR))
            self.vc.createRouteAndManifest()
            self.vc.positioner.enable()
            self.vc.search(keyword: "pol")
        }
    }
    override func onError(buildingId: String, errorMessage: String) {
        print("onError: \(buildingId), message: \(errorMessage)")
    }
}

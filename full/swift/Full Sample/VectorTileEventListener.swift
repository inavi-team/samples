import Foundation
import YolbilMobileSDK

class VectorTileEventListener : YBVectorTileEventListener {
    var tileDecoder: YBMBVectorTileDecoder!
    
    init (tileDecoder: YBMBVectorTileDecoder) {
        super.init()
        self.tileDecoder = tileDecoder
    }
    
    override init!(cptr: UnsafeMutableRawPointer!, swigOwnCObject ownCObject: Bool) {
        super.init(cptr: cptr, swigOwnCObject: ownCObject)
    }
    
    override func onVectorTileClicked(_ clickInfo: YBVectorTileClickInfo?) -> Bool {
        if let info = clickInfo {
            let id: String = info.getFeature().getProperties().getObjectElement("id").getString()
            let name: String = info.getFeature().getProperties().getObjectElement("name").getString()
            print("onVectorTileClicked -> name: \(name), id: \(id)")
            self.tileDecoder.setStyleParameter("selectedObjectId", value: id)

        }
        return false
    }
}

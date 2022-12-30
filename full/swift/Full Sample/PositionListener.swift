import Foundation
import INVPositioner
import YolbilMobileSDK

class PositionListener : INVPositionListener {
    var vc: ViewController!
    
    init(vc: ViewController) {
        super.init()
        self.vc = vc
    }
    
    override func onLocationChanged(location: INVLocation) {
        print("Location x: \(location.x), y: \(location.y), f: \(location.f)")

        if (self.vc.floor != Int(location.f)) {
            self.vc.floor = Int(location.f)
            self.vc.tileDecoder.setStyleParameter("floorIndex", value: String(self.vc.floor))
        }

        let locationBuilder = YBLocationBuilder()!
        locationBuilder.setCoordinate(YBMapPos(x: location.x, y: location.y))
        locationBuilder.setHorizontalAccuracy(Double(location.accuracy))
        self.vc.locationSource.update(locationBuilder.build())

        if (location.accuracy < 13) {
            self.vc.mapView.setZoom(21, durationSeconds: 1)
            self.vc.mapView.setFocus(YBMapPos(x: location.x, y: location.y), durationSeconds: 1)
        } else {
            let minScreen = YBScreenPos()
            let maxScreen = YBScreenPos(x: Float(self.vc.mapView.frame.size.width), y: Float(self.vc.mapView.frame.size.height))
            let screenPos = YBScreenBounds(min: minScreen, max: maxScreen)
            let crossDist = location.accuracy * sqrt(2)
            let minMapPos = YBMapPos(
                x: location.x - Double(crossDist),
                y: location.y - Double(crossDist)
            )
            let maxMapPos = YBMapPos(
                x: location.x + Double(crossDist),
                y: location.y + Double(crossDist)
            )
            let mapBound = YBMapBounds(min: minMapPos, max: maxMapPos)
            self.vc.mapView.move(toFit: mapBound, screenBounds: screenPos, integerZoom: false, resetRotation: false, resetTilt: false, durationSeconds: 1)
        }

        let point = INVPoint(coordX: location.x, coordY: location.y, coordZ: location.f, coordType: EPSG3857)!
        let navResult = self.vc.manifest.queryDynamicManifest(point: point)
        print("distance: \(navResult.distance)")
        print("command enum: \(navResult.commandEnum)")
        print("command type: \(navResult.commandType)")
        print("edge type: \(navResult.edgeType)")
        print("next command enum: \(navResult.nextCommandEnum)")
        print("next edge type: \(navResult.nextEdgeType)")
        self.vc.drawRoute(navResult: navResult)

    }
}

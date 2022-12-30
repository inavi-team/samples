import Foundation
import INVPositioner

class IOSPositionListener : INVPositionListener {
    override func onLocationChanged(location: INVLocation) {
        print("\n Location -> ", location.x, location.y, location.f, location.accuracy, location.timestamp, "\n")
        
        var points = [INVPoint]()
        
        points.append(INVPoint(coordX: location.x, coordY: location.y, coordZ: location.f))
        points.append(INVPoint(coordX: 3646571.781801, coordY: 4851386.385695, coordZ: 0.0))
        
        let routingRes = r.calculateRouteAsGeoJSON(points: points, coordType: EPSG3857, profile: "visitor")


    }
}

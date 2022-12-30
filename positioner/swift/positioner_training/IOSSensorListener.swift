import Foundation
import INVSensorManager
import INVPositioner

class IOSSensorListener : INVSensorManagerListener {
    override func onSensorChanged(data: [Float], time: Int, sensorType: Int32, accuracyType: Int32) {
        print("\nSensorListener ->", sensorType, data[0], "\n")
    }
    override func onAccuracyChanged(sensorType: Int32, accuracyType: Int32) {
        print("\noooo ->", sensorType, accuracyType, "\n")
    }

    override func onBeaconReceived(beacons: Array<INVBeaconObject>) {
        print("\n***Beacons size ", beacons.count);
    }
}

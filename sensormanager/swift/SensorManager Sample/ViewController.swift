import UIKit
import INVSensorManager

class SensorListener : INVSensorManagerListener {
    override func onAccuracyChanged(sensorType: Int32, accuracyType: Int32) {
        print("accuracy: \(accuracyType)");
        if (accuracyType == INVAccuracyType.HIGH_ACCURACY.ordinal()) {
            print("Calibration completed")
        }
    }
    override func onSensorChanged(data: [Float], time: Int, sensorType: Int32, accuracyType: Int32) {}
    override func onWifiReceived(wifis: Array<Any>) {}
    override func onBeaconReceived(beacons: Array<INVBeaconObject>) {}
}

class ViewController: UIViewController {
    var sensorListener: SensorListener!

    override func viewDidLoad() {
        super.viewDidLoad()
        
        sensorListener = SensorListener()
        INVSensorManager.registerListener(sensorManagerListener: sensorListener, sensorType: INVSensorType.MAGNETIC_FIELD_CUSTOM.ordinal())
        INVSensorManager.startCustomMagneticFieldCalibration()
    }
}


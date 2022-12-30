package us.inavi.sample.sensormanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.basarsoft.inavi.libs.sensormanager.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SensorManager.registerListener(object : SensorListener {
            override fun onAccuracyChanged(sensorType: Int, accuracyType: Int) {
                Log.d("iNavi", "accuracy: " + accuracyType);
                if (accuracyType == AccuracyType.HIGH_ACCURACY.ordinal) {
                    Log.d("iNavi", "Calibration completed")
                }
            }
            override fun onSensorChanged(event: Event) {}
            override fun onWifiReceived(wifis: Array<WifiObject>) {}
            override fun onBeaconReceived(beacons: Array<BeaconObject>) {}
        }, SensorType.MAGNETIC_FIELD_CUSTOM.ordinal)

        SensorManager.startCustomMagneticFieldCalibration()
    }
}

import Foundation
import INVLicenseManager

class LicenseListener: INVLicenseListener {
    override func onLicenseSuccess() {
        print("License successful")
    }

    override func onLicenseError(message: String) {
        print("License failed")
    }
}

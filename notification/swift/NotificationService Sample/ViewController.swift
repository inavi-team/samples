//
//  ViewController.swift
//  Notification Sample
//
//  Created by OZAN on 9.11.2022.
//

import UIKit
import INVNotificationService
import INVPackageManager
import INVLicenseManager
import INVObjects

var myDownloadListener: MyDownloadListener!
var licenseRequestMap: INVLicenseRequestMap!
var myLicenseListener: MyLicenseListener!
var notificaationListener : ConcreteNotificationListener!

class MyDownloadListener : INVDownloadListener {
    override func onProgress(buildingId: String, percentage: Int) {
        print("onProgress: \(buildingId), percentage: \(percentage)")
    }
    override func onCompleted(buildingId: String) {
        print("onCompleted: \(buildingId)")
        INVPackageManager.selectBuildingData(buildingId: buildingId, versionMajor: 3)

        notificaationListener = ConcreteNotificationListener()
        //dataType 7 is required for setting notificationData to notificationService
        //we are trying to get notification of areas which notificationType is 1. This value is setted from "iNavi-Web"
        let notificationService = INVNotificationService(dataType: 7, keys: ["notificationType"], values: [1])
        print(notificationService.registerListener(listener: notificaationListener))

        
        notificationService.sendLocation(point: INVPoint(coordX: 5190002.199046696, coordY: 2840437.0460758056, coordZ: 1.0, coordType: EPSG3857))
    }
    override func onError(buildingId: String, errorMessage: String) {
        print("onError: \(buildingId), message: \(errorMessage)")
    }
}

class MyLicenseListener : INVLicenseListener {
    override func onLicenseSuccess() {
        myDownloadListener = MyDownloadListener()
        print("playground onLicenseSuccess")
        let buildingList = INVPackageManager.getBuildingList()

        for buildingInfo in buildingList {
            print("buildingInfo: " + buildingInfo.id)
        }

        INVPackageManager.downloadBuildingData(
            buildingId: "ENTER_YOUR_BUILDING_ID",
            versionMajor: 3,
            buildingDataTypes: [MAP, ROUTING, POSITIONING],
            listener: myDownloadListener
        )

    }

    override func onLicenseError(message: String) {
        print("playground onLicenseError: " + message)
    }
}

class ConcreteNotificationListener : INVNotificationListener {
    override func onEnterArea(notificationResult: INVNotificationResult) {
        print("on enter area\n")
        print("notifaction result feature: \(notificationResult.feature.points) \(notificationResult.feature.featureType)\n")
        print("notifaction result distance: \(notificationResult.distance)\n")
        print("notifaction result closestPoint: \(notificationResult.closestPoint.coordX) \(notificationResult.closestPoint.coordY) \(notificationResult.closestPoint.coordZ) \(notificationResult.closestPoint.coordType)\n")
        print("notifaction result description: \(notificationResult.desc)\n")
        print("notifaction result id: \(notificationResult.objectId)\n")

    }
}

class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        licenseRequestMap = INVLicenseRequestMap()
        myLicenseListener = MyLicenseListener()
        let licenseRequests: [INVLicenseRequest] = [
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "3", type: MAP),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "3", type: POSITIONING),
            INVBuildingLicenseRequest(id: "ENTER_YOUR_BUILDING_ID", version: "3", type: ROUTING),
            ]
        licenseRequestMap.put(apiKey: "ENTER_YOUR_API_KEY", licenseRequests: licenseRequests)
        INVLicenseManager.requestLicense(licenseRequestMap: licenseRequestMap, licenseListener: myLicenseListener)
    }


}


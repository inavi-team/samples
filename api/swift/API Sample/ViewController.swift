import UIKit
import ParseSwift

struct Floor: ParseObject {
    var objectId: String?
    var createdAt: Date?
    var updatedAt: Date?
    var ACL: ParseACL?
    var originalData: Data?

    //: Your own properties.
    var rooms: [Room]?
    var name: String?
    var index: Int?
}

struct Room: ParseObject {
    var objectId: String?
    var createdAt: Date?
    var updatedAt: Date?
    var ACL: ParseACL?
    var originalData: Data?

    //: Your own properties.
    var name: String?
    var floorId: String?
    var coordinates: [[[String:Double]]]?
    var doors: [[String:Double]]?
}

struct User: ParseUser {
    //: These are required by `ParseObject`.
    var objectId: String?
    var createdAt: Date?
    var updatedAt: Date?
    var ACL: ParseACL?
    var originalData: Data?

    //: These are required by `ParseUser`.
    var username: String?
    var email: String?
    var emailVerified: Bool?
    var password: String?
    var authData: [String: [String: String]?]?
}

extension User {
    //: Custom init for signup.
    init(username: String, password: String, email: String) {
        self.username = username
        self.password = password
        self.email = email
    }
}

struct Category : Decodable {
    var id: String
    var name: [String:String]
    var tags: [String:[String]]
    var type: String?
    var color: String?
}

struct CategoriesWrapper : Decodable {
    var categories: [String: Category]
}

struct GetCategories: ParseCloud {

    //: Return type of your Cloud Function
    typealias ReturnType = CategoriesWrapper

    //: These are required by `ParseCloud`, you can set the default value to make it easier
    //: to use.
    var functionJobName: String = "getCategories"
}

class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        ParseSwift.initialize(applicationId: "iNaviProd", serverURL: URL(string: "https://inavi.us/db")!)
        
        let getCategories = GetCategories()

        getCategories.runFunction { result in
            switch result {
            case .success(let response):
                response.categories.forEach { (id: String, cat: Category) in
                    if (cat.name["en"] == nil) {
                        return
                    }
                    print("cat id: \(id), name: \(cat.name["en"]!), type: \(cat.type ?? "none"), color: \(cat.color ?? "none"), tags: \(cat.tags["en"] ?? [])")
                }
            case .failure(let error):
                assertionFailure("Error calling cloud function: \(error)")
            }
        }
        
        login("username", "password") {
            self.getFloors(buildingId: "ENTER_YOUR_BUILDING_ID") { floors in
                floors.forEach { floor in
                    if let rooms = floor.rooms {
                        rooms.forEach { room in
                            if room.name == nil {
                                return
                            }
                            let firstCoord = room.coordinates![0][0]
                            var firstDoorX: Double = -1
                            var firstDoorY: Double = -1
                            if let doors = room.doors {
                                if (doors.count > 0) {
                                    firstDoorX = doors[0]["x"]!
                                    firstDoorY = doors[0]["y"]!
                                }
                            }
                            print("room id: \(room.objectId!), name: \(room.name!), floorIndex: \(floor.index!), floorName: \(floor.name!), firstCoord -> x: \(firstCoord["x"]!), y: \(firstCoord["y"]!), firstDoor -> x: \(firstDoorX), y: \(firstDoorY)")
                        }
                    }
                }
            }
        }
    }
    
    func login(_ username: String, _ password: String, callback: @escaping () -> Void) {
        User.login(username: username, password: password) { result in

            switch result {
            case .success(let user):

                guard let currentUser = User.current else {
                    assertionFailure("Error: current user not stored locally")
                    return
                }
                assert(currentUser.hasSameObjectId(as: user))
                callback()

            case .failure(let error):
                print("Error logging in: \(error)")
            }
        }
    }
    
    func getFloors(buildingId: String, callback: @escaping (_ floors: [Floor]) -> Void) {
        Floor.query("buildingId" == buildingId)
        .include(["floors", "rooms"])
        .find(callbackQueue: .main) { floorsResult in
            switch floorsResult {
                case .success(let floors):
                    callback(floors)

                case .failure(let error):
                    if error.equalsTo(.objectNotFound) {
                        assertionFailure("Object not found for this query")
                    } else {
                        assertionFailure("Error querying: \(error)")
                    }
            }
        }
    }
}


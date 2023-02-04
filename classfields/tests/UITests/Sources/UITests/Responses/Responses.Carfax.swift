import Foundation
import AutoRuProtoModels

extension Responses {
    enum Carfax {
        enum Offer {
            enum Cars { }
        }
    }
}

extension Responses.Carfax.Offer.Cars {
    static func get(offer: BackendState.Offer, state: BackendState) -> Auto_Api_RawVinReportResponse {
        let offer = makeOffer(offer, state: state)

        return .with { msg in
            msg.report = .with { msg in
                msg.reportType = .paidReport
                msg.status = .ok
                msg.header = .with { msg in
                    msg.isUpdating = true
                    msg.title = "\(offer.carInfo.mark) \(offer.carInfo.model), \(offer.documents.year)"
                }
                msg.vehicle = .with { msg in
                    msg.colorHex = "0000CC"
                }
                msg.sources = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
                msg.content = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
                msg.ptsInfo = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                    msg.status = .ok
                }
                msg.ptsOwners = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
                msg.dtp = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
                msg.legal = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                    msg.wantedStatus = .ok
                    msg.constraintsStatus = .ok
                }
                msg.history = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
                msg.carSharing = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
                msg.constraints = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                    msg.status = .ok
                }
                msg.wanted = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                    msg.status = .ok
                }
                msg.healthScore = .with { msg in
                    msg.header = .with { msg in
                        msg.isUpdating = true
                    }
                }
            }

            msg.status = .success
        }
    }
}

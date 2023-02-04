import Foundation
import AutoRuProtoModels

extension Responses {
    enum Search {
        enum Cars { }
    }
}

extension Responses.Search.Cars {
    static func success(for state: BackendState) -> Auto_Api_OfferListingResponse {
        return .with { msg in
            msg.offers = state.search.listing.map { offer in
                makeOffer(offer, state: state)
            }

            msg.pagination = .with { msg in
                msg.page = 1
                msg.pageSize = 20
                msg.totalOffersCount = Int32(state.search.listing.count)
                msg.totalPageCount = {
                    let (quotient, remainder) = msg.totalOffersCount.quotientAndRemainder(dividingBy: msg.page)
                    return quotient + (remainder > 0 ? 1 : 0)
                }()
            }

            msg.status = .success
        }
    }
}

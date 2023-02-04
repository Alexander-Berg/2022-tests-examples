import AutoRuRateCall
import AutoRuProtoModels

final class ComplaintServiceMock: ComplaintService {
    struct Args: Equatable {
        var reason: Vertis_Complaints_Complaint.Reason
        var text: String?
    }

    var input: [Args] = []

    func sendComplaint(_ reason: Vertis_Complaints_Complaint.Reason, _ text: String?) {
        input.append(Args(reason: reason, text: text))
    }
}

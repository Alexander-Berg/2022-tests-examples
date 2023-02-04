#include <maps/automotive/libs/test_helpers/helpers.h>

#include <google/protobuf/util/message_differencer.h>

namespace maps::automotive {

std::pair<bool, std::string> compareProtobuf(
    const google::protobuf::Message& expected, const google::protobuf::Message& actual)
{
    google::protobuf::util::MessageDifferencer differencer;
    TString diff;
    differencer.ReportDifferencesToString(&diff);
    if (!differencer.Compare(expected, actual)) {
        return {false, diff};
    } else {
        return {true, {}};
    }
}

} // namespace maps::automotive

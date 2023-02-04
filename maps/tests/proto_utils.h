#pragma once

#include <google/protobuf/message.h>
#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/util/json_util.h>

// TODO: Reuse proto_utils from datamodel

namespace maps::quotateka::tests {

#define EXPECT_PROTO_EQ(expected, actual) { \
    google::protobuf::util::MessageDifferencer comparer; \
    TString diff; \
    comparer.ReportDifferencesToString(&diff); \
    if (!comparer.Compare(expected, actual)) \
        ADD_FAILURE() << "Messages different:\n" << diff; \
}

inline std::string protoToString(const google::protobuf::Message& protoMessage)
{
    TString payload;
    Y_ENSURE(protoMessage.SerializeToString(&payload));
    return payload;
}

template<typename ProtoMessage>
inline ProtoMessage stringToProto(std::string_view payload)
{
    ProtoMessage protoMessage;
    protoMessage.ParseFromString(TString(payload));
    return protoMessage;
}

template<typename ProtoMessage>
inline ProtoMessage jsonToProto(std::string_view jsonText)
{
    ProtoMessage protoMessage;
    google::protobuf::util::JsonStringToMessage(TString(jsonText), &protoMessage);
    return protoMessage;
}

}  // namespace maps::quotateka::tests

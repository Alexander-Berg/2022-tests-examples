#include <google/protobuf/text_format.h>
#include <util/generic/string.h>
#include <util/system/type_name.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>

#include <string>

template <typename TMessage>
inline void parseFromString(const std::string& s, TMessage& msg)
{
    google::protobuf::string str(s);
    REQUIRE(NProtoBuf::TextFormat::ParseFromString(str, &msg),
            "Error parsing " << TypeName<TMessage>() << " from " << s);
}

template <typename TMessage>
inline std::string printToString(const TMessage& msg)
{
    google::protobuf::string s;
    NProtoBuf::TextFormat::Printer p;
    p.SetUseUtf8StringEscaping(true);
    REQUIRE(p.PrintToString(msg, &s),
            "Error printing " << TypeName<TMessage>());
    return s;
}

template <typename TMessage>
void parseFromFile(const std::string& s, TMessage& msg)
{
    parseFromString(maps::common::readFileToString(s), msg);
}

template <typename TMessage>
void printToFile(const std::string& s, TMessage& msg)
{
    maps::common::writeFile(s, printToString(msg));
}

template <typename TMessage>
inline std::string binaryFromString(const std::string& s)
{
    TMessage message;
    parseFromString(s, message);
    TString binary;
    message.SerializeToString(&binary);
    return binary;
}

template <typename TMessage>
inline std::string binaryFromFile(const std::string& s)
{
    return binaryFromString<TMessage>(maps::common::readFileToString(s));
}

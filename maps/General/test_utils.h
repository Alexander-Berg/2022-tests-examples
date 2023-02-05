#pragma once

#include <yandex/maps/mms/cast.h>
#include <yandex/maps/mms/type_traits.h>
#include <yandex/maps/mms/writer.h>

#include <sstream>
#include <string>

namespace maps::search::common::test_utils {

template <typename StandaloneType>
const typename mms::MmappedType<StandaloneType>::type& convertToMmapped(const StandaloneType& object, std::string& buffer)
{
    std::ostringstream stream;
    mms::write(stream, object);
    buffer = stream.str();
    return mms::safeCast<typename mms::MmappedType<StandaloneType>::type>(buffer.data(), buffer.size());
}

} // namespace maps::search::common::test_utils

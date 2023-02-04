#pragma once

#include "maps/b2bgeo/libs/jwt/py/tests/include/simple_payload_type.h"
#include <maps/b2bgeo/libs/jwt/include/py/py_payload_wrapper.h>

namespace maps::b2bgeo::jwt {

// Wrapper for python JWT binding, implements IPayloadType interface
using SimplePayloadWrapper = PyPayloadWrapper<SimplePayloadType>;

} // namespace maps::b2bgeo::jwt

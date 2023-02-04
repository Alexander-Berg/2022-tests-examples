from maps.b2bgeo.libs.jwt.py._jwt cimport PyPayloadWrapper
from maps.b2bgeo.libs.jwt.py._picojson cimport picojson_object

cdef extern from "maps/b2bgeo/libs/jwt/py/tests/include/simple_payload_type.h" namespace "maps::b2bgeo::jwt":
    cdef cppclass SimplePayloadType:
        picojson_object serialize() const

cdef extern from "maps/b2bgeo/libs/jwt/py/tests/include/py_simple_payload_wrapper.h" namespace "maps::b2bgeo::jwt":
    ctypedef PyPayloadWrapper[SimplePayloadType] SimplePayloadWrapper

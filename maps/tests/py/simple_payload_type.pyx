from libcpp.utility cimport move

from maps.b2bgeo.libs.jwt.py._variant cimport variant, holds_alternative, get1
from maps.b2bgeo.libs.jwt.py._jwt cimport IPayloadType, buildSubjectPayload
from maps.b2bgeo.libs.jwt.py.jwt cimport PayloadHolder
from maps.b2bgeo.libs.jwt.py._picojson cimport picojson_object
from maps.b2bgeo.libs.jwt.py.picojson cimport convert_object, convert_to_picojson

from _simple_payload_type cimport (
    SimplePayloadType as _SimplePayloadType,
    SimplePayloadWrapper as _SimplePayloadWrapper,
)


cdef class SimplePayloadType(PayloadHolder):
    cdef _SimplePayloadWrapper payload

    def __init__(self, dict payload):
        cdef picojson_object payload_picojson = \
            move(convert_to_picojson(payload).get[picojson_object]())
        self.payload = <_SimplePayloadWrapper><_SimplePayloadType> move(payload_picojson)

    cdef IPayloadType* get_payload_ptr(self):
        return <IPayloadType*> &self.payload

    @staticmethod
    cdef SimplePayloadType create(_SimplePayloadType payload):
        cdef SimplePayloadType wrapper = SimplePayloadType.__new__(SimplePayloadType)
        wrapper.payload = <_SimplePayloadWrapper> move(payload)
        return wrapper

    def serialize(self):
        cdef picojson_object serialized = self.payload.get().serialize()
        return convert_object(serialized)


def buildSimplePayload(object payload):
    cdef picojson_object payloadPicojson = convert_to_picojson(payload).get[picojson_object]()
    cdef variant[_SimplePayloadType] var = move(buildSubjectPayload[_SimplePayloadType](payloadPicojson))

    if holds_alternative[_SimplePayloadType, _SimplePayloadType](var):
        return SimplePayloadType.create(move(get1[_SimplePayloadType, _SimplePayloadType](var)))
    else:
        raise Exception('Unexpected payload subject')

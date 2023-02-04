# -*- coding: utf-8 -*-
import balancer.test.util.proto.http2.framing.flags as flags


class Field(object):
    def __init__(self, name, size):
        super(Field, self).__init__()
        self.__name = name
        self.__size = size

    @property
    def name(self):
        return self.__name

    @property
    def size(self):
        return self.__size

    def check(self, ctx):
        return True

    def check_unit(self, unit):  # TODO: store all values in ParseUnit and join check_unit with check
        return True

    def __eq__(self, other):
        return \
            isinstance(other, Field) and \
            self.name == other.name and \
            self.size == other.size


class FlagConditionalField(Field):
    def __init__(self, name, size, flag):
        super(FlagConditionalField, self).__init__(name, size)
        self.__flag = flag

    @property
    def flag(self):
        return self.__flag

    def check(self, ctx):
        return ('flags' in ctx) and (ctx['flags'] & self.flag)

    def check_unit(self, unit):
        return isinstance(unit, Frame) and unit.flags & self.flag

    def __eq__(self, other):
        return \
            isinstance(other, FlagConditionalField) and \
            self.flag == other.flag and \
            super(FlagConditionalField, self).__eq__(other)


_FCF = FlagConditionalField  # alias for inner usage


def _build_field(value):
    if isinstance(value, Field):
        return value
    else:
        return Field(value[0], value[1])


class ParseUnit(object):
    DEFINITION = []

    CONSTRAINTS = {}

    def __init__(self, **kwargs):
        super(ParseUnit, self).__init__(**kwargs)

    @classmethod
    def __get_parse_unit_parent(cls):
        for base in cls.__bases__:
            if issubclass(base, ParseUnit):
                return base

    @classmethod
    def get_constraint(cls, name):
        if name in cls.CONSTRAINTS:
            return cls.CONSTRAINTS[name]
        else:
            if cls != ParseUnit:
                return cls.__get_parse_unit_parent().get_constraint(name)

    @classmethod
    def __get_definition(cls):
        return [_build_field(field) for field in cls.DEFINITION]

    @classmethod
    def get_full_definition(cls):
        if cls != ParseUnit:
            return cls.__get_parse_unit_parent().get_full_definition() + cls.__get_definition()
        else:
            return cls.__get_definition()

    def get_definition(self):
        return [field for field in self.get_full_definition() if field.check_unit(self)]

    def get_meta_length(self):
        return sum([field.size for field in self.get_definition()])

    def __repr__(self):
        result = [self.__class__.__name__]
        for field in self.get_definition():
            result.append('    %s: %s' % (field.name, getattr(self, field.name)))
        if isinstance(self, Payload):
            result.append('Payload:')
            result.append(repr(self.data))
        if isinstance(self, Padded) and (self.flags & flags.PADDED):
            result.append('Padding:')
            result.append(repr(self.padding))
        return '\n'.join(result)

    def __eq__(self, other):
        if not isinstance(other, ParseUnit):
            return False
        definition = self.get_definition()
        if definition != other.get_definition():
            return False
        for field in definition:
            name = field.name
            if getattr(self, name) != getattr(other, name):
                return False
        if isinstance(self, Payload):
            if not isinstance(other, Payload):
                return False
            elif self.data != other.data:
                return False
        if isinstance(self, Padded) and (self.flags & flags.PADDED):
            if not isinstance(other, Padded):
                return False
            elif self.padding != other.padding:
                return False
        return True


class Frame(ParseUnit):
    """
    +-----------------------------------------------+
    |                 Length (24)                   |
    +---------------+---------------+---------------+
    |   Type (8)    |   Flags (8)   |
    +-+-------------+---------------+-------------------------------+
    |R|                 Stream Identifier (31)                      |
    +=+=============================================================+
    |                   Frame Payload (0...)                      ...
    +---------------------------------------------------------------+
    """

    DEFINITION = [
        ('length', 24),
        ('frame_type', 8), ('flags', 8),
        ('reserved', 1), ('stream_id', 31),
    ]

    CONSTRAINTS = {}

    FLAGS = []

    def __init__(self, length, frame_type, flags, reserved, stream_id, **kwargs):
        super(Frame, self).__init__(**kwargs)
        self.length = length
        self.frame_type = frame_type
        self.flags = flags
        self.reserved = reserved
        self.stream_id = stream_id


class Payload(object):
    def __init__(self, data, **kwargs):
        super(Payload, self).__init__(**kwargs)
        self.data = data


class Padded(Payload):
    def __init__(self, data=None, pad_length=None, padding=None, **kwargs):
        super(Padded, self).__init__(data=data, **kwargs)
        self.pad_length = pad_length
        self.padding = padding


class Data(Frame, Padded):
    """
    +---------------+
    |Pad Length? (8)|
    +---------------+-----------------------------------------------+
    |                            Data (*)                         ...
    +---------------------------------------------------------------+
    |                           Padding (*)                       ...
    +---------------------------------------------------------------+
    """
    DEFINITION = [
        _FCF('pad_length', 8, flags.PADDED),
    ]

    CONSTRAINTS = {
        'frame_type': 0x0,
    }

    FLAGS = [
        flags.END_STREAM,
        flags.PADDED,
    ]

    def __init__(
        self, length, flags, reserved, stream_id,
        pad_length=None, data=None, padding=None
    ):
        super(Data, self).__init__(
            length=length,
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
            data=data,
            pad_length=pad_length,
            padding=padding,
        )


class Headers(Frame, Padded):
    """
    +---------------+
    |Pad Length? (8)|
    +-+-------------+-----------------------------------------------+
    |E|                 Stream Dependency? (31)                     |
    +-+-------------+-----------------------------------------------+
    |  Weight? (8)  |
    +-+-------------+-----------------------------------------------+
    |                   Header Block Fragment (*)                 ...
    +---------------------------------------------------------------+
    |                           Padding (*)                       ...
    +---------------------------------------------------------------+
    """

    DEFINITION = [
        _FCF('pad_length', 8, flags.PADDED),
        _FCF('exclusive', 1, flags.PRIORITY), _FCF('stream_dependency', 31, flags.PRIORITY),
        _FCF('weight', 8, flags.PRIORITY),
    ]

    CONSTRAINTS = {
        'frame_type': 0x1,
    }

    FLAGS = [
        flags.END_STREAM,
        flags.END_HEADERS,
        flags.PADDED,
        flags.PRIORITY,
    ]

    def __init__(
        self, length, flags, reserved, stream_id,
        pad_length=None, exclusive=None, stream_dependency=None, weight=None, data=None, padding=None
    ):
        super(Headers, self).__init__(
            length=length,
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
            data=data,
            pad_length=pad_length,
            padding=padding,
        )
        self.exclusive = exclusive
        self.stream_dependency = stream_dependency
        self.weight = weight


class Priority(Frame):
    """
    +-+-------------------------------------------------------------+
    |E|                  Stream Dependency (31)                     |
    +-+-------------+-----------------------------------------------+
    |   Weight (8)  |
    +-+-------------+
    """
    DEFINITION = [
        ('exclusive', 1), ('stream_dependency', 31),
        ('weight', 8),
    ]

    CONSTRAINTS = {
        'frame_type': 0x2,
        'length': 5,
    }

    def __init__(
        self, flags, reserved, stream_id,
        exclusive=None, stream_dependency=None, weight=None,
    ):
        super(Priority, self).__init__(
            length=self.get_constraint('length'),
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
        )
        self.exclusive = exclusive
        self.stream_dependency = stream_dependency
        self.weight = weight


class RstStream(Frame):
    """
    +---------------------------------------------------------------+
    |                        Error Code (32)                        |
    +---------------------------------------------------------------+
    """
    DEFINITION = [
        ('error_code', 32),
    ]

    CONSTRAINTS = {
        'frame_type': 0x3,
        'length': 4,
    }

    def __init__(
        self, flags, reserved, stream_id,
        error_code=None,
    ):
        super(RstStream, self).__init__(
            length=self.get_constraint('length'),
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
        )
        self.error_code = error_code


class Parameter(ParseUnit):
    """
    +-------------------------------+
    |       Identifier (16)         |
    +-------------------------------+-------------------------------+
    |                        Value (32)                             |
    +---------------------------------------------------------------+
    """
    DEFINITION = [
        ('identifier', 16),
        ('value', 32),
    ]

    HEADER_TABLE_SIZE = 0x1
    ENABLE_PUSH = 0x2
    MAX_CONCURRENT_STREAMS = 0x3
    INITIAL_WINDOW_SIZE = 0x4
    MAX_FRAME_SIZE = 0x5
    MAX_HEADER_LIST_SIZE = 0x6

    def __init__(self, identifier, value):
        super(Parameter, self).__init__()
        self.identifier = identifier
        self.value = value


class Settings(Frame, Payload):
    DEFINITION = []

    CONSTRAINTS = {
        'frame_type': 0x4,
        'stream_id': 0,
    }

    FLAGS = [
        flags.ACK,
    ]

    def __init__(
        self, length, flags, reserved,
        data=None,
    ):
        super(Settings, self).__init__(
            length=length,
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=self.get_constraint('stream_id'),
            data=data,
        )


class PushPromise(Frame, Padded):
    """
    +---------------+
    |Pad Length? (8)|
    +-+-------------+-----------------------------------------------+
    |R|                  Promised Stream ID (31)                    |
    +-+-----------------------------+-------------------------------+
    |                   Header Block Fragment (*)                 ...
    +---------------------------------------------------------------+
    |                           Padding (*)                       ...
    +---------------------------------------------------------------+
    """
    DEFINITION = [
        _FCF('pad_length', 8, flags.PADDED),
        ('push_promise_reserved', 1), ('promised_stream_id', 31),
    ]

    CONSTRAINTS = {
        'frame_type': 0x5,
    }

    FLAGS = [
        flags.END_HEADERS,
        flags.PADDED,
    ]

    def __init__(
        self, length, flags, reserved, stream_id,
        pad_length=None, push_promise_reserved=None, promised_stream_id=None, data=None, padding=None,
    ):
        super(PushPromise, self).__init__(
            length=length,
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
            data=data,
            pad_length=pad_length,
            padding=padding,
        )
        self.push_promise_reserved = push_promise_reserved
        self.promised_stream_id = promised_stream_id


class Ping(Frame, Payload):
    """
    +---------------------------------------------------------------+
    |                                                               |
    |                      Opaque Data (64)                         |
    |                                                               |
    +---------------------------------------------------------------+
    """
    DEFINITION = [
        # ('opaque_data', 64),
    ]

    CONSTRAINTS = {
        'frame_type': 0x6,
        'length': 8,
        'stream_id': 0,
    }

    FLAGS = [
        flags.ACK,
    ]

    def __init__(
        self, flags, reserved,
        data=None,
    ):
        super(Ping, self).__init__(
            length=self.get_constraint('length'),
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=self.get_constraint('stream_id'),
            data=data,
        )


class Goaway(Frame, Payload):
    """
    +-+-------------------------------------------------------------+
    |R|                  Last-Stream-ID (31)                        |
    +-+-------------------------------------------------------------+
    |                      Error Code (32)                          |
    +---------------------------------------------------------------+
    |                  Additional Debug Data (*)                    |
    +---------------------------------------------------------------+
    """
    DEFINITION = [
        ('goaway_reserved', 1), ('last_stream_id', 31),
        ('error_code', 32),
    ]

    CONSTRAINTS = {
        'frame_type': 0x7,
        'stream_id': 0x0,
    }

    def __init__(
        self, length, flags, reserved,
        goaway_reserved=None, last_stream_id=None, error_code=None, data=None,
    ):
        super(Goaway, self).__init__(
            length=length,
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=self.get_constraint('stream_id'),
            data=data,
        )
        self.goaway_reserved = goaway_reserved
        self.last_stream_id = last_stream_id
        self.error_code = error_code


class WindowUpdate(Frame):
    """
    +-+-------------------------------------------------------------+
    |R|              Window Size Increment (31)                     |
    +-+-------------------------------------------------------------+
    """
    DEFINITION = [
        ('window_update_reserved', 1), ('window_size_increment', 31),
    ]

    CONSTRAINTS = {
        'frame_type': 0x8,
        'length': 4,
    }

    def __init__(
        self, flags, reserved, stream_id,
        window_update_reserved=None, window_size_increment=None
    ):
        super(WindowUpdate, self).__init__(
            length=self.get_constraint('length'),
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
        )
        self.window_update_reserved = window_update_reserved
        self.window_size_increment = window_size_increment


class Continuation(Frame, Payload):
    """
    +---------------------------------------------------------------+
    |                   Header Block Fragment (*)                 ...
    +---------------------------------------------------------------+
    """
    DEFINITION = []

    FLAGS = [
        flags.END_HEADERS,
    ]

    CONSTRAINTS = {
        'frame_type': 0x9,
    }

    def __init__(
        self, length, flags, reserved, stream_id,
        data=None,
    ):
        super(Continuation, self).__init__(
            length=length,
            frame_type=self.get_constraint('frame_type'),
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
            data=data,
        )


class Unknown(Frame, Payload):
    DEFINITION = []
    FLAGS = []
    CONSTRAINTS = {}

    def __init__(self, length, frame_type, flags, reserved, stream_id, data=None):
        super(Unknown, self).__init__(
            length=length,
            frame_type=frame_type,
            flags=flags,
            reserved=reserved,
            stream_id=stream_id,
            data=data,
        )


FRAME_TYPES = {frame_cls.CONSTRAINTS['frame_type']: frame_cls for frame_cls in [
    Data,
    Headers,
    Priority,
    RstStream,
    Settings,
    PushPromise,
    Ping,
    Goaway,
    WindowUpdate,
    Continuation,
]}

# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: _test_case_for_infer_schema.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


import test_awacs_field_schema_pb2 as test__awacs__field__schema__pb2
import test_awacs_message_schema_pb2 as test__awacs__message__schema__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='_test_case_for_infer_schema.proto',
  package='test_case_for_infer_schema',
  syntax='proto2',
  serialized_pb=_b('\n!_test_case_for_infer_schema.proto\x12\x1atest_case_for_infer_schema\x1a\x1dtest_awacs_field_schema.proto\x1a\x1ftest_awacs_message_schema.proto\"\xda\x01\n\x0c\x45mbeddedTest\x12=\n\tint_field\x18\x01 \x01(\x05:\x01\x30\x42\'\x88\xa6\x1d\x9c\xff\xff\xff\xff\xff\xff\xff\xff\x01\x90\xa6\x1d\xe8\x07\xd2\xac\x1d\x11This is int field\x12\x13\n\x0bint64_field\x18\x02 \x01(\x03\x12\x13\n\x0b\x66loat_field\x18\x03 \x01(\x02\x12#\n\x0cstring_field\x18\x04 \x01(\t:\rDefault value\x12\x12\n\nbool_field\x18\x05 \x01(\x08\x12\x16\n\x0erepeated_field\x18\x06 \x03(\x05:\x10\xba\xa6\x1d\x0c\x45mbeddedTest\"\xd8\x01\n\x04Test\x12\x1f\n\tint_field\x18\x01 \x01(\x05:\x01\x30\x42\t\x88\xa6\x1d\x64\x90\xa6\x1d\xf4\x03\x12\x13\n\x0b\x66loat_field\x18\x02 \x01(\x02\x12\x14\n\x0cstring_field\x18\x03 \x01(\t\x12\x12\n\nbool_field\x18\x04 \x01(\x08\x12\x16\n\x0erepeated_field\x18\x05 \x03(\x05\x12?\n\rmessage_field\x18\x06 \x01(\x0b\x32(.test_case_for_infer_schema.EmbeddedTest:\x17\xba\xa6\x1d\x04Test\xc2\xa6\x1d\x0b\x44\x65scription')
  ,
  dependencies=[test__awacs__field__schema__pb2.DESCRIPTOR,test__awacs__message__schema__pb2.DESCRIPTOR,])
_sym_db.RegisterFileDescriptor(DESCRIPTOR)




_EMBEDDEDTEST = _descriptor.Descriptor(
  name='EmbeddedTest',
  full_name='test_case_for_infer_schema.EmbeddedTest',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='int_field', full_name='test_case_for_infer_schema.EmbeddedTest.int_field', index=0,
      number=1, type=5, cpp_type=1, label=1,
      has_default_value=True, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=_descriptor._ParseOptions(descriptor_pb2.FieldOptions(), _b('\210\246\035\234\377\377\377\377\377\377\377\377\001\220\246\035\350\007\322\254\035\021This is int field'))),
    _descriptor.FieldDescriptor(
      name='int64_field', full_name='test_case_for_infer_schema.EmbeddedTest.int64_field', index=1,
      number=2, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='float_field', full_name='test_case_for_infer_schema.EmbeddedTest.float_field', index=2,
      number=3, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='string_field', full_name='test_case_for_infer_schema.EmbeddedTest.string_field', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=True, default_value=_b("Default value").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='bool_field', full_name='test_case_for_infer_schema.EmbeddedTest.bool_field', index=4,
      number=5, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='repeated_field', full_name='test_case_for_infer_schema.EmbeddedTest.repeated_field', index=5,
      number=6, type=5, cpp_type=1, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=_descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('\272\246\035\014EmbeddedTest')),
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=130,
  serialized_end=348,
)


_TEST = _descriptor.Descriptor(
  name='Test',
  full_name='test_case_for_infer_schema.Test',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='int_field', full_name='test_case_for_infer_schema.Test.int_field', index=0,
      number=1, type=5, cpp_type=1, label=1,
      has_default_value=True, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=_descriptor._ParseOptions(descriptor_pb2.FieldOptions(), _b('\210\246\035d\220\246\035\364\003'))),
    _descriptor.FieldDescriptor(
      name='float_field', full_name='test_case_for_infer_schema.Test.float_field', index=1,
      number=2, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='string_field', full_name='test_case_for_infer_schema.Test.string_field', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='bool_field', full_name='test_case_for_infer_schema.Test.bool_field', index=3,
      number=4, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='repeated_field', full_name='test_case_for_infer_schema.Test.repeated_field', index=4,
      number=5, type=5, cpp_type=1, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='message_field', full_name='test_case_for_infer_schema.Test.message_field', index=5,
      number=6, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=_descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('\272\246\035\004Test\302\246\035\013Description')),
  is_extendable=False,
  syntax='proto2',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=351,
  serialized_end=567,
)

_TEST.fields_by_name['message_field'].message_type = _EMBEDDEDTEST
DESCRIPTOR.message_types_by_name['EmbeddedTest'] = _EMBEDDEDTEST
DESCRIPTOR.message_types_by_name['Test'] = _TEST

EmbeddedTest = _reflection.GeneratedProtocolMessageType('EmbeddedTest', (_message.Message,), dict(
  DESCRIPTOR = _EMBEDDEDTEST,
  __module__ = '_test_case_for_infer_schema_pb2'
  # @@protoc_insertion_point(class_scope:test_case_for_infer_schema.EmbeddedTest)
  ))
_sym_db.RegisterMessage(EmbeddedTest)

Test = _reflection.GeneratedProtocolMessageType('Test', (_message.Message,), dict(
  DESCRIPTOR = _TEST,
  __module__ = '_test_case_for_infer_schema_pb2'
  # @@protoc_insertion_point(class_scope:test_case_for_infer_schema.Test)
  ))
_sym_db.RegisterMessage(Test)


_EMBEDDEDTEST.fields_by_name['int_field'].has_options = True
_EMBEDDEDTEST.fields_by_name['int_field']._options = _descriptor._ParseOptions(descriptor_pb2.FieldOptions(), _b('\210\246\035\234\377\377\377\377\377\377\377\377\001\220\246\035\350\007\322\254\035\021This is int field'))
_EMBEDDEDTEST.has_options = True
_EMBEDDEDTEST._options = _descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('\272\246\035\014EmbeddedTest'))
_TEST.fields_by_name['int_field'].has_options = True
_TEST.fields_by_name['int_field']._options = _descriptor._ParseOptions(descriptor_pb2.FieldOptions(), _b('\210\246\035d\220\246\035\364\003'))
_TEST.has_options = True
_TEST._options = _descriptor._ParseOptions(descriptor_pb2.MessageOptions(), _b('\272\246\035\004Test\302\246\035\013Description'))
# @@protoc_insertion_point(module_scope)

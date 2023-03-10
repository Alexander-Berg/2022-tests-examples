# Generated by the protocol buffer compiler.  DO NOT EDIT!

from google.protobuf import descriptor
from google.protobuf import message
from google.protobuf import reflection
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)


DESCRIPTOR = descriptor.FileDescriptor(
  name='kernel/querydata/idl/querydata_structs.proto',
  package='NQueryData',
  serialized_pb='\n,kernel/querydata/idl/querydata_structs.proto\x12\nNQueryData\"R\n\x07TFactor\x12\x0c\n\x04Name\x18\x01 \x02(\t\x12\x13\n\x0bStringValue\x18\x02 \x01(\x0c\x12\x10\n\x08IntValue\x18\x03 \x01(\x03\x12\x12\n\nFloatValue\x18\x04 \x01(\x02\" \n\x0cTMergeTraits\x12\x10\n\x08Priority\x18\x01 \x01(\x04\";\n\nTKeyTraits\x12\x16\n\x0eMustBeInScheme\x18\x01 \x01(\x08\x12\x15\n\rIsPrioritized\x18\x02 \x01(\x08\"h\n\rTSourceSubkey\x12\x0b\n\x03Key\x18\x01 \x02(\t\x12\"\n\x04Type\x18\x02 \x02(\x0e\x32\x14.NQueryData.EKeyType\x12&\n\x06Traits\x18\x03 \x01(\x0b\x32\x16.NQueryData.TKeyTraits\"\xe0\x02\n\x0eTSourceFactors\x12\x12\n\nSourceName\x18\x01 \x02(\t\x12\x0f\n\x07Version\x18\x02 \x01(\x04\x12\x13\n\x0bShardNumber\x18\x0b \x01(\r\x12\x11\n\tSourceKey\x18\x04 \x01(\t\x12+\n\rSourceKeyType\x18\x05 \x01(\x0e\x32\x14.NQueryData.EKeyType\x12/\n\x0fSourceKeyTraits\x18\n \x01(\x0b\x32\x16.NQueryData.TKeyTraits\x12-\n\x0bMergeTraits\x18\t \x01(\x0b\x32\x18.NQueryData.TMergeTraits\x12\x30\n\rSourceSubkeys\x18\x06 \x03(\x0b\x32\x19.NQueryData.TSourceSubkey\x12$\n\x07\x46\x61\x63tors\x18\x03 \x03(\x0b\x32\x13.NQueryData.TFactor\x12\x0c\n\x04Json\x18\x08 \x01(\t\x12\x0e\n\x06\x43ommon\x18\x07 \x01(\x08\"?\n\nTQueryData\x12\x31\n\rSourceFactors\x18\x01 \x03(\x0b\x32\x1a.NQueryData.TSourceFactors\"\x1b\n\x0bTFactorMeta\x12\x0c\n\x04Name\x18\x01 \x02(\t\"S\n\nTRawFactor\x12\n\n\x02Id\x18\x01 \x02(\r\x12\x13\n\x0bStringValue\x18\x02 \x01(\x0c\x12\x10\n\x08IntValue\x18\x03 \x01(\x03\x12\x12\n\nFloatValue\x18\x04 \x01(\x02\"V\n\rTRawQueryData\x12\'\n\x07\x46\x61\x63tors\x18\x01 \x03(\x0b\x32\x16.NQueryData.TRawFactor\x12\x0c\n\x04Json\x18\x02 \x01(\t\x12\x0e\n\x06KeyRef\x18\x03 \x01(\t\"\x8d\x03\n\x10TFileDescription\x12;\n\x0bTrieVariant\x18\x06 \x01(\x0e\x32\x18.NQueryData.ETrieVariant:\x0cTV_COMP_TRIE\x12\x12\n\x06Shards\x18\x08 \x01(\x05:\x02-1\x12\x17\n\x0bShardNumber\x18\x07 \x01(\x05:\x02-1\x12\x12\n\nSourceName\x18\x01 \x02(\t\x12\x0f\n\x07Version\x18\x04 \x01(\x04\x12\x19\n\x11IndexingTimestamp\x18\x0c \x01(\x04\x12%\n\x07KeyType\x18\x02 \x02(\x0e\x32\x14.NQueryData.EKeyType\x12)\n\x0bSubkeyTypes\x18\t \x03(\x0e\x32\x14.NQueryData.EKeyType\x12,\n\x0b\x46\x61\x63torsMeta\x18\x03 \x03(\x0b\x32\x17.NQueryData.TFactorMeta\x12*\n\rCommonFactors\x18\x05 \x03(\x0b\x32\x13.NQueryData.TFactor\x12\x12\n\nCommonJson\x18\n \x01(\t\x12\x0f\n\x07HasJson\x18\x0b \x01(\x08*\xbe\x02\n\x08\x45KeyType\x12\x0b\n\x07KT_NONE\x10\x00\x12\r\n\tKT_SIMPLE\x10\x08\x12\x10\n\x0cKT_LOWERCASE\x10\x02\x12\r\n\tKT_DOPPEL\x10\x01\x12\x11\n\rKT_DOPPEL_TOK\x10\x07\x12\x12\n\x0eKT_DOPPEL_PAIR\x10\x0e\x12\n\n\x06KT_YID\x10\x04\x12\r\n\tKT_YLOGIN\x10\x05\x12\x11\n\rKT_YLOGINHASH\x10\x0c\x12\x0c\n\x08KT_DOCID\x10\t\x12\x13\n\x0fKT_SNIPPETDOCID\x10\x12\x12\x10\n\x0cKT_EXACT_URL\x10\x13\x12\x14\n\x10KT_SEARCHER_NAME\x10\x06\x12\x10\n\x0cKT_SERP_TYPE\x10\x14\x12\x15\n\x11KT_STRUCTURED_KEY\x10\x0f\x12\x12\n\x0eKT_USER_REGION\x10\x10\x12\n\n\x06KT_TLD\x10\x11\x12\x0c\n\x08KT_COUNT\x10\x15*}\n\x0c\x45TrieVariant\x12\x10\n\x0cTV_COMP_TRIE\x10\x00\x12\x11\n\rTV_CODEC_TRIE\x10\x01\x12\x11\n\rTV_SOLAR_TRIE\x10\x02\x12\x0f\n\x0bTV_METATRIE\x10\x03\x12\x16\n\x12TV_CODED_BLOB_TRIE\x10\x04\x12\x0c\n\x08TV_COUNT\x10\x05\x42\x02H\x01')

_EKEYTYPE = descriptor.EnumDescriptor(
  name='EKeyType',
  full_name='NQueryData.EKeyType',
  filename=None,
  file=DESCRIPTOR,
  values=[
    descriptor.EnumValueDescriptor(
      name='KT_NONE', index=0, number=0,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_SIMPLE', index=1, number=8,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_LOWERCASE', index=2, number=2,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_DOPPEL', index=3, number=1,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_DOPPEL_TOK', index=4, number=7,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_DOPPEL_PAIR', index=5, number=14,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_YID', index=6, number=4,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_YLOGIN', index=7, number=5,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_YLOGINHASH', index=8, number=12,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_DOCID', index=9, number=9,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_SNIPPETDOCID', index=10, number=18,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_EXACT_URL', index=11, number=19,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_SEARCHER_NAME', index=12, number=6,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_SERP_TYPE', index=13, number=20,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_STRUCTURED_KEY', index=14, number=15,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_USER_REGION', index=15, number=16,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_TLD', index=16, number=17,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='KT_COUNT', index=17, number=21,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=1368,
  serialized_end=1686,
)


_ETRIEVARIANT = descriptor.EnumDescriptor(
  name='ETrieVariant',
  full_name='NQueryData.ETrieVariant',
  filename=None,
  file=DESCRIPTOR,
  values=[
    descriptor.EnumValueDescriptor(
      name='TV_COMP_TRIE', index=0, number=0,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='TV_CODEC_TRIE', index=1, number=1,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='TV_SOLAR_TRIE', index=2, number=2,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='TV_METATRIE', index=3, number=3,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='TV_CODED_BLOB_TRIE', index=4, number=4,
      options=None,
      type=None),
    descriptor.EnumValueDescriptor(
      name='TV_COUNT', index=5, number=5,
      options=None,
      type=None),
  ],
  containing_type=None,
  options=None,
  serialized_start=1688,
  serialized_end=1813,
)


KT_NONE = 0
KT_SIMPLE = 8
KT_LOWERCASE = 2
KT_DOPPEL = 1
KT_DOPPEL_TOK = 7
KT_DOPPEL_PAIR = 14
KT_YID = 4
KT_YLOGIN = 5
KT_YLOGINHASH = 12
KT_DOCID = 9
KT_SNIPPETDOCID = 18
KT_EXACT_URL = 19
KT_SEARCHER_NAME = 6
KT_SERP_TYPE = 20
KT_STRUCTURED_KEY = 15
KT_USER_REGION = 16
KT_TLD = 17
KT_COUNT = 21
TV_COMP_TRIE = 0
TV_CODEC_TRIE = 1
TV_SOLAR_TRIE = 2
TV_METATRIE = 3
TV_CODED_BLOB_TRIE = 4
TV_COUNT = 5



_TFACTOR = descriptor.Descriptor(
  name='TFactor',
  full_name='NQueryData.TFactor',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='Name', full_name='NQueryData.TFactor.Name', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='StringValue', full_name='NQueryData.TFactor.StringValue', index=1,
      number=2, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='IntValue', full_name='NQueryData.TFactor.IntValue', index=2,
      number=3, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='FloatValue', full_name='NQueryData.TFactor.FloatValue', index=3,
      number=4, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=60,
  serialized_end=142,
)


_TMERGETRAITS = descriptor.Descriptor(
  name='TMergeTraits',
  full_name='NQueryData.TMergeTraits',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='Priority', full_name='NQueryData.TMergeTraits.Priority', index=0,
      number=1, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=144,
  serialized_end=176,
)


_TKEYTRAITS = descriptor.Descriptor(
  name='TKeyTraits',
  full_name='NQueryData.TKeyTraits',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='MustBeInScheme', full_name='NQueryData.TKeyTraits.MustBeInScheme', index=0,
      number=1, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='IsPrioritized', full_name='NQueryData.TKeyTraits.IsPrioritized', index=1,
      number=2, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=178,
  serialized_end=237,
)


_TSOURCESUBKEY = descriptor.Descriptor(
  name='TSourceSubkey',
  full_name='NQueryData.TSourceSubkey',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='Key', full_name='NQueryData.TSourceSubkey.Key', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Type', full_name='NQueryData.TSourceSubkey.Type', index=1,
      number=2, type=14, cpp_type=8, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Traits', full_name='NQueryData.TSourceSubkey.Traits', index=2,
      number=3, type=11, cpp_type=10, label=1,
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
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=239,
  serialized_end=343,
)


_TSOURCEFACTORS = descriptor.Descriptor(
  name='TSourceFactors',
  full_name='NQueryData.TSourceFactors',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='SourceName', full_name='NQueryData.TSourceFactors.SourceName', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Version', full_name='NQueryData.TSourceFactors.Version', index=1,
      number=2, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='ShardNumber', full_name='NQueryData.TSourceFactors.ShardNumber', index=2,
      number=11, type=13, cpp_type=3, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='SourceKey', full_name='NQueryData.TSourceFactors.SourceKey', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='SourceKeyType', full_name='NQueryData.TSourceFactors.SourceKeyType', index=4,
      number=5, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='SourceKeyTraits', full_name='NQueryData.TSourceFactors.SourceKeyTraits', index=5,
      number=10, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='MergeTraits', full_name='NQueryData.TSourceFactors.MergeTraits', index=6,
      number=9, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='SourceSubkeys', full_name='NQueryData.TSourceFactors.SourceSubkeys', index=7,
      number=6, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Factors', full_name='NQueryData.TSourceFactors.Factors', index=8,
      number=3, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Json', full_name='NQueryData.TSourceFactors.Json', index=9,
      number=8, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Common', full_name='NQueryData.TSourceFactors.Common', index=10,
      number=7, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=346,
  serialized_end=698,
)


_TQUERYDATA = descriptor.Descriptor(
  name='TQueryData',
  full_name='NQueryData.TQueryData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='SourceFactors', full_name='NQueryData.TQueryData.SourceFactors', index=0,
      number=1, type=11, cpp_type=10, label=3,
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
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=700,
  serialized_end=763,
)


_TFACTORMETA = descriptor.Descriptor(
  name='TFactorMeta',
  full_name='NQueryData.TFactorMeta',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='Name', full_name='NQueryData.TFactorMeta.Name', index=0,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=765,
  serialized_end=792,
)


_TRAWFACTOR = descriptor.Descriptor(
  name='TRawFactor',
  full_name='NQueryData.TRawFactor',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='Id', full_name='NQueryData.TRawFactor.Id', index=0,
      number=1, type=13, cpp_type=3, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='StringValue', full_name='NQueryData.TRawFactor.StringValue', index=1,
      number=2, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='IntValue', full_name='NQueryData.TRawFactor.IntValue', index=2,
      number=3, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='FloatValue', full_name='NQueryData.TRawFactor.FloatValue', index=3,
      number=4, type=2, cpp_type=6, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=794,
  serialized_end=877,
)


_TRAWQUERYDATA = descriptor.Descriptor(
  name='TRawQueryData',
  full_name='NQueryData.TRawQueryData',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='Factors', full_name='NQueryData.TRawQueryData.Factors', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Json', full_name='NQueryData.TRawQueryData.Json', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='KeyRef', full_name='NQueryData.TRawQueryData.KeyRef', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=879,
  serialized_end=965,
)


_TFILEDESCRIPTION = descriptor.Descriptor(
  name='TFileDescription',
  full_name='NQueryData.TFileDescription',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    descriptor.FieldDescriptor(
      name='TrieVariant', full_name='NQueryData.TFileDescription.TrieVariant', index=0,
      number=6, type=14, cpp_type=8, label=1,
      has_default_value=True, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Shards', full_name='NQueryData.TFileDescription.Shards', index=1,
      number=8, type=5, cpp_type=1, label=1,
      has_default_value=True, default_value=-1,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='ShardNumber', full_name='NQueryData.TFileDescription.ShardNumber', index=2,
      number=7, type=5, cpp_type=1, label=1,
      has_default_value=True, default_value=-1,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='SourceName', full_name='NQueryData.TFileDescription.SourceName', index=3,
      number=1, type=9, cpp_type=9, label=2,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='Version', full_name='NQueryData.TFileDescription.Version', index=4,
      number=4, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='IndexingTimestamp', full_name='NQueryData.TFileDescription.IndexingTimestamp', index=5,
      number=12, type=4, cpp_type=4, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='KeyType', full_name='NQueryData.TFileDescription.KeyType', index=6,
      number=2, type=14, cpp_type=8, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='SubkeyTypes', full_name='NQueryData.TFileDescription.SubkeyTypes', index=7,
      number=9, type=14, cpp_type=8, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='FactorsMeta', full_name='NQueryData.TFileDescription.FactorsMeta', index=8,
      number=3, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='CommonFactors', full_name='NQueryData.TFileDescription.CommonFactors', index=9,
      number=5, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='CommonJson', full_name='NQueryData.TFileDescription.CommonJson', index=10,
      number=10, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    descriptor.FieldDescriptor(
      name='HasJson', full_name='NQueryData.TFileDescription.HasJson', index=11,
      number=11, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=968,
  serialized_end=1365,
)


_TSOURCESUBKEY.fields_by_name['Type'].enum_type = _EKEYTYPE
_TSOURCESUBKEY.fields_by_name['Traits'].message_type = _TKEYTRAITS
_TSOURCEFACTORS.fields_by_name['SourceKeyType'].enum_type = _EKEYTYPE
_TSOURCEFACTORS.fields_by_name['SourceKeyTraits'].message_type = _TKEYTRAITS
_TSOURCEFACTORS.fields_by_name['MergeTraits'].message_type = _TMERGETRAITS
_TSOURCEFACTORS.fields_by_name['SourceSubkeys'].message_type = _TSOURCESUBKEY
_TSOURCEFACTORS.fields_by_name['Factors'].message_type = _TFACTOR
_TQUERYDATA.fields_by_name['SourceFactors'].message_type = _TSOURCEFACTORS
_TRAWQUERYDATA.fields_by_name['Factors'].message_type = _TRAWFACTOR
_TFILEDESCRIPTION.fields_by_name['TrieVariant'].enum_type = _ETRIEVARIANT
_TFILEDESCRIPTION.fields_by_name['KeyType'].enum_type = _EKEYTYPE
_TFILEDESCRIPTION.fields_by_name['SubkeyTypes'].enum_type = _EKEYTYPE
_TFILEDESCRIPTION.fields_by_name['FactorsMeta'].message_type = _TFACTORMETA
_TFILEDESCRIPTION.fields_by_name['CommonFactors'].message_type = _TFACTOR

class TFactor(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TFACTOR
  
  # @@protoc_insertion_point(class_scope:NQueryData.TFactor)

class TMergeTraits(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TMERGETRAITS
  
  # @@protoc_insertion_point(class_scope:NQueryData.TMergeTraits)

class TKeyTraits(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TKEYTRAITS
  
  # @@protoc_insertion_point(class_scope:NQueryData.TKeyTraits)

class TSourceSubkey(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TSOURCESUBKEY
  
  # @@protoc_insertion_point(class_scope:NQueryData.TSourceSubkey)

class TSourceFactors(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TSOURCEFACTORS
  
  # @@protoc_insertion_point(class_scope:NQueryData.TSourceFactors)

class TQueryData(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TQUERYDATA
  
  # @@protoc_insertion_point(class_scope:NQueryData.TQueryData)

class TFactorMeta(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TFACTORMETA
  
  # @@protoc_insertion_point(class_scope:NQueryData.TFactorMeta)

class TRawFactor(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TRAWFACTOR
  
  # @@protoc_insertion_point(class_scope:NQueryData.TRawFactor)

class TRawQueryData(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TRAWQUERYDATA
  
  # @@protoc_insertion_point(class_scope:NQueryData.TRawQueryData)

class TFileDescription(message.Message):
  __metaclass__ = reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _TFILEDESCRIPTION
  
  # @@protoc_insertion_point(class_scope:NQueryData.TFileDescription)

# @@protoc_insertion_point(module_scope)

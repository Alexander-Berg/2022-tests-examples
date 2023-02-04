from operator import attrgetter

from google.protobuf.descriptor import FieldDescriptor


def check_fields_presence(src, src_fields, dst, dst_fields, empty_string_as_nonpresent=False):
    def is_field_present(src, field):
        splitted_field = field.rsplit(".", maxsplit=1)
        if len(splitted_field) > 1:
            assert len(splitted_field) == 2, "Incorrect split"
            inner_message, field = splitted_field
            src = attrgetter(inner_message)(src)
        is_exist = src.HasField(field)
        if (
            is_exist
            and empty_string_as_nonpresent
            and src.DESCRIPTOR.fields_by_name[field].cpp_type == FieldDescriptor.CPPTYPE_STRING
            and not getattr(src, field)
        ):
            return False
        return is_exist

    assert len(src_fields) == len(dst_fields), "Different fields count: src %s, dst %s" % (
        len(src_fields),
        len(dst_fields),
    )

    for src_field, dst_field in zip(src_fields, dst_fields):
        is_src = is_field_present(src, src_field)
        is_dst = is_field_present(dst, dst_field)
        assert (
            is_src == is_dst
        ), "HasField for %s return %s in src proto, but HasField for %s return %s in dst proto" % (
            src_field,
            is_src,
            dst_field,
            is_dst,
        )

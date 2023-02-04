PROTO_LIBRARY()

OWNER(g:awacs)

SRCS(
    _test_case_for_blueprint.proto
    _test_case_for_infer_schema.proto
    _test_case_for_parse_request.proto
    test_awacs_field_schema.proto
    test_awacs_message_schema.proto
)

EXCLUDE_TAGS(GO_PROTO)

END()

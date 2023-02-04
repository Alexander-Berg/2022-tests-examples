def compare_requests_sets(test, validate):
    assert len(test) == len(validate)
    for test_sets, validate_sets in zip(test, validate):
        assert type(test_sets) == type(validate_sets)
        assert test_sets.name == validate_sets.name
        assert test_sets.requirements == validate_sets.requirements

        assert len(test_sets.requests) == len(validate_sets.requests)
        for test_request, validate_request in zip(test_sets.requests, validate_sets.requests):
            assert type(test_request) == type(validate_request)
            assert test_request.name == validate_request.name
            assert test_request.start_point == validate_request.start_point
            assert test_request.end_point == validate_request.end_point
            assert test_request.request_format == validate_request.request_format
            assert test_request.important == validate_request.important

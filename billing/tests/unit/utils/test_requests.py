import pytest
from multidict import CIMultiDict

from sendr_utils.requests import extract_correlation_headers


@pytest.mark.parametrize(
    'headers,expected',
    [
        (None, {}),
        ({}, {}),
        ({'Correlation-ID': 1, 'Other': 2}, {'Correlation-ID': 1}),
        (
            CIMultiDict([('Correlation-ID', 1), ('correlation-id', 2), ('Other', 3)]),
            {'Correlation-ID': 1, 'correlation-id': 2}
        ),
        (
            CIMultiDict(
                [
                    ('Correlation-ID', 1),
                    ('Correlation-ID', 10),
                    ('correlation-id', 2),
                    ('VTS-Correlation-Id', 3),
                    ('VTS-Response-Id', 4),
                    ('Some-Flow-id', 5),
                    ('and-request-id', 6),
                ]
            ),
            {
                'Correlation-ID': 10,
                'correlation-id': 2,
                'VTS-Correlation-Id': 3,
                'VTS-Response-Id': 4,
                'Some-Flow-id': 5,
                'and-request-id': 6,
            }
        ),
    ]
)
def test_extract_correlation_headers(headers, expected):
    assert extract_correlation_headers(headers) == expected

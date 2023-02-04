from maps.wikimap.stat.tasks_payment.tasks_logging.libs.parsing import isotime_normalizer


def test_unifies_isotime_formats_from_nmaps_logs():
    # Default
    assert isotime_normalizer()(b'2020-01-25T01:02:03+05:30') == b'2020-01-25T01:02:03+05:30'

    # Unusual date-time separator: space
    assert isotime_normalizer()(b'2020-01-25 01:02:03+05:30') == b'2020-01-25T01:02:03+05:30'

    # Timezone format: +HH
    assert isotime_normalizer()(b'2020-01-25T01:02:03+01') == b'2020-01-25T01:02:03+01:00'

    # Timespec: microseconds
    assert isotime_normalizer()(b'2020-01-25T01:02:03.456789+05:30') == b'2020-01-25T01:02:03+05:30'

    # Unusual timespec: truncated microseconds (less than 6 digits)
    assert isotime_normalizer()(b'2020-01-25T01:02:03.45678+09:00') == b'2020-01-25T01:02:03+09:00'


def test_preserves_specified_timezone():
    assert isotime_normalizer('Asia/Irkutsk')(b'2020-02-02T23:32:24+04:00') == b'2020-02-02T23:32:24+04:00'


def test_timezone_defaults_to_specified_default_timezone():
    assert isotime_normalizer('Asia/Colombo')(b'2020-02-01T13:25:12') == b'2020-02-01T13:25:12+05:30'


def test_timezone_defaults_to_Moscow_when_no_default_timezone_is_specified():
    assert b'2020-01-25T18:58:02+03:00' == isotime_normalizer()(b'2020-01-25T18:58:02')

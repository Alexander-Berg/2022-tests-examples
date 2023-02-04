import pytest

from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema.datatransfer import (
    LogfellerField, LogfellerSection, DatatransferSection,
)
from maps.infra.sedem.lib.config.schema.tests.shared import extract_errors


def test_logfeller_field_parse_obj() -> None:
    LogfellerField.parse_obj({
        'name': 'message',
        'type': 'YT_TYPE_STRING',
    })

    LogfellerField.parse_obj({
        'name': 'message',
        'type': 'YT_TYPE_STRING',
        'sort': True
    })


def test_logfeller_field_name() -> None:
    with pytest.raises(ValidationError, match=r'name\s+field required'):
        LogfellerField.parse_obj({
            'type': 'YT_TYPE_STRING',
        })


def test_logfeller_field_type() -> None:
    with pytest.raises(ValidationError, match=r'type\s+field required'):
        LogfellerField.parse_obj({
            'name': 'message',
        })


def test_logfeller_field_name_regex() -> None:
    with pytest.raises(ValidationError) as exc:
        LogfellerField.parse_obj({
            'name': '🦆',
            'type': 'YT_TYPE_STRING',
        })

    assert extract_errors(exc) == ['string does not match regex "^[-_a-z0-9]+$"']


def test_logfeller_field_type_regex() -> None:
    with pytest.raises(ValidationError) as exc:
        LogfellerField.parse_obj({
            'name': 'message',
            'type': 'My ___ Super ___ Mega ___ Type',
        })

    assert extract_errors(exc) == [
        "unexpected value; permitted: 'YT_TYPE_STRING', 'YT_TYPE_INT64', "
        "'YT_TYPE_INT32', 'YT_TYPE_INT16', 'YT_TYPE_INT8', 'YT_TYPE_UINT64', 'YT_TYPE_UINT32', "
        "'YT_TYPE_UINT16', 'YT_TYPE_UINT8', 'YT_TYPE_BOOL', 'YT_TYPE_DOUBLE', 'YT_TYPE_YSON', "
        "'YT_TYPE_DATETIME', 'YT_TYPE_TIMESTAMP', 'YT_TYPE_INTERVAL'"
    ]


def test_logfeller_field_sort_type() -> None:
    with pytest.raises(ValidationError) as exc:
        LogfellerField.parse_obj({
            'name': 'message',
            'type': 'YT_TYPE_STRING',
            'sort': 'Could you sort this field for me, please? (^.^)'
        })

    assert extract_errors(exc) == ["value could not be parsed to a boolean"]


def test_logfeller_section_parse_obj() -> None:
    matcher = LogfellerSection.parse_obj({
        'topic': 'maps/infra-super-topic',
        'log_name': 'maps/infra-super-log-name',
        'fields': [{
            'name': 'field',
            'type': 'YT_TYPE_YSON'
        }],
        'lifetime': '100500d',
        'yt_account': 'maps-duck-infra',
    })

    assert matcher == LogfellerSection.construct(
        topic='maps/infra-super-topic',
        log_name='maps/infra-super-log-name',
        fields=[
            LogfellerField.construct(
                name='field',
                type='YT_TYPE_YSON',
            ),
        ],
        lifetime='100500d',
        yt_account='maps-duck-infra',
    )


def test_logfeller_section_topic_required() -> None:
    with pytest.raises(ValidationError, match=r'topic\s+field required'):
        LogfellerSection.parse_obj({
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_log_name() -> None:
    with pytest.raises(ValidationError, match=r'log_name\s+field required'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_fields_is_a_list() -> None:
    with pytest.raises(ValidationError, match=r'fields\s+value is not a valid list'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': {
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            },
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_fields() -> None:
    with pytest.raises(ValidationError, match=r'fields\s+field required'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_lifetime() -> None:
    with pytest.raises(ValidationError, match=r'lifetime\s+field required'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_precision() -> None:
    logfeller = LogfellerSection.parse_obj({
        'topic': 'maps/infra-super-topic',
        'log_name': 'maps/infra-super-log-name',
        'fields': [{
            'name': 'field',
            'type': 'YT_TYPE_YSON'
        }],
        'timestamp_precision': 0,
        'lifetime': '100500d',
        'yt_account': 'maps-duck-infra',
    })

    assert logfeller.timestamp_precision == 0


def test_logfeller_section_precision_incorrect() -> None:
    with pytest.raises(ValidationError, match=r'ensure this value is greater than or equal to 0'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'timestamp_precision': -1,
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_precision_is_default() -> None:
    logfeller = LogfellerSection.parse_obj({
        'topic': 'maps/infra-super-topic',
        'log_name': 'maps/infra-super-log-name',
        'fields': [{
            'name': 'field',
            'type': 'YT_TYPE_YSON'
        }],
        'lifetime': '100500d',
        'yt_account': 'maps-duck-infra',
    })

    assert logfeller.timestamp_precision == 6


def test_logfeller_section_yt_cluster() -> None:
    logfeller = LogfellerSection.parse_obj({
        'topic': 'maps/infra-super-topic',
        'log_name': 'maps/infra-super-log-name',
        'fields': [{
            'name': 'field',
            'type': 'YT_TYPE_YSON'
        }],
        'lifetime': '100500d',
        'yt_cluster': 'Arnold',
        'yt_account': 'maps-duck-infra',
    })

    assert logfeller.yt_cluster == 'Arnold'


def test_logfeller_section_yt_cluster_incorrect() -> None:
    with pytest.raises(ValidationError, match=r"unexpected value; permitted: 'Hahn', 'Arnold'"):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
            'yt_cluster': '🦆',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_yt_cluster_is_default() -> None:
    logfeller = LogfellerSection.parse_obj({
        'topic': 'maps/infra-super-topic',
        'log_name': 'maps/infra-super-log-name',
        'fields': [{
            'name': 'field',
            'type': 'YT_TYPE_YSON'
        }],
        'lifetime': '100500d',
        'yt_account': 'maps-duck-infra',
    })

    assert logfeller.yt_cluster == 'Hahn'


def test_logfeller_section_yt_accout() -> None:
    with pytest.raises(ValidationError, match=r'yt_account\s+field required'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
        })


def test_logfeller_section_topic_regex() -> None:
    with pytest.raises(ValidationError, match=r'topic\s+string does not match regex'):
        LogfellerSection.parse_obj({
            'topic': 'maps-🦆-infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_log_name_regex() -> None:
    with pytest.raises(ValidationError, match=r'log_name\s+string does not match regex'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps-🦆-infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_lifetime_regex() -> None:
    with pytest.raises(ValidationError, match=r'lifetime\s+string does not match regex "\^\\d\+d\$"'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500🦆',
            'yt_account': 'maps-duck-infra',
        })


def test_logfeller_section_yt_account_regex() -> None:
    with pytest.raises(ValidationError, match=r'yt_account\s+string does not match regex'):
        LogfellerSection.parse_obj({
            'topic': 'maps/infra-super-topic',
            'log_name': 'maps/infra-super-log-name',
            'fields': [{
                'name': 'field',
                'type': 'YT_TYPE_YSON'
            }],
            'lifetime': '100500d',
            'yt_account': 'maps-🦆-infra',
        })


def test_transfer_section_parse_obj() -> None:
    transfer = DatatransferSection.parse_obj({
        'logfeller': {
            'duck': {
                'topic': 'maps/infra-super-topic',
                'log_name': 'maps/infra-super-log-name',
                'fields': [{
                    'name': 'field',
                    'type': 'YT_TYPE_YSON'
                }],
                'lifetime': '100500d',
                'yt_account': 'maps-duck-infra',
            }
        },
    })

    assert list(transfer.logfeller.keys()) == ['duck']


def test_transfer_section_parse_obj_transfer_id_invalid() -> None:
    with pytest.raises(ValidationError, match=r'logfeller -> __key__\s+string does not match regex'):
        DatatransferSection.parse_obj({
            'logfeller': {
                '🦆🦆🦆': {
                    'topic': 'maps/infra-super-topic',
                    'log_name': 'maps/infra-super-log-name',
                    'fields': [{
                        'name': 'field',
                        'type': 'YT_TYPE_YSON'
                    }],
                    'lifetime': '100500d',
                    'yt_account': 'maps-duck-infra',
                }
            }
        })


def test_datatransfer_section_parse_obj() -> None:
    matcher = DatatransferSection.parse_obj({
        'logfeller': {
            'duck': {
                'topic': 'maps/infra-super-topic',
                'log_name': 'maps/infra-super-log-name',
                'fields': [{
                    'name': 'field',
                    'type': 'YT_TYPE_YSON'
                }],
                'lifetime': '100500d',
                'timestamp_precision': 6,
                'yt_cluster': 'Hahn',
                'yt_account': 'maps-duck-infra',
            }
        },
    })

    assert matcher == DatatransferSection.construct(
        logfeller=dict(
            duck=LogfellerSection.construct(
                topic='maps/infra-super-topic',
                log_name='maps/infra-super-log-name',
                fields=[
                    LogfellerField.construct(
                        name='field',
                        type='YT_TYPE_YSON',
                    ),
                ],
                lifetime='100500d',
                timestamp_precision=6,
                yt_cluster='Hahn',
                yt_account='maps-duck-infra',
            )
        )
    )

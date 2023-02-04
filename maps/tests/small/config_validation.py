import pytest
import typing as tp
from dataclasses import dataclass


@dataclass
class ConfigValidationTestCase:
    name: str
    config: str
    error_message: tp.Optional[str]

    def __str__(self):
        return self.name


CASES = [
    ConfigValidationTestCase(
        name='storage_tvm_not_set',
        config="""\
generate pkg-a on tvm:1
expire pkg-a never
@stable deploy pkg-a to rtc:maps_a
    """,
        error_message='Tvm id must be set for storages'
    ),
    ConfigValidationTestCase(
        name='storage_tvm_correct_config',
        config="""\
generate pkg-a on tvm:1
expire pkg-a never
@stable deploy pkg-a to rtc:maps_a
group rtc:maps_core_ecstatic_storage_unstable has tvm:2
    """,
        error_message=None
    ),
    ConfigValidationTestCase(
        name='tvm_of_service_not_set',
        config="""\
generate pkg-a on tvm:1
expire pkg-a never
restrict download pkg-a
@stable deploy pkg-a to rtc:maps_a
group rtc:maps_core_ecstatic_storage_unstable has tvm:2
    """,
        error_message='You must setup tvm when deploying secure dataset pkg-a to rtc:maps_a'
    ),
    ConfigValidationTestCase(
        name='tvm_of_service_not_set',
        config="""\
generate pkg-a on tvm:1
expire pkg-a never
restrict download pkg-a
@stable deploy pkg-a to rtc:maps_a
group rtc:maps_core_ecstatic_storage_unstable has tvm:2
group rtc:maps_a has tvm:3
    """,
        error_message=None
    ),

]


@pytest.mark.parametrize('case', CASES, ids=str)
def test_validation(validate, case: ConfigValidationTestCase):
    if case.error_message is None:
        validate(case.config)
    else:
        with pytest.raises(Exception, match=case.error_message):
            validate(case.config)

import pytest
import typing as tp
from dataclasses import dataclass

from maps.infra.ecstatic.sandbox.reconfigurer.lib.config_parser import (Parser, ParserError)


@dataclass
class ParseTestCase:
    name: str
    config: str
    exception_content: tp.Optional[str] = None

    def __str__(self) -> str:
        return self.name


CASES = [
    ParseTestCase(
        name='empty',
        config=''
    ),
    ParseTestCase(
        name='example_1',
        config="""# Comment
    @stable deploy source to %group"""
    ),
    ParseTestCase(
        name='example_2',
        config="""# Comment
    @stable_myt deploy this-source to name:this_group@msk_myt (tolerance 100%)"""
    ),
    ParseTestCase(
        name='example_3',
        config="""# Comment
    @stable deploy source to %group
    priority source 100"""
    ),
    ParseTestCase(
        name='incorrect_token',
        config='!#$%',
        exception_content="illegal character '!'"
    ),
    ParseTestCase(
        name='incorrect_destination_1',
        config="""# Comment
    @stable_myt deploy this-source to name:this_group@msk_mytf (tolerance 100%)""",
        exception_content="illegal character 'f'"
    ),
    ParseTestCase(
        name='incorrect_destination_2',
        config="""# Comment
    @stable_myt deploy this-source to name:this_group@mskf_myt (tolerance 100%)""",
        exception_content=r":2: syntax error at `\('"
    ),
    ParseTestCase(
        name='incorrect_destination_3',
        config="""# Comment
    @stable_myt deploy this-source to name:this_group@msk_myt3 (tolerance 100%)""",
        exception_content=":2: syntax error at `3'"
    ),
    ParseTestCase(
        name='incorrect_switch',
        config="""# Comment
    switch dc of grp in parallel""",
    ),
    ParseTestCase(
        name='incorrect_priority_1',
        config="""# Comment
    @stable deploy source to %group
    priority source -1""",
        exception_content="Priority for source must be in range of 1 to 255"
    ),
    ParseTestCase(
        name='incorrect_priority_2',
        config="""# Comment
    @stable deploy source to %group
    priority source 256""",
        exception_content="Priority for source must be in range of 1 to 255"
    ),
    ParseTestCase(
        name='incorrect_replication_1',
        config="""replicate dataset from datavalidation""",
        exception_content="Unknown replication source for dataset"
    ),
    ParseTestCase(
        name='incorrect_replication_2',
        config="""replicate dataset from stable
    replicate dataset from testing""",
        exception_content="dataset replication has already been configured"
    ),
    ParseTestCase(
        name='incorrect_replication_3',
        config="""replicate dataset from stable
    generate dataset on tvm:12345""",
        exception_content="Repliction conflicts with generation for dataset dataset"
    ),
    ParseTestCase(
        name='incorrect_meta_switch_1',
        config="""switch 2 of rtc:maps_a, rtc:maps_b in parallel
    switch 1 of rtc:maps_a in parallel
    """,
        exception_content=""
    ),
    ParseTestCase(
        name='incorrect_meta_switch_2',
        config="""switch 2 of rtc:maps_a, rtc:maps_b in parallel
    switch 1 of rtc:maps_a, rtc:maps_c in parallel
    """,
        exception_content=""
    ),
    ParseTestCase(
        name='tvm_of_service',
        config="""group rtc:maps_a has tvm:12345""",
    ),
    ParseTestCase(
        name="duplicate_tvm_of_service",
        config="""group rtc:maps_a has tvm:12345
        group rtc:maps_a has tvm:12345
        """,
        exception_content="Tvm of rtc:maps_a was already configured"
    ),
]


@pytest.mark.parametrize('case', CASES, ids=str)
def test_parse(case: ParseTestCase):
    parser = Parser()
    if case.exception_content is None:
        parser.parse_file(case.config, '')
        return
    with pytest.raises(ParserError, match=case.exception_content):
        parser.parse_file(case.config, '')


def test_correct_replication():
    parser = Parser()
    config = """replicate dataset1 from stable
    replicate dataset2 from testing
    replicate dataset3 from stable
    """
    parser.parse_file(config, '')
    config = parser.dump_config()
    for dataset in ('dataset1', 'dataset2', 'dataset3'):
        assert config.acls[dataset] == ['tvm:2017335']
        assert config.expirations[dataset] is None
    assert config.dump_replication_config() == {
        'dataset1': {'from': 'stable', 'branches': []},
        'dataset2': {'from': 'testing', 'branches': []},
        'dataset3': {'from': 'stable', 'branches': []}
    }


def test_meta_switch():
    parser = Parser()
    config = """switch 2 of rtc:maps_a, rtc:maps_b in parallel"""
    parser.parse_file(config, '')
    config = parser.dump_config()

    assert len(config.host_parallel) == 1
    assert len(config.meta_switch_groups) == 1
    assert set(list(config.meta_switch_groups.values())[0]) == {'rtc:maps_a', 'rtc:maps_b'}

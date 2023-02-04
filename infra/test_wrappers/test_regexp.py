# coding: utf-8
import mock
import pytest

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ANY_MODULE
from awacs.wrappers.main import Regexp, Matcher, MatchFsm, MatchSourceIp, RegexpSection, MatchMethod, Http
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_regexp():
    pb = modules_pb2.RegexpModule()
    regexp = Regexp(pb)

    with pytest.raises(ValidationError) as e:
        regexp.validate()
    e.match('at least one of the "include_upstreams", "sections" must be specified')

    section_entry_pb = pb.sections.add()
    section_entry_pb.key = 'section_1'
    section_entry_pb.value.SetInParent()
    regexp.update_pb(pb)

    with mock.patch.object(regexp.sections['section_1'], 'validate',
                           side_effect=ValidationError('BAD')):
        with pytest.raises(ValidationError) as e:
            regexp.validate()
    e.match(r'sections\[section_1\].*BAD')

    with mock.patch.object(regexp.sections['section_1'], 'validate') as s_1_validate:
        regexp.validate()
    s_1_validate.assert_called_once()

    section_entry_pb = pb.sections.add()
    section_entry_pb.key = 'default_http'
    default_section_pb = section_entry_pb.value
    default_section_pb.matcher.SetInParent()
    regexp.update_pb(pb)

    with mock.patch.object(regexp.sections['section_1'], 'validate') as s_1_validate:
        with mock.patch.object(regexp.sections['default_http'], 'validate') as default_validate:
            regexp.validate()

    s_1_validate.assert_called_once()
    default_validate.assert_called_once()

    for call in s_1_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'section_1'

    for call in default_validate.mock_calls:
        _, args, kwargs = call
        assert kwargs['key'] == 'default_http'

    section_entry_pb = pb.sections.add()
    section_entry_pb.key = 'purum'
    default_section_2_pb = section_entry_pb.value
    default_section_2_pb.matcher.SetInParent()
    regexp.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        regexp.validate()
    e.match('too many sections with an empty matcher: "default_http", "purum"')

    default_section_2_pb.matcher.match_fsm.host = 'hhhost'
    regexp.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        regexp.validate()
    e.match(r'sections\[default_http\]: section with an empty matcher must go last')

    pb.ClearField('sections')
    pb.include_upstreams.SetInParent()
    regexp.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        regexp.validate()
    e.match('include_upstreams -> filter: is required')


def test_regexp_section():
    pb = modules_pb2.RegexpSection()
    pb.nested.errorlog.log = './log'

    section = RegexpSection(pb)

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate(preceding_modules=[ANY_MODULE])
    e.match('matcher: is required')

    pb.matcher.match_fsm.host = 'ya.ru'
    section.update_pb(pb)
    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate()
    e.match('must be a child of "regexp" module')

    with mock.patch.object(section, 'require_nested'):
        with pytest.raises(ValidationError) as e:
            section.validate(preceding_modules=[ANY_MODULE], key=RegexpSection.DEFAULT_KEY)
    e.match('"default" section must have an empty matcher')


def test_matcher():
    pb = modules_pb2.Matcher()
    matcher = Matcher(pb)
    assert matcher.is_empty()

    matcher.validate()

    with pytest.raises(ValidationError) as e:
        matcher.validate(is_nested=True)
    e.match('either "match_fsm", "match_source_ip", "match_method", '
            '"match_not", "match_and" or "match_or" must be specified')

    pb.match_fsm.host = 'test'
    matcher.update_pb(pb)

    matcher.validate()

    assert isinstance(matcher.match_fsm, MatchFsm)

    pb.match_source_ip.source_mask = 'test'
    matcher.update_pb(pb)

    assert isinstance(matcher.match_fsm, MatchFsm)
    assert isinstance(matcher.match_source_ip, MatchSourceIp)

    with pytest.raises(ValidationError) as e:
        matcher.validate()
    e.match('at most one of the "match_fsm", "match_source_ip", "match_method", '
            '"match_not", "match_and" or "match_or" must be specified')

    pb = modules_pb2.Matcher()
    nested_m_pb_1 = pb.match_and.add()
    nested_m_pb_1.match_fsm.host = 'a'
    nested_m_pb_2 = pb.match_and.add()
    subnested_m_pb_1 = nested_m_pb_2.match_or.add()
    subnested_m_pb_1.match_fsm.host = 'b'
    subnested_m_pb_2 = nested_m_pb_2.match_or.add()
    subnested_m_pb_2.match_fsm.host = 'c'

    matcher = Matcher(pb)
    assert len(matcher.match_and) == 2
    assert matcher.match_and[0].match_fsm.pb.host == 'a'
    assert len(matcher.match_and[1].match_or) == 2

    matcher.validate()

    with mock.patch.object(matcher.match_and[1].match_or[1].match_fsm, 'validate',
                           side_effect=ValidationError('BAD')):
        with pytest.raises(ValidationError) as e:
            matcher.validate()
    e.match(r'match_and\[1\] -> match_or\[1\] -> match_fsm: BAD')

    nested_m_pb_3 = pb.match_and.add()
    subnested_m_pb_1 = nested_m_pb_3.match_not.match_fsm
    subnested_m_pb_1.host = 'd'
    matcher = Matcher(pb)

    matcher.validate()

    matcher.to_config().to_lua()


def test_match_fsm():
    pb = modules_pb2.MatchFsm()
    match_fsm = MatchFsm(pb)
    pb.host = 'Host'
    pb.uri = '/test'
    match_fsm.update_pb(pb)

    with pytest.raises(ValidationError) as e:
        match_fsm.validate()
    e.match('at most one of the "host", "uri" must be specified')


def test_match_method():
    pb = modules_pb2.MatchMethod()
    match_method = MatchMethod(pb)

    e = get_validation_exception(match_method.validate)
    e.match(r'methods: is required')

    pb.methods[:] = ['XXX']
    e = get_validation_exception(match_method.validate)
    e.match(r'methods\[0\]: must be all lowercase')

    pb.methods[:] = ['xxx']
    e = get_validation_exception(match_method.validate)
    e.match(r'methods\[0\]: is not supported')

    pb.methods[:] = ['post', 'trace']
    e = get_validation_exception(match_method.validate)
    e.match(r'methods\[1\]: using "trace" method requires preceding "http" module with enabled "allow_trace" option')

    e = get_validation_exception(match_method.validate, preceding_modules=[Http(modules_pb2.HttpModule())])
    e.match(r'methods\[1\]: using "trace" method requires preceding "http" module with enabled "allow_trace" option')

    match_method.validate(preceding_modules=[Http(modules_pb2.HttpModule(allow_trace=True))])

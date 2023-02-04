# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import IncludeUpstreams, IncludeUpstreamsFilterSpec
from awtest.wrappers import get_validation_exception


def test_include_upstreams():
    pb = modules_pb2.IncludeUpstreams()
    m = IncludeUpstreams(pb)
    e = get_validation_exception(m.validate)
    e.match('filter: is required')

    pb.type = modules_pb2.BY_ID
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('ids: is required')

    pb.ids.append('test')
    m.update_pb(pb)
    m.validate()

    pb.type = modules_pb2.ALL
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('neither of "filter", "order", "ids" must be set')

    pb.type = modules_pb2.NONE
    pb.filter.SetInParent()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('ids: must not be set')

    del pb.ids[:]

    e = get_validation_exception(m.validate)
    e.match('filter: at least one of the "and", "any", "id", "id_prefix", "id_prefix_in", "id_suffix", "id_suffix_in", '
            '"ids", "not", "or" must be specified')

    pb.filter.id = 'soad'
    m.update_pb(pb)
    m.validate()

    p = m.filter.to_predicate()

    assert not p('muse')
    assert p('soad')

    pb.filter.id_prefix_in.extend(['nirvana', 'soad'])
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('filter: at most one of the "and", "any", "id", "id_prefix", "id_prefix_in", "id_suffix", "id_suffix_in", '
            '"ids", "not", "or" must be specified')

    pb.filter.id = ''
    m.update_pb(pb)
    m.validate()

    p = m.filter.to_predicate()

    assert not p('muse')
    assert p('soad')
    assert p('nirvana')

    del pb.filter.id_prefix_in[:]
    getattr(pb.filter, 'and').add(id_prefix='ni')
    getattr(pb.filter, 'and').add(id_suffix='na')
    m.update_pb(pb)
    m.validate()

    p = m.filter.to_predicate()

    assert not p('muse')
    assert not p('soad')
    assert p('nirvana')

    getattr(pb.filter, 'and').add()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'filter -> and\[2\]: at least one of the "and", "any", "id", "id_prefix", "id_prefix_in", '
            r'"id_suffix", "id_suffix_in", "ids", "not", "or" must be specified')


def test_include_upstreams_filter_1():
    pb = modules_pb2.IncludeUpstreams.FilterSpec()
    getattr(pb, 'and').add(id_suffix_in=['es', 'rs'])
    and_2_pb = getattr(pb, 'and').add()
    getattr(and_2_pb, 'not').id_prefix = 'beatles'

    m = IncludeUpstreamsFilterSpec(pb)
    m.validate()

    p = m.to_predicate()
    assert p('doors')
    assert p('libertines')
    assert not p('beatles')


def test_include_upstreams_filter_2():
    pb = modules_pb2.IncludeUpstreams.FilterSpec()
    getattr(pb, 'or').add(id='metallica')
    getattr(pb, 'or').add(id='muse')
    or_3_pb = getattr(pb, 'or').add()
    getattr(or_3_pb, 'and').add(id_prefix_in=('be', 'li'))
    getattr(or_3_pb, 'and').add(id_suffix='es')

    m = IncludeUpstreamsFilterSpec(pb)
    m.validate()

    p = m.to_predicate()
    assert not p('soad')
    assert p('metallica')
    assert p('muse')
    assert p('beatles')
    assert p('libertines')
    assert not p('linkinpark')


def test_include_upstreams_filter_3():
    pb = modules_pb2.IncludeUpstreams.FilterSpec()
    getattr(pb, 'and').add(ids=['metallica', 'muse', 'soad'])
    and_2_pb = getattr(pb, 'and').add()
    getattr(getattr(and_2_pb, 'not'), 'or').add(id_suffix_in=['ca', 'se'])
    getattr(getattr(and_2_pb, 'not'), 'or').add(id_prefix='so')

    m = IncludeUpstreamsFilterSpec(pb)
    m.validate()

    p = m.to_predicate()
    assert not p('test')
    assert not p('soad')
    assert not p('metallica')
    assert not p('muse')

    new_pb = modules_pb2.IncludeUpstreams.FilterSpec()
    getattr(new_pb, 'or').add().CopyFrom(pb)
    getattr(new_pb, 'or').add(id='muse')

    m = IncludeUpstreamsFilterSpec(new_pb)
    m.validate()

    p = m.to_predicate()
    assert not p('test')
    assert not p('soad')
    assert not p('metallica')
    assert p('muse')


def test_include_upstreams_filter_4():
    pb = modules_pb2.IncludeUpstreams()
    pb.filter.any = True
    pb.order.SetInParent()

    m = IncludeUpstreams(pb)
    e = get_validation_exception(m.validate)
    e.match('order -> label: is required')

    pb.order.label.SetInParent()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('order -> label -> name: is required')

    pb.order.label.name = 'my-super-label'
    m.update_pb(pb)
    m.validate()

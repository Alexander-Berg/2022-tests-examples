# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import CgiHasher
from awtest.wrappers import get_validation_exception


def test_cgi_hasher():
    pb = modules_pb2.CgiHasherModule()

    cgi_hasher = CgiHasher(pb)
    cgi_hasher.update_pb(pb)

    e = get_validation_exception(cgi_hasher.validate, chained_modules=True)
    e.match('parameters:.*is required')

    parameters_entry = pb.parameters.add()
    parameters_entry.value = 'one'

    cgi_hasher.update_pb(pb)
    cgi_hasher.validate(chained_modules=True)

    pb.randomize_empty_match.value = True
    cgi_hasher.update_pb(pb)
    cgi_hasher.validate(chained_modules=True)

    config = cgi_hasher.to_config()
    assert set(config.table.keys()) == {'parameters', 'randomize_empty_match', 'case_insensitive'}
    assert config.table['randomize_empty_match'] is True
    cgi_hasher.to_config()  # smoke test

    pb.randomize_empty_match.value = False
    cgi_hasher.update_pb(pb)
    cgi_hasher.validate(chained_modules=True)
    config = cgi_hasher.to_config()
    assert config.table['randomize_empty_match'] is False

    pb.ClearField('randomize_empty_match')
    cgi_hasher.update_pb(pb)
    cgi_hasher.validate(chained_modules=True)
    config = cgi_hasher.to_config()
    assert config.table['randomize_empty_match'] is CgiHasher.DEFAULT_RANDOMIZE_EMPTY_MATCH

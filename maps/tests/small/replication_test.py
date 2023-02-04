import yatest

from maps.infra.ecstatic.sandbox.reconfigurer.lib.config_parser import Parser


def test_replication_output():
    parser = Parser()
    config = yatest.common.test_source_path('data/replication.conf')
    with open(config) as file:
        parser.parse_file(file.read(), config)
    replication_config = parser.dump_config().dump_replication_config()
    for dataset in replication_config.keys():
        replication_config[dataset]['branches'] = set(replication_config[dataset]['branches'])
    assert replication_config == {'dataset1': {'from': 'stable', 'branches': {'stable', 'testing'}},
                                  'dataset2': {'from': 'testing', 'branches': {'stable'}}}

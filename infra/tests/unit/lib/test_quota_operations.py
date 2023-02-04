
from infra.capacity_planning.utils.quota_mover.src.lib.quota_operations import (
    transfers_from_parameters,
    get_unit_multipliers,
)


def test_transfers_from_parameters():
    provider = 'yp'
    segments = ['default']
    from_service = 'service_1'
    to_service = 'service_2'
    from_folder = 'folder_1'
    to_folder = 'folder_2'
    resource = ['cpu:1:core']
    ans = transfers_from_parameters(
        provider=provider,
        segments=segments,
        from_service=from_service,
        to_service=to_service,
        from_folder=from_folder,
        to_folder=to_folder,
        resource=resource,
    )
    row = ans[0]

    assert row['provider'] == provider
    assert row['segments'] == segments
    assert row['source_service'] == from_service
    assert row['target_service'] == to_service
    assert row['source_folder'] == from_folder
    assert row['target_folder'] == to_folder
    assert [row['resource'], str(row['amount']), row['unit']] == resource[0].split(':')


def test_get_unit_multipliers():
    ensemble_id = 'ens_id_1'
    unit_ensemblies = {
        'ens_id_1': [
            {
                'id': 'unit_id_1',
                'key': 'cores',
                'base': 2,
                'power': 10,
            },
            {
                'id': 'unit_id_2',
                'key': 'multi_cores',
                'base': 2,
                'power': 0,
            },
        ]
    }
    unit_multipliers = get_unit_multipliers(unit_ensemblies, ensemble_id)
    assert unit_multipliers['cores'] == (1024, 'multi_cores', 'unit_id_2')
    assert unit_multipliers['multi_cores'] == (1, 'multi_cores', 'unit_id_2')

from nirvana.operations import autorelease_to_nirvana_on_trunk_commit


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/my_other_new_operation/0.0.1',
    nirvana_quota='default',
    script_method='my_other_new_operation',
)
def my_other_new_operation():
    ...

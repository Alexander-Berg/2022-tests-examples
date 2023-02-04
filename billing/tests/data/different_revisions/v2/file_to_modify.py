from nirvana.operations import autorelease_to_nirvana_on_trunk_commit


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/my_constant_operation/0.0.1',
    nirvana_quota='ml-marines',
    script_method='my_constant_operation',
)
def my_constant_operation():
    ...


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/my_modified_operation/0.0.2',
    nirvana_quota='default',
    script_method='my_modified_operation',
)
def my_modified_operation():
    ...


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/my_new_operation/0.0.1',
    nirvana_quota='default',
    script_method='my_new_operation',
)
def my_new_operation():
    ...

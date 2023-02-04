from nirvana.operations import autorelease_to_nirvana_on_trunk_commit


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/dj_operation/0.1.22',
    nirvana_quota='ml-marines',
    vcs_token_nirvana_secret_name='solozobov_vcs_token',
    ya_make_folder_path='dj/nirvana/operations/dj/dj_operation/',
    ya_make_build_artifact_path='dj/nirvana/operations/dj/dj_operation/dj_operation',
    script_method='dj/dj_operation',
    sandbox_group='my_group',
)
def my_operation():
    ...


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/dj_operation2/4.1.22',
    script_method='dj/dj_operation',
    ya_make_build_artifact_path='path/path/path',
)
def my_operation2():
    ...


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/dj_operation2_5/4.1.22',
    script_method='dj/dj_operation',
    ya_make_build_artifact_path='path/path/path',
)
def my_operation2_5():
    ...


@autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/dj_operation3/4.1.22',
    script_method='dj/dj_operation',
    ya_make_build_artifact_path=None,
    ya_make_folder_path=None,
)
def my_operation3():
    ...

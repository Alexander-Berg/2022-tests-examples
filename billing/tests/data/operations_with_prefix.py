import nirvana
import vh3


@nirvana.operations.autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/dj_operation/0.1.22',
    script_method='dj/my_operation',
)
def my_operation():
    ...


@vh3.decorator.autorelease_to_nirvana_on_trunk_commit(
    version='http://nirvana.yandex-team.ru/alias/operation/dj_operation2/4.1.22',
    script_method='dj/my_operation2',
)
def my_operation2():
    ...


# @vh3.decorator.autorelease_to_nirvana_on_trunk_commit()
def my_operation3():
    ...

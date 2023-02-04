from nirvana.operations import autorelease_to_nirvana_on_trunk_commit


@operation()
@autorelease_to_nirvana_on_trunk_commit(version='http://nirvana.yandex-team.ru/alias/operation/my_operation/0.1.22')
@other_decorator()
def my_operation():
    ...


@autorelease_to_nirvana_on_trunk_commit(version='http://nirvana.yandex-team.ru/alias/operation/nope_operation/0.1.22')
class NopeOperation(...):
    ...

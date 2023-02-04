# coding: utf-8
from hamcrest import (
    has_entries
)


# здесь собраны разные декларативные проверки, которые можно
# переиспользовать в ассертах внутри юнит-тестов

# проверяем, что группа, представляет администраторов организации
organization_admin = has_entries(
    type='organization_admin',
    name=has_entries(en='Organization administrator'))

# проверка того, что группа является generic с указанным именем
generic_group = lambda name: has_entries(
    type='generic',
    name=has_entries(en=name))

# coding: utf-8
__author__ = 'blubimov'

import pytest

from btestlib.constants import ClientCategories
from post_restore_common import get_client_linked_with_login_or_create

"""
Восстановление данных необходимых для проверки интеграции Вертикали -> Balance
Заказчики: marbya
"""


# создать клиента и привязать к нему логин seme4kiniv
# создать агентство и привязать к нему логин andrej-kisilew
@pytest.mark.parametrize('login, client_category', [
    ('seme4kiniv', ClientCategories.CLIENT),    # актуально на 03.02.21
], ids=lambda login, cc: '{}'.format(login))
def test_restore_user_client_association(login, client_category):
    get_client_linked_with_login_or_create(login, client_category)

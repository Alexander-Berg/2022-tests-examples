# coding: utf-8

import pytest

from balance.actions.dcs.utils.session import ARRAY_SIZE, create_session


@pytest.mark.usefixtures('app')
def test_create_session():
    session = create_session()
    r = session.execute('select 1 from dual')
    assert r.cursor.arraysize == ARRAY_SIZE

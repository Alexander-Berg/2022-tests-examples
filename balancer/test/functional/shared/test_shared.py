# -*- coding: utf-8 -*-
import pytest

from configs import SharedTwoUuidsConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError


@pytest.mark.parametrize('workers', [
    None,
    2
], ids=[
    'no-workers',
    '2-workers'
])
@pytest.mark.parametrize('default_uuid', [
    'immortal', 'darkthrone'])
def test_shared_works(ctx, workers, default_uuid):
    """
    BALANCER-696
    shared c uuid без дубликатов должны работать,
    shared без подмодуля с uuid, указывающим на существующий модуль,
    должен передавать управление в соответствующий модуль.
    Наличие/отсутствие воркеров не должно влиять на работу
    """
    cfg = SharedTwoUuidsConfig(workers=workers,
                               darkthrone_uuid='darkthrone',
                               immortal_uuid='immortal',
                               default_uuid=default_uuid)

    ctx.start_balancer(cfg)
    response = ctx.perform_request(http.request.get())
    asserts.status(response, 200)
    asserts.content(response, default_uuid)


@pytest.mark.parametrize('workers', [
    None,
    2
], ids=[
    'no-workers',
    '2-workers'
])
def test_duplicates_forbidden(ctx, workers):
    """
    BALANCER-696
    При наличии двух модулей shared с подмодулями и одинаковыми uuid
    балансер не должен взлетать
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(SharedTwoUuidsConfig(
            immortal_uuid='uuid', darkthrone_uuid='uuid', default_uuid='uuid'))


@pytest.mark.parametrize('workers', [
    None,
    2
], ids=[
    'no-workers',
    '2-workers'
])
def test_orphans_forbidden(ctx, workers):
    """
    BALANCER-696
    Если у shared отсутствует подмодуль и нет другого shared с подмодулем
    с таким же uuid, балансер должен не взлетать
    """
    with pytest.raises(BalancerStartError):
        ctx.start_balancer(SharedTwoUuidsConfig(
            immortal_uuid='uuid', darkthrone_uuid='uuid', default_uuid='another_uuid'))

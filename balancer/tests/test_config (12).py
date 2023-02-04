# -*- coding: utf-8 -*-

NATIVE_LOCATION = 'MAN'


def test_admin(balancer_fixture):
    balancer_fixture.start()
    assert 1 == 1


def test_search_balancer_hint(term_ctx):
    term_ctx.run_knoss_balancer_hint(
        'search',
        '/search',
        ['backends_man#balancer_knoss_search_yp_man'],
        location=NATIVE_LOCATION,
        balancer_hint='man',
    )

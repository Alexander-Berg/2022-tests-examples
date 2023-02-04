# -*- coding: utf-8 -*-
def failing_test_admin(balancer_fixture):
    balancer_fixture.start()
    assert 1 == 1

# -*- coding: utf-8 -*-

import pytest

from tests import object_builder as ob


def create_passport(session, *roles):
    return ob.create_passport(session, *roles)


def create_role(session, *perms):
    return ob.create_role(session, *perms)


@pytest.fixture(name='client')
def create_client(session, **params):
    return ob.ClientBuilder.construct(session, **params)


@pytest.fixture(name='client_batch_id')
def create_client_batch_id(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    ).client_batch_id

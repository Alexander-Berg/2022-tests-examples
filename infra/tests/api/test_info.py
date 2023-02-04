from infra.cauth.server.public.constants import SOURCE_NAME
from infra.cauth.server.public.api.views.info import InfoController


def users_to_set(users):
    return {user.login for user in users}


def test_ok(client, server):
    client.server = server
    response = client.get('/info/')
    assert response.status_code == 200


def test_default_logic(server):
    controller = InfoController(server, sources=None, can_use_access=True)

    ssh_users = users_to_set(controller.ssh_users)
    root_users = users_to_set(controller.root_users)
    disabled_users = users_to_set(controller.disabled_users)

    assert ssh_users == {'user_ssh'}
    assert disabled_users == set()
    assert root_users == {
        'user_ssh_root',
        'user_ssh_2',
        'user_cms_1',
        'user_cms_2',
        'user_conductor_1',
        'user_conductor_2',
        'user_golem',
        'user_super',
    }


def test_empty_sources(server):
    controller = InfoController(server, sources=[], can_use_access=False)

    ssh_users = users_to_set(controller.ssh_users)
    root_users = users_to_set(controller.root_users)
    disabled_users = users_to_set(controller.disabled_users)

    assert ssh_users == set()
    assert root_users == set()
    assert disabled_users == {
        'user_ssh',
        'user_ssh_root',
        'user_ssh_2',
        'user_cms_1',
        'user_cms_2',
        'user_conductor_1',
        'user_conductor_2',
        'user_golem',
        'user_super',
    }


def test_idm_only_source(server, sources):
    controller = InfoController(server, sources=[sources[SOURCE_NAME.IDM]], can_use_access=True)

    ssh_users = users_to_set(controller.ssh_users)
    root_users = users_to_set(controller.root_users)
    disabled_users = users_to_set(controller.disabled_users)

    assert ssh_users == {'user_ssh'}
    assert root_users == {'user_ssh_root'}
    assert disabled_users == {
        'user_ssh_2',
        'user_cms_1',
        'user_cms_2',
        'user_conductor_1',
        'user_conductor_2',
        'user_golem',
        'user_super',
    }


def test_cms_source(server, sources):
    controller = InfoController(server, sources=[sources[SOURCE_NAME.CMS]], can_use_access=False)

    ssh_users = users_to_set(controller.ssh_users)
    root_users = users_to_set(controller.root_users)
    disabled_users = users_to_set(controller.disabled_users)

    assert ssh_users == set()
    assert root_users == {
        'user_cms_1',
        'user_cms_2',
        'user_super',
    }
    assert disabled_users == {
        'user_ssh',
        'user_ssh_2',
        'user_ssh_root',
        'user_conductor_1',
        'user_conductor_2',
        'user_golem',
    }


def test_idm_cms_source(server, sources):
    new_sources = [sources[SOURCE_NAME.IDM], sources[SOURCE_NAME.CMS]]
    controller = InfoController(server, sources=new_sources, can_use_access=True)

    ssh_users = users_to_set(controller.ssh_users)
    root_users = users_to_set(controller.root_users)
    disabled_users = users_to_set(controller.disabled_users)

    assert ssh_users == {'user_ssh'}
    assert root_users == {
        'user_cms_1',
        'user_cms_2',
        'user_super',
        'user_ssh_root',
    }
    assert disabled_users == {
        'user_ssh_2',
        'user_conductor_1',
        'user_conductor_2',
        'user_golem',
    }


def test_idm_and_cms_source(server, sources):
    new_sources = [sources[SOURCE_NAME.IDM_CMS]]
    controller = InfoController(server, sources=new_sources, can_use_access=True)

    ssh_users = users_to_set(controller.ssh_users)
    root_users = users_to_set(controller.root_users)
    disabled_users = users_to_set(controller.disabled_users)

    assert ssh_users == {'user_ssh'}
    assert root_users == {
        'user_cms_1',
        'user_ssh_root',
    }
    assert disabled_users == {
        'user_cms_2',
        'user_ssh_2',
        'user_conductor_1',
        'user_conductor_2',
        'user_golem',
        'user_super',
    }

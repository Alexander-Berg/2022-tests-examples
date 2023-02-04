# coding: utf-8


import pytest


@pytest.fixture()
def github(transactional_db,):
    from intranet.dogma.dogma.core.models import Source
    return Source.objects.create(
        name='Github Enterprise',
        code='github',
        vcs_type='git',
        web_type='github',
        web_url='https://github.com',
        host='github.com',
    )


@pytest.fixture()
def gitlab(transactional_db,):
    from intranet.dogma.dogma.core.models import Source
    return Source.objects.create(
        name='gitlab',
        code='gitlab',
        vcs_type='git',
        web_type='gitlab',
        web_url='https://gitlab.com',
        host='gitlab.com',
    )


@pytest.fixture()
def bitbucket(transactional_db,):
    from intranet.dogma.dogma.core.models import Source
    return Source.objects.create(
        name='bitbucket ext',
        code='bitbucket',
        vcs_type='git_stash',
        web_type='bitbucket_ext',
        web_url='https://bitbucket.org',
        host='bitbucket.org',
    )


@pytest.fixture()
def organization(transactional_db, github):
    from intranet.dogma.dogma.core.models import OrganisationsToClone
    return OrganisationsToClone.objects.create(
        source=github,
        name='some github org',
    )


@pytest.fixture()
def repo_github(transactional_db, github):
    from intranet.dogma.dogma.core.models import Repo
    return Repo.objects.create(
        source=github,
        name='dogma',
        owner='tools',
        vcs_name='tools/dogma',
        description='Догма',
    )


@pytest.fixture
def org_repo(transactional_db, github, organization):
    from intranet.dogma.dogma.core.models import Repo
    return Repo.objects.create(
        source=github,
        name='dogma1',
        owner='tools',
        vcs_name='tools/dogma1',
        description='Догма1',
        organisation=organization,
    )


@pytest.fixture
def clone_github(transactional_db, org_repo):
    from intranet.dogma.dogma.core.models import Clone
    from intranet.dogma.dogma.core.utils import get_current_node
    return Clone.objects.create(
        repo=org_repo,
        path='/some/path',
        node=get_current_node(create_missing=True),
        status=Clone.STATUSES.active,
    )


@pytest.fixture
def user(transactional_db,):
    from intranet.dogma.dogma.core.models import User
    return User.objects.create(
        uid='12345',
        login='vsem_privet',
        email='test@ya.ru',
        name='Someone',
        other_emails='test@ya.ru,anotheremail@test.com'
    )


@pytest.fixture
def operating_mode():
    from dir_data_sync.models import OperatingMode
    return OperatingMode.objects.create(
        name='test mode'
    )


@pytest.fixture
def org(operating_mode):
    from dir_data_sync.models import Organization
    return Organization.objects.create(
        id=1,
        name='yandex', dir_id='1', label='1',
        mode=operating_mode,
    )


@pytest.fixture
def another_org(operating_mode):
    from dir_data_sync.models import Organization
    return Organization.objects.create(
        name='yandex another', dir_id='2', label='2',
        mode=operating_mode,
    )


@pytest.fixture
def credential(transactional_db, org):
    from intranet.dogma.dogma.core.models import Credential
    return Credential.objects.create(
        name='test',
        auth_type='token',
        auth_data={'token': 'test token'},
        connect_organization=org,
    )


@pytest.fixture
def another_credential(transactional_db, another_org):
    from intranet.dogma.dogma.core.models import Credential
    return Credential.objects.create(
        name='test 1',
        auth_type='token 1',
        auth_data={'token': 'test token 1'},
        connect_organization=another_org,
    )

# coding: utf-8


import pytest

from .utils import THIS_REPO_PATH


@pytest.fixture()
def github(transactional_db,):
    from intranet.dogma.dogma.core.models import Source
    github = Source(
        name='Github Enterprise',
        code='github',
        vcs_type='git',
        web_type='github',
        web_url='https://github.example.com/',
        host='github.example.com',
    )
    github.save()
    return github


@pytest.fixture()
def repo(transactional_db, github):
    from intranet.dogma.dogma.core.models import Repo
    repo = Repo(
        source=github,
        name='dogma',
        owner='tools',
        vcs_name='tools/dogma',
        description='Догма',
    )
    repo.save()

    return repo


@pytest.fixture()
def clone(transactional_db, repo):
    from intranet.dogma.dogma.core.utils import get_current_node
    from intranet.dogma.dogma.core.models import Clone

    clone = Clone(
        repo=repo,
        node=get_current_node(),
        status='active',
        space_required=100500,
        path=THIS_REPO_PATH,  # Этот репозиторий
    )
    clone.save()
    return clone


@pytest.fixture
def repos(transactional_db, github):
    from intranet.dogma.dogma.core.utils import get_current_node
    from intranet.dogma.dogma.core.models import Repo
    repos = []
    for owner, name in [
        ('cardinalis', 'athos'),
        ('cardinalis', 'porthos'),
        ('roi', 'aramis'),
    ]:
        repo = Repo(
            source=github,
            name=name,
            owner=owner,
            vcs_name='%s/%s' % (owner, name),
            description=name,
        )
        repo.save()

        repo.clones.create(
            repo=repo,
            node=get_current_node(),
            status='active',
            space_required=100500,
            path=THIS_REPO_PATH,  # Этот репозиторий
        )
        repos.append(repo)

    return repos


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
def another_user(transactional_db,):
    from intranet.dogma.dogma.core.models import User
    return User.objects.create(
        uid='123456',
        login='smosker',
        email='smosker@ya.ru',
        name='Anotherone',
        other_emails='smosker@ya.ru, email@email.com'
    )


@pytest.fixture
def gerrit_commit(transactional_db, another_user):
    from intranet.dogma.dogma.core.utils import get_current_node
    from intranet.dogma.dogma.core.models import Repo, Source, PushedCommit
    source = Source.objects.create(
        vcs_type='gerrit', web_type='gerrit',
        host='testhost', web_url='https://gerrit.yandex-team.ru',
        status='success', code='gerrit',
    )
    repo = Repo.objects.create(
        source=source, owner='yandex-phone',
        name='yandex-phone/device/yandex',
        vcs_name='yandex-phone/device/yandex',

    )

    repo.clones.create(
        repo=repo,
        node=get_current_node(),
        status='active',
        space_required=100500,
        path=THIS_REPO_PATH,  # Этот репозиторий
    )
    return PushedCommit.objects.create(
        repo=repo,
        author=another_user,
        committer=another_user,
        commit='Ib602700dc9103241fd420d5f4e8903b15eac658d_1989',
        commit_time='2019-04-01T07:15:30-03:00',
        lines_added=20,
        lines_deleted=10,
        message='Revert "PHONE-4641: update default wallpaper"',
        branch_name='master',
        tree_hex='',
        commit_id='1989',
        create_changed_files=True,
    )


@pytest.fixture
def pushedcommits(transactional_db, repo, user):
    from intranet.dogma.dogma.core.models import PushedCommit
    pushedcommits = []

    pushedcommit = PushedCommit(
        repo=repo,
        author=user,
        committer=user,
        commit='edf1d67f432003d3d84d3d7b46af7fc518eec475',
        commit_time='1996-12-19T16:39:57-08:00',
        lines_added=43,
        lines_deleted=50,
        message='test commit number one',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d665',
        commit_id='17092ba4edd863ada20009558b9b55e194ffbe56',
    )
    pushedcommit.save()
    pushedcommits.append(pushedcommit)

    for i in range(210):
        new_pushedcommit = PushedCommit(
            repo=repo,
            author=user,
            committer=user,
            commit='edf1d67f432003d3d84d3d7b46af7fc518eec{}'.format(i),
            commit_time='1996-12-19T16:39:57-08:00',
            lines_added=5,
            lines_deleted=5,
            message='test commit number {}'.format(i),
            branch_name='master',
            tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d{}'.format(i),
            commit_id='17092ba4edd863ada20009558b9b55e194ffb{}'.format(i),
        )
        new_pushedcommit.save()
        new_pushedcommit.parents.add(pushedcommit)
        pushedcommits.append(new_pushedcommit)

    return pushedcommits


@pytest.fixture
def pushedcommits_dates(transactional_db, repo, user):
    from intranet.dogma.dogma.core.models import PushedCommit
    pushedcommits = []
    pushedcommit = PushedCommit(
        repo=repo,
        author=user,
        committer=user,
        commit='edf1d67f432003d3d84d3d7b46af7fc518eec475',
        commit_time='1996-12-19T16:39:57-08:00',
        lines_added=43,
        lines_deleted=50,
        message='test commit number one',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d665',
        commit_id='17092ba4edd863ada20009558b9b55e194ffbe56',
    )
    pushedcommit.save()
    pushedcommits.append(pushedcommit)

    pushedcommit = PushedCommit(
        repo=repo,
        author=user,
        committer=user,
        commit='0f7153fccd4a40b2b34bcb35fde5695ea2cabc6e',
        commit_time='2016-12-19T16:39:57-08:00',
        lines_added=5,
        lines_deleted=4,
        message='test commit number two',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a5678e58d665',
        commit_id='17092ba4edd863ada35689558b9b55e194ffbe56',
    )
    pushedcommit.save()
    pushedcommits.append(pushedcommit)
    return pushedcommits


@pytest.fixture
def commit(transactional_db, repo, another_user):
    from intranet.dogma.dogma.core.models import PushedCommit
    return PushedCommit.objects.create(
        repo=repo,
        author=another_user,
        committer=another_user,
        commit='edf1d67f432003d3d84d3d7b46af7fc518eec475',
        commit_time='1996-12-19T16:39:57-08:00',
        lines_added=43,
        lines_deleted=50,
        message='some message text',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d665',
        commit_id='17092ba4edd863ada20009558b9b55e194ffbe56',
        tickets=['TEST-5'],
        queues=['TEST'],
    )


@pytest.fixture
def commit_duplicate(transactional_db, commit, repo, another_user):
    from intranet.dogma.dogma.core.models import PushedCommitDuplicate
    return PushedCommitDuplicate.objects.create(
        repo=repo,
        author=another_user,
        committer=another_user,
        commit=commit.commit,
        commit_time=commit.commit_time,
        lines_added=commit.lines_added,
        lines_deleted=commit.lines_deleted,
        message=commit.message,
        branch_name=commit.branch_name,
        tree_hex=commit.tree_hex,
        commit_id=commit.commit_id,
        tickets=commit.tickets,
        queues=commit.queues,
    )

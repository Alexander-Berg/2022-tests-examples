import mock
import pytest

from staff.person_avatar.controllers import PreprofileAvatarCollection
from staff.person_avatar.tasks import CheckUserPhotos
from hashlib import md5
from staff.person_avatar.models import AvatarMetadata, PreprofileExtraFields
from staff.preprofile.tests.utils import PreprofileFactory
from staff.lib.testing import DepartmentFactory
from staff.preprofile.models import EMAIL_DOMAIN


@pytest.mark.django_db
def test_preprofile_avatar_collection_operations():
    AVATAR_URL = 'avatar_url'

    storage_calls = []

    class AvatarStorageMock(object):
        def __init__(self, scope):
            self.scope = scope

        def __getattr__(self, item):
            def generic_func(*args, **kwargs):
                storage_calls.append({'method': item, 'args': args, 'kwargs': kwargs})
            return generic_func

    dep = DepartmentFactory(name='myndex')
    preprofile = PreprofileFactory(
        login='login',
        email_domain=EMAIL_DOMAIN.YANDEX_TEAM_COM_UA,
        department=dep,
    )

    collection = PreprofileAvatarCollection(owner=preprofile)
    assert collection.count() == 0
    assert collection.exists() is False

    # Загружаем первую аватарку.
    path = 'staff.person_avatar.controllers.base.CheckUserPhotos'
    with mock.patch(path, return_value=mock.MagicMock()) as check_avatars_task:
        with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
            collection.upload(picture_url=AVATAR_URL)

    # В рантайме прошла только одна загрузка по id. Остальное отложенно.
    assert len(storage_calls) == 1
    uploaded_avatar = collection.first()
    assert storage_calls[0] == {
        'method': 'upload_by_url',
        'args': (str(uploaded_avatar.id), AVATAR_URL),
        'kwargs': {},
    }
    storage_calls = []

    # В базе есть записи о том, что надо загрузить таской
    extra = PreprofileExtraFields.objects.get(preprofile=preprofile)
    assert extra.avatar_img_for_upload_id == uploaded_avatar.id
    assert extra.main_img_for_upload_id == uploaded_avatar.id
    assert extra.gravatar_img_for_upload_id == uploaded_avatar.id

    # Поставили таску на через 10 сек с правильным owner_id
    assert check_avatars_task.called and check_avatars_task.call_count == 1
    assert check_avatars_task.call_args == mock.call(
        owner_type='preprofile',
        owner_id=preprofile.id,
    )

    # Теперь выполним таску c теми же параметрами по-настоящему (без celery).
    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        CheckUserPhotos(owner_type='preprofile', owner_id=preprofile.id)

    email_hash = md5(
        '{}@{}'.format(preprofile.login, preprofile.email_domain).encode('utf-8')
    ).hexdigest()

    # Таска залила в сторадж эту картинку как main и как avatar и как граватар
    assert storage_calls[0] == {
        'method': 'upload_by_url',
        'args': (
            '{}-main'.format(preprofile.login), uploaded_avatar.url
        ),
        'kwargs': {},
    }
    assert storage_calls[1] == {
        'method': 'upload_by_url',
        'args': (
            '{}-avatar'.format(preprofile.login), uploaded_avatar.url
        ),
        'kwargs': {},
    }
    assert storage_calls[2] == {
        'method': 'upload_by_url',
        'args': (
            email_hash, uploaded_avatar.url
        ),
        'kwargs': {},
    }

    storage_calls = []

    av = AvatarMetadata.objects.get(preprofile=preprofile)
    assert av.is_deleted is False
    assert av.is_main is True
    assert av.is_avatar is True

    # Убедимся что таска почистила за собой поля в ExtraFields
    extra = PreprofileExtraFields.objects.get(id=extra.id)
    assert extra.main_img_for_upload is None
    assert extra.avatar_img_for_upload is None
    assert extra.gravatar_img_for_upload is None

    # Отлично. Теперь у нас preprofile с одной фоткой, которая во всех трёх ролях.
    # Добавим вторую аватарку заливкой из файла и убедимся что первая удалена и все роле перенесены на залитую новую

    import staff
    with open('{root}/api/1px.png'.format(root=staff.__path__[0]), 'rb') as file_:
        photo_file = file_.read()

    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        collection.upload(picture_file=photo_file)

    # количество не изменилось
    assert collection.count() == 1

    metas = list(AvatarMetadata.objects.filter(preprofile=preprofile).order_by('id'))
    assert len(metas) == 2

    assert metas[0].is_deleted is True
    assert metas[0].is_avatar is False
    assert metas[0].is_main is False

    assert metas[1].is_deleted is False
    assert metas[1].is_avatar is True
    assert metas[1].is_main is True

    assert len(storage_calls) == 5

    uploaded_avatar = collection.first()

    # залили в сторадж новую фотку файлом
    assert storage_calls[0] == {'args': (str(uploaded_avatar.id), photo_file), 'method': 'upload_by_file', 'kwargs': {}}
    # залили новые признаки
    assert storage_calls[1] == {
        'args': ('{}-main'.format(preprofile.login), uploaded_avatar.url),
        'method': 'upload_by_url',
        'kwargs': {},
    }
    assert storage_calls[2] == {
        'args': ('{}-avatar'.format(preprofile.login), uploaded_avatar.url),
        'method': 'upload_by_url',
        'kwargs': {},
    }
    assert storage_calls[3] == {'args': (email_hash, uploaded_avatar.url), 'method': 'upload_by_url', 'kwargs': {}}

    # удалили старую фотку из стоража
    assert storage_calls[4] == {'args': (str(metas[0].id),), 'method': 'delete', 'kwargs': {}}

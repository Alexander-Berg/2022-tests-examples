import mock

from staff.person.models import StaffExtraFields
from staff.person_avatar.controllers import PersonAvatarCollection, AVATAR
from staff.person_avatar.tasks import CheckUserPhotos
from hashlib import md5
from staff.person_avatar.models import AvatarMetadata


def test_person_avatar_collection_operations(company):
    test_avatar_url = 'lorem_ipsum'

    storage_calls = []

    class AvatarStorageMock(object):
        def __init__(self, scope):
            self.scope = scope

        def __getattr__(self, item):
            def generic_func(*args, **kwargs):
                storage_calls.append({'method': item, 'args': args, 'kwargs': kwargs})
            return generic_func

    person = company.persons['yandex-chief']

    # Для загрузки граватарок сотруднику нужен рабочий email
    person.work_email = '{login}@yandex-team.ru'.format(login=person.login)
    person.save()

    collection = PersonAvatarCollection(owner=person)
    assert collection.count() == 0
    assert collection.exists() is False

    # Пользователь загружает первую аватарку.
    path = 'staff.person_avatar.controllers.base.CheckUserPhotos'
    with mock.patch(path, return_value=mock.MagicMock()) as check_avatars_task:
        with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
            collection.upload(picture_url=test_avatar_url)

    # В рантайме прошла только одна загрузка по id. Остальное отложенно.
    assert len(storage_calls) == 1
    uploded_avatar = collection.first()
    assert storage_calls[0] == {
        'method': 'upload_by_url',
        'args': (str(uploded_avatar.id), test_avatar_url),
        'kwargs': {},
    }
    storage_calls = []

    # В базе есть записи о том, что надо загрузить таской
    extra = StaffExtraFields.objects.get(staff=person)
    assert extra.avatar_img_for_upload_id == uploded_avatar.id
    assert extra.main_img_for_upload_id == uploded_avatar.id
    assert extra.gravatar_img_for_upload_id == uploded_avatar.id

    # Поставили таску на через 10 сек с правильным owner_id
    assert check_avatars_task.called and check_avatars_task.call_count == 1
    assert check_avatars_task.call_args == mock.call(
        owner_type='person',
        owner_id=person.id,
    )

    # Теперь выполним таску c теми же параметрами по-настоящему (без celery).
    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        CheckUserPhotos(owner_type='person', owner_id=person.id)

    email_hash = md5(person.work_email.encode('utf-8')).hexdigest()

    # Таска залила в сторадж эту картинку как main и как avatar
    assert storage_calls[0] == {
        'method': 'upload_by_url',
        'args': (
            '{}-main'.format(person.login), uploded_avatar.url
        ),
        'kwargs': {},
    }
    assert storage_calls[1] == {
        'method': 'upload_by_url',
        'args': (
            '{}-avatar'.format(person.login), uploded_avatar.url
        ),
        'kwargs': {},
    }
    assert storage_calls[2] == {
        'method': 'upload_by_url',
        'args': (
            email_hash, uploded_avatar.url
        ),
        'kwargs': {},
    }
    storage_calls = []
    # Убедимся что таска почистила за собой поля в ExtraFields
    extra = StaffExtraFields.objects.get(id=extra.id)
    assert extra.main_img_for_upload is None
    assert extra.avatar_img_for_upload is None
    assert extra.gravatar_img_for_upload is None

    # Отлично. Теперь у нас сотрудник с одной фоткой, которая во всех трёх ролях.
    # Добавим вторую аватарку заливкой из файла
    import staff
    with open('{root}/api/1px.png'.format(root=staff.__path__[0]), 'rb') as file_:
        photo_file = file_.read()

    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        collection.upload(picture_file=photo_file)

    assert collection.count() == 2
    uploded_avatar = collection.owner_avatars[-1]

    assert storage_calls[0] == {
        'method': 'upload_by_file',
        'args': (str(uploded_avatar.id), photo_file),
        'kwargs': {},
    }
    storage_calls = []

    # Всё хорошо. Первая картинка со всеми ролями, вторая - просто картинка.
    # Назначим вторую картинку аватаркой.
    path = 'staff.person_avatar.controllers.base.CheckUserPhotos'
    with mock.patch(path, return_value=mock.MagicMock()) as check_avatars_task:
        collection.make(uploded_avatar.id, [AVATAR])

    # Таска была поставлена в очередь с правильным owner_id
    assert check_avatars_task.called and check_avatars_task.call_count == 1
    assert check_avatars_task.call_args == mock.call(
        owner_type='person',
        owner_id=person.id,
    )

    extra = StaffExtraFields.objects.get(id=extra.id)
    assert extra.avatar_img_for_upload_id == uploded_avatar.id

    # Теперь выполним таску c теми же параметрами по-настоящему.
    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        CheckUserPhotos(owner_type='person', owner_id=person.id)

    # залили в сторадж новый avatar
    assert storage_calls[0] == {
        'method': 'upload_by_url',
        'args': ('{}-avatar'.format(person.login), uploded_avatar.url),
        'kwargs': {},
    }

    # залили как gravatar
    assert storage_calls[1] == {'args': (email_hash, uploded_avatar.url), 'method': 'upload_by_url', 'kwargs': {}}

    storage_calls = []

    # Вторая картинка теперь аватарка
    assert AvatarMetadata.objects.get(id=uploded_avatar.id).is_avatar

    # Набъём до масимума 5-ти фоток
    count_to_max = collection.AVATARS_MAX_COUNT - collection.count()
    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        for _ in range(count_to_max):
            collection.upload(picture_file=photo_file)

    assert collection.count() == collection.AVATARS_MAX_COUNT
    # Лишних запросов не было
    assert len(storage_calls) == count_to_max
    for call in storage_calls:
        assert call['method'] == 'upload_by_file'

    storage_calls = []

    # Теперь интересное. Добавляем 6-ю фотку, при этом должна удалиться первая,
    # а так как она main, то после её удаления должны вторую фотку сдлеать main

    first_avatar_before = collection.first()  # первая фотка до удаления

    with mock.patch('staff.person_avatar.controllers.base.AvatarStorage', new=AvatarStorageMock):
        collection.upload(picture_url=test_avatar_url)

    # количество не изменилось
    assert collection.count() == collection.AVATARS_MAX_COUNT

    first_avatar_after = collection.first()  # первая фотка после удаления
    uploded_avatar = collection.owner_avatars[-1]
    assert len(storage_calls) == 4

    # залили в сторадж новую фотку
    assert storage_calls[0] == {
        'args': (str(uploded_avatar.id), test_avatar_url),
        'method': 'upload_by_url',
        'kwargs': {},
    }

    # удалили из стораджа старый main
    assert storage_calls[1] == {'args': ('{}-main'.format(person.login),), 'method': 'delete', 'kwargs': {}}

    # сделали первую фотку (ранее вторую) main
    assert storage_calls[2] == {
        'args': ('{}-main'.format(person.login), first_avatar_after.url),
        'method': 'upload_by_url',
        'kwargs': {},
    }

    # удалили из стораджа старую фотку по id
    assert storage_calls[3] == {'args': (str(first_avatar_before.id),), 'method': 'delete', 'kwargs': {}}

    assert AvatarMetadata.objects.get(id=first_avatar_before.id).is_deleted is True

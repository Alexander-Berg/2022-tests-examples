from builtins import object

import pytest
from mock import MagicMock, call
from PIL import Image

from kelvin.resources.models import Resource, ResourceTag, resource_upload_path
from kelvin.resources.services import set_image_dimensions


class TestResourceTag(object):
    """
    Тесты для модели `ResourceTag`
    """
    unicode_cases = [
        ({'name': u'Бульдог 4 метра'}, u'Бульдог 4 метра'),
        ({'name': u'Nondimenticabile'}, u'Nondimenticabile'),
        ({'name': u''}, u''),
    ]

    @pytest.mark.parametrize('initial,expected', unicode_cases)
    def test_unicode(self, initial, expected):
        """
        Тесты метода `__str__`
        """
        assert ResourceTag(**initial).__str__() == expected


class TestResource(object):
    """
    Тесты модели ресурсов
    """
    unicode_cases = [
        (
            {'id': 1},
            'http://example.com/1.png',
            '1: : http://example.com/1.png',
        ),
        (
            {'id': 2},
            '',
            '2: : ',
        ),
        (
            {'id': 3},
            'http://example.com/3.png',
            '3: : http://example.com/3.png',
        ),
        (
            {'id': 3, 'name': u'Картинка'},
            'http://example.com/3.png',
            u'3: Картинка: http://example.com/3.png',
        ),
    ]

    @pytest.mark.parametrize('initial,get_content_url_ret,expected',
                             unicode_cases)
    def test_unicode(self, mocker, initial, get_content_url_ret, expected):
        """
        Тесты метода `__str__`
        """
        resource = Resource(**initial)
        resource_get_content_url = mocker.patch.object(resource,
                                                       'get_content_url')
        resource_get_content_url.return_value = get_content_url_ret

        assert resource.__str__() == expected, (
            u"Неправильное строковое представление объекта")
        assert resource_get_content_url.called, (
            u"Не было получения ссылки на файл ресурса")

    get_content_url_cases = (
        (
            Resource(id=1, file=MagicMock(url='fileurl')),
            'fileurl',
        ),
        (
            Resource(id=2, file=MagicMock(url='')),
            '',
        ),
    )

    @pytest.mark.parametrize("case,expected", get_content_url_cases)
    def test_get_content_url(self, case, expected):
        """
        Тест `get_content_url`
        """
        assert case.get_content_url() == expected, (
            u"Неправильная ссылка на контент"
        )

    get_type_cases = (
        (
            {'id': 1},
            {'name': '/var/example/1.png'},
            'png',
        ),
        (
            {'id': 1},
            {'name': '/var/example/image.gif'},
            'gif',
        ),
        (
            {'id': 1},
            {'name': '/var/example/file.mp4'},
            'mp4',
        ),
        (
            {'id': 1},
            {'name': '/var/example/file'},
            'other',
        ),
        (
            {'id': 1},
            {},
            '',
        ),
    )

    @pytest.mark.parametrize('initial,file_args,expected', get_type_cases)
    def test_get_type(self, initial, file_args, expected, mocker):
        """
        Тестироване метода `get_type`
        """
        resource = Resource(**initial)
        if file_args:
            resource.file = file_args['name']
        assert resource.get_type() == expected


def test_resource_upload_path(mocker):
    """
    Тест получения пути для загрузки файла ресурса
    """
    resource = MagicMock()
    mocked_safe_filename = mocker.patch(
        'kelvin.resources.models.safe_filename')
    mocked_safe_filename.return_value = 'safe.png'
    assert (resource_upload_path(resource, 'unsafe.png') == 'safe.png'), u'Неправильные имя и путь файла'
    assert mocked_safe_filename.mock_calls == [
        call('unsafe.png', 'resources')], (
        u'Должно быть преобразование имени файла в безопасное и уникальное')
    assert resource.mock_calls == [], (
        u'Не должно быть обращений к инстансу ресурса')


@pytest.mark.skip()
def test_check_image_dimensions(mocker):
    """
    Тест обработчика изменений файла ресурса
    """
    mocked_sender = MagicMock()
    res_file = MagicMock()
    resource = Resource(image_width=100, image_height=100, file=res_file)
    mocked_image_open = mocker.patch.object(Image, 'open')
    mocked_tracker = mocker.patch.object(resource, 'tracker')
    kwargs = {}

    # Файл не поменялся - ничего не должно произойти
    mocked_tracker.has_changed.return_value = False

    ret = set_image_dimensions(mocked_sender, resource, **kwargs)

    assert ret is None, u'Обработчик не должен ничего возвращать'
    assert mocked_tracker.mock_calls == [
        call.has_changed('file'),
    ], u'Должна быть проверка изменения файла'
    assert mocked_image_open.mock_calls == [], (
        u'Если файл не изменился, то PIL не должен открывать файл')
    assert resource.image_width == 100, u'Ширина не должна измениться'
    assert resource.image_height == 100, u'Высота не должна измениться'

    # Файл поменялся на не-изображение
    mocked_image_open.reset_mock()
    mocked_image_open.side_effect = IOError('not a valid image file')
    mocked_tracker.reset_mock()
    mocked_tracker.has_changed.return_value = True

    ret = set_image_dimensions(mocked_sender, resource, **kwargs)

    assert ret is None, u'Обработчик не должен ничего возвращать'
    assert mocked_tracker.mock_calls == [
        call.has_changed('file'),
    ], u'Должна быть проверка изменения файла'
    assert mocked_image_open.mock_calls == [call(res_file)], (
        u'При изменении файла PIL должен его открыть')
    assert resource.image_width is None, (
        u'Ширина для не-картинки должна выставиться None')
    assert resource.image_height is None, (
        u'Высота для не-картинки должна выставиться None')

    # Файл поменялся на изображение
    mocked_image_open.reset_mock()
    mocked_image_open.side_effect = None
    mocked_image_open.return_value = MagicMock(size=(200, 200))
    mocked_tracker.reset_mock()

    ret = set_image_dimensions(mocked_sender, resource, **kwargs)

    assert ret is None, u'Обработчик не должен ничего возвращать'
    assert mocked_tracker.mock_calls == [
        call.has_changed('file'),
    ], u'Должна быть проверка изменения файла'
    assert mocked_image_open.mock_calls == [call(res_file)], (
        u'При изменении файла PIL должен его открыть')
    assert resource.image_width == 200, (
        u'При успешном открытии должна быть задана ширина')
    assert resource.image_height == 200, (
        u'При успешном открытии должна быть задана высота')

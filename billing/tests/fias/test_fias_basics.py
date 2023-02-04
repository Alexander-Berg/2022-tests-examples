from datetime import date
from textwrap import dedent

import pytest

from freezegun import freeze_time
from refs.core.common import run_synchronizers
from refs.core.models import Log
from refs.fias.models import House, AddrObject, Room
from refs.fias.synchronizers import FiasSynchronizer
from refs.fias.tasks import sync_fias


COMMON_RESPONSE = (
    '[{"VersionId":604,"TextVersion":"Версия БД от 25.12.2019", '
    '"FiasCompleteXmlUrl":"https://fias-file.nalog.ru/ExportDownloads?file=345",'
    '"FiasDeltaXmlUrl":"https://fias-file.nalog.ru/ExportDownloads?file=123"}]')


@pytest.mark.django_db
def test_importer_custom(extract_fixture, monkeypatch, client, django_assert_num_queries, response_mock):
    """Тест обновлений ФИАС."""

    target_date = date(2019, 12, 25)

    fixture_path = extract_fixture(f'fias_xml.zip', True)

    monkeypatch.setattr(
        'refs.fias.synchronizers.FiasSynchronizer.get_media_path',
        lambda *args, **kwargs: fixture_path)

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.check_file_change',
        lambda *args, **kwargs: (fixture_path, fixture_path, False)
    )

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.probe',
        lambda *args, **kwargs: ('fakename.zip', True))

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.get_response',
        lambda *args, **kwargs: None)

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.dump_contents_to_file',
        lambda *args, **kwargs: None)

    # не удаляем ничего после импорта
    monkeypatch.setattr(
        'os.remove', lambda filepath: None
    )

    url = 'http://fias.nalog.ru/WebServices/Public/GetAllDownloadFileInfo'

    # Проверка импорта с чистого листа.
    with response_mock(f'GET {url} -> 200 :{COMMON_RESPONSE}'), freeze_time(target_date):
        run_synchronizers(['fias'], bootstrap=True)

    log = Log.objects.order_by('id').last()
    assert log.task_info[16:] == dedent("""
        NorDocTypeImporter. Created: 26 Updated: 0 Deleted: 0
        EstStatusImporter. Created: 7 Updated: 0 Deleted: 0
        HstStatusImporter. Created: 43 Updated: 0 Deleted: 0
        CenterStImporter. Created: 5 Updated: 0 Deleted: 0
        StrStatusImporter. Created: 4 Updated: 0 Deleted: 0
        ActStatusImporter. Created: 2 Updated: 0 Deleted: 0
        OperStatusImporter. Created: 17 Updated: 0 Deleted: 0
        CurrentStatusImporter. Created: 2 Updated: 0 Deleted: 0
        RoomTypeImporter. Created: 3 Updated: 0 Deleted: 0
        FlatTypeImporter. Created: 11 Updated: 0 Deleted: 0
        AddrObjTypeImporter. Created: 3 Updated: 0 Deleted: 0
        NormativeDocumentImporter. Created: 14 Updated: 0 Deleted: 0
        AddrObjImporter. Created: 5 Updated: 0 Deleted: 0
        HouseImporter. Created: 9 Updated: 0 Deleted: 0
        SteadImporter. Created: 2 Updated: 0 Deleted: 0
        RoomImporter. Created: 11 Updated: 0 Deleted: 0
    """).strip()

    # Проверка вызова фонового задания.
    out = (
        '[{"VersionId":604,"TextVersion":"Версия БД от 25.12.2019", '
        '"FiasCompleteXmlUrl":"https://fias-file.nalog.ru/ExportDownloads?file=567",'
        '"FiasDeltaXmlUrl":"https://fias-file.nalog.ru/ExportDownloads?file=123"}]')

    with response_mock(f'GET {url} -> 200 :{out}'), freeze_time(target_date):
        sync_fias('')

    # Проверка импорта дельты.
    out = (
        '[{"VersionId":604,"TextVersion":"Версия БД от 25.12.2019", '
        '"FiasCompleteXmlUrl":"https://fias-file.nalog.ru/ExportDownloads?file=5e13431b-08e8-4fff-addd-ba4f766dd49a",'
        '"FiasDeltaXmlUrl":"https://fias-file.nalog.ru/ExportDownloads?file=123"}]')

    with response_mock(f'GET {url} -> 200 :{out}'):
        run_synchronizers(['fias'], params={'date': '2019-12-25'})

    log = Log.objects.order_by('id').last()

    assert log.task_info[16:] == dedent("""
        NorDocTypeImporter. Created: 0 Updated: 26 Deleted: 0
        EstStatusImporter. Created: 0 Updated: 7 Deleted: 0
        HstStatusImporter. Created: 0 Updated: 43 Deleted: 0
        CenterStImporter. Created: 0 Updated: 5 Deleted: 0
        StrStatusImporter. Created: 0 Updated: 4 Deleted: 0
        ActStatusImporter. Created: 0 Updated: 2 Deleted: 0
        OperStatusImporter. Created: 0 Updated: 17 Deleted: 0
        CurrentStatusImporter. Created: 0 Updated: 2 Deleted: 0
        RoomTypeImporter. Created: 0 Updated: 3 Deleted: 0
        FlatTypeImporter. Created: 0 Updated: 11 Deleted: 0
        AddrObjTypeImporter. Created: 0 Updated: 3 Deleted: 0
        NormativeDocumentImporter. Created: 0 Updated: 14 Deleted: 0
        AddrObjImporter. Created: 0 Updated: 5 Deleted: 0
        HouseImporter. Created: 0 Updated: 9 Deleted: 0
        SteadImporter. Created: 0 Updated: 2 Deleted: 0
        RoomImporter. Created: 0 Updated: 11 Deleted: 0
    """).strip()

    #  В проверка составляния полного адреса.
    assert AddrObject.objects.filter(
        base_id='8915eae1-57f3-480f-ac36-f824aca00b6c'
    ).last().address_text() == '299023, пер. Генерала Мельника'
    region = AddrObject.objects.get(id='45493625-63cd-4f25-882d-773fe3faf954')
    region.code_postal = '123213'
    region.save()

    street = AddrObject.objects.get(id='45493625-63cd-4f25-882d-773fe3faf955')
    assert street.address_text() == '123213, обл. Новосибирская, ул. Красноярская'

    house = House.objects.order_by('id').last()
    house.num_building = '1'
    house.num_struct = '2'
    house.save()
    assert house.address_text() == '123213, обл. Новосибирская, д. 55, корп. 1, стр. 2'

    # Базовая проверка ручек API.

    with django_assert_num_queries(0) as _:
        assert client.get(
            '/api/fias/?query=query{addresses'
            '{baseId statusOpId nameShort nameFormal}}').json() == {'data': {'addresses': []}}

    with django_assert_num_queries(2) as _:
        result = client.get(
            '/api/fias/?query=query{addresses(baseIds:["0a2f18d4-d2ab-44be-94d5-ad6cdd0627bf"])'
            '{baseId statusOpId nameShort nameFormal}}').json()

    assert result == {
        'data': {'addresses': [
            {
                'baseId': '0a2f18d4-d2ab-44be-94d5-ad6cdd0627bf',
                'nameShort': 'ул',
                'nameFormal': 'Красноярская',
                'statusOpId': 10
            }
        ]}
    }

    adr_obj = AddrObject.actual.get(base_id='8915eae1-57f3-480f-ac36-f824aca00b6c')
    adr_obj.parent_id = '45493625-63cd-4f25-882d-773fe3faf955'
    adr_obj.save()

    with django_assert_num_queries(2) as _:
        result = client.get(
            '/api/fias/?query=query{addresses(parentIds:"45493625-63cd-4f25-882d-773fe3faf955")'
            '{baseId statusOpId nameShort nameFormal}}').json()

    assert result == {
        'data': {'addresses': [
            {
                'baseId': '8915eae1-57f3-480f-ac36-f824aca00b6c',
                'statusOpId': 50,
                'nameShort': 'обл',
                'nameFormal': 'Новосибирская'
            }
         ]}
    }

    with django_assert_num_queries(2) as _:
        result = client.get(
            """/api/fias/?query=query{steads(baseIds:["4b9c8abf-3068-447f-9d78-a0f743b77814"]parentIds:["8915eae1-57f3-480f-ac36-f824aca00b6c"]){
                baseId num numCad parentId
          }}"""
        ).json()

    assert result == {
        'data':{'steads':
            [{
                 'num': '28',
                 'numCad': None,
                 'baseId': '4b9c8abf-3068-447f-9d78-a0f743b77814',
                 'parentId': '8915eae1-57f3-480f-ac36-f824aca00b6c'
            }]
         }
    }

    with django_assert_num_queries(0) as _:
        result = client.get(
            '/api/fias/?query=query{addresses(parentIds:"45493625-63cd-4f25-882d-773fe3faf955", name: "ad")'
            '{baseId statusOpId nameShort nameFormal}}').json()

    assert result == {'errors': [
        {'message': 'Name must be longer than 3 characters.',
         'path': ['addresses'],
         'locations': [{'column': 7, 'line': 1}]}],
        'data': {'addresses': None}
    }

    with django_assert_num_queries(2) as _:
        result = client.get(
            '/api/fias/?query=query{addresses(parentIds:"45493625-63cd-4f25-882d-773fe3faf955", name: "Новосибир", baseIds:"8915eae1-57f3-480f-ac36-f824aca00b6c" levels: 1)'
            '{baseId statusOpId nameShort nameFormal}}').json()
    assert result == {'data':
        {'addresses': [
          {'baseId': '8915eae1-57f3-480f-ac36-f824aca00b6c',
           'statusOpId': 50,
           'nameShort': 'обл',
           'nameFormal': 'Новосибирская'}
    ]}}

    with django_assert_num_queries(2) as _:
        result = client.get(
            '/api/fias/?query=query{docs(ids:"ca9eaddf-a9ea-4bf8-a60f-002820f39d0b")'
            '{date num}}').json()
    assert result == {'data': {'docs': [{'date': '1972-12-21', 'num': '4/24'}]}}

    room = Room.objects.get(id='f81297a4-28de-4ca8-aea1-0b586b149139')
    room.num = '2'
    room.save()

    with django_assert_num_queries(2) as _:
        result = client.get(
            '/api/fias/?query=query{rooms(baseIds:"f81297a4-28de-4ca8-aea1-0b586b149139" numFlat: "1" num:"2" parentIds:"44992355-e2de-4da5-a176-185b427f8bfb")'
            '{numFlat parentId num}}').json()
    assert result == {'data': {'rooms': [{'numFlat': '1', 'parentId': '44992355-e2de-4da5-a176-185b427f8bfb', 'num': '2'}]}}

    with django_assert_num_queries(0) as _:
        result = client.get(
            '/api/fias/?query=query{rooms'
            '{numFlat parentId num}}').json()
    assert result == {'data': {'rooms': []}}

    with django_assert_num_queries(0) as _:
        result = client.get(
            '/api/fias/?query=query{levels'
            '{id name endpoint}}').json()

    assert result == {'data': {
        'levels': [
            {'endpoint': 'addresses', 'id': 1, 'name': 'регион'},
            {'endpoint': 'addresses', 'id': 2, 'name': 'автономный округ (устарел)'},
            {'endpoint': 'addresses', 'id': 3, 'name': 'район'},
            {'endpoint': 'addresses', 'id': 35, 'name': 'городское/сельское поселение'},
            {'endpoint': 'addresses', 'id': 4, 'name': 'город'},
            {'endpoint': 'addresses', 'id': 5, 'name': 'внутригородская территория (устарела)'},
            {'endpoint': 'addresses', 'id': 6, 'name': 'населенный пункт'},
            {'endpoint': 'addresses', 'id': 65, 'name': 'планировочная структура'},
            {'endpoint': 'addresses', 'id': 7, 'name': 'улица'},
            {'endpoint': 'steads', 'id': 75, 'name': 'земельный участок'},
            {'endpoint': 'houses', 'id': 8, 'name': 'здание/сооружение/объект незавершенного строительства'},
            {'endpoint': 'rooms','id': 9, 'name': 'помещение в пределах здания/сооружения'},
            {'endpoint': 'addresses', 'id': 90, 'name': 'дополнительная территория (устарела)'},
            {'endpoint': 'addresses', 'id': 91, 'name': 'объект на дополнительной территории (устарел)'}
        ]}
    }

    with django_assert_num_queries(0) as _:
        result = client.get(
            '/api/fias/?query=query{houses'
            '{baseId parentId num}}').json()

    assert result == {'data': {'houses': []}}

    with django_assert_num_queries(2) as _:
        result = client.get(
            '/api/fias/?query=query{houses('
            'baseIds:"0f48d756-5d9a-409d-a47d-e6c7f199b2db" parentIds:"8915eae1-57f3-480f-ac36-f824aca00b6c" archived:true num:"55" numStruct:"2" numBuilding:"1")'
            '{baseId parentId num}}').json()

    assert result == {'data': {'houses': [{'num': '55', 'parentId': '8915eae1-57f3-480f-ac36-f824aca00b6c', 'baseId': '0f48d756-5d9a-409d-a47d-e6c7f199b2db'}]}}

    with django_assert_num_queries(0) as _:
        result = client.get('/api/fias/?query=query{levelByGuid{baseId level}}').json()

    assert result == {'data': {'levelByGuid': []}}

    with django_assert_num_queries(1) as _:
        result = client.get('/api/fias/?query=query{levelByGuid(baseId:"0a2f18d4-d2ab-44be-94d5-ad6cdd0627bf"){baseId level}}').json()

    assert result == {'data': {'levelByGuid': [{'baseId': '0a2f18d4-d2ab-44be-94d5-ad6cdd0627bf', 'level': 7}]}}

    with django_assert_num_queries(2) as _:
        result = client.get('/api/fias/?query=query{levelByGuid(baseId:"0f48d756-5d9a-409d-a47d-e6c7f199b2db"){baseId level}}').json()

    assert result == {'data': {'levelByGuid': [{'baseId': '0f48d756-5d9a-409d-a47d-e6c7f199b2db', 'level': 8}]}}

    with django_assert_num_queries(3) as _:
        result = client.get('/api/fias/?query=query{levelByGuid(baseId:"f81297a4-28de-4ca8-aea1-0b586b149139"){baseId level}}').json()

    assert result == {'data': {'levelByGuid': [{'baseId': 'f81297a4-28de-4ca8-aea1-0b586b149139', 'level': 9}]}}

    with django_assert_num_queries(4) as _:
        result = client.get('/api/fias/?query=query{levelByGuid(baseId:"4b9c8abf-3068-447f-9d78-a0f743b77814"){baseId level}}').json()

    assert result == {'data': {'levelByGuid': [{'baseId': '4b9c8abf-3068-447f-9d78-a0f743b77814', 'level': 75}]}}

    with django_assert_num_queries(4) as _:
        result = client.get('/api/fias/?query=query{levelByGuid(baseId:"4b9c8abf-3068-447f-9d78-a0f743b77822"){baseId level}}').json()

    assert result == {'data': {'levelByGuid': []}}

    with django_assert_num_queries(1) as _:
        result = client.get('/api/fias/?query={statusEstateId{id name}}').json()

    assert result['data'] == {
        'statusEstateId': [
            {'id': 0, 'name': 'Не определено'},
            {'id': 1, 'name': 'Владение'},
            {'id': 2, 'name': 'Дом'},
            {'id': 3, 'name': 'Домовладение'},
            {'id': 4, 'name': 'Гараж'},
            {'id': 5, 'name': 'Здание'},
            {'id': 6, 'name': 'Шахта'}
        ]
    }

    with django_assert_num_queries(0) as _:
        result = client.get('/api/fias/?query={division{id name}}').json()

    assert result['data'] == {
        'division': [
            {'id': 0, 'name': 'Не определено'},
            {'id': 1, 'name': 'Муниципальное'},
            {'id': 2, 'name': 'Административное'}
        ]
    }


@pytest.mark.django_db
def test_fetcher(response_mock):

    filename = 'delta_604_fias_xml.zip'

    target_date = date(2019, 12, 25)

    out_common = (
        '[{"VersionId": 604, "TextVersion": "Версия БД от 25.12.2019",'
        '"FiasCompleteXmlUrl": "https://fias-file.nalog.ru/ExportDownloads?file=5e13431b-08e8-4fff-addd-ba4f766dd49a",'
        '"FiasDeltaXmlUrl": "https://fias-file.nalog.ru/ExportDownloads?file=123"}]'
    )

    with response_mock([
        f'GET http://fias.nalog.ru/WebServices/Public/GetAllDownloadFileInfo -> 200 :{out_common}',
        f'HEAD https://file.nalog.ru/Downloads/20191225/fias_delta_xml.zip -> 200:{out_common}',
    ]) as http_mock:
        http_mock.add(
            'GET', 'https://file.nalog.ru/Downloads/20191225/fias_delta_xml.zip',
            adding_headers={'content-length': '10'}
        )
        sync = FiasSynchronizer()
        sync._init_log_model()
        with freeze_time(target_date):
            assert sync._run_fetcher().items.popitem()[0] == filename

    with response_mock([
        f'GET http://fias.nalog.ru/WebServices/Public/GetAllDownloadFileInfo -> 200 :{out_common}',
        'HEAD https://file.nalog.ru/Downloads/20191225/fias_delta_xml.zip -> 404:'
    ]):
        sync.fetcher.get_previous_result = lambda sync: {filename: ''}
        with freeze_time(target_date):
            assert sync._run_fetcher().items.popitem()[0] == filename


@pytest.mark.django_db
def test_importer_broken(tmpdir, monkeypatch, client, extract_fixture, response_mock):

    filepath = extract_fixture('fias_xml_broken.zip', True)

    monkeypatch.setattr(
        'refs.fias.synchronizers.FiasSynchronizer.get_media_path',
        lambda *args, **kwargs: filepath
    )

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.probe',
        lambda *args, **kwargs: ('fakename.rar', False)
    )

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.check_file_change',
        lambda *args, **kwargs: (filepath, filepath, False)
    )
    monkeypatch.setattr(
        'os.remove', lambda filepath: None
    )

    with response_mock(f'GET http://fias.nalog.ru/WebServices/Public/GetAllDownloadFileInfo -> 200:{COMMON_RESPONSE}'):
        # Проверяем обработку отсутствия файлов.
        run_synchronizers(['fias'], params={'date': '2019-12-25'}, bootstrap=True)

    log = Log.objects.order_by('id').last()

    text = (
        "fias.exceptions.FiasImporterException: Archive: fias_xml_broken.zip, "
        "file: AS_ESTSTAT_20190311_50d034a1-06fb-48f5-9e8f-1f72a3227fae.XML")

    assert text in log.task_info


@pytest.mark.django_db
def test_importer_invalid_date(dir_fixtures, monkeypatch, client, response_mock):

    monkeypatch.setattr(
        'refs.fias.synchronizers.FiasSynchronizer.get_media_path',
        lambda *args, **kwargs: dir_fixtures('fias_xml_broken.zip'))

    monkeypatch.setattr(
        'refs.fias.fetchers.FiasFetcher.probe',
        lambda *args, **kwargs: ('fakename.zip', True))

    monkeypatch.setattr(
        'os.remove', lambda filepath: None
    )

    with response_mock(f'GET http://fias.nalog.ru/WebServices/Public/GetAllDownloadFileInfo -> 200:{COMMON_RESPONSE}'):
        # Проверяем обработку отсутствия файлов.
        run_synchronizers(['fias'], params={'date': '2019-12-26'}, bootstrap=True)

    log = Log.objects.order_by('id').last()

    text = 'refs.fias.exceptions.FiasImporterException: Archive for 26.12.2019 not found.'

    assert text in log.task_info

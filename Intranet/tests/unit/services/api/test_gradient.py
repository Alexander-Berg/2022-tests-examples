import pretend
import pytest

from datetime import timedelta
from django.core.urlresolvers import reverse
from django.utils import timezone
from waffle.testutils import override_switch

from plan.maintenance.tasks import clear_readonly
from plan.services.models import ServiceMoveRequest, ServiceTag
from plan.services.tasks import calculate_gradient_fields

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def gradient_data(db, staff_factory):
    """
    Структура сервисов:


        metaservice (VS)
        |
        |_  umbservice (UMB)
            |
            |_ service (OUTLINE)


        meta_other (нет в градиенте)
        |
        |_  submeta_other (UMB)

    """
    metaservice = factories.ServiceFactory(parent=None)
    umbservice = factories.ServiceFactory(
        parent=metaservice,
        valuestream=metaservice,
        owner=staff_factory('full_access')
    )
    service = factories.ServiceFactory(parent=umbservice, valuestream=metaservice, umbrella=umbservice)

    metaservice.valuestream = metaservice
    metaservice.save()
    umbservice.umbrella = umbservice
    umbservice.save()

    meta_other = factories.ServiceFactory(parent=None)
    submeta_other = factories.ServiceFactory(parent=meta_other)
    submeta_other.umbrella = submeta_other
    submeta_other.save()

    vs = factories.ServiceTagFactory(slug='vs')
    umb = factories.ServiceTagFactory(slug='umb')
    outline = factories.ServiceTagFactory(slug='outline')
    bu = factories.ServiceTagFactory(slug='bu')

    metaservice.tags.add(vs)
    umbservice.tags.add(umb)
    submeta_other.tags.add(umb)
    service.tags.add(outline)

    return pretend.stub(
        metaservice=metaservice,
        umbservice=umbservice,
        service=service,
        meta_other=meta_other,
        submeta_other=submeta_other,

        tag_vs=vs,
        tag_umb=umb,
        tag_outline=outline,
        tag_bu=bu,
    )


def test_get_gradient(client, gradient_data):
    response = client.json.get(reverse('api-v4:service-gradient-list'))
    assert response.status_code == 200

    result = response.json()['results']
    assert len(result) == 3

    # проверим данные valuestream'a
    assert result[0]['id'] == gradient_data.metaservice.id
    assert result[0]['type'] == 'vs'
    assert result[0]['valuestream'] is None


def test_get_gradient_num_queries(client, gradient_data, django_assert_num_queries):
    """
    Проверим, что количество запросов не зависит от количества сервисов.
    Ожидаем:
        # 1 select intranet_staff join auth_user
        # 1 middleware
        # 1 select service
        # 1 select prefetch_related service_tags
        # 1 pg_is_in_recovery()
        # 1 select в waffle readonly switch
    """

    with django_assert_num_queries(6):
        response = client.json.get(reverse('api-v4:service-gradient-list'))

    assert response.status_code == 200

    # добавим в результат ещё два сервиса
    meta_other = gradient_data.meta_other
    meta_other.tags.add(gradient_data.tag_vs)
    meta_other.valuestream = meta_other
    meta_other.save()
    gradient_data.submeta_other.valuestream = meta_other
    gradient_data.submeta_other.save()

    with django_assert_num_queries(6):
        response = client.json.get(reverse('api-v4:service-gradient-list'))

    assert response.status_code == 200
    assert len(response.json()['results']) == 5


@override_switch('async_move_service_abc_side', active=True)
def test_move_service_add_fields(gradient_data):
    """
    При перемещении должны обновляться поля vs и umb.
    Переместим submeta_other под metaservice.
    Ожидаем у сервиса submeta_other:
        - valuestream == metaservice
        - umbrella == submeta_other
    """

    move_request = factories.ServiceMoveRequestFactory(
        service=gradient_data.submeta_other,
        destination=gradient_data.metaservice,
        requester=gradient_data.metaservice.owner,
        state=ServiceMoveRequest.PROCESSING_ABC,
        approver_incoming=gradient_data.metaservice.owner,
        approver_outgoing=gradient_data.metaservice.owner,
    )
    yesterday = timezone.now() - timedelta(days=1)
    ServiceMoveRequest.objects.filter(pk=move_request.id).update(updated_at=yesterday)

    assert gradient_data.submeta_other.valuestream is None
    assert gradient_data.submeta_other.umbrella == gradient_data.submeta_other

    clear_readonly(service_ids=[gradient_data.submeta_other.id])

    gradient_data.submeta_other.refresh_from_db()
    assert gradient_data.submeta_other.valuestream == gradient_data.metaservice
    assert gradient_data.submeta_other.umbrella == gradient_data.submeta_other


@override_switch('async_move_service_abc_side', active=True)
def test_move_service_delete_fields(gradient_data):
    """
    При перемещении должны обновляться поля vs и umb.
    Переместим service под submeta_other.
    Ожидаем у сервиса submeta_other:
        - valuestream == None
        - umbrella == submeta_other
    """

    move_request = factories.ServiceMoveRequestFactory(
        service=gradient_data.service,
        destination=gradient_data.submeta_other,
        requester=gradient_data.submeta_other.owner,
        state=ServiceMoveRequest.PROCESSING_ABC,
        approver_incoming=gradient_data.submeta_other.owner,
        approver_outgoing=gradient_data.submeta_other.owner,
    )
    yesterday = timezone.now() - timedelta(days=1)
    ServiceMoveRequest.objects.filter(pk=move_request.id).update(updated_at=yesterday)

    assert gradient_data.service.valuestream == gradient_data.metaservice
    assert gradient_data.service.umbrella == gradient_data.umbservice

    clear_readonly(service_ids=[gradient_data.service.id])

    gradient_data.service.refresh_from_db()
    assert gradient_data.service.valuestream is None
    assert gradient_data.service.umbrella is None


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_add_tags(gradient_data, api, client, superuser):
    """
    Добавим тег vs meta_other.
    Ожидаем у сервиса meta_other: valuestream == meta_other
    У сервиса submeta_other:
        - valuestream == meta_other
        - umbrella == submeta_other
    """

    endpoint_path = f'{api}:service-detail'
    meta_other = gradient_data.meta_other
    submeta_other = gradient_data.submeta_other

    client.login(superuser.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[meta_other.id]),
        data={'tags': [gradient_data.tag_vs.id]}
    )
    assert response.status_code == 200

    meta_other.refresh_from_db()
    submeta_other.refresh_from_db()

    assert meta_other.valuestream == meta_other
    assert submeta_other.valuestream == meta_other
    assert submeta_other.umbrella == submeta_other


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('tag_slug', ('vs', 'umb', 'outline'))
@pytest.mark.parametrize('is_superuser', (True, False))
def test_add_tags_permission(gradient_data, client, api, tag_slug, is_superuser, superuser, person):
    """
    Теги outline и umb могут ставит все, а тег vs только суперюзер
    """

    endpoint_path = f'{api}:service-detail'
    tag = ServiceTag.objects.get(slug=tag_slug)

    parent = None
    if tag == gradient_data.tag_umb:
        parent = gradient_data.metaservice

    elif tag == gradient_data.tag_outline:
        parent = gradient_data.umbservice

    service = factories.ServiceFactory(parent=parent, owner=person)
    if is_superuser:
        service.owner = superuser
        service.save()

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [tag.id]}
    )

    if tag == gradient_data.tag_vs and not is_superuser:
        assert response.status_code == 403
        assert response.json()['error']['message']['ru'] == 'У вас недостаточно прав для добавления тега vs.'

    else:
        assert response.status_code == 200


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('was', ('vs', 'umb', 'outline', 'bu'))
@pytest.mark.parametrize('add', ('vs', 'umb', 'outline', 'bu'))
def test_add_tags_exclusive_gradient_tags(gradient_data, client, superuser, api, was, add, person):
    """
    Градиентные теги являются взаимоисключающими.
    """
    endpoint_path = f'{api}:service-detail'
    service = factories.ServiceFactory(owner=person)
    old_tag = ServiceTag.objects.get(slug=was)
    service.tags.add(old_tag)

    tag = ServiceTag.objects.get(slug=add)

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [old_tag.id, tag.id]}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Нельзя добавить несколько градиентных тегов одновременно.'


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('has_parent_vs', (True, False, None))
def test_add_tags_umb_parent_is_not_vs(gradient_data, client, api, has_parent_vs, person):
    """
    При добавлении тега umb проверяем, что прямой родитель является VS.
    """

    endpoint_path = f'{api}:service-detail'

    parent = None
    if has_parent_vs is not None:
        if has_parent_vs:
            parent = gradient_data.metaservice

        else:
            parent = gradient_data.meta_other

    service = factories.ServiceFactory(parent=parent, owner=person)

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [gradient_data.tag_umb.id]}
    )

    if has_parent_vs:
        assert response.status_code == 200

    else:
        assert response.status_code == 400
        assert response.json()['error']['message']['ru'] == 'Прямой предок сервиса не является valuestream.'


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_add_tags_ancestors_has_two_gradient_tags(gradient_data, client, api, person):
    """
    Поломанная выше структура (наличие двух тегов у сервиса) не даст добавить градиентный тег.
    """

    endpoint_path = f'{api}:service-detail'
    parent = gradient_data.submeta_other
    service = factories.ServiceFactory(parent=parent, owner=person)
    parent.tags.add(gradient_data.tag_vs)

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [gradient_data.tag_umb.id]}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'У одного из предков используется несколько градиентных тегов.'


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('has_tag', ('umb', 'outline'))
def test_add_tags_umb_ancestors_has_tag_umb(gradient_data, client, api, has_tag, person):
    """
    При добавлении тега umb выше по дереву есть контур или зонтик
    """
    endpoint_path = f'{api}:service-detail'
    parent = gradient_data.metaservice
    new_metaservice = factories.ServiceFactory()
    new_metaservice.tags.add(gradient_data.tag_vs)
    new_service = factories.ServiceFactory(parent=new_metaservice)
    tag = ServiceTag.objects.get(slug=has_tag)
    new_service.tags.add(tag)
    parent.parent = new_service
    parent.save()

    service = factories.ServiceFactory(parent=parent, owner=person)
    calculate_gradient_fields(new_metaservice.id)

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [gradient_data.tag_umb.id]}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Выше по дереву уже есть контур или зонтик.'


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('has_vs', (True, False))
@pytest.mark.parametrize('has_umb', (True, False))
def test_add_tags_outline(gradient_data, client, api, has_vs, has_umb, person):
    """
    Добавляем тег контур. Если нет зонтика или vs - ошибка.
    """
    endpoint_path = f'{api}:service-detail'
    service_parent_parent = factories.ServiceFactory()
    service_parent = factories.ServiceFactory(parent=service_parent_parent)
    service = factories.ServiceFactory(parent=service_parent, owner=person)

    if has_vs:
        service_parent_parent.tags.add(gradient_data.tag_vs)

    if has_umb:
        service_parent.tags.add(gradient_data.tag_umb)

    calculate_gradient_fields(service_parent_parent.id)

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [gradient_data.tag_outline.id]}
    )

    if not has_vs or not has_umb:
        assert response.status_code == 400
        assert response.json()['error']['message']['ru'] == 'Выше по дереву нет зонтика или уже есть контур.'

    else:
        assert response.status_code == 200


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_add_tags_outline_ancestors_outline(gradient_data, client, api, person):
    """
    Добавляем тег контур. Сервис уже под контуром
    """
    endpoint_path = f'{api}:service-detail'
    service = factories.ServiceFactory(parent=gradient_data.service, owner=person)

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': [gradient_data.tag_outline.id]}
    )

    assert response.status_code == 400
    assert response.json()['error']['message']['ru'] == 'Выше по дереву нет зонтика или уже есть контур.'


def test_add_tags_num_queries(gradient_data, client, person,
                              django_assert_num_queries, django_assert_num_queries_lte):
    """
    Проверим разницу между добавлением обычного тега и градиентного.
    Дополнительно делаем:
        # 1 select service ancestors
        # 1 select prefetch_related service_tags

        # так же тут учитываются 9 запросов, которые на самом деле относятся к таске calculate_gradient_fields
        # и непосредственно в запросе не вызываются
    """
    service = factories.ServiceFactory(parent=gradient_data.umbservice, owner=person)
    tag = factories.ServiceTagFactory(slug='mau')
    client.login(service.owner.login)
    with django_assert_num_queries_lte(30):
        client.json.patch(
            reverse('api-v4:service-detail', args=[service.id]),
            data={'tags': [tag.id]}
        )
    # удалим тег
    client.json.patch(
        reverse('api-v4:service-detail', args=[service.id]),
        data={'tags': []}
    )

    assert len(service.tags.all()) == 0

    with django_assert_num_queries(46):
        client.json.patch(
            reverse('api-v4:service-detail', args=[service.id]),
            data={'tags': [gradient_data.tag_outline.id]}
        )


@pytest.mark.parametrize('api', ('services-api', 'api-v3', 'api-v4'))
def test_delete_tags(gradient_data, api, client):
    """
    Удалим тег у umbservice.
    Ожидаем у сервиса umbservice:
        - valuestream == metaservice
        - umbrella == None
    У сервиса service:
        - valuestream == metaservice
        - umbrella == None
    """

    endpoint_path = f'{api}:service-detail'
    umbservice = gradient_data.umbservice
    service = gradient_data.service

    client.login(umbservice.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[umbservice.id]),
        data={'tags': []}
    )
    assert response.status_code == 200

    umbservice.refresh_from_db()
    service.refresh_from_db()

    assert umbservice.valuestream == gradient_data.metaservice
    assert umbservice.umbrella is None
    assert service.valuestream == gradient_data.metaservice
    assert service.umbrella is None


@pytest.mark.parametrize('api', ('services-api', 'api-frontend', 'api-v3', 'api-v4'))
@pytest.mark.parametrize('tag_slug', ('vs', 'umb', 'outline', 'bu'))
@pytest.mark.parametrize('is_superuser', (True, False))
def test_delete_tags_permission(gradient_data, client, api, tag_slug, is_superuser, superuser, person):
    """
    Теги outline и umb могут удалить все, а тег vs только суперюзер
    """
    endpoint_path = f'{api}:service-detail'
    service = factories.ServiceFactory(owner=person)
    if is_superuser:
        service.owner = superuser
        service.save()

    tag = ServiceTag.objects.get(slug=tag_slug)
    service.tags.add(tag)
    assert len(service.tags.all()) == 1

    client.login(service.owner.login)
    response = client.json.patch(
        reverse(endpoint_path, args=[service.id]),
        data={'tags': []}
    )

    if tag in [gradient_data.tag_vs, gradient_data.tag_bu] and not is_superuser:
        assert response.status_code == 403
        assert response.json()['error']['message']['ru'] == f'У вас недостаточно прав для удаления тега {tag_slug}.'

    else:
        assert response.status_code == 200

        service.refresh_from_db()
        assert len(service.tags.all()) == 0


def test_calculate_gradient_fields_two_tags(gradient_data):
    """
    Проставим сервису metaservice два тега. Запустим таску рассчета.
    Ожидаем, что у metaservice и его потомков не будет проставленных полей vs и umb.
    """

    metaservice = gradient_data.metaservice
    metaservice.tags.add(gradient_data.tag_umb)
    service = gradient_data.service

    assert metaservice.valuestream == metaservice
    assert gradient_data.umbservice.valuestream == metaservice
    assert service.valuestream == metaservice

    assert metaservice.umbrella is None
    assert gradient_data.umbservice.umbrella == gradient_data.umbservice
    assert service.umbrella == gradient_data.umbservice

    calculate_gradient_fields(metaservice.id)
    metaservice.refresh_from_db()
    gradient_data.umbservice.refresh_from_db()
    service.refresh_from_db()

    assert metaservice.valuestream is None
    assert gradient_data.umbservice.valuestream is None
    assert service.valuestream is None

    assert metaservice.umbrella is None
    assert gradient_data.umbservice.umbrella is None
    assert service.umbrella is None

    # присвоим service тег vs и попробуем пересчитать
    service.tags.add(gradient_data.tag_vs)
    calculate_gradient_fields(service.id)
    service.refresh_from_db()
    assert service.valuestream is None
    assert service.umbrella is None


def test_calculate_gradient_fields_two_vs(gradient_data):
    """
    Если над сервисом оказалось два valuestream, в полях появится тот, что выше.
    Переносим metaservice под meta_other.
    """

    metaservice = gradient_data.metaservice
    meta_other = gradient_data.meta_other
    metaservice.parent = meta_other
    metaservice.save()
    meta_other.tags.add(gradient_data.tag_vs)
    meta_other.valuestream = meta_other
    meta_other.save()

    calculate_gradient_fields(metaservice.id)
    metaservice.refresh_from_db()
    gradient_data.umbservice.refresh_from_db()
    gradient_data.service.refresh_from_db()

    assert metaservice.valuestream == meta_other
    assert gradient_data.umbservice.valuestream == meta_other
    assert gradient_data.service.valuestream == meta_other


def test_calculate_gradient_fields_two_umb(gradient_data):
    """
    Если зонтик окажется без valuestream, то мы не должны учитывать его для рассчёта зонтика у сервиса.
    Например, перенесём metaservice под submeta_other.
    Получим такую структуру:

        UMB (submeta_other) (нет вышестоящего VS)
        |
        |_  VS (metaservice)
            |
            |_ UMB (umbservice)
                |
                |_ OUTLINE (service)

    Для всех сервисов в структуре не должны считать зонтиком сервис submeta_other.
    Структура должна сохранится.
    """

    metaservice = gradient_data.metaservice
    umbservice = gradient_data.umbservice
    metaservice.parent = gradient_data.submeta_other
    metaservice.save()

    calculate_gradient_fields(metaservice.id)
    metaservice.refresh_from_db()
    umbservice.refresh_from_db()
    gradient_data.service.refresh_from_db()

    assert metaservice.valuestream == metaservice
    assert umbservice.valuestream == metaservice
    assert gradient_data.service.valuestream == metaservice

    assert metaservice.umbrella is None
    assert umbservice.umbrella == umbservice
    assert gradient_data.service.umbrella == umbservice


def test_calculate_gradient_fields_not_gradient(gradient_data):
    """
    Учимся отличать отсуствие vs и зонтиков у верхнеуровненвых сервисов от зануления сломанной ветки.
    Запустим пересчет для сервиса, у которого нет vs и зонтика, но под ним есть структура.
    """

    service = factories.ServiceFactory(parent=None)
    metaservice = gradient_data.metaservice
    metaservice.parent = service
    metaservice.save()
    umbservice = gradient_data.umbservice

    calculate_gradient_fields(service.id)
    service.refresh_from_db()
    metaservice.refresh_from_db()
    umbservice.refresh_from_db()
    gradient_data.service.refresh_from_db()

    assert service.valuestream is None
    assert metaservice.valuestream == metaservice
    assert umbservice.valuestream == metaservice
    assert gradient_data.service.valuestream == metaservice

    assert service.umbrella is None
    assert metaservice.umbrella is None
    assert umbservice.umbrella == umbservice
    assert gradient_data.service.umbrella == umbservice


def test_calculate_gradient_fields_num_queries(gradient_data, django_assert_num_queries):
    with django_assert_num_queries(15):
        # Для одного сервиса:

        # 1 select service
        # 2 select service with descendants
        # 3 select service_tags
        # 2 select service with ancestors
        # 1 update

        calculate_gradient_fields(gradient_data.service.id)

    with django_assert_num_queries(13):
        # Для трёх сервисов

        # 1 select service
        # 3 select service with descendants
        # 2 select service_tags
        # 2 update
        # 1 select service with ancestors

        calculate_gradient_fields(gradient_data.metaservice.id)

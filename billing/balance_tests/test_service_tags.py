# coding=utf-8
from balance.mapper.service_tags import ServiceTag, ServiceTagMark

# используем большие service_id, чтобы не пересекаться с существующими сервисами
TEST_SERVICE_ID_START = 100000
SERVICE_ID_1 = TEST_SERVICE_ID_START + 1
SERVICE_ID_2 = TEST_SERVICE_ID_START + 2
SERVICE_ID_3 = TEST_SERVICE_ID_START + 3


class TestServiceTags:
    def test_sorted_by_weight(self, session):
        """При чтении сервисов с заданным тегом
        сервисы упорядочиваются по весу (колонка service_weight)"""

        tag1 = ServiceTag(code='TAG1', name='Test Tag 1')
        session.add(tag1)
        session.flush()

        session.add(ServiceTagMark(service_id=SERVICE_ID_2, service_tag_code=tag1.code, service_weight=2000))
        session.add(ServiceTagMark(service_id=SERVICE_ID_1, service_tag_code=tag1.code, service_weight=1000))
        session.flush()

        assert ServiceTag.get_services_by_tag(session, tag1.code) == [SERVICE_ID_1, SERVICE_ID_2]

    def test_service_with_none_weight_located_at_the_end(self, session):
        """При чтении сервисов с заданным тегом
        сервисы с неуказанным весом помещаются в конец списка"""

        tag1 = ServiceTag(code='TAG1', name='Test Tag 1')
        session.add(tag1)
        session.flush()

        session.add(ServiceTagMark(service_id=SERVICE_ID_1, service_tag_code=tag1.code))
        session.add(ServiceTagMark(service_id=SERVICE_ID_2, service_tag_code=tag1.code, service_weight=2000))
        session.flush()

        assert ServiceTag.get_services_by_tag(session, tag1.code) == [SERVICE_ID_2, SERVICE_ID_1]

    def test_get_tags_by_service(self, session):
        """Проверяем чтение списка тегов, присвоенных сервису"""

        tag1 = ServiceTag(code='TAG1', name='Test Tag 1')
        tag2 = ServiceTag(code='TAG2', name='Test Tag 2')
        session.add(tag1)
        session.add(tag2)
        session.flush()

        session.add(ServiceTagMark(service_id=SERVICE_ID_1, service_tag_code=tag1.code))
        session.add(ServiceTagMark(service_id=SERVICE_ID_2, service_tag_code=tag1.code))
        session.add(ServiceTagMark(service_id=SERVICE_ID_1, service_tag_code=tag2.code))
        session.add(ServiceTagMark(service_id=SERVICE_ID_3, service_tag_code=tag2.code))
        session.flush()

        assert ServiceTag.get_tags_by_service(session, SERVICE_ID_1) == {tag1.code, tag2.code}
        assert ServiceTag.get_tags_by_service(session, SERVICE_ID_2) == {tag1.code}
        assert ServiceTag.get_tags_by_service(session, SERVICE_ID_3) == {tag2.code}

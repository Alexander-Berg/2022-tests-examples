from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search.related_adverts_1x_pb2 import RelatedAdverts

validator = Validator('search')


def add_place(msg, name, oid, pt, tags=[]):
    place = msg.nearby_on_map.add()
    place.name = name
    place.uri = f'ymapsbm1://org?oid={oid}'
    place.point.lon, place.point.lat = pt
    for tag in tags:
        place.tag.append(tag)


def test_related_adverts_1x_snippet():
    snippet = RelatedAdverts()

    add_place(snippet,
              'Borodach',
              '1102127119620',
              (40.380278, 56.121245),
              ['icon:hairdressers'])

    add_place(snippet,
              'Zamkov33',
              '242827959579',
              (40.351739, 56.118616))

    add_place(snippet,
              'Перекресток',
              '1365446008',
              (40.357842, 56.118317),
              ['icon:supermarket', 'icon:shop'])

    validator.validate_example(snippet, 'related_adverts_snippet')

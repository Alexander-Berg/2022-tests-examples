from requests import Response, HTTPError

from intranet.search.core.models import IndexationStats
from intranet.search.core.storages import StageStatusStorage
from intranet.search.core.storages.indexation import IndexationStatsWrapper

from intranet.search.core.storages.revision import rev_model_to_dict
from .models_helpers import create_revision
from intranet.search.core.swarm import Document
from intranet.search.core.swarm.storage import DocumentSerializer, SaasDocumentStorage, YTDocumentStorage


def create_document(url='example.yandex-team.ru/doc', updated=None, body=None):
    doc = Document(url, updated)
    doc.emit_body(body or {})
    return doc


def get_document_serializer(doc, revision=None, body_format='json'):
    if not revision:
        revision = rev_model_to_dict(create_revision())
    return DocumentSerializer(doc, revision, body_format)


def get_document_storage(type='saas', revision=None, indexation_id=None, setup=False):
    if not revision:
        revision = rev_model_to_dict(create_revision())
    storage_cls = SaasDocumentStorage if type == 'saas' else YTDocumentStorage
    storage = storage_cls(revision, indexation_id)
    if setup:
        storage.setup()
    return storage


def get_base_expected_doc(doc, revision, body=None, **options):
    expected = {
        'action': 'modify',
        'prefix': revision['id'],
        'docs': [
            {
                'options': {
                    'mime_type': 'text/xml',
                    'charset': 'utf8',
                    'language': 'ru',
                    'language2': 'ru',
                    'language_default': 'ru',
                    'language_default2': 'ru',
                    'modification_timestamp': doc.updated_ts,
                    'check_only_before_reply': False,
                    'realtime': True,
                },
                'url': doc.url,
                'body': body or {'children': {}, 'type': 'zone', 'value': ''},
                'updated': [{'type': '#g', 'value': doc.updated_ts}]
            }
        ],
    }
    expected['docs'][0]['options'].update(options)
    return expected


def set_saas_error(mocked_http_request, status, body=''):
    """ Мокает все запросы к индексатору саас.
    """
    resp = Response()
    resp.status_code = status
    resp._content = body.encode('utf-8')
    mocked_http_request.side_effect = HTTPError(response=resp)


def create_stats(indexation, hostname, mutation):
    stats = StageStatusStorage.get_default_stats()
    stats.update(mutation)

    wrapper = IndexationStatsWrapper()
    wrapper.update(stats)

    stat = IndexationStats.objects.create(revision=indexation.revision, indexation=indexation,
                                          hostname=hostname, stats=wrapper.data)
    return stat

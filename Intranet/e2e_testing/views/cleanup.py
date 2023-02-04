import logging

from django.db.models import Q

from wiki.api_core.errors.rest_api_error import RestApiError
from wiki.api_core.framework import WikiAPIView
from wiki.api_core.raises import raises
from wiki.api_core.waffle_switches import E2E_TESTS
from wiki.api_frontend.serializers.io import ok_response
from wiki.e2e_testing.utils import purge, MDSException
from wiki.pages.models import Page

logger = logging.getLogger(__name__)


class FixtureDelete(RestApiError):
    error_code = 'FIXTURE_DELETE'
    debug_message = 'User tried to delete fixture cluster'


class CleanupView(WikiAPIView):
    FEATURE_FLAG = E2E_TESTS

    @raises()
    def post(self, request, *args, **kwargs):
        """
        data: {'run_id': 1234567890123456}
        """
        run_id: str = request.data['run_id']

        if run_id == 'fixture':
            raise FixtureDelete()

        supertag: str = 'e2e/' + run_id

        pages = Page.objects.filter(Q(supertag__startswith=supertag + '/') | Q(supertag=supertag))
        logger.info(f'going to purge {pages.count()} pages')

        try:
            for page in pages:
                purge(page)
                logger.info(f'deleted: {page}')
        except Exception as e:
            logger.exception(f'exception during purging {e}')
            raise MDSException(str(e))
        return ok_response()

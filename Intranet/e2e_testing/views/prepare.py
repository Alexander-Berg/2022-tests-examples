import logging
import re
from uuid import uuid4

from wiki.api_core.framework import Response
from wiki.api_core.framework import WikiAPIView
from wiki.api_core.raises import raises
from wiki.api_core.waffle_switches import E2E_TESTS
from wiki.e2e_testing.utils import copy_cluster, E2E_RESERVED

logger = logging.getLogger(__name__)


class PrepareView(WikiAPIView):
    FEATURE_FLAG = E2E_TESTS

    @raises()
    def post(self, request, *args, **kwargs):
        run_id: str = str(uuid4().hex)
        supertag = 'e2e/' + run_id

        rewrite_rules = [(rf'{E2E_RESERVED}', supertag, re.IGNORECASE)]
        copy_cluster(E2E_RESERVED, supertag, rewrite_rules)

        return Response({'run_id': run_id})

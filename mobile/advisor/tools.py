# -*- coding: utf-8 -*-

import logging
import random

logger = logging.getLogger(__name__)

POPULAR_APPS_TITLE_ID = 'backend_editors_choice_col39'

UNIVERSAL_TITLES = [POPULAR_APPS_TITLE_ID, 'backend_editors_choice_col38', 'backend_editors_choice_col40']


def get_random_universal_title_key(category=None):
    if category:
        if category.name == 'GAME':
            return POPULAR_APPS_TITLE_ID
    return random.choice(UNIVERSAL_TITLES)

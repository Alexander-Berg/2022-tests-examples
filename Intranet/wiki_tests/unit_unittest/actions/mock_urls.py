from django.conf import settings
from django.conf.urls import include, url

from wiki.actions.classes.base_action_deprecated import SimpleWikiAction, WikiActionWithPOST

actions_patterns = [
    url(r'^_api/actions/test/?$', SimpleWikiAction.as_view(), name='test'),
    url(r'^_api/actions/dynamic_test/?$', WikiActionWithPOST.as_view(), name='dynamic_test'),
]

frontend_patterns = [
    url('', include((actions_patterns, 'wiki.api'), namespace='actions')),
]

urlpatterns = [
    url('^%s?/' % settings.TAG_REGEX, include((frontend_patterns, 'wiki.api'), namespace='frontend')),
]

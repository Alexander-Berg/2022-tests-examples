from django.conf.urls import url

from yaphone.advisor.advisor.models.client import UserAgent
from yaphone.advisor.advisor.user_agent_creator import user_agent_creator
from yaphone.advisor.advisor.views.device import AndroidClientInfoView, PackagesInfoView, LbsInfoView

user_agent_creator.set_fallback(UserAgent.from_string)

urlpatterns = [
    # migrating views
    url(r'^v[123]/android_client_info/?$', AndroidClientInfoView.as_view()),
    url(r'^v[123]/packages_info/?$', PackagesInfoView.as_view()),
    url(r'^v[123]/lbs_info/?$', LbsInfoView.as_view()),
]

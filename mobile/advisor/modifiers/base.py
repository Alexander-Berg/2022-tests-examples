import logging

from yaphone.advisor.advisor.app_info_loader import app_info_loader
from yaphone.advisor.advisor.jafar import AppsBlock, RecommendationBlock

logger = logging.getLogger(__name__)

DEFAULT_TITLE = 'Super apps list'


class GroupModifier(object):
    modified_types = (RecommendationBlock,)  # iterable of types of blocks to apply this modifier

    def __init__(self, context, client):
        self.client = client
        self.context = context

    def apply(self, recommendation):
        raise NotImplementedError

    def apply_multi(self, recommendations):
        for recommendation in recommendations:
            if isinstance(recommendation, self.modified_types):
                self.apply(recommendation)


class AppsInfoFiller(GroupModifier):
    max_dpi = None
    modified_types = (AppsBlock,)

    def __init__(self, context, client):
        super(AppsInfoFiller, self).__init__(context, client)
        self.app_info_map = {}

    def fill_apps_info(self, app):
        if app.package_name is None:
            return
        app_info = self.app_info_map[app.package_name]
        app.fill_info(app_info)
        app.patch_icon_url(self.client.get_icon_size_name(max_dpi=self.max_dpi))

    def apply(self, recommendation):
        for app in recommendation.all_items:
            self.fill_apps_info(app)

    def apply_multi(self, recommendations):
        self.load_apps_info(recommendations)
        super(AppsInfoFiller, self).apply_multi(recommendations)

    def load_apps_info(self, recommendations):
        language = self.client.language
        package_names = (
            app.package_name
            for recommendation in recommendations if isinstance(recommendation, self.modified_types)
            for app in recommendation.all_items
            if app.package_name is not None
        )
        self.app_info_map = app_info_loader.get_apps_info(package_names, language)


class ImpressionIDFiller(GroupModifier):
    def apply_multi(self, recommendations):
        for card_number, recommendation in enumerate(recommendations, start=1):
            for app_number, app in enumerate(recommendation.all_items, start=1):
                imp_id = app.impression_id
                imp_id.application = self.client.application
                imp_id.experiment = self.context.experiment
                imp_id.position = (self.context.page, card_number, app_number)
                imp_id.client = self.client
                imp_id.algorithm_code = recommendation.resulting_experiment
                imp_id.clid = self.client.get_clid('clid1006') or ''

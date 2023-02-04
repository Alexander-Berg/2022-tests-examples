import logging
import os.path

import yatest.common as yc

from yaphone.advisor.advisor.models import check_mock
from yaphone.advisor.advisor.models.client import Client
from yaphone.advisor.advisor.models.profile import Profile
from yaphone.advisor.setup_wizard.models import Gifts, BonusCardResources


logger = logging.getLogger(__name__)


def make_fixtures_path(fixture_file):
    path = yc.source_path(os.path.join('yaphone/advisor/advisor/tests/fixtures', fixture_file))
    logger.info('Loading fixtures: file %s path %s', fixture_file, path)
    return path


class DatabaseFixturesMixin(object):
    def load_fixtures(self):
        check_mock()

        client_filename = make_fixtures_path('client.json')
        profile_filename = make_fixtures_path('profile.json')
        with open(client_filename) as client_file, open(profile_filename) as profile_file:
            self.client_model = Client.from_json(client_file.read())
            self.client_model.profile = Profile.from_json(profile_file.read())
            self.client_model.profile.save(force_insert=True)
            self.client_model.save(force_insert=True)

            fake_collection('gifts.json', Gifts)
            fake_collection('bonus_cards.json', BonusCardResources)

    def cleanup_fixtures(self):
        check_mock()
        Profile.objects(pk=self.client_model.profile.pk).delete()
        Client.objects(pk=self.client_model.pk).delete()
        Gifts.objects.delete()
        BonusCardResources.objects.delete()


def fake_collection(filename, model_cls):
    check_mock()
    with open(make_fixtures_path(filename)) as fixtures_file:
        for model in model_cls.objects.from_json(fixtures_file.read()):
            model.save(force_insert=True)

from datetime import datetime
from unittest import TestCase

import pytest

from haraba_avito_converter.models.sh import SHAutoffer


@pytest.mark.usefixtures("resources_class")
class TestHarabaConverter(TestCase):
    def test_converter(self):
        offers = []
        for offer in self.haraba_json["Result"]:
            offer = SHAutoffer.from_haraba(offer, sh_last_visited=datetime.now().isoformat())
            offers.append(offer)
        self.assertEqual(len(offers), len(self.haraba_json["Result"]))

    def test_changes_converter(self):
        offers = []
        for offer in self.haraba_changes_json["Result"]:
            offer = SHAutoffer.from_haraba_changes(offer)
            offers.append(offer)
        self.assertEqual(len(offers), len(self.haraba_changes_json["Result"]))

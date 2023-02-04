from unittest import mock

from django.db.models.signals import post_save
from django.test import TestCase

from parts.models import PartInfo, RecentlyUpdatedPart


class Test(TestCase):
    @mock.patch.object(RecentlyUpdatedPart, "add_last_updated")
    def test_save_recently_updated_part(self, add_last_updated):
        """recently udpated part should be created """
        wanted = "part"
        post_save.send(sender=PartInfo, instance=wanted)
        self.assertEqual(mock.call([(wanted)]), add_last_updated.call_args)

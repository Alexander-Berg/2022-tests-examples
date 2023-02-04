# from maps.carparks.renderer.config.ecstatic_hooks.lib import renderer
from maps.carparks.renderer.config.ecstatic_hooks.lib.activate_version import (
    activate_version, mark_version_as_obsolete)
from test_common import CONFIG, TestCaseBase


class ActivateReleaseTests(TestCaseBase):

    def test_activation_fails_if_no_release(self):
        self.set_release_version('100')

        self.mox.ReplayAll()
        with self.assert_raises(Exception('No release with version 200')):
            activate_version(CONFIG, '200')

        self.assert_no_release('100')
        self.assert_no_release('200')

    def test_activation_changes_version(self):
        self.add_release('200')
        self.set_release_version('100')

        self.assert_release_version('100')

        self.mox.ReplayAll()
        activate_version(CONFIG, '200')

        self.assert_release_version('200')

    def test_activation_makes_layer_version_non_obsolete(self):
        self.add_release('200')
        self.set_release_version('100')
        self.set_version_obsolete(True)

        self.mox.ReplayAll()
        activate_version(CONFIG, '200')

        self.assert_release_version('200')
        self.assert_version_is_obsolete(False)


class MarkVersionObsoleteTests(TestCaseBase):
    def test_version_can_be_marked_obsolete(self):
        self.set_release_version('100')

        self.mox.ReplayAll()
        mark_version_as_obsolete(CONFIG)

        self.assert_version_is_obsolete(True)

    def test_version_can_be_marked_obsolete_even_if_its_obsolete(self):
        self.set_release_version('100')
        self.set_version_obsolete(True)

        self.mox.ReplayAll()
        mark_version_as_obsolete(CONFIG)

        self.assert_version_is_obsolete(True)

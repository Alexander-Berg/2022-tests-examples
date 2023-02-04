from maps.carparks.renderer.config.ecstatic_hooks.lib import renderer
from maps.carparks.renderer.config.ecstatic_hooks.lib.remove_version import remove_version
from test_common import TestCaseBase, CONFIG


class RemoveReleaseTests(TestCaseBase):

    def renderer_close(self, version):
        return renderer._http_post(
            'renderer_vhost',
            'close?path={}'.format(self.release_dir(version)))

    def test_hook_removes_release_folder(self):
        self.add_release('100')
        self.renderer_close('100')

        self.mox.ReplayAll()

        remove_version(CONFIG, '100')

        self.assert_no_release('100')

    def test_remove_does_nothing_if_version_not_added(self):
        self.add_release('100')

        self.mox.ReplayAll()

        remove_version(CONFIG, '200')
        self.assert_release('100')

    def test_remove_fails_if_release_version_is_active(self):
        self.add_release('200')
        self.set_release_version('200')

        self.mox.ReplayAll()
        with self.assert_raises(Exception(
                'Could not remove active release version 200')):
            remove_version(CONFIG, '200')

        self.assert_release('200')

    def test_remove_twice(self):
        self.add_release('100')

        self.renderer_close('100')

        self.mox.ReplayAll()
        remove_version(CONFIG, '100')
        remove_version(CONFIG, '100')

        self.assert_no_release('100')

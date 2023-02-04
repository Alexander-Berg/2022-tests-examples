from maps.carparks.renderer.config.ecstatic_hooks.lib import renderer
from maps.carparks.renderer.config.ecstatic_hooks.lib.add_version import (
    add_version)
from test_common import CONFIG, TestCaseBase


class AddReleaseTests(TestCaseBase):

    def renderer_open(self, version):
        return renderer._http_post(
            'renderer_vhost',
            'open?path={}'.format(self.release_dir(version)))

    def test_hook_adds_folder(self):
        self.add_dataset('100')
        self.renderer_open('100')

        self.mox.ReplayAll()
        add_version(CONFIG, '100', self.dataset_dir('100').path)

        self.assert_release('100')

    def test_add_two_same_versions(self):
        self.add_release('100')
        self.renderer_open('100')
        self.renderer_open('100')

        self.mox.ReplayAll()
        add_version(CONFIG, '100', self.dataset_dir('100').path)
        add_version(CONFIG, '100', self.dataset_dir('100').path)

        self.assert_release('100')

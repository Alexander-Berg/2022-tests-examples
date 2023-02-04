import os
import yatest.common

from maps.pylibs.nginx_testlib.nginx_test_base import NginxTestBase
from yatest.common import source_path, binary_path, execute

class ServiceTestBase(NginxTestBase):
    def setup(self):
        super(ServiceTestBase, self).setup()

        self.set_config(
            nginx_path=os.path.join(binary_path('nginx/bin-noperl'), 'nginx'),
            config_dir=source_path('maps/pylibs/nginx_testlib/config'),
            environments_dir=source_path('maps/automotive/proxy/config/static-vars'),
            executor=execute)
        self._locations_path = 'maps/automotive/proxy/config/locations'

        self.add_require_path(source_path(
            'maps/automotive/proxy/tests/conf'))
        self.add_require_path(binary_path(
            'maps/automotive/libs/signature/dynamic'))
        self.add_require_path(source_path(
            'maps/automotive/libs/signature/lua'))
        self.add_require_path(source_path(
            'maps/automotive/proxy/docker/install' +
            '/usr/lib/yandex/maps/yacare/lua/init.d'))
        self.add_package('03-init-http-signature')

        self.add_config_file(source_path(
            'maps/automotive/proxy/docker/install' +
            '/etc/nginx/confroot.d/01-environment.conf'))
        self.add_confroot_include('01-environment.conf')

        valid_tvm_dst = [
            'auto-parking-receiver',
            'auto-parking-api',
            'auto-updater',
            'maps-core-startrek-proxy',
            'auto-radio',
        ]
        valid_tvm_dst_stringified = ', \n'.join(
            '"{}"'.format(tvm_dst)
            for tvm_dst in valid_tvm_dst)
        self.add_file_template(
            source_path('maps/pylibs/nginx_testlib/config/tvm2_mock.lua.template'),
            'tvm2_mock.lua',
            {'valid_tvm_dst': valid_tvm_dst_stringified})
        self.add_package('tvm2_mock')

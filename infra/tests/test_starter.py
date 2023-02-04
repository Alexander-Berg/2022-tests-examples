from __future__ import absolute_import

try:
    from gevent.coros import RLock
except ImportError:
    from gevent.lock import RLock

import mock

from skycore.kernel_util.unittest import TestCase, main
from skycore.procs import starter


class TestStarter(TestCase):
    def test_starter_run(self):
        args = {
            'args': [],
            'service_root': '/',
            'user': 'root',
            'cgroups': None,
            'limits': None,
            'env': None,
            'root_container': None,
            'porto_meta_options': None,
            'porto_options': None,
            'tags': (),
            'log': mock.Mock(),
            'out_log': mock.Mock(),
            'raw_args': [],
        }

        strtr = starter.Starter(
            porto=None,
            portowatcher=mock.Mock(),
            workdir=mock.Mock(),
            process_lock=RLock(),
            cgroup_controller=mock.Mock()
        )
        with self.assertRaises(RuntimeError):
            strtr.run(porto='yes', fast=True, **args)

        with self.assertRaises(RuntimeError):
            strtr.run(porto='yes', fast=False, **args)

        strtr = starter.Starter(
            porto=mock.Mock(),
            portowatcher=mock.Mock(),
            workdir=mock.Mock(),
            process_lock=RLock(),
            cgroup_controller=mock.Mock()
        )
        porto_mock = mock.patch('skycore.procs.starter.PortoProcess')
        liner_mock = mock.patch('skycore.procs.starter.LinerProcess')
        subpr_mock = mock.patch('skycore.procs.starter.Subprocess')
        exists_mock = mock.patch('os.path.exists')
        with porto_mock as pmock, liner_mock as lmock, subpr_mock as smock, exists_mock as xmock:
            ret_obj_porto = object()
            ret_obj_liner = object()
            ret_obj_subpr = object()

            pmock.return_value = ret_obj_porto
            lmock.return_value = ret_obj_liner
            smock.return_value = ret_obj_subpr
            xmock.return_value = True

            self.assertIs(strtr.run(porto='yes', fast=True, **args), ret_obj_porto)
            self.assertIs(strtr.run(porto='yes', fast=False, **args), ret_obj_porto)

            self.assertIs(strtr.run(porto='auto', fast=True, **args), ret_obj_porto)
            self.assertIs(strtr.run(porto='auto', fast=False, **args), ret_obj_porto)

            self.assertIs(strtr.run(porto='no', fast=True, **args), ret_obj_subpr)
            self.assertIs(strtr.run(porto='no', fast=False, **args), ret_obj_liner)

            pmock.side_effect = Exception('porto is not available')

            with self.assertRaises(Exception):
                strtr.run(porto='yes', fast=True, **args)

            with self.assertRaises(Exception):
                strtr.run(porto='yes', fast=False, **args)

            self.assertIs(strtr.run(porto='auto', fast=True, **args), ret_obj_subpr)
            self.assertIs(strtr.run(porto='auto', fast=False, **args), ret_obj_liner)


if __name__ == '__main__':
    main()

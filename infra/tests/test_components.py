from __future__ import absolute_import

import gevent

from skycore.kernel_util.unittest import TestCase, main
from skycore.framework.component import Component
from skycore import initialize_skycore


class MyComponent(Component):
    def __init__(self, *args, **kwargs):
        super(MyComponent, self).__init__(*args, **kwargs)
        self.loop_waked = False

    @Component.green_loop(logname='loop1')
    def loop(self, log):
        try:
            gevent.sleep(100)
        except Component.WakeUp:
            self.loop_waked = True

        return 0


class TestComponent(TestCase):
    def setUp(self):
        initialize_skycore()
        super(TestComponent, self).setUp()

    def test_multiple_instances(self):
        obj1 = MyComponent()
        obj2 = MyComponent()
        obj1.start()
        obj2.start()
        gevent.sleep()
        self.assertFalse(obj1.loop_waked or obj2.loop_waked)

        obj1.loop.wakeup()
        gevent.sleep()
        self.assertFalse(obj2.loop_waked)
        self.assertTrue(obj1.loop_waked)

        obj1.loop_waked = False
        obj2.loop.wakeup()
        gevent.sleep()
        self.assertFalse(obj1.loop_waked)
        self.assertTrue(obj2.loop_waked)


if __name__ == '__main__':
    main()

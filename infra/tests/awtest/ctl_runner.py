from awtest.core import wait_until


class CtlRunner(object):
    def __init__(self, zk_client):
        """
        :type zk_client: zookeeper_client.ZookeeperClient
        """
        self._ctls = set()
        self._zk = zk_client

    def run_ctl(self, ctl):
        self._ctls.add(ctl)
        ctl.start()
        wait_until(lambda: ctl.started and not ctl.busy, timeout=10)

    def stop(self):
        for ctl in self._ctls:
            ctl.stop()
        self._ctls.clear()

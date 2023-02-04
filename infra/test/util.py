import contextlib
import time
from requests.adapters import HTTPAdapter, DEFAULT_POOLSIZE, DEFAULT_RETRIES, DEFAULT_POOLBLOCK


class Checker:
    ATTEMPT_SUCCEEDED = object()
    EXCEPTION_RETRIED = object()

    def __init__(self, timeout=1, interval=1):
        self._checker = None
        self._timeout = timeout
        self._interval = interval

    @contextlib.contextmanager
    def _attempt(self):
        try:
            yield
        except Exception as e:
            outcome = self._checker.throw(e)
            assert outcome is self.EXCEPTION_RETRIED
        else:
            try:
                self._checker.send(self.ATTEMPT_SUCCEEDED)
            except StopIteration:
                pass

    def __iter__(self):
        def checker():
            deadline = time.time() + self._timeout

            err = None
            try:
                outcome = yield self._attempt()
            except Exception as e:
                err = e
            else:
                assert outcome is self.ATTEMPT_SUCCEEDED
                return

            while 1:
                outcome = yield self.EXCEPTION_RETRIED
                assert outcome is None

                if time.time() > deadline:
                    raise err

                try:
                    outcome = yield self._attempt()
                except Exception:
                    pass
                else:
                    assert outcome is self.ATTEMPT_SUCCEEDED
                    return

                time.sleep(self._interval)

        self._checker = checker()
        return self._checker


class DNSResolverHTTPSAdapter(HTTPAdapter):
    def __init__(self, fqdn, ip, pool_connections=DEFAULT_POOLSIZE, pool_maxsize=DEFAULT_POOLSIZE,
                 max_retries=DEFAULT_RETRIES, pool_block=DEFAULT_POOLBLOCK):
        self.fqdn = fqdn
        self.ip = ip
        super().__init__(pool_connections=pool_connections, pool_maxsize=pool_maxsize,
                         max_retries=max_retries, pool_block=pool_block)

    def get_connection(self, url, proxies=None):
        redirected_url = url.replace(self.fqdn, self.ip)
        return super().get_connection(redirected_url, proxies=proxies)

    def init_poolmanager(self, connections, maxsize, block=DEFAULT_POOLBLOCK, **pool_kwargs):
        pool_kwargs['assert_hostname'] = self.fqdn
        super().init_poolmanager(connections, maxsize, block=block, **pool_kwargs)

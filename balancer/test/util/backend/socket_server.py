import time
import socket
import select
import errno
import threading

__all__ = ["TCPServer"]


def _eintr_retry(func, *args):
    """restart a system call interrupted by EINTR"""
    while True:
        try:
            return func(*args)
        except (OSError, select.error) as e:
            if e.args[0] != errno.EINTR:
                raise


def _handle_error(client_address):
    print '-'*40
    print 'Exception happened during processing of request from',
    print client_address
    import traceback
    traceback.print_exc()  # XXX But this goes to stderr!
    print '-'*40


# noinspection PyBroadException
class TCPServer(object):
    def __init__(
        self,
        addrs,
        RequestHandlerClass,
        bind_and_activate=True,
        listen_queue=64,
        allow_reuse_address=False
    ):
        self.server_address = None
        self.RequestHandlerClass = RequestHandlerClass
        self.__is_shut_down = threading.Event()
        self.__shutdown_request = False
        self.addrs = addrs
        self.pollfd = select.poll()
        self.sockets = [
            (
                socket.socket(addr[0], socket.SOCK_STREAM),
                (addr[1], addr[2])
            ) for addr in addrs
        ]
        self.socket_map = {
            sock[0].fileno(): sock for sock in self.sockets
        }
        self.listen_queue = listen_queue
        self.conn_cnt = 0
        self.allow_reuse_address = allow_reuse_address
        if bind_and_activate:
            try:
                self.server_bind()
                self.server_activate()
            except:  # TODO: finally
                self.server_close()
                raise

        self.__request_count = 0
        self.__lock = threading.Lock()

    def server_bind(self):
        if self.allow_reuse_address:
            for sock in self.sockets:
                sock[0].setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        for sock, addr in self.sockets:
            sock.bind(addr)

    def server_activate(self):
        for sock in self.sockets:
            sock[0].listen(max(self.listen_queue, 1))

    def server_close(self):
        for sock in self.sockets:
            sock[0].close()

    def _get_request(self, sock):
        """Get the request and client address from the socket.
        May be overridden.
        """
        return sock.accept()

    def serve_forever(self, poll_interval=0.5):
        """Handle one request at a time until shutdown.

        Polls for shutdown every poll_interval seconds. Ignores
        self.timeout. If you need to do periodic tasks, do them in
        another thread.
        """
        self.__is_shut_down.clear()
        for fd in self.socket_map.keys():
            self.pollfd.register(fd, select.POLLIN)
        try:
            while not self.__shutdown_request:
                if self._stop_working():
                    time.sleep(poll_interval)
                    continue
                events = _eintr_retry(self.pollfd.poll, poll_interval)
                for fd, ev in events:
                    if fd in self.socket_map and (ev & select.POLLIN) == select.POLLIN:
                        if self._stop_working():
                            break
                        self.conn_cnt += 1
                        self._handle_request(self.socket_map[fd][0])
        finally:
            self.__shutdown_request = False
            self.__is_shut_down.set()

    def shutdown(self):
        """Stops the serve_forever loop.

        Blocks until the loop has finished. This must be called while
        serve_forever() is running in another thread, or it will
        deadlock.
        """
        self.__shutdown_request = True
        self.__is_shut_down.wait()

        start_wait = time.time()
        while time.time() - start_wait < 10:
            with self.__lock:
                if self.__request_count == 0:
                    break

            time.sleep(0.5)

    def _stop_working(self):
        return self.listen_queue == 0 and self.conn_cnt > 0

    def _process_request_thread(self, request, client_address):
        try:
            self._finish_request(request, client_address)
            self._shutdown_request(request)
        except:
            _handle_error(client_address)
            self._shutdown_request(request)

    def _handle_request(self, sock):
        try:
            request, client_address = self._get_request(sock)
        except socket.error:
            return
        try:
            with self.__lock:
                self.__request_count += 1

            t = threading.Thread(target=self._process_request_thread,
                                 args=(request, client_address))
            t.daemon = False
            t.start()
        except:
            _handle_error(client_address)
            self._shutdown_request(request)

    def _shutdown_request(self, request):
        with self.__lock:
            self.__request_count -= 1
        try:
            # explicitly shutdown.  socket.close() merely releases
            # the socket and waits for GC to perform the actual close.
            request.shutdown(socket.SHUT_WR)
        except socket.error:
            pass  # some platforms may raise ENOTCONN here
        request.close()

    def _finish_request(self, request, client_address):
        """Finish one request by instantiating RequestHandlerClass."""
        self.RequestHandlerClass(request, client_address, self)

# -*- coding: utf-8 -*-
import errno
import socket
from balancer.test.util.stream.ssl.parse import SSLHandshakeInfo
from balancer.test.util.stream.io.stream import ProcessStream


class SSLClientOptions(object):
    __SESS_OUT_KEY = '-sess_out'

    def __init__(self, quiet=True, msg=False, ca_file=None, sess_in=None, sess_out=None, no_ticket=False, server_name=None,
                 cipher=None, next_proto=None, alpn=None, ssl2=None, ssl3=None, tls1=None, tls1_1=None, tls1_2=None, tls1_3=None,
                 key=None, cert=None,):
        super(SSLClientOptions, self).__init__()
        self.__quiet = quiet
        self.__msg = msg
        self.no_ticket = no_ticket
        self.__params = {
            '-CAfile': ca_file,
            '-sess_in': sess_in,
            self.__SESS_OUT_KEY: sess_out,
            '-servername': server_name,
            '-cipher': cipher,
            '-nextprotoneg': next_proto,
            '-alpn': alpn,
            '-key': key,
            '-cert': cert,
        }
        self.__protos = {
            'ssl2': ssl2,
            'ssl3': ssl3,
            'tls1': tls1,
            'tls1_1': tls1_1,
            'tls1_2': tls1_2,
            'tls1_3': tls1_3,
        }

    @property
    def quiet(self):
        return self.__quiet

    @property
    def alpn(self):
        return self.__params['-alpn']

    @property
    def sess_out(self):
        return self.__params[self.__SESS_OUT_KEY]

    @sess_out.setter
    def sess_out(self, value):
        self.__params[self.__SESS_OUT_KEY] = value

    def build_options(self):
        result = list()
        if self.__quiet:
            result.append('-quiet')
        else:
            result.extend(['-tlsextdebug', '-status'])
        if self.__msg:
            result.append('-msg')
        if self.no_ticket:
            result.append('-no_ticket')

        for param, value in self.__params.iteritems():
            if value is not None:
                result.extend([param, value])

        for proto, value in self.__protos.iteritems():
            if value is not None:
                if value:
                    result.append('-%s' % proto)
                else:
                    result.append('-no_%s' % proto)
        return result


class SSLClientStream(ProcessStream):
    HANDSHAKE_INFO_TIMEOUT = 0.5

    def __init__(self, process_manager, host, port, ssl_options, openssl_path, check_closed=True, conn_timeout=None):
        cmd = [openssl_path, 's_client', '-connect', '%s:%d' % (host, port)]
        self.ssl_options = ssl_options
        cmd.extend(ssl_options.build_options())

        super(SSLClientStream, self).__init__(cmd, process_manager, multiprocess=False, timeout=0, conn_timeout=conn_timeout)

        if not self.ssl_options.quiet:
            if conn_timeout is None:
                self.set_timeout(self.HANDSHAKE_INFO_TIMEOUT)
            data = ''
            for i in range(20):  # workaround to make tests more stable
                data += self.__recv_all()
                handshake_info = SSLHandshakeInfo(data)
                if handshake_info.verified is not None:
                    break
            self.handshake_info = handshake_info
            self.restore_timeout()
        else:
            self.handshake_info = None

        if check_closed and self.is_closed(0.1):
            raise socket.error(errno.ECONNREFUSED, '')

    def __recv_all(self):
        result = list()
        data = '1'
        while data:
            data = self.recv_quiet()
            result.append(data)
        return ''.join(result)


class SSLServerStream(ProcessStream):
    def __init__(self, process_manager, port, key, cert, openssl_path, quiet=True, opts=None):
        cmd = [openssl_path, 's_server', '-port', str(port), '-key', key, '-cert', cert]
        if quiet:
            cmd.append('-quiet')
        if opts is not None:
            cmd.extend(opts)
        super(SSLServerStream, self).__init__(cmd, process_manager)

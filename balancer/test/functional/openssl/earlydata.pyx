from libc.stdint cimport uint8_t, uint16_t, uint32_t
from libc.stdlib cimport malloc
from libc.stdio cimport stderr, fprintf, FILE
from libc.string cimport memset, strlen, memcpy
from posix.unistd cimport close
from posix.ioctl cimport ioctl

from cython.operator cimport dereference
from util.generic.hash cimport THashMap

import balancer.test.util.proto.http2.connection as http2_conn

from balancer.test.util.balancer import asserts
from balancer.test.util.predef import http2
from balancer.test.util.proto.http2.framing import frames


class WriteEarlyDataError(Exception):
    pass


cdef extern from "contrib/libs/python/Include/Python.h":
    object PyString_FromStringAndSize(char *s, int len)


cdef extern from *:
    int FIONREAD


cdef extern from "arpa/inet.h" nogil:
    int inet_pton(int af, const char *src, void *dst);
    uint16_t htons(uint16_t hostshort);


cdef extern from "sys/socket.h" nogil:
    ctypedef unsigned short int sa_family_t
    ctypedef uint16_t in_port_t
    ctypedef int socklen_t

    struct in6_addr:
        uint8_t s6_addr[16]

    struct sockaddr:
        pass

    struct sockaddr_in6:
        sa_family_t   sin6_family
        in_port_t     sin6_port
        unsigned int  sin6_flowinfo
        in6_addr      sin6_addr
        unsigned int  sin6_scope_id


    int SOCK_STREAM, SOCK_DGRAM
    int AF_UNIX, AF_INET, AF_INET6, AF_UNSPEC

    int socket(int domain, int type, int protocol)
    int connect(int sockfd, const sockaddr *serv_addr, socklen_t addrlen)


cdef extern from "contrib/libs/openssl/include/openssl/ssl.h" nogil:
    struct SSL_METHOD:
        pass

    struct ssl_st:
        pass
    ctypedef ssl_st SSL;

    struct ssl_ctx_st:
        pass
    ctypedef ssl_ctx_st SSL_CTX

    struct ssl_session_st:
        pass
    ctypedef ssl_session_st SSL_SESSION

    struct bio_st:
        pass
    ctypedef bio_st BIO


    cdef int SSL_OP_ENABLE_MIDDLEBOX_COMPAT
    cdef int SSL_OP_NO_TICKET
    cdef int TLS1_3_VERSION

    cdef int SSL_SESS_CACHE_CLIENT
    cdef int SSL_SESS_CACHE_NO_INTERNAL_STORE

    cdef int SSL_ERROR_WANT_READ
    cdef int SSL_ERROR_WANT_WRITE
    cdef int SSL_ERROR_WANT_ASYNC
    cdef int SSL_ERROR_ZERO_RETURN

    cdef int BIO_NOCLOSE

    cdef int SSL_EARLY_DATA_NOT_SENT
    cdef int SSL_EARLY_DATA_REJECTED
    cdef int SSL_EARLY_DATA_ACCEPTED

    cdef int SSL_ERROR_NONE

    int SSL_library_init();
    int SSL_version(const SSL *s);

    const SSL_METHOD *TLS_client_method();
    SSL_CTX *SSL_CTX_new(const SSL_METHOD *meth);
    SSL *SSL_new(SSL_CTX *ctx);
    int SSL_shutdown(SSL *ssl);
    void SSL_free(SSL *ssl);
    int SSL_read(SSL *ssl, void *buf, int num);
    int SSL_write(SSL *ssl, const void *buf, int num);
    int SSL_set_alpn_protos(SSL *ssl, const unsigned char *protos,
                         unsigned int protos_len);

    int SSL_CTX_set_max_early_data(SSL_CTX *ctx, uint32_t max_early_data);
    long SSL_CTX_clear_options(SSL_CTX *ctx, long options);
    long SSL_CTX_set_session_cache_mode(SSL_CTX *ctx, long mode);
    int SSL_CTX_set_min_proto_version(SSL_CTX *ctx, int version);
    int SSL_CTX_set_max_proto_version(SSL_CTX *ctx, int version);

    BIO *BIO_new_file(const char *filename, const char *mode);
    BIO *BIO_new_fp(FILE *stream, int flags);
    int BIO_free(BIO *a);
    int BIO_flush(BIO *b);
    int PEM_write_bio_SSL_SESSION(BIO *bp, const SSL_SESSION *a);

    ctypedef int pem_password_cb(char *buf, int size, int rwflag, void *u);
    SSL_SESSION *PEM_read_bio_SSL_SESSION(BIO *bp, SSL_SESSION **a, pem_password_cb *cb, void *u);

    int SSL_set_fd(SSL *ssl, int fd);
    int SSL_connect(SSL *ssl);
    int SSL_do_handshake(SSL *ssl);
    void SSL_CTX_sess_set_new_cb(SSL_CTX *ctx,
                              int (*new_session_cb)(SSL *, SSL_SESSION *));
    int SSL_set_session(SSL *ssl, SSL_SESSION *session);
    int SSL_session_reused(SSL *ssl);
    int SSL_SESSION_print(BIO *fp, const SSL_SESSION *ses);
    int SSL_is_init_finished(const SSL *s);
    int SSL_SESSION_is_resumable(const SSL_SESSION *s);
    int SSL_write_early_data(SSL *s, const void *buf, size_t num, size_t *written);
    int SSL_get_early_data_status(const SSL *s);


    int SSL_get_error(const SSL *ssl, int ret);

    void SSL_set_msg_callback(SSL *ssl, void (*cb)(int write_p, int version, int content_type, const void *buf, size_t len, SSL *ssl, void *arg));
    void SSL_set_msg_callback_arg(SSL *ssl, void *arg);

    void SSL_trace(int write_p, int version, int content_type, const void *buf, size_t msglen, SSL *ssl, void *arg)
    # By default disabled in Arcadia
    cdef void ifdef_notrace "#ifndef OPENSSL_NO_SSL_TRACE //" ()
    cdef void endif_notrace "#endif //" ()

    void SSL_set_psk_client_callback(SSL *ssl,
        unsigned int (*callback)(SSL *ssl, const char *hint,
        char *identity, unsigned int max_identity_len,
        unsigned char *psk, unsigned int max_psk_len));

    void OPENSSL_free(void *addr)

    struct crypto_ex_data_st:
        pass
    ctypedef crypto_ex_data_st CRYPTO_EX_DATA

    ctypedef void CRYPTO_EX_new (void *parent, void *ptr, CRYPTO_EX_DATA *ad,
                           int idx, long argl, void *argp);
    ctypedef void CRYPTO_EX_free (void *parent, void *ptr, CRYPTO_EX_DATA *ad,
                             int idx, long argl, void *argp);
    ctypedef int CRYPTO_EX_dup (CRYPTO_EX_DATA *to, const CRYPTO_EX_DATA *_from,
                           void *from_d, int idx, long argl, void *argp);

    int SSL_get_ex_new_index(long argl, void *argp,
                CRYPTO_EX_new *new_func,
                CRYPTO_EX_dup *dup_func,
                CRYPTO_EX_free *free_func);

    int SSL_set_ex_data(SSL *ssl, int idx, void *arg);
    void *SSL_get_ex_data(const SSL *ssl, int idx);


cdef extern from "contrib/libs/openssl/include/openssl/bio.h" nogil:
    struct bio_method_st:
        pass
    ctypedef bio_method_st BIO_METHOD

    BIO_METHOD*   BIO_s_mem();
    BIO*  BIO_new(BIO_METHOD *type);
    void SSL_set_bio(SSL *ssl, BIO *rbio, BIO *wbio);
    int BIO_printf(BIO *bio, const char *format, ...)
    int BIO_snprintf(char *buf, size_t n, const char *format, ...)
    BIO *BIO_new_socket(int sock, int close_flag);


cdef extern from "contrib/libs/openssl/include/openssl/err.h" nogil:
    void ERR_clear_error();
    void ERR_error_string_n(unsigned long e, char *buf, size_t len);
    unsigned long ERR_get_error();
    void SSL_load_error_strings();
    void ERR_print_errors(BIO *bp);


cdef extern from "contrib/libs/openssl/include/openssl/pem.h" nogil:
    int PEM_write_bio_SSL_SESSION(BIO *bp, const SSL_SESSION *a);


cdef int SSL_SESSION_BIO_IDX

cdef int ssl_session_cb(SSL *s, SSL_SESSION *sess) nogil:
    # Uncomment this for debugging
    #
    # cdef BIO* out1 = BIO_new_file("~/foobar1.txt", "a")
    # BIO_printf(out1,
    #               "---\nPost-Handshake New Session Ticket arrived:\n")
    # SSL_SESSION_print(out1, sess)
    # BIO_printf(out1, "---\n")
    # BIO_free(out1)

    # Store session in global index

    cdef BIO* out = BIO_new(BIO_s_mem())
    PEM_write_bio_SSL_SESSION(out, sess)
    
    SSL_set_ex_data(s, SSL_SESSION_BIO_IDX, out)


cdef char* psk_identity = "Client_identity"
cdef unsigned char* psk_key = "Client_psk"

cdef unsigned int psk_cb(SSL *ssl, const char *hint, char *_id, unsigned int max_id_len, unsigned char *psk, unsigned int max_psk_len) nogil:
    # Set pre-shared key
    cdef int ret = BIO_snprintf(_id, max_id_len, "%s", psk_identity);

    if ret < 0 or <unsigned int>ret > max_id_len:
        return 0

    memcpy(psk, &psk_key, sizeof(psk_key))
    return sizeof(psk_key) - 1;


cdef int init_socket(char* host, int port) nogil:
    cdef int fd = socket(AF_INET6, SOCK_STREAM, 0);
    if fd < 0:
        return -1

    cdef sockaddr_in6 addr
    memset(&addr, 0, sizeof(addr));
    addr.sin6_family = AF_INET6;
    inet_pton(AF_INET6, host, &addr.sin6_addr);
    addr.sin6_port = htons(port);

    cdef int ret = connect(fd, <sockaddr*>&addr, sizeof(addr));
    if ret == -1:
        close(fd)
        return ret

    return fd


cdef SSL* ssl_init(char* host, int port, int *sfd, BIO* out):
    SSL_load_error_strings()
    SSL_library_init()

    cdef SSL_CTX* client_ctx = SSL_CTX_new(TLS_client_method())

    SSL_CTX_set_max_early_data(client_ctx, 0xffff);
    # Hide TLS1.3 in TLS1.2
    SSL_CTX_clear_options(client_ctx, SSL_OP_ENABLE_MIDDLEBOX_COMPAT);
    # Disable internal caching
    SSL_CTX_set_session_cache_mode(client_ctx, SSL_SESS_CACHE_CLIENT | SSL_SESS_CACHE_NO_INTERNAL_STORE);
    # Use only TLS1.3
    if SSL_CTX_set_min_proto_version(client_ctx, TLS1_3_VERSION) != 1:
        if out != NULL:
            BIO_printf(out, "---\nCould not set TLS 1.3 min version\n---\n")
        return NULL
    if SSL_CTX_set_max_proto_version(client_ctx, TLS1_3_VERSION) != 1:
        if out != NULL:
            BIO_printf(out, "---\nCould not set TLS 1.3 max version\n---\n")
        return NULL

    SSL_CTX_sess_set_new_cb(client_ctx, &ssl_session_cb)

    cdef SSL *client_ssl = SSL_new(client_ctx);
    if out != NULL:
        # comment ifndef OPENSSL_NO_SSL_TRACE in
        # contrib/libs/openssl/include/openssl/opensslconf-linux.h
        ifdef_notrace()
        SSL_set_msg_callback(client_ssl, SSL_trace);
        SSL_set_msg_callback_arg(client_ssl, out);
        endif_notrace()

    SSL_set_psk_client_callback(client_ssl, psk_cb)

    ERR_clear_error();

    # dereference pointer
    sfd[0] = init_socket(host, port);

    if sfd[0] < 0:
        if out != NULL:
            BIO_printf(out, "---\nCould not create socket: %d\n---\n", sfd[0])
        return NULL

    cdef BIO *b = BIO_new_socket(sfd[0], BIO_NOCLOSE);
    SSL_set_bio(client_ssl, b, b);

    SSL_SESSION_BIO_IDX = SSL_get_ex_new_index(0, "Session Idx", NULL, NULL, NULL); 

    return client_ssl


cdef int ssl_connect(SSL* client_ssl):
    cdef ret = SSL_connect(client_ssl);
    cdef int err = 0
    if ret < 0:
        err = SSL_get_error(client_ssl, ret)

        if err != SSL_ERROR_WANT_READ and err != SSL_ERROR_WANT_WRITE:
            return err * -1
    return ret


cdef int ssl_set_alpn(SSL* client_ssl, unsigned char* alpn, unsigned int _len) nogil:
    if SSL_set_alpn_protos(client_ssl, alpn, _len) < 0:
        return -1
    return 1


cdef int ssl_set_session(SSL* client_ssl, BIO* session) nogil:
    cdef SSL_SESSION *sess = PEM_read_bio_SSL_SESSION(session, NULL, NULL, NULL);
    if sess == NULL:
        return -1

    # Check if stored session is valid
    if SSL_SESSION_is_resumable(sess) != 1:
        return -2

    if SSL_set_session(client_ssl, sess) != 1:
        return -3
    return 1


cdef int ssl_is_reused(SSL* client_ssl) nogil:
    cdef int ret = SSL_session_reused(client_ssl)
    return ret


cdef int ssl_write_earlydata(SSL* client_ssl, char* data, size_t _len, BIO* out) nogil:
    cdef size_t wrtn = 0
    cdef int err = 0

    while SSL_write_early_data(client_ssl, data, _len, &wrtn) != 1:
        err = SSL_get_error(client_ssl, 0);
        if err == SSL_ERROR_WANT_WRITE or err == SSL_ERROR_WANT_ASYNC or err == SSL_ERROR_WANT_READ:
            continue
        elif out != NULL:
            BIO_printf(out,
                   "---\nEarly data error [%d]:\n", err);
            ERR_print_errors(out);
            BIO_printf(out, "---\n")

        return err * -1
    return wrtn


cdef int socket_peek(int fd, BIO* out) nogil:
    # SSL buffer could be empty while the socket still have data
    cdef int ret = -1
    ioctl(fd, FIONREAD, &ret)
    if out != NULL:
        BIO_printf(out, "---\nSocket peak: %d\n---\n", ret)
    return ret


cdef size_t ssl_read_data(SSL* client_ssl, char* rbuf, size_t _len, int sfd, BIO *out) nogil:
    cdef int ret = 0
    cdef int err = 0
    cdef int blocked = 0
    cdef size_t off = 0
    while True:
        ret = SSL_read(client_ssl, rbuf + off, _len - off)
        if ret < 1:
            err = SSL_get_error(client_ssl, ret)
            if err == SSL_ERROR_WANT_READ:
                if out != NULL:
                    BIO_printf(out, "---\nContinue want read\n---\n")
                blocked = 1
                continue
            elif err == SSL_ERROR_ZERO_RETURN:
                if out != NULL:
                    BIO_printf(out, "---\nExiting from read with zero return\n---\n")
                break
            elif out != NULL:
                BIO_printf(out,
                       "---\nRead data error [%d]:\n", err);
                ERR_print_errors(out);
                BIO_printf(out, "---\n")

            return err * -1

        off += ret
        if (socket_peek(sfd, out) < 1 and not blocked) or off >= _len:
            break

    return off


cdef class SSLSocket:
    cdef SSL* ssl
    cdef BIO* out
    cdef int socket_fd

    def __init__(self, host, port, debug=False, debug_log="~/foobar.txt"):
        self.socket_fd = -1

        if debug:
            self.out = BIO_new_file(debug_log, "a"); 
        else:
            self.out = NULL

        self.ssl = ssl_init(host, port, &self.socket_fd, self.out)

        if self.ssl == NULL:
            raise Exception("Could not init SSL")

    cpdef void connect(self) except *:
        if ssl_connect(self.ssl) < 1:
            raise Exception("Could not connect to host")

        if SSL_version(self.ssl) != TLS1_3_VERSION:
            raise Exception("Could not init TLS1.3 connection")

    cpdef void write(self, char* data, int _len) except *: 
        if SSL_write(self.ssl, data, _len) <= 0:
            raise Exception("Could not write to socket")

    def send(self, data):
        self.write(data, len(data))

    cpdef void read(self, char* data, int _len) except *:
        if ssl_read_data(self.ssl, data, _len, self.socket_fd, self.out) < 0:
            raise Exception("Could not read from socket")

    def recv(self, sz):
        cdef char* rbuf = <char*>malloc(sizeof(char) * sz)
        memset(rbuf, 0, sizeof(char) * sz)
        self.read(rbuf, sizeof(char) * sz)
        # Could not use default PyString_FromString for converting
        # to python object because of HTTP/2 frames are not null
        # terminated
        return PyString_FromStringAndSize(rbuf, sz)

    cpdef void shutdown(self):
        SSL_shutdown(self.ssl)
        SSL_free(self.ssl)
        if self.out != NULL:
            BIO_free(self.out)

    cdef BIO* get_session(self):
        return <BIO*>SSL_get_ex_data(self.ssl, SSL_SESSION_BIO_IDX)

    cdef void set_session(self, BIO* session) except *:
        if ssl_set_session(self.ssl, session) < 0:
            raise Exception("Could not set session")

    cdef void write_early(self, char* data, int _len) except *:
        if ssl_write_earlydata(self.ssl, data, _len, self.out) < 0:
            raise WriteEarlyDataError("Error writing early data")

    cdef void check_early_accepted(self) except *:
        cdef int ret = SSL_get_early_data_status(self.ssl)
        if ret == SSL_EARLY_DATA_NOT_SENT:
            raise Exception("Early data not sent")
        elif ret == SSL_EARLY_DATA_REJECTED:
            raise Exception("Early data was rejected")

    cdef void check_reused(self) except *:
        if ssl_is_reused(self.ssl) != 1:
            raise Exception("Session was not reused")

    cdef void check_finished(self) except *:
        if SSL_is_init_finished(self.ssl) != 1:
            raise Exception("SSL not initialised")

    cdef void set_alpn_http2(self) except *:
        cdef unsigned char* alpn = "\x02h2\x08http/1.1"
        if ssl_set_alpn(self.ssl, alpn, 12) < 0:
            raise Exception("Could not set ALPN")


def simple_http(host, port):
    socket_init = SSLSocket(host, port, debug=False)
    socket_init.connect()

    # use char* and constant size because cython translates
    # this code to memcpy(wbuf, char[18]...)
    cdef char* wbuf = "GET / HTTP/1.1\r\n\r\n"
    socket_init.write(wbuf, 18)

    # Complete post-handshake for new ticket from server side
    cdef char[1024] rbuf
    memset(rbuf, 0, sizeof(rbuf))
    socket_init.read(rbuf, sizeof(rbuf))

    if "early-data: 0" not in rbuf or "Hello" not in rbuf:
        raise Exception("Incorrent first response")

    cdef BIO* session = socket_init.get_session()

    socket_init.shutdown()

    socket_worker = SSLSocket(host, port, debug=False)
    socket_worker.set_session(session)

    cdef char* early = "GET /early HTTP/1.1\r\n\r\n"
    socket_worker.write_early(early, 23)

    socket_worker.connect()

    socket_worker.check_reused()
    socket_worker.check_early_accepted()
    socket_worker.check_finished()

    socket_worker.write(wbuf, 18)

    memset(rbuf, 0, 1024)
    socket_worker.read(rbuf, sizeof(rbuf))

    if "early-data: 1" not in rbuf or "Early" not in rbuf:
        raise Exception("Incorrect early data response")

    socket_worker.shutdown()


class Http2EarlyBuffer:
    # Fake class for building HTTP/2 binary data
    buf = ""

    def send(self, data):
        self.buf += data

    def raw_buffer(self):
        return self.buf


def simple_http2(host, port):
    socket_init = SSLSocket(host, port, debug=False)
    socket_init.set_alpn_http2()
    socket_init.connect()

    conn = http2_conn.ClientConnection(socket_init)
    conn.write_preface()
    conn.wait_frame(frames.Settings)
    
    stream = conn.create_stream(stream_id=1)
    stream.write_message(http2.request.get("/").to_raw_request())

    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
    asserts.content(resp, "Hello")
    asserts.header_value(resp, "early-data", "0")

    cdef BIO* session = socket_init.get_session()

    socket_init.shutdown()

    http2early = Http2EarlyBuffer()
    early_conn = http2_conn.ClientConnection(http2early)
    early_conn.write_preface()
    stream = early_conn.create_stream(stream_id=1)
    stream.write_message(http2.request.get("/early").to_raw_request())

    socket_worker = SSLSocket(host, port, debug=False)
    socket_worker.set_session(session)

    socket_worker.set_alpn_http2()

    early = http2early.raw_buffer()
    socket_worker.write_early(early, len(early))

    socket_worker.connect()

    socket_worker.check_reused()
    socket_worker.check_early_accepted()
    socket_worker.check_finished()

    conn = http2_conn.ClientConnection(socket_worker)
    stream = conn.create_stream(stream_id=1)
    stream.write_message(http2.request.get("/").to_raw_request())
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
    asserts.content(resp, "Early")
    asserts.header_value(resp, "early-data", "1")


def separate_preface_http2(host, port):
    socket_init = SSLSocket(host, port, debug=False)
    socket_init.set_alpn_http2()
    socket_init.connect()

    conn = http2_conn.ClientConnection(socket_init)
    conn.write_preface()
    conn.wait_frame(frames.Settings)
    
    stream = conn.create_stream(stream_id=1)
    stream.write_message(http2.request.get("/").to_raw_request())

    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
    asserts.content(resp, "Hello")
    asserts.header_value(resp, "early-data", "0")

    cdef BIO* session = socket_init.get_session()

    socket_init.shutdown()

    http2early = Http2EarlyBuffer()
    early_conn = http2_conn.ClientConnection(http2early)
    early_conn.write_preface()
    stream = early_conn.create_stream(stream_id=1)

    socket_worker = SSLSocket(host, port, debug=False)
    socket_worker.set_session(session)

    socket_worker.set_alpn_http2()

    early = http2early.raw_buffer()
    socket_worker.write_early(early, len(early))

    socket_worker.connect()

    socket_worker.check_reused()
    socket_worker.check_early_accepted()
    socket_worker.check_finished()

    conn = http2_conn.ClientConnection(socket_worker)
    stream = conn.create_stream(stream_id=1)
    stream.write_message(http2.request.get("/").to_raw_request())
    resp = stream.read_message().to_response()
    asserts.status(resp, 200)
    asserts.content(resp, "Hello")
    asserts.header_value(resp, "early-data", "1")



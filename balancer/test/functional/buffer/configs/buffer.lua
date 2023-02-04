if backend_timeout == nil then backend_timeout = "5s"; end
if buffer_ == nil then buffer_ = 32 * 1024; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    workers = workers;
    buffer = buffer_;
    calc_buffer_fullness = calc_buffer_fullness;
    _sock_inbufsize = socket_buffer;
    _sock_outbufsize = socket_buffer;

    events = {
        stats = "report";
    }; -- events

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    accesslog = {
                        log = accesslog;
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                            resolve_timeout = "0.3s";
                            fail_on_5xx = 0;
                            _sock_inbufsize = socket_buffer;
                            _sock_outbufsize = socket_buffer;
                            keepalive_count = 1;
                        }; -- proxy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if timeout == nil then timeout = "5s"; end
if keepalive == nil then keepalive = 1; end

if port == nil then port = 8081; end
if admin_port == nil then port = 8082; end
if backend_port == nil then port = 8765; end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    dns_timeout = dns_timeout;

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                admin = {};
            }; -- http
        }; -- admin
        remote = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = len;
                    maxreq = req;
                    keepalive = keepalive;
                    no_keepalive_file = no_keepalive_file;
                    allow_trace = allow_trace;
                    accesslog = {
                        log = accesslog;
                        regexp = {
                            admin = {
                                match_fsm = { path = "/admin"; case_insensitive = false; surround = false; };
                                admin = {};
                            }; -- admin
                            default = {
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "0.3s"; backend_timeout = timeout;
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                    allow_connection_upgrade = true;
                                }; -- proxy
                            }; -- default
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- remote
    }; -- ipdispatch
}; -- instance

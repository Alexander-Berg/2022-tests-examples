instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    log = log;

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
                proxy = {
                    host = "localhost"; port = backend_port;
                    keepalive_count = keepalive_count or 0;
                    keepalive_timeout = keepalive_timeout;
                    backend_timeout = backend_timeout or "20s";
                    connect_timeout = "1s"; resolve_timeout = "1s";
                    watch_client_close = watch_client_close;
                    https_settings = {
                        ciphers = ciphers or "DEFAULT";
                        ca_file = ca_file;
                        sni_on = sni_on;
                        sni_host = sni_host;
                        verify_depth = verify_depth;
                    };
                };
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if connection_manager_required == 'true' then instance.connection_manager = {}; end

backend_timeout = backend_timeout or "10s";

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {}
            };
        };
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                h100 = {
                    proxy = {
                        host = "localhost"; port = backend_port;
                        backend_timeout = backend_timeout;
                        connect_timeout = "1s"; resolve_timeout = "1s";
                        keepalive_count = 1;
                    };
                };
            };
        };
    };
};

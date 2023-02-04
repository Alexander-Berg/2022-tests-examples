instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            };
        };
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                debug = {
                    delay = delay;
                    client_read_delay = client_read_delay;
                    client_read_size = client_read_size;
                    client_write_delay = client_write_delay;
                    client_write_size = client_write_size;
                    freeze_on_run = freeze_on_run;

                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "5s"; backend_timeout = backend_timeout;
                        resolve_timeout = "1s";
                    };
                };
            };
        };
    };
};

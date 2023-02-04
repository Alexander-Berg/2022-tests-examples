instance = {
    thread_mode = thread_mode; set_no_file = false;
    shutdown_accept_connections = shutdown_accept_connections;
    shutdown_close_using_bpf = shutdown_close_using_bpf;

    addrs = {
        { ip = "localhost"; port = port; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    workers = workers;
    log = log;
    maxconn = 10000;

    ipdispatch = {
        localhost = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {
                };
            };
        }; -- localhost
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                proxy = {
                    host = "localhost"; port = backend_port;
                    connect_timeout = "1s"; backend_timeout = "60s";
                    resolve_timeout = "1s";
                }; -- proxy
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

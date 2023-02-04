instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    enable_reuse_port = true;
    workers = workers;
    log = log;
    maxconn = 10000;
    worker_start_delay = '10s';

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
                errordocument = {
                    status = 200;
                    content = response;
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

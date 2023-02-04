instance = {
    addrs = {
        { ip = "localhost"; port = port; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    workers = workers;
    thread_mode = thread_mode; set_no_file = false;
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
                errordocument = {
                    status = 200;
                    content = response;
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

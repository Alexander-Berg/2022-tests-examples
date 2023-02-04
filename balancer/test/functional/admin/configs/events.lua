instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs
    workers = workers;
    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;

            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                balancer2 = {
                    attempts = 1;
                    events = {
                        status = 'status';
                        enable_on_error = 'enable';
                        disable_on_error = 'disable';
                    }; -- events
                    weighted2 = {
                        {
                            proxy = {
                                backend_timeout = "5s";
                                host = "localhost";
                                port = backend_port;
                                connect_timeout = "1s"; backend_timeout = "1s";
                                resolve_timeout = "1s";
                            }; -- proxy
                        };
                    }; -- weighted2
                    on_error = {
                        errordocument = {
                            status = 503;
                            content = 'Error';
                        }; -- errordocument
                    }; -- on_error
                }; -- balancer2
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = log;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    balancer2 = {
                        attempts = 1;

                        weighted2 = {
                            {
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "1s"; backend_timeout = "1s"; resolve_timeout = "1s";
                                    watch_client_close = watch_client_close;
                                }; -- proxy
                            };
                        }; -- weighted2

                        on_error = {
                            errordocument = {
                                status = 200;
                                content = 'OK';
                            }; -- errordocument
                        }; -- on_error
                    }; -- balancer2
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

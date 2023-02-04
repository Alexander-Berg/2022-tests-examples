instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    };
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
                        attempts = 1; -- XXX: it's still `attempts = 1`
                        rr = {
                            randomize_initial_state = false;
                            {
                                accesslog = {
                                    log = accesslog;
                                    proxy = {
                                        buffering = 1;
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "1s"; backend_timeout = "2.1s"; resolve_timeout = "1s";
                                        connect_retry_delay = "1s";
                                        connect_retry_timeout = "4.5s"; -- XXX: actual timeout is ~4s in case of `Connection refused'
                                        watch_client_close = watch_client_close;
                                    }; -- proxy
                                }; -- accesslog
                            };
                        }; -- rr
                        on_error = {
                            errordocument = {
                                status = "501";
                                content = "Sorry :(";
                            };
                        };
                    }; -- balancer2
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

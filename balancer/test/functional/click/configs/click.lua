instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

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
            ip = "localhost";
            port = port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                accesslog = {
                    log = accesslog;
                    click = {
                        keys = keys;
                        file_switch = file_switch;
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                            resolve_timeout = "0.3s";
                            keepalive_count = 5;
                            fail_on_5xx = 0;
                        }; -- proxy
                    }; -- click
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

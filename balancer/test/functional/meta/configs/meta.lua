instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

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
                    meta = {
                        id = 'test-id';
                        fields = {
                            fieldA = 'aaa';
                            fieldB = 'bbb';
                        };
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "0.3s"; backend_timeout = "5s";
                            resolve_timeout = "0.3s";
                            fail_on_5xx = 0;
                        }; -- proxy
                    }; -- meta
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

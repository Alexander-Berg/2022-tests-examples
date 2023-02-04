instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    thread_mode = thread_mode; set_no_file = false;

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                keepalive = 1;
                cache_server = {
                    memory_limit = 1024;
                    check_modified = 1;
                    valid_for = 60 * 60;
                    async_init = 0;
                }; -- cache_server
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

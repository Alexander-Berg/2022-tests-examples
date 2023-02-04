instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs

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
                cache_client = {
                    id_regexp = "/id=(\\d+)&n=(\\d+)";
                    server = {
                        cache_server = {
                            check_modified = 1;
                            valid_for = "1000s";
                            memory_limit = 1024;
                        }; -- cache_server
                    }; -- server
                    module = {
                        errordocument = {
                            status = 200;
                            content = "ololo";
                        }; -- errordocument
                    }; -- module
                }; -- cache_client
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

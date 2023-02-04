instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

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
                accesslog = {
                    log = accesslog;

                    cache_client = {
                        id_regexp = "/(.*)";
                        server = {
                            cache_server = {
                                check_modified = 1;
                                valid_for = "1000s";
                                memory_limit = 1024;
                            }; -- cache_server
                        }; -- server
                        module = {
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "5s"; backend_timeout = "5s";
                                resolve_timeout = "1s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        }; -- module
                    }; -- cache_client
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

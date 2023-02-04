check = check and check == "true"
keepalive = keepalive and keepalive == "true"
print(match)
instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
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
                keepalive = keepalive;
                errorlog = {
                    log = errorlog;
                    log_level = "DEBUG";

                    cache_client = {
                        id_regexp = match;
                        server = {
                            cache_server = {
                                check_modified = check;
                                valid_for = ttl;
                                memory_limit = mem;
                            }; -- cache_server
                        }; -- server
                        module = {
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "5s"; backend_timeout = backend_timeout;
                                resolve_timeout = "1s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        }; -- module
                    }; -- cache_client
                }; -- errorlog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

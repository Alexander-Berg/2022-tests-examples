instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
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
        default = {
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                keepalive = true;
                errorlog = {
                    log = errorlog;
                    log_level = "DEBUG";
                    accesslog = {
                        log = accesslog;

                        regexp = {
                            check_head = {
                                match_fsm = {
                                    url = "/check_head"; case_insensitive = false; surround = false;
                                }; -- match_fsm
                                errordocument = {
                                    status = 200;
                                    content = "match";
                                }; -- errordocument
                            }; -- check_head
                            default = {
                                cachalot = {
                                    collection = collection;
                                    cacher = {
                                        proxy = {
                                            host = "localhost"; port = cacher_port;
                                            connect_timeout = "5s"; backend_timeout = cacher_timeout;
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 0;
                                            keepalive_count = 0;
                                            cached_ip = "::1";
                                            buffering = 1;
                                        }; -- proxy
                                    }; -- server
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "5s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                        cached_ip = "::1";
                                    }; -- proxy
                                }; -- cachalot
                            }; -- default
                        }; -- regexp
                    }; -- accesslog
                }; -- errorlog
            }; -- http
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance

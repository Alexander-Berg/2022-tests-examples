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
                    headers = {
                        create_func_weak = {
                            ["reqid"] = "reqid";
                        };
                        log_headers = {
                            name_re = name_re;
                            response_name_re = response_name_re;
                            cookie_fields = cookie_fields;
                            log_response_body_md5 = log_body_md5;
                            log_cookie_meta = log_cookie_meta;
                            log_set_cookie = log_set_cookie;
                            log_set_cookie_meta = log_set_cookie_meta;
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "0.3s"; backend_timeout = "5s";
                                resolve_timeout = "0.3s";
                                fail_on_5xx = 0;
                            }; -- proxy
                        }; -- log_headers
                    }; -- headers
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

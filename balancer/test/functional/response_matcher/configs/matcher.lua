instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                response_matcher = {
                    module = {
                        proxy = {
                            host = "localhost";
                            port = backend1_port;
                            backend_timeout = "1s"; connect_timeout = "1s";
                        };
                    };
                    forward_headers = forward_headers;
                    buffer_size = buffer_size;
                    on_response = {
                        succ = {
                            match_response_codes = { codes = {code} };
                            proxy = {
                                host = "localhost";
                                port = backend2_port;
                                backend_timeout = "1s"; connect_timeout = "1s";
                            };
                        };
                    };
                }; -- regexp_path
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

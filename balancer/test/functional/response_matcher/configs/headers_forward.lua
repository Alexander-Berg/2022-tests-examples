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
                        headers = {
                            create = { [header] = header_value; };
                            errordocument = {
                                status = 200;
                                remain_headers = header;
                            };
                        };
                    };
                    forward_headers = forward_headers;
                    on_response = {
                        succ = {
                            match_response_codes = { codes = {200} };
                            proxy = {
                                host = "localhost";
                                port = backend_port;
                                backend_timeout = "1s"; connect_timeout = "1s";
                            };
                        };
                    };
                }; -- regexp_path
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance



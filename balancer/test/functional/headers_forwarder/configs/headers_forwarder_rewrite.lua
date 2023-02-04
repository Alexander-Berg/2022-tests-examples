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
                headers = {
                    create = {
                        [request_header] = header_value;
                    };
                    rewrite = {
                        actions = {
                            {
                                header_name = request_header;
                                regexp = regexp;
                                rewrite = rewrite;
                            };
                        }; -- actions
                        headers_forwarder = {
                            actions = {
                                {
                                    request_header = request_header;
                                    response_header = response_header;
                                    erase_from_request = erase_from_request;
                                };
                            }; -- actions

                            proxy = {
                                host = "localhost"; port = backend_port;
                                backend_timeout = "5s";
                                connect_timeout = "1s"; resolve_timeout = "1s";
                            }; -- proxy
                        }; -- headers_forwarder
                    }; -- rewrite
                }; -- headers
            }; -- http
        }; -- test
    }; -- ipdispatch

}; -- instance

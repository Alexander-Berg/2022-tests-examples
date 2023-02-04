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
                headers_forwarder = {
                    actions = {
                        {
                            request_header = first_req_header;
                            response_header = first_resp_header;
                        };
                        {
                            request_header = second_req_header;
                            response_header = second_resp_header;
                            erase_from_response = true;
                        };
                        {
                            request_header = third_req_header;
                            response_header = third_resp_header;
                            erase_from_request = true;
                        };
                    };

                    proxy = {
                        host = "localhost"; port = backend_port;
                        backend_timeout = "5s";
                        connect_timeout = "1s"; resolve_timeout = "1s";
                    }; -- proxy
                }; -- headers_forwarder
            }; -- http
        }; -- test
    }; -- ipdispatch

}; -- instance

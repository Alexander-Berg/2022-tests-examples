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
                response_headers_if = {
                    if_has_header = if_has_header;
                    create_header = {
                        [string.format("%s", header_name_1)] = header_value_1;
                        [string.format("%s", header_name_2)] = header_value_2;
                    };
                    erase_if_has_header = erase_if_has_header;
                    delete_header = delete_header;
                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "100ms"; backend_timeout = "500ms";
                        resolve_timeout = "1s";
                    }; -- proxy
                }; -- response_headers_if
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

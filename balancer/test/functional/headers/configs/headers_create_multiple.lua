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
                    [create_mod_name] = {
                        [realip_header] = "realip";
                        [realport_header] = "realport";
                        [url_header] = "url";
                    }; -- create
                    proxy = {
                        host = "localhost"; port = backend_port;
                        backend_timeout = "5s";
                        connect_timeout = "1s";
                        resolve_timeout = "1s";
                    }; -- proxy
                }; -- headers
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

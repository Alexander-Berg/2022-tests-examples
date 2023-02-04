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
                hdrcgi = {
                    cgi_from_hdr = {
                        [cgi_1] = cgi_hdr_1;
                        [cgi_2] = cgi_hdr_2;
                    }; -- cgi_from_hdr
                    hdr_from_cgi = {
                        [hdr_1] = hdr_cgi_1;
                        [hdr_2] = hdr_cgi_2;
                    }; -- hdr_from_cgi
                    body_scan_limit = body_scan_limit;
                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "0.3s"; backend_timeout = "5s";
                        resolve_timeout = "0.3s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; -- hdrcgi
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

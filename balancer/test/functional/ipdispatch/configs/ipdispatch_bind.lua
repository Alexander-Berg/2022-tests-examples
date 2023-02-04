instance = {
    addrs = {
        { ip = "localhost"; port = port; };
        { ip = "*"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    thread_mode = thread_mode; set_no_file = false;

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        local_p = {
            ip = "localhost";
            port = listen_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "local";
                }; -- errordocument
            }; -- http
        }; -- local_p
        external_p = {
            ip = "*";
            port = listen_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "external";
                }; -- errordocument
            }; -- http
        }; -- external_p
    }; -- ipdispatch
}; -- instance

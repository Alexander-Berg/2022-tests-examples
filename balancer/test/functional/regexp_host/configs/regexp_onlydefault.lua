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
                regexp_host = {
                    default = {
                        errordocument = {
                            status = 200;
                            content = "default";
                        }; -- errordocument
                    }; -- regexp_host/yandex.ru
                }; -- regexp_path
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

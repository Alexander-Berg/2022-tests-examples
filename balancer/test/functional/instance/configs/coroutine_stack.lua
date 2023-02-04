instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; }
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; }
    };

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
	        errordocument = {
                    status = 200;
                    content = "OK";
                }; -- errordocument
            }; -- http
        }; -- test
    }; --ipdispatch
}; -- instance

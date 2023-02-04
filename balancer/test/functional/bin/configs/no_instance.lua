instance2 = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            http = {
                errordocument = {
                    status = 200;
                    content = "OK";
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

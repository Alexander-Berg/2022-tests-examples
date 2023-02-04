instance = {
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
                proxy = {};
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

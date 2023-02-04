instance = {
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
                brand_new_errordocument = {
                    status = 200;
                    content = "OK";
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

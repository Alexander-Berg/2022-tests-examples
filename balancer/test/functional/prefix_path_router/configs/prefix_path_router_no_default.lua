instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
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
                prefix_path_router = {
                    case_insensitive = case_insensitive;
                    ac = {
                        route = "/ac";
                        errordocument = {
                            status = 200;
                            content = "ac";
                        };
                    };
                    acc = {
                        route = "/acc/";
                        errordocument = {
                            status = 200;
                            content = "acc";
                        };
                    };
                    ac_dc = {
                        route = "/ac/dc";
                        errordocument = {
                            status = 200;
                            content = "ac/dc";
                        };
                    };
                };
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

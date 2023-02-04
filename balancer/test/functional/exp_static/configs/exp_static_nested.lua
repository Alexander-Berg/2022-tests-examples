instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "127.0.0.1"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;

            http = {
                maxlen = 65536;
                maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin

        test = {
            exp_static = {
                exp_id = exp_id;
                cont_id = cont_id;
                salt = salt;
                slots_count = slots_count;
                rate_file = rate_file;

                http = {
                    maxlen = 65536;
                    maxreq = 65536;

                    exp_static = {
                        exp_id = exp_id_nested;
                        cont_id = cont_id_nested;
                        salt = salt;
                        slots_count = slots_count;
                        rate_file = rate_file;

                        headers = {
                            create_func = {
                                ["Y-ExpStatic-Test"] = 'exp_static';
                            }; -- create_func

                            proxy = {
                                host = "localhost"; port = backend_port;
                                backend_timeout = "5s";
                                connect_timeout = "1s"; resolve_timeout = "1s";
                            }; -- proxy
                        } -- headers
                    }; -- exp_static

                }; -- http
            }; -- exp_static
        }; -- test
    }; -- ipdispatch
}; -- instance

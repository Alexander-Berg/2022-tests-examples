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
                    delete = 'namesake';

                    create = {
                        ["namesake"] = 'strong';
                    }; -- create

                    create_weak = {
                        ["namesake"] = 'weak';
                    }; -- create_weak

                    proxy = {
                        host = "localhost";
                        port = backend_port;
                        backend_timeout = "5s";
                        connect_timeout = "1s";
                        resolve_timeout = "1s";
                    }; -- proxy
                }; -- headers
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

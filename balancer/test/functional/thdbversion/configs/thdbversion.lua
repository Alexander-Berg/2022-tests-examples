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
                thdb_version = {
                    file_name = file_name;
                    file_read_timeout = file_read_timeout;
                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "5s"; backend_timeout = "5s";
                        resolve_timeout = "1s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; -- thdb_version
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

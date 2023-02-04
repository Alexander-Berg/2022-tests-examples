instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
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

                accesslog = {
                    log = accesslog;
                    static = {
                        expires = expires;
                        etag_inode = etag_inode;
                        file = static_file;
                    }; -- static
                }; -- accesslog
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

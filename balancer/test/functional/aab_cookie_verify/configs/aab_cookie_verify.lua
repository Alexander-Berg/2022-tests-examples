if port == nil then port = 8080; end
if admin_port == nil then admin_port = 8081; end
if aes_key_path == nil then aes_key_path = "key.priv"; end

instance = {
    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    };

    workers = workers;

    thread_mode = thread_mode; set_no_file = false;

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
            http = {
                maxlen = 65536; maxreq = 65536;
                aab_cookie_verify = {
                    events = {
                        stats = "report";
                    };
                    aes_key_path = aes_key_path;
                    disable_antiadblock_file = disable_antiadblock_file;
                    cookie = cookie;
                    cookie_lifetime = cookie_lifetime;
                    ip_header = ip_header;
                    default = {
                        errordocument = {
                            status = 200;
                            content = "default";
                        };
                    };
                    antiadblock = {
                        errordocument = {
                            status = 200;
                            content = "antiadblock";
                        };
                    };
                };
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

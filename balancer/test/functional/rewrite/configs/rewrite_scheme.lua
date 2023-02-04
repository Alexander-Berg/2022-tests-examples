port = port or 8081;
ssl_port = ssl_port or 7999;

cert_dir = cert_dir or "./certs/";

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
        { ip = "localhost"; port = ssl_port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    log = log;

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
            shared = {
                uuid = "http";
                errorlog = {
                    log = errorlog;
                    log_level = "DEBUG";
                    http = {
                        maxlen = 65536; maxreq = 65536;
                        headers = {
                            create = {
                                cardigans = "erase and rewind"
                            };
                            rewrite = {
                                actions = {
                                    {
                                        header_name = "cardigans";
                                        rewrite = "%{scheme}";
                                        regexp = ".*";
                                    };
                                }; -- rewrite/actions
                                errordocument = {
                                    status = 200;
                                    content = "ok\n";
                                    remain_headers = "cardigans";
                                }; -- errordocument
                            }; -- rewrite
                        }; -- headers
                    }; -- http
                }; -- errorlog
            }; -- shared
        }; -- test
        default = {
            ssl_sni = {
                contexts = {
                    default = {
                        ciphers = "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                        cert = cert_dir .. "/default.crt"; priv = cert_dir .. "/default.key";
                    };
                };
                shared = {
                    uuid = "http";
                }; -- shared
            }; -- ssl_sni
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance

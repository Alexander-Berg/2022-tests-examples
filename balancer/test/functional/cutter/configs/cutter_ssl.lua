ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
backend_timeout = backend_timeout or "10s";

function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; disabled = 0; }
        };
    };

    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {}
            }; -- http
        }; -- admin
        test = {
            errorlog = {
                log = errorlog;
                ssl_sni = {
                    contexts = {
                        default = {
                            cert = cert; priv = priv; ca = ca;
                            ciphers = ciphers;
                        }; -- default
                    }; -- contexts
                    http = {
                        maxreq = 64 * 1024; maxlen = 64 * 1024;
                        accesslog = {
                            log = accesslog;
                            report = {
                                uuid = "total";
                                cutter = {
                                    bytes = cutter_bytes;
                                    timeout = cutter_timeout;
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "1s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        keepalive_count = 1;
                                    }; -- proxy
                                }; -- cutter
                            }; -- report
                        }; -- accesslog
                    }; -- http
                }; -- ssl_sni
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
function file_path(file_name)
    return certs_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");

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
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        test = {
            ssl_sni = {
                contexts = {
                    default = {
                        ciphers = ciphers;
                        cert = cert; priv = priv;
                    };
                };
                http2_alpn_freq = 1;
                http2 = {
                    http = {
                        maxreq = 64 * 1024; maxlen = 64 * 1024;
                        accesslog = {
                            log = accesslog;
                            additional_ip_header = additional_ip_header;
                            additional_port_header = additional_port_header;
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "0.3s"; backend_timeout = "10s";
                                resolve_timeout = "1s";
                            }; -- proxy
                        }; -- accesslog
                    }; -- http
                }; -- http2
            }; -- ssl_sni
        }; -- ipdispatch/test
    }; -- ipdispatch
}; -- instance

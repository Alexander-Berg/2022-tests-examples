ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs
    workers = workers;
    log = log;
    maxconn = 10000;

    ipdispatch = {
        localhost = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {
                };
            };
        }; -- localhost
        test = {
            ip = "localhost";
            port = port;
            ssl_sni = {
                contexts = {
                    default = {
                        cert = cert; priv = priv; ca = ca;
                        ciphers = ciphers;
                    }; -- default
                }; -- contexts
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "1s"; backend_timeout = "60s";
                        resolve_timeout = "1s";
                    }; -- proxy
                }; -- http
            }; -- ssl_sni
        }; -- test
    }; -- ipdispatch
}; -- instance

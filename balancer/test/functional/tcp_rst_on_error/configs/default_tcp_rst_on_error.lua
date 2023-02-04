backend_timeout = backend_timeout or "10s";

if default_tcp_rst_on_error ~= nil then
    if default_tcp_rst_on_error == "false" or default_tcp_rst_on_error == "0" then
        default_tcp_rst_on_error = false;
    else
        default_tcp_rst_on_error = true;
    end
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    log = instance_log;
    default_tcp_rst_on_error = default_tcp_rst_on_error;

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
                log_level = "DEBUG";
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    accesslog = {
                        log = accesslog;
                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = connect_timeout or "500ms";
                            backend_timeout = backend_timeout;
                            resolve_timeout = "1s";
                        }; -- proxy
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

if use_ssl ~= nil and (use_ssl ~= "false" and use_ssl ~= "0")  then
    ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

    function file_path(file_name)
        return cert_dir .. "/" .. file_name
    end

    cert = cert or file_path("default.crt");
    priv = priv or file_path("default.key");

    instance.ipdispatch.test.http = nil;
    instance.ipdispatch.test.ssl_sni = {
        force_ssl = false;
        contexts = {
            default = {
                cert = cert;
                priv = priv;
                ciphers = ciphers;
            };
        };
        http = {
            maxreq = 64 * 1024; maxlen = 64 * 1024;
            accesslog = {
                log = accesslog;
                proxy = {
                    host = "localhost"; port = backend_port;
                    connect_timeout = connect_timeout or "500ms";
                    backend_timeout = backend_timeout;
                    resolve_timeout = "1s";
                }; -- proxy
            }; -- accesslog
        }; -- http
    };
end

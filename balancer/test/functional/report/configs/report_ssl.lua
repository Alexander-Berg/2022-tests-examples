ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

if ranges == nil then ranges = "10s"; end
if backend_timeout == nil then backend_timeout = "5s"; end
if keepalive == nil then keepalive = 1; end

function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");
ocsp = ocsp or file_path("default_ocsp.0.der");
ticket = ticket or file_path("default_ticket.0.key");

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                ssl_sni = {
                    contexts = {
                        default = {
                            cert = cert; priv = priv; ca = ca;
                            ocsp = ocsp; ciphers = ciphers;
                            ticket_keys_list = {
                                {
                                    keyfile = ticket;
                                    priority = 1;
                                },
                            }; -- ticket_keys_list
                        }; -- default
                    }; -- contexts
                    force_ssl = false;
                    http = {
                        maxlen = 65536; maxreq = 65536;
                        keepalive = keepalive;
                        accesslog = {
                            log = accesslog;
                            report = {
                                uuid = "default";
                                ranges = ranges;
                                disable_robotness = disable_robotness;
                                disable_sslness = disable_sslness;
                                events = {
                                    stats = "report";
                                }; -- events
                                proxy = {
                                    host = "localhost"; port = backend_port;
                                    connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                    resolve_timeout = "1s";
                                    fail_on_5xx = 0;
                                }; -- proxy
                            }; -- report
                        }; -- accesslog
                    }; -- http
                }; -- ssl_sni
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

function file_path(file_name)
    return certs_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");
ocsp = ocsp or file_path("default_ocsp.0.der");
ticket = ticket or file_path("default_ticket.0.key");
if backend_timeout == nil then
    backend_timeout = "5s";
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; disabled = 0; }
        };
    };

    workers = workers;

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
            errorlog = {
                log = errorlog;
                ssl_sni = {
                    force_ssl = force_ssl;
                    http2_alpn_freq = 1;
                    contexts = {
                        default = {
                            cert = cert; priv = priv; ca = ca;
                            ocsp = ocsp; ciphers = ciphers;
                            timeout = timeout;
                            log = log;
                            ticket_keys_list = {
                                {
                                    keyfile = ticket;
                                    priority = 1;
                                },
                            }; -- ticket_keys_list
                            events = {
                                reload_ocsp_response = "default_reload_ocsp";
                                reload_ticket_keys = "default_reload_tickets";
                            }; -- events
                        }; -- default
                    }; -- contexts
                    http2 = {
                        http = {
                            maxlen = 65536; maxreq = 65536;
                            accesslog = {
                                log = accesslog;
                                report = {
                                    uuid = "total";
                                    antirobot = {
                                        checker = {
                                            report = {
                                                uuid = "antirobot";
                                                proxy = {
                                                    host = 'localhost'; port = antirobot_port;
                                                    backend_timeout = antirobot_timeout;
                                                    connect_timeout = "1s"; resolve_timeout = "1s";
                                                    fail_on_5xx = false;
                                                }; -- proxy
                                            }; -- report
                                        }; -- checker
                                        module = {
                                            proxy = {
                                                host = 'localhost'; port = backend_port;
                                                backend_timeout = backend_timeout;
                                                connect_timeout = "1s"; resolve_timeout = "1s";
                                                fail_on_5xx = false;
                                            }; -- proxy
                                        }; -- module
                                    }; -- antirobot
                                }; -- report
                            }; -- accesslog
                        }; -- http
                    }; -- http2
                }; -- ssl_sni
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance

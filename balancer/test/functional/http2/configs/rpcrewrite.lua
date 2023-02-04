ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

function file_path(file_name)
    return certs_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");
ocsp = ocsp or file_path("default_ocsp.0.der");
ticket = ticket or file_path("default_ticket.0.key");

timeout = timeout or "5s";
url = url or "/proxy/";
host = host or "localhost";
dry_run = dry_run or 0;
if keepalive_count == nil then keepalive_count = 1; end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
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
                admin = {};
            };
        };
        test = {
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
                        maxreq = 64 * 1024; maxlen = 64 * 1024;
                        accesslog = {
                            log = accesslog;
                            report = {
                                uuid = "total";
                                rpcrewrite = {
                                    url = url;
                                    host = host;
                                    dry_run = dry_run;
                                    rpc_success_header = rpc_success_header;
                                    file_switch = file_switch;
                                    rpc = {
                                        report = {
                                            uuid = "rpc";
                                            proxy = {
                                                keepalive_count = 1;
                                                host = "localhost"; port = rpc_port;
                                                connect_timeout = "0.3s"; backend_timeout = rpc_timeout;
                                                resolve_timeout = "1s";
                                                fail_on_5xx = 0;
                                            }; -- proxy
                                        }; -- report
                                    }; -- rpc
                                    proxy = {
                                        keepalive_count = keepalive_count;
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- rpcrewrite
                            }; -- report
                        }; -- accesslog
                    }; -- http
                }; -- http2
            }; -- ssl_sni
        }; -- test
    }; -- ipdispatch
}; -- instance

ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

port = port or 8085;
admin_port = admin_port or 8086;
timeout = timeout or "3s";

function file_path(file_name)
    return cert_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");
ocsp = ocsp or file_path("default_ocsp.0.der");
ticket = ticket or file_path("default_ticket.0.key");

content = content or "Hello";

debug_log_enabled = os.getenv("Y_BALANCER_TESTS_DEBUG_LOG") and 1 or 0

instance = {
    thread_mode = thread_mode; set_no_file = false;
    workers = workers;
    addrs = {
        { ip = "::1"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB: "" ]]
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };
    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "::1"; port = port;
            exp_static = {
                exp_id = "2000";
                cont_id = "1000";
                rate_file = exp_rate_file;
                slots_count = 100;

                ssl_sni = {
                    force_ssl = force_ssl;
                    http2_alpn_file = http2_alpn_file;
                    http2_alpn_freq = http2_alpn_freq;
                    http2_alpn_rand_mode_file = http2_alpn_rand_mode_file;
                    http2_alpn_rand_mode = http2_alpn_rand_mode;
                    http2_alpn_exp_id = http2_alpn_exp_id;
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
                        rfc_max_concurrent_streams = max_concurrent_streams;
                        debug_log_enabled = debug_log_enabled;

                        http = {
                            maxreq = 64 * 1024; maxlen = 64 * 1024;

                            response_headers = {
                                create_func = {
                                    ["Y-ExpStatic-Test"] = 'exp_static';
                                }; -- create_func

                                errordocument = {
                                    status = 200;
                                    content = content;
                                }; -- errordocument
                            }; -- headers_response
                        }; -- http
                    }; -- http2
                }; -- ssl_sni
            }; -- exp_static
        }; -- remote
    }; -- ipdispatch
}; -- instance

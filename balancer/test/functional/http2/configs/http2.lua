ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

port = port or 8085;
admin_port = admin_port or 8086;
timeout = timeout or "3s";

function file_path(file_name)
    return certs_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");
ca = ca or file_path("root_ca.crt");
ocsp = ocsp or file_path("default_ocsp.0.der");
ticket = ticket or file_path("default_ticket.0.key");
debug_log_enabled = 1;

instance = {
    thread_mode = thread_mode; set_no_file = false;
    _coro_fail_on_error = 1;
    buffer = 0;
    maxconn = maxconn;
    workers = workers;
    events = {
        stats = "report";
    }; -- events
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB: "" ]]
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB: "" ]]
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; disabled = 0; }
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
            report = {
                ranges = "0,100s";
                uuid = "service_total";
                matcher_map = {
                    h2 = { match_proto = { proto = 'http2' }};
                    h1 = { match_proto = { proto = 'http1x' }};
                };
                just_storage = true; disable_robotness = true; disable_sslness = true;
                events = {
                    stats = "report";
                }; -- events
                errorlog = {
                    log_level = "ERROR";
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
                            allow_http2_without_ssl = allow_http2_without_ssl or false;
                            goaway_debug_data_enabled = 1;
                            rfc_max_concurrent_streams = max_concurrent_streams;
                            rfc_header_table_size = header_table_size;
                            streams_closed_max = streams_closed_max;
                            edge_prio_fix_enabled = edge_prio_fix_enabled;
                            streams_prio_queue_type = streams_prio_queue_type;
                            debug_log_enabled = debug_log_enabled;
                            debug_log_freq = debug_log_freq or 1.0;
                            refused_stream_file_switch = refused_stream_file_switch;
                            rfc_max_header_list_size = header_list_size;
                            rfc_header_table_size = header_table_size;
                            rfc_max_frame_size = max_frame_size;
                            rfc_initial_window_size = initial_window_size;
                            events = {
                                stats = "report";
                            }; -- events
                            http = {
                                stats_attr = "balancer";
                                events = {
                                    stats = "report";
                                }; -- events
                                maxreq = max_req; maxlen = max_req;
                                accesslog = {
                                    report = {
                                        ranges = "0,100s";
                                        refers = "service_total";
                                        just_storage = false;
                                        disable_robotness = true;
                                        disable_sslness = true;
                                        proxy = {
                                            host = "localhost";
                                            port = backend_port;
                                            connect_timeout = backend_timeout;
                                            backend_timeout = backend_timeout;
                                            resolve_timeout = "1s";
                                            fail_on_5xx = 1;
                                            check_http_client_read_error = check_http_client_read_error;
                                        }; -- proxy
                                    }; -- report
                                }; -- accesslog
                            }; -- http
                        }; -- http2
                    }; -- ssl_sni
                }; -- errorlog
            };
        }; -- remote
    }; -- ipdispatch
}; -- instance

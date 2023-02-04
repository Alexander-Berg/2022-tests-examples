instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
        { ip = "localhost"; port = http2_port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

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
            ip = "localhost"; port = port;
            shared = {
                uuid = "http";
                http = {
                    maxlen = 64 * 1024; maxreq = 64 * 1024;
                    headers = {
                        create_func = {
                            Proto = "proto";
                            Scheme = "scheme";
                            TcpInfo = "tcp_info";
                            Cn = "ssl_client_cert_cn";
                            Subj = "ssl_client_cert_subject";
                            Verify = "ssl_client_cert_verify_result";
                            Serial = "ssl_client_cert_serial_number";
                            Handshake = "ssl_handshake_info";

                            RealIp = "realip";
                            LocalIp = "localip";
                            RealPort = "realport";
                            StartTime = "starttime";
                        };
                        response_headers = {
                            create_func = {
                                Proto = "proto";
                                Scheme = "scheme";
                                TcpInfo = "tcp_info";
                                Cn = "ssl_client_cert_cn";
                                Subj = "ssl_client_cert_subject";
                                Verify = "ssl_client_cert_verify_result";
                                Serial = "ssl_client_cert_serial_number";
                                Handshake = "ssl_handshake_info";

                                RealIp = "realip";
                                LocalIp = "localip";
                                RealPort = "realport";
                                StartTime = "starttime";
                            };
                            proxy = {
                                host = "localhost"; port = backend_port;
                                connect_timeout = "1s"; resolve_timeout = "1s";
                                backend_timeout = "1s";
                            }; -- proxy
                        }; -- response_headers
                    }; -- headers
                }; -- http
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
                http2_alpn_freq = 1;
                http2 = {
                    shared = {
                        uuid = "http";
                    }; -- shared
                }; -- http2
            }; -- ssl_sni
        }; -- ipdispatch/default
    }; -- ipdispatch
}; -- instance


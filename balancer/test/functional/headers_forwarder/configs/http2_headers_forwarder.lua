ciphers = ciphers or "kEECDH:kRSA+AES128:kRSA:+3DES:RC4:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";

function file_path(file_name)
    return certs_dir .. "/" .. file_name
end

cert = cert or file_path("default.crt");
priv = priv or file_path("default.key");

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
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
            ip = "localhost";
            port = port;

            ssl_sni = {
                force_ssl = force_ssl;
                http2_alpn_freq = 1;
                contexts = {
                    default = {
                        cert = cert; priv = priv;
                        ciphers = ciphers;
                    }; -- default
                }; -- contexts

                http2 = {
                    http = {
                        maxlen = 65536; maxreq = 65536;
                        headers_forwarder = {
                            actions = {
                                {
                                    request_header = request_header;
                                    response_header = response_header;
                                    erase_from_request = erase_from_request;
                                    erase_from_response = erase_from_response;
                                    weak = weak;
                                };
                            };

                            proxy = {
                                host = "localhost"; port = backend_port;
                                backend_timeout = "5s";
                                connect_timeout = "1s"; resolve_timeout = "1s";
                            }; -- proxy
                        }; -- headers_forwarder
                    }; -- http
                }; -- http2
            }; -- ssl_sni
        }; -- test
    }; -- ipdispatch

}; -- instance

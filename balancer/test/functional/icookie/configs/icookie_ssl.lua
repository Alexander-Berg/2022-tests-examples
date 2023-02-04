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
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
    ipdispatch = {
        admin = {
            ip = "localhost"; port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- ipdispatch/admin
        default = {
            ssl_sni = {
                contexts = {
                    default = {
                        cert = cert; priv = priv; ca = ca;
                        ciphers = ciphers;
                    }; -- default
                }; -- contexts
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    icookie = {
                        file_switch = file_switch;

                        trust_parent = trust_parent;
                        trust_children = trust_children;

                        keys_file = keys_file;
                        use_default_keys = use_default_keys;

                        enable_set_cookie = enable_set_cookie;
                        enable_decrypting = enable_decrypting;

                        decrypted_uid_header = decrypted_uid_header;
                        error_header = error_header;
                        info_header = info_header;
                        encrypted_header = encrypted_header;

                        take_randomuid_from = take_randomuid_from;

                        domains = domains;

                        flag_secure = flag_secure;
                        scheme_bitmask = scheme_bitmask;

                        force_equal_to_yandexuid = force_equal_to_yandexuid;
                        force_generate_from_searchapp_uuid = force_generate_from_searchapp_uuid;
                        force_generate_from_yandex_browser_uuid = force_generate_from_yandex_browser_uuid;
                        force_generate_from_transport = force_generate_from_transport;

                        max_transport_age = max_transport_age;

                        exp_type = exp_type;
                        exp_A_testid = exp_A_testid;
                        exp_B_testid = exp_B_testid;
                        exp_salt = exp_salt;
                        exp_A_slots = exp_A_slots;
                        exp_B_slots = exp_B_slots;

                        enable_parse_searchapp_uuid = enable_parse_searchapp_uuid;
                        enable_guess_searchapp = enable_guess_searchapp;

                        proxy = {
                            host = "localhost"; port = backend_port;
                            connect_timeout = "5s"; backend_timeout = "5s";
                            resolve_timeout = "1s";
                        }; -- proxy
                    }; -- icookie
                }; -- http
            }; -- ssl_sni
        }; -- ipdispatch/default
    }; -- ipdispatch
};

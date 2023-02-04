default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ciphers_ecdsa = "ECDHE-ECDSA-AES128-GCM-SHA256:kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function gen_proxy_backends(backends, proxy_options)
  local result = {}

  for index, backend in pairs(backends) do
    local proxy = {
      host = backend[1] or backend['host'];
      port = backend[2] or backend['port'];
      cached_ip = backend[4] or backend['cached_ip'];
    };

    if proxy_options ~= nil then
      for optname, optvalue in pairs(proxy_options) do
        proxy[optname] = optvalue
      end
    end

    result[index] = {
      weight = backend[3] or backend['weight'];
      proxy = proxy;
    };
  end

  if next(result) == nil then
    error("backends list is empty")
  end

  return result
end


function get_ca_cert_path(name, default_ca_cert_dir)
  default_ca_cert_dir = default_ca_cert_dir or "/dev/shm/balancer/priv"
  return (ca_cert_dir or default_ca_cert_dir) .. "/" .. name;
end


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
end


function get_ip_by_iproute(addr_family)
  if disable_external then
    if addr_family == "v4" then
      return "127.1.1.1"
    elseif addr_family == "v6" then
      return "127.2.2.2"
    else
      error("invalid parameter")
    end
  end

  local ipcmd
  if addr_family == "v4" then
    ipcmd = "ip route get 77.88.8.8 2>/dev/null| awk '/src/ {print $NF}'"
  elseif addr_family == "v6" then
    ipcmd = "ip route get 2a00:1450:4010:c05::65 2>/dev/null | grep -oE '2a02[:0-9a-f]+' | tail -1"
  else
    error("invalid parameter")
  end
  local handler = io.popen(ipcmd)
  local ip = handler:read("*l")
  handler:close()
  if ip == nil or ip == "" or ip == "proto" then
    return "127.0.0.2"
  end
  return ip
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
end


function get_private_cert_path(name, default_private_cert_dir)
  default_private_cert_dir = default_private_cert_dir or "/dev/shm/balancer/priv"
  return (private_cert_dir or default_private_cert_dir) .. "/" .. name;
end


function get_public_cert_path(name, default_public_cert_dir)
  default_public_cert_dir = default_public_cert_dir or "/dev/shm/balancer"
  return (public_cert_dir or default_public_cert_dir) .. "/" .. name;
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


instance = {
  workers = 1;
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  storage_gc_required = true;
  private_address = "127.0.0.10";
  shutdown_close_using_bpf = true;
  default_tcp_rst_on_error = false;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 16100, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 16100;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 16100;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 16100;
      ip = "127.0.0.4";
    };
    {
      port = 16100;
      ip = "127.0.0.5";
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::107e";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 16100;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16100;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8:0:3400::107e";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 16101;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16101;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 16102;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16102;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 8080;
      ip = "2a02:6b8:0:3400::107e";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 9090;
      ip = "2a02:6b8:0:3400::107e";
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        16100;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {};
      }; -- http
    }; -- admin
    ["local"] = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        16100;
      }; -- ports
      report = {
        ranges = get_str_var("default_ranges");
        input_size_ranges = "1,5,3,1000";
        output_size_ranges = "50,500,3000";
        just_storage = false;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        refers = "service_total";
        shared = {
          uuid = "2191510784648030126";
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            errordocument = {
              status = 200;
              force_conn_close = false;
            }; -- errordocument
          }; -- http
        }; -- shared
      }; -- report
    }; -- ["local"]
    stats_storage_2 = {
      ips = {
        "127.0.0.5";
      }; -- ips
      ports = {
        16100;
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        shared = {
          uuid = "2191510784648030126";
        }; -- shared
      }; -- report
    }; -- stats_storage_2
    remote_ips_80 = {
      ips = {
        "2a02:6b8:0:3400::107e";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "6939502992790477594";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 16100, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 16100, "/place/db/www/logs");
              report = {
                uuid = "http";
                refers = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                regexp = {
                  ping = {
                    priority = 3;
                    match_fsm = {
                      URI = "/ping";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "6133217153220833481";
                      stats_eater = {
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/slb_check.weights";
                            to_upstream = {
                              weight = 1.000;
                              balancer2 = {
                                unique_policy = {};
                                attempts = 10;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                    { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "10ms";
                                    backend_timeout = "1500ms";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- to_upstream
                            switch_off = {
                              weight = -1.000;
                              errordocument = {
                                status = 502;
                                force_conn_close = false;
                              }; -- errordocument
                            }; -- switch_off
                          }; -- rr
                        }; -- balancer2
                      }; -- stats_eater
                    }; -- shared
                  }; -- ping
                  gobabygo = {
                    priority = 2;
                    match_fsm = {
                      URI = "/gobabygo(/.*)?";
                      case_insensitive = false;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "1545307416464520382";
                      rewrite = {
                        actions = {
                          {
                            split = "url";
                            global = false;
                            literal = false;
                            rewrite = "/%1";
                            case_insensitive = false;
                            regexp = "/gobabygo/?(.*)";
                          };
                        }; -- actions
                        headers = {
                          create_func = {
                            ["X-Real-IP"] = "realip";
                          }; -- create_func
                          create_func_weak = {
                            ["X-Forwarded-For"] = "realip";
                            ["X-Req-Id"] = "reqid";
                            ["X-Scheme"] = "scheme";
                            ["X-Source-Port"] = "realport";
                          }; -- create_func_weak
                          cookie_hasher = {
                            cookie = "yandexuid";
                            file_switch = "./controls/disable_cookie_hasher";
                            stats_eater = {
                              balancer2 = {
                                by_hash_policy = {
                                  unique_policy = {};
                                }; -- by_hash_policy
                                attempts = 3;
                                active = {
                                  delay = "1s";
                                  request = "GET /ping HTTP/1.1\nHost: test.yandex.ru\n\n";
                                  quorum = 66.000;
                                  hysteresis = 0.000;
                                  {
                                    weight = 100.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      retry_non_idempotent = false;
                                      use_on_error_for_non_idempotent = true;
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "20s";
                                          feedback_time = "10s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.050;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                          { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "999ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 3;
                                          need_resolve = true;
                                          keepalive_timeout = "10s";
                                          status_code_blacklist = {
                                            "4xx";
                                            "5xx";
                                          }; -- status_code_blacklist
                                          status_code_blacklist_exceptions = {
                                            "403";
                                          }; -- status_code_blacklist_exceptions
                                        }))
                                      }; -- weighted2
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  };
                                  {
                                    weight = 100.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      retry_non_idempotent = false;
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "20s";
                                          feedback_time = "10s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.050;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                          { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "999ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 3;
                                          need_resolve = true;
                                          keepalive_timeout = "10s";
                                          status_code_blacklist = {
                                            "4xx";
                                            "5xx";
                                          }; -- status_code_blacklist
                                          status_code_blacklist_exceptions = {
                                            "403";
                                          }; -- status_code_blacklist_exceptions
                                        }))
                                      }; -- weighted2
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  };
                                }; -- active
                              }; -- balancer2
                            }; -- stats_eater
                          }; -- cookie_hasher
                        }; -- headers
                      }; -- rewrite
                    }; -- shared
                  }; -- gobabygo
                  atomsearch = {
                    priority = 1;
                    match_fsm = {
                      URI = "/((m)?search/)?atomsearch(/.*)?";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "4305323049064945286";
                      rewrite = {
                        actions = {
                          {
                            global = false;
                            literal = false;
                            rewrite = "/%2";
                            case_insensitive = false;
                            regexp = "/(m)?search/(.*)";
                          };
                        }; -- actions
                        stats_eater = {
                          request_replier = {
                            sink = {
                              balancer2 = {
                                unique_policy = {};
                                attempts = 5;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:20"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "150ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- sink
                            enable_failed_requests_replication = false;
                            rate = 0.000;
                            rate_file = "./controls/request_replier_atomsearch.ratefile";
                            balancer2 = {
                              unique_policy = {};
                              attempts = 2;
                              weighted2 = {
                                slow_reply_time = "1s";
                                correction_params = {
                                  max_weight = 5.000;
                                  min_weight = 0.050;
                                  history_time = "100s";
                                  feedback_time = "300s";
                                  plus_diff_per_sec = 0.050;
                                  minus_diff_per_sec = 0.100;
                                }; -- correction_params
                                unpack(gen_proxy_backends({
                                  { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                  { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "150ms";
                                  backend_timeout = "5s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                              on_error = {
                                errordocument = {
                                  status = 504;
                                  force_conn_close = false;
                                }; -- errordocument
                              }; -- on_error
                            }; -- balancer2
                          }; -- request_replier
                        }; -- stats_eater
                      }; -- rewrite
                    }; -- shared
                  }; -- atomsearch
                }; -- regexp
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- remote_ips_80
    local_ips_16100 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        16100;
      }; -- ports
      shared = {
        uuid = "6939502992790477594";
      }; -- shared
    }; -- local_ips_16100
    remote_ips_443 = {
      ips = {
        "2a02:6b8:0:3400::107e";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "8510374336571068456";
        errorlog = {
          log_level = "ERROR";
          log = "/place/db/www/logs/current-error_log-balancer-16101";
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            contexts = {
              default = {
                priority = 1;
                timeout = "100800s";
                disable_sslv3 = true;
                ocsp_file_switch = "./controls/disable_ocsp";
                ciphers = get_str_var("default_ciphers_ecdsa");
                ocsp = "./ocsp/allCAs-rcss-ext.search.yandex.net.der";
                log = "/place/db/www/logs/current-ssl_sni-balancer-16101";
                priv = "/dev/shm/balancer/priv/rcss-ext.search.yandex.net.pem";
                cert = "/dev/shm/balancer/allCAs-rcss-ext.search.yandex.net.pem";
                secondary = {
                  priv = "/dev/shm/balancer/priv/rcss-ext.search.yandex.net_secondary.pem";
                  cert = "/dev/shm/balancer/allCAs-rcss-ext.search.yandex.net_secondary.pem";
                }; -- secondary
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = "/dev/shm/balancer/priv/1st.rcss-ext.search.yandex.net.key";
                  };
                  {
                    priority = 2;
                    keyfile = "/dev/shm/balancer/priv/2nd.rcss-ext.search.yandex.net.key";
                  };
                  {
                    priority = 1;
                    keyfile = "/dev/shm/balancer/priv/3rd.rcss-ext.search.yandex.net.key";
                  };
                }; -- ticket_keys_list
              }; -- default
            }; -- contexts
            http = {
              maxlen = 65536;
              maxreq = 65536;
              keepalive = true;
              no_keepalive_file = "./controls/keepalive_disabled";
              events = {
                stats = "report";
              }; -- events
              accesslog = {
                log = "/place/db/www/logs/current-access_log-balancer-16101";
                report = {
                  uuid = "https";
                  refers = "service_total";
                  ranges = get_str_var("default_ranges");
                  just_storage = false;
                  disable_robotness = true;
                  disable_sslness = true;
                  events = {
                    stats = "report";
                  }; -- events
                  headers = {
                    create_func = {
                      ["X-Req-Id"] = "reqid";
                      ["X-SSL-Client-CN"] = "ssl_client_cert_cn";
                      ["X-SSL-Client-Subject"] = "ssl_client_cert_subject";
                      ["X-SSL-Client-Verify"] = "ssl_client_cert_verify_result";
                      ["X-Source-Port-Y"] = "realport";
                      ["X-Start-Time"] = "starttime";
                      ["X-Yandex-HTTPS-Info"] = "ssl_handshake_info";
                      ["X-Yandex-RandomUID"] = "yuid";
                    }; -- create_func
                    create_func_weak = {
                      ["X-Forwarded-For"] = "realip";
                      ["X-Forwarded-For-Y"] = "realip";
                      ["X-Time"] = "time:+2h";
                    }; -- create_func_weak
                    response_headers = {
                      create_weak = {
                        ["X-Content-Type-Options"] = "nosniff";
                        ["X-XSS-Protection"] = "1; mode=block";
                      }; -- create_weak
                      rpcrewrite = {
                        url = "/proxy";
                        dry_run = false;
                        host = "bolver.yandex-team.ru";
                        rpc_success_header = "X-Metabalancer-Answered";
                        file_switch = "./controls/disable_rpcrewrite_module";
                        on_rpc_error = {
                          cache2 = {
                            cache_ttl = "10s";
                            shard_number = 10;
                            ignore_cgi = false;
                            errordocument = {
                              status = 500;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- cache2
                        }; -- on_rpc_error
                        rpc = {
                          balancer2 = {
                            simple_policy = {};
                            attempts = 3;
                            connection_attempts = 1;
                            rr = {
                              unpack(gen_proxy_backends({
                                { "bolver.yandex-team.ru"; 80; 1.000; "2a02:6b8:0:3400::32"; };
                              }, {
                                resolve_timeout = "10ms";
                                connect_timeout = "150ms";
                                backend_timeout = "10s";
                                fail_on_5xx = true;
                                http_backend = true;
                                buffering = false;
                                keepalive_count = 0;
                                need_resolve = true;
                              }))
                            }; -- rr
                          }; -- balancer2
                        }; -- rpc
                        shared = {
                          uuid = "8059592162194920881";
                          regexp = {
                            atomsearch = {
                              priority = 6;
                              match_fsm = {
                                URI = "/((m)?search/)?atomsearch(/.*)?";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              shared = {
                                uuid = "4305323049064945286";
                              }; -- shared
                            }; -- atomsearch
                            captcha = {
                              priority = 5;
                              match_fsm = {
                                URI = "/x?(show|check)?captcha.*";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              webauth = {
                                auth_path = "/check_oauth_token";
                                checker = {
                                  headers = {
                                    create = {
                                      Host = "webauth.yandex-team.ru";
                                    }; -- create
                                    proxy = {
                                      host = "webauth.yandex-team.ru";
                                      port = 443;
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "5s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                      https_settings = {
                                        ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                        ca_file = get_ca_cert_path("allCAs-hamster.yandex.tld.pem", "");
                                        sni_on = true;
                                        verify_depth = 3;
                                        sni_host = "webauth.yandex-team.ru";
                                      }; -- https_settings
                                    }; -- proxy
                                  }; -- headers
                                }; -- checker
                                on_forbidden = {
                                  errordocument = {
                                    status = 403;
                                    force_conn_close = false;
                                    content = "Access forbidden";
                                  }; -- errordocument
                                }; -- on_forbidden
                                unauthorized_set_cookie = "webauth_csrf_token={csrf_token}; Path=/";
                                unauthorized_redirect = "https://oauth.yandex-team.ru/authorize?response_type=code&client_id={app_id}&state={csrf_state}";
                                role = "/webauth-qloud/qloud-ext/education/yashchenko-klein/envs/crowdtest/user";
                                allow_options_passthrough = true;
                                report = {
                                  uuid = "captchasearch";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  h100 = {
                                    cutter = {
                                      bytes = 512;
                                      timeout = "0.1s";
                                      antirobot = {
                                        cut_request = true;
                                        no_cut_request_file = "./controls/no_cut_request_file";
                                        file_switch = "./controls/do.not.use.it";
                                        cut_request_bytes = 512;
                                        checker = {
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              weighted2 = {
                                                slow_reply_time = "1s";
                                                correction_params = {
                                                  max_weight = 5.000;
                                                  min_weight = 0.050;
                                                  history_time = "100s";
                                                  feedback_time = "300s";
                                                  plus_diff_per_sec = 0.050;
                                                  minus_diff_per_sec = 0.100;
                                                }; -- correction_params
                                                unpack(gen_proxy_backends({
                                                  { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                                  { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "10s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- weighted2
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- checker
                                        module = {
                                          errordocument = {
                                            status = 403;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- report
                              }; -- webauth
                            }; -- captcha
                            clck = {
                              priority = 4;
                              match_fsm = {
                                URI = "/clck/(.*)?";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              hasher = {
                                mode = "subnet";
                                take_ip_from = "X-Real-IP";
                                h100 = {
                                  cutter = {
                                    bytes = 512;
                                    timeout = "0.1s";
                                    antirobot = {
                                      cut_request = true;
                                      no_cut_request_file = "./controls/no_cut_request_file";
                                      file_switch = "./controls/disable_antirobot_module";
                                      cut_request_bytes = 4096;
                                      checker = {
                                        report = {
                                          uuid = "antirobot";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              hashing = {
                                                unpack(gen_proxy_backends({
                                                  { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                                  { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "30ms";
                                                  backend_timeout = "100ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                }))
                                              }; -- hashing
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- report
                                      }; -- checker
                                      module = {
                                        rewrite = {
                                          actions = {
                                            {
                                              global = false;
                                              rewrite = "%1";
                                              literal = false;
                                              regexp = "/clck(/.*)";
                                              case_insensitive = false;
                                            };
                                          }; -- actions
                                          click = {
                                            keys = "./data/clickdaemon.keys";
                                            report = {
                                              uuid = "clcksearch";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              stats_eater = {
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 5;
                                                  weighted2 = {
                                                    slow_reply_time = "1s";
                                                    correction_params = {
                                                      max_weight = 5.000;
                                                      min_weight = 0.050;
                                                      history_time = "100s";
                                                      feedback_time = "300s";
                                                      plus_diff_per_sec = 0.050;
                                                      minus_diff_per_sec = 0.100;
                                                    }; -- correction_params
                                                    unpack(gen_proxy_backends({
                                                      { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                                      { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "30ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                      allow_connection_upgrade = true;
                                                      allow_connection_upgrade_without_connection_header = true;
                                                    }))
                                                  }; -- weighted2
                                                }; -- balancer2
                                              }; -- stats_eater
                                            }; -- report
                                          }; -- click
                                        }; -- rewrite
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- clck
                            gobabygo = {
                              priority = 3;
                              match_fsm = {
                                URI = "/gobabygo(/.*)?";
                                case_insensitive = false;
                                surround = false;
                              }; -- match_fsm
                              shared = {
                                uuid = "1545307416464520382";
                              }; -- shared
                            }; -- gobabygo
                            ping = {
                              priority = 2;
                              match_fsm = {
                                URI = "/ping";
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              shared = {
                                uuid = "6133217153220833481";
                              }; -- shared
                            }; -- ping
                            rtfront = {
                              priority = 1;
                              match_fsm = {
                                header = {
                                  name = "X-Golovan-Rt-Request";
                                  value = ".*";
                                }; -- header
                                case_insensitive = true;
                                surround = false;
                              }; -- match_fsm
                              rewrite = {
                                actions = {
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/sigstat";
                                    regexp = "/rt/sigstat";
                                    case_insensitive = false;
                                  };
                                  {
                                    global = false;
                                    literal = false;
                                    rewrite = "/subs";
                                    regexp = "/rt/subs";
                                    case_insensitive = false;
                                  };
                                }; -- actions
                                headers_hasher = {
                                  header_name = "X-Golovan-Rt-Request";
                                  surround = false;
                                  randomize_empty_match = true;
                                  stats_eater = {
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      attempts_file = "./controls/attempts.file";
                                      hashing = {
                                        unpack(gen_proxy_backends({
                                          { "mtn.google.com"; 9090; 2.000; "2a00:1450:4010:c0d::8b"; };
                                          { "mtn.ya.ru"; 8080; 1.000; "2a02:6b8::3"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "999ms";
                                          backend_timeout = "5s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- hashing
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- headers_hasher
                              }; -- rewrite
                            }; -- rtfront
                          }; -- regexp
                        }; -- shared
                      }; -- rpcrewrite
                    }; -- response_headers
                  }; -- headers
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- remote_ips_443
    local_ips_16101 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        16101;
      }; -- ports
      shared = {
        uuid = "8510374336571068456";
      }; -- shared
    }; -- local_ips_16101
    local_ips_16102 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        16102;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 16102, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            ["exp.yandex-team.ru"] = {
              priority = 2;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 16102, "/place/db/www/logs");
              priv = get_private_cert_path("exp.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-exp.yandex-team.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(exp|exp-beta|ab)\\\\.test\\\\.yandex-team\\\\.ru";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.exp.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.exp.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.exp.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["exp.yandex-team.ru"]
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers_ecdsa");
              log = get_log_path("ssl_sni", 16102, "/place/db/www/logs");
              priv = get_private_cert_path("ab.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-ab.yandex-team.ru.pem", "/dev/shm/balancer");
              secondary = {
                priv = get_private_cert_path("ab.yandex-team.ru_secondary.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-ab.yandex-team.ru_secondary.pem", "/dev/shm/balancer");
              }; -- secondary
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.ab.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.ab.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.ab.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              additional_ip_header = "X-Forwarded-For-Y";
              additional_port_header = "X-Source-Port-Y";
              log = get_log_path("access_log", 16102, "/place/db/www/logs");
              report = {
                refers = "service_total,https";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                headers = {
                  create_func = {
                    ["X-Req-Id"] = "reqid";
                    ["X-SSL-Client-CN"] = "ssl_client_cert_cn";
                    ["X-SSL-Client-Subject"] = "ssl_client_cert_subject";
                    ["X-SSL-Client-Verify"] = "ssl_client_cert_verify_result";
                    ["X-Source-Port-Y"] = "realport";
                    ["X-Start-Time"] = "starttime";
                    ["X-Yandex-HTTPS-Info"] = "ssl_handshake_info";
                    ["X-Yandex-RandomUID"] = "yuid";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Forwarded-For-Y"] = "realip";
                    ["X-Time"] = "time:+2h";
                  }; -- create_func_weak
                  response_headers = {
                    create_weak = {
                      ["X-Content-Type-Options"] = "nosniff";
                      ["X-XSS-Protection"] = "1; mode=block";
                    }; -- create_weak
                    rpcrewrite = {
                      url = "/proxy";
                      dry_run = false;
                      host = "bolver.yandex-team.ru";
                      rpc_success_header = "X-Metabalancer-Answered";
                      file_switch = "./controls/disable_rpcrewrite_module";
                      rpc = {
                        report = {
                          uuid = "rpcrewrite-backend";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          stats_eater = {
                            balancer2 = {
                              simple_policy = {};
                              attempts = 3;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "bolver.yandex-team.ru"; 80; 1.000; "2a02:6b8:0:3400::32"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "150ms";
                                  backend_timeout = "10s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- rr
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- rpc
                      shared = {
                        uuid = "8059592162194920881";
                      }; -- shared
                    }; -- rpcrewrite
                  }; -- response_headers
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- local_ips_16102
    remote_ips_8080 = {
      ips = {
        "2a02:6b8:0:3400::107e";
      }; -- ips
      ports = {
        8080;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 8080, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 8080, "/place/db/www/logs");
            report = {
              refers = "service_total,http";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              headers = {
                create = {
                  Location = "1";
                }; -- create
                rewrite = {
                  actions = {
                    {
                      regexp = ".*";
                      global = false;
                      literal = false;
                      case_insensitive = false;
                      header_name = "Location";
                      rewrite = "https://%{host}%{url}";
                    };
                  }; -- actions
                  regexp = {
                    unsafe_methods = {
                      priority = 2;
                      match_fsm = {
                        match = "(DELETE|PATCH|POST|PUT).*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      errordocument = {
                        status = 307;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- unsafe_methods
                    default = {
                      priority = 1;
                      errordocument = {
                        status = 302;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- default
                  }; -- regexp
                }; -- rewrite
              }; -- headers
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- remote_ips_8080
    remote_ips_9090 = {
      ips = {
        "2a02:6b8:0:3400::107e";
      }; -- ips
      ports = {
        9090;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 9090, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 9090, "/place/db/www/logs");
            report = {
              refers = "service_total,http";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              headers = {
                create = {
                  Location = "1";
                }; -- create
                rewrite = {
                  actions = {
                    {
                      regexp = ".*";
                      global = false;
                      literal = false;
                      case_insensitive = false;
                      header_name = "Location";
                      rewrite = "https://%{host}%{url}";
                    };
                  }; -- actions
                  regexp = {
                    unsafe_methods = {
                      priority = 2;
                      match_fsm = {
                        match = "(DELETE|PATCH|POST|PUT).*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      errordocument = {
                        status = 307;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- unsafe_methods
                    default = {
                      priority = 1;
                      errordocument = {
                        status = 301;
                        force_conn_close = false;
                        remain_headers = "Location";
                      }; -- errordocument
                    }; -- default
                  }; -- regexp
                }; -- rewrite
              }; -- headers
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- remote_ips_9090
  }; -- ipdispatch
}
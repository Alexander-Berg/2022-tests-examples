default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


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


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
end


function do_get_its_control_path(filename)
  -- actual get_its_control_path() implementation, can be overridden
  return "./controls/" .. filename
end

function get_its_control_path(filename)
  return do_get_its_control_path(filename)
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
end


function get_port_var(name, offset, default)
  value = get_int_var(name, default)
  if value == nil then
    error("Neither port variable \"" .. name .. "\" nor default port is specified.")
  end
  if value < 0 or value > 65535 then
    error("Variable \"" .. name .. "\" is not a valid port: " .. value)
  end
  if offset ~= nil then
    value = value + offset
  end
  return value
end


function get_str_var(name, default)
  return _G[name] or default
end


instance = {
  workers = 0;
  buffer = 65536;
  maxconn = 1000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  log = "/place/db/www/logs/log.txt";
  events = {
    stats = "report";
  }; -- events
  reset_dns_cache_file = get_its_control_path("reset-dns-cache-file");
  admin_addrs = {
    {
      port = 15010;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15010;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 15010;
      ip = "127.0.0.4";
    };
    {
      ip = "127.0.0.44";
      port = get_port_var("http_port", 0, 80);
    };
    {
      ip = "127.0.0.44";
      port = get_port_var("https_port");
    };
    {
      port = 8888;
      ip = "8.8.8.8";
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
        15010;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = get_its_control_path("no-keepalive-file");
        events = {
          stats = "report";
        }; -- events
        admin = {};
      }; -- http
    }; -- admin
    check = {
      ips = {
        "8.8.8.8";
      }; -- ips
      ports = {
        8888;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = get_its_control_path("balancer_disable_keepalive");
        events = {
          stats = "report";
        }; -- events
        active_check_reply = {
          default_weight = 100;
          use_header = true;
          use_body = true;
          use_dynamic_weight = false;
          weight_file = get_its_control_path("active-check-reply-weight-file");
        }; -- active_check_reply
      }; -- http
    }; -- check
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        15010;
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
        antirobot = {
          cut_request = true;
          no_cut_request_file = get_its_control_path("antirobot-no-cut-request-file");
          file_switch = get_its_control_path("antirobot-file-switch");
          cut_request_bytes = 512;
          checker = {
            errordocument = {
              status = 200;
              force_conn_close = false;
            }; -- errordocument
          }; -- checker
          module = {
            http = {
              maxlen = 65536;
              maxreq = 65536;
              keepalive = true;
              no_keepalive_file = get_its_control_path("balancer_disable_keepalive");
              events = {
                stats = "report";
              }; -- events
              errordocument = {
                status = 200;
                force_conn_close = false;
              }; -- errordocument
            }; -- http
          }; -- module
        }; -- antirobot
      }; -- report
    }; -- stats_storage
    remote_ips_80 = {
      ips = {
        "127.0.0.44";
      }; -- ips
      ports = {
        get_port_var("http_port", 0, 80);
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", get_port_var("http_port", 0, 80), "");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = get_its_control_path("balancer_disable_keepalive");
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", get_port_var("http_port", 0, 80), "");
            report = {
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              threshold = {
                lo_bytes = 524288;
                hi_bytes = 1048576;
                recv_timeout = "1s";
                pass_timeout = "10s";
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
                  shared = {
                    uuid = "2217635822518746117";
                    regexp = {
                      gobabygo = {
                        priority = 2;
                        match_fsm = {
                          URI = "/gobabygo(/.*)?";
                          case_insensitive = false;
                          surround = false;
                        }; -- match_fsm
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
                            antirobot_wrapper = {
                              cut_request = true;
                              no_cut_request_file = get_its_control_path("antirobot-wrapper-no-cut-request-file");
                              cut_request_bytes = 512;
                              icookie = {
                                use_default_keys = true;
                                domains = ".yandex.ru,.yandex.tr";
                                trust_parent = false;
                                trust_children = false;
                                enable_set_cookie = true;
                                enable_decrypting = true;
                                decrypted_uid_header = "X-Yandex-ICookie";
                                error_header = "X-Yandex-ICookie-Error";
                                file_switch = get_its_control_path("icookie-hasher-file-switch");
                                force_equal_to_yandexuid = false;
                                cookie_hasher = {
                                  cookie = "yandexuid";
                                  file_switch = get_its_control_path("cookie-hasher-file-switch");
                                  geobase = {
                                    trusted = false;
                                    geo_host = "laas.yandex.ru";
                                    take_ip_from = "X-Forwarded-For-Y";
                                    laas_answer_header = "X-LaaS-Answered";
                                    file_switch = get_its_control_path("balancer_geolib_switch");
                                    geo_path = "/region?response_format=header&version=1&service=balancer";
                                    geo = {
                                      report = {
                                        uuid = "geobasemodule";
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
                                            attempts = 2;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "laas.yandex.ru"; 80; 1.000; "2a02:6b8::91"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "15ms";
                                                backend_timeout = "20ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 10;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- stats_eater
                                      }; -- report
                                    }; -- geo
                                    rpcrewrite = {
                                      url = "/proxy";
                                      dry_run = false;
                                      host = "bolver.yandex-team.ru";
                                      rpc_success_header = "X-Metabalancer-Answered";
                                      file_switch = get_its_control_path("rpcrewrite-file-switch");
                                      on_rpc_error = {
                                        errordocument = {
                                          status = 500;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_rpc_error
                                      rpc = {
                                        hasher = {
                                          mode = "subnet";
                                          take_ip_from = "X-Forwarded-For-Y";
                                          subnet_v4_mask = 32;
                                          subnet_v6_mask = 128;
                                          balancer2 = {
                                            simple_policy = {};
                                            attempts = 3;
                                            connection_attempts = 1;
                                            rendezvous_hashing = {
                                              weights_file = get_its_control_path("rendezvous-hashing-weights-file");
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
                                            }; -- rendezvous_hashing
                                          }; -- balancer2
                                        }; -- hasher
                                      }; -- rpc
                                      rpcrewrite = {
                                        url = "/proxy";
                                        dry_run = false;
                                        host = "bolver.yandex-team.ru";
                                        rpc_success_header = "X-Metabalancer-Answered";
                                        file_switch = get_its_control_path("balancer_disable_rpcrewrite_module");
                                        on_rpc_error = {
                                          errordocument = {
                                            status = 500;
                                            force_conn_close = false;
                                            content = "Failed to rewrite request using RPC";
                                          }; -- errordocument
                                        }; -- on_rpc_error
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
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          attempts_file = get_its_control_path("between-locations-attempts");
                                          rr = {
                                            weights_file = get_its_control_path("rr-weights-file");
                                            {
                                              weight = 100.000;
                                              balancer2 = {
                                                unique_policy = {};
                                                attempts = 3;
                                                attempts_file = get_its_control_path("attempts");
                                                weighted2 = {
                                                  weights_file = get_its_control_path("weighted2-weights-file");
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
                                                    { "gobabygo.yandex.net"; 80; 1.000; "8.8.8.8"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "999ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = false;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 3;
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
                                            };
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- rpcrewrite
                                    }; -- rpcrewrite
                                  }; -- geobase
                                }; -- cookie_hasher
                              }; -- icookie
                            }; -- antirobot_wrapper
                          }; -- headers
                        }; -- rewrite
                      }; -- gobabygo
                      exp = {
                        priority = 1;
                        match_fsm = {
                          header = {
                            name = "X-L7-EXP";
                            value = "true";
                          }; -- header
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        headers = {
                          create_func_weak = {
                            ["X-Req-Id"] = "reqid";
                            ["X-Yandex-RandomUID"] = "yuid";
                          }; -- create_func_weak
                          remote_log = {
                            uaas_mode = true;
                            no_remote_log_file = get_its_control_path("no-remote-log-file");
                            remote_log_storage = {
                              shared = {
                                uuid = "5435287694027952480";
                                shared = {
                                  uuid = "testuuid";
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      unpack(gen_proxy_backends({
                                        { "laas.yandex.ru"; 80; 1.000; "2a02:6b8::91"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "10s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- rr
                                  }; -- balancer2
                                }; -- shared
                              }; -- shared
                            }; -- remote_log_storage
                            remote_log = {
                              uaas_mode = true;
                              no_remote_log_file = get_its_control_path("balancer_remote_log_switch");
                              remote_log_storage = {
                                shared = {
                                  uuid = "5435287694027952480";
                                }; -- shared
                              }; -- remote_log_storage
                              errordocument = {
                                status = 200;
                                content = "USERSPLIT";
                                force_conn_close = false;
                                remain_headers = "X-Yandex-ExpConfigVersion|X-Yandex-ExpBoxes|X-Yandex-ExpFlags|X-Yandex-ExpConfigVersion-Pre|X-Yandex-ExpBoxes-Pre|X-Yandex-ExpFlags-Pre|X-Yandex-RandomUID|X-Yandex-LogstatUID";
                              }; -- errordocument
                            }; -- remote_log
                          }; -- remote_log
                        }; -- headers
                      }; -- exp
                    }; -- regexp
                  }; -- shared
                }; -- headers
              }; -- threshold
              refers = "service_total";
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- remote_ips_80
    local_ips_443 = {
      ips = {
        "127.0.0.44";
      }; -- ips
      ports = {
        get_port_var("https_port");
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = "/place/db/www/logs/current-error_log-balancer-443";
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          http2_alpn_file = get_its_control_path("http2_request_rate");
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = "/place/db/www/logs/current-ssl_sni-balancer-443";
              priv = "/dev/shm/balancer/priv/awacs.yandex-team.ru.pem";
              cert = "/dev/shm/balancer/allCAs-awacs.yandex-team.ru.pem";
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = "/dev/shm/balancer/priv/1st.awacs.yandex-team.ru.key";
                };
                {
                  priority = 2;
                  keyfile = "/dev/shm/balancer/priv/2nd.awacs.yandex-team.ru.key";
                };
                {
                  priority = 1;
                  keyfile = "/dev/shm/balancer/priv/3rd.awacs.yandex-team.ru.key";
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
              log = "/place/db/www/logs/current-access_log-balancer-443";
              threshold = {
                lo_bytes = 524288;
                hi_bytes = 1048576;
                recv_timeout = "1s";
                pass_timeout = "10s";
                report = {
                  ranges = get_str_var("default_ranges");
                  just_storage = false;
                  disable_robotness = true;
                  disable_sslness = true;
                  events = {
                    stats = "report";
                  }; -- events
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
                    hasher = {
                      mode = "subnet";
                      take_ip_from = "X-Real-IP";
                      h100 = {
                        cutter = {
                          bytes = 512;
                          timeout = "0.1s";
                          antirobot = {
                            cut_request = true;
                            no_cut_request_file = get_its_control_path("no_cut_request_file");
                            file_switch = get_its_control_path("antirobot-macro-file-switch");
                            cut_request_bytes = 512;
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
                                        { "antirobot.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::121"; };
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
                                        connect_timeout = "100ms";
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
                                rate_file = get_its_control_path("request-replier-rate-file");
                                shared = {
                                  uuid = "2217635822518746117";
                                }; -- shared
                              }; -- request_replier
                            }; -- module
                          }; -- antirobot
                        }; -- cutter
                      }; -- h100
                    }; -- hasher
                  }; -- headers
                  refers = "service_total";
                }; -- report
              }; -- threshold
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- local_ips_443
  }; -- ipdispatch
}
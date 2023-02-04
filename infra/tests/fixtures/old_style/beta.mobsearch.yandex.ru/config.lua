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


function get_ca_cert_path(name, default_ca_cert_dir)
  default_ca_cert_dir = default_ca_cert_dir or "/dev/shm/balancer/priv"
  return (ca_cert_dir or default_ca_cert_dir) .. "/" .. name;
end


function get_geo(name, default_geo)
  default_geo = default_geo or "random"
  return name .. (DC or default_geo);
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


function get_private_cert_path(name, default_private_cert_dir)
  default_private_cert_dir = default_private_cert_dir or "/dev/shm/balancer/priv"
  return (private_cert_dir or default_private_cert_dir) .. "/" .. name;
end


function get_public_cert_path(name, default_public_cert_dir)
  default_public_cert_dir = default_public_cert_dir or "/dev/shm/balancer"
  return (public_cert_dir or default_public_cert_dir) .. "/" .. name;
end


function get_str_env_var(name, default)
  rv = os.getenv(name)
  if rv == nil then
    if default == nil then
      error(string.format('Environment variable "%s" is not set.', name))
    else
      return default
    end
  else
    return rv
  end
end


function get_str_var(name, default)
  return _G[name] or default
end


instance = {
  workers = 1;
  maxconn = 4000;
  buffer = 1048576;
  dns_ttl = "300s";
  tcp_fastopen = 256;
  pinger_required = true;
  tcp_listen_queue = 128;
  coro_stack_guard = true;
  coro_stack_size = 16384;
  enable_reuse_port = true;
  worker_start_delay = "1s";
  state_directory = "./state/";
  worker_start_duration = "30s";
  private_address = "127.0.0.10";
  tcp_congestion_control = "bbr";
  default_tcp_rst_on_error = true;
  shutdown_accept_connections = true;
  events = {
    stats = "report";
  }; -- events
  pinger_log = get_log_path("pingwerty", 80, "");
  dynamic_balancing_log = get_log_path("qwerty", 80, "");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15220, "/place/db/www/logs/");
  config_check = {
    quorums_file = "./controls/backend_check_quorums";
  }; -- config_check
  admin_addrs = {
    {
      port = 15220;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15220;
    };
  }; -- admin_addrs
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
    hide_legacy_signals = true;
  }; -- unistat
  sd = {
    client_name = "awacs-l7-balancer(beta.mobsearch.yandex.ru:balancer)";
    host = "::1";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = get_str_var("sd_cache_dir", "./sd_cache");
  }; -- sd
  cpu_limiter = {
    active_check_subnet_default = true;
    cpu_usage_coeff = 0.500;
    enable_conn_reject = true;
    conn_reject_lo = 0.000;
    conn_reject_hi = 0.990;
    conn_hold_count = 100;
    conn_hold_duration = "60s";
    enable_http2_drop = true;
    http2_drop_lo = 0.100;
    http2_drop_hi = 0.660;
    enable_keepalive_close = true;
    keepalive_close_lo = 0.660;
    keepalive_close_hi = 1.000;
    disable_file = "./disable-cpu-limiter";
    disable_http2_file = "./disable-http2";
    active_check_subnet_file = "./active-check-subnet";
  }; -- cpu_limiter
  addrs = {
    {
      port = 15220;
      ip = "127.0.0.4";
    };
    {
      port = 8888;
      ip = "8.8.8.8";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 30000;
      ip = "127.0.0.1";
    };
    {
      port = 80;
      ip = "2a02:6b8::1:62";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "2a02:6b8::1:63";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15220;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15220;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 443;
      ip = "2a02:6b8::1:62";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 443;
      ip = "2a02:6b8::1:63";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15221;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15221;
      ip = get_ip_by_iproute("v6");
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        15220;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {
          disable_xml_stats = true;
        }; -- admin
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        15220;
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        matcher_map = {
          ru = {
            match_fsm = {
              host = "beta.mobsearch.yandex.ru";
              case_insensitive = true;
              surround = false;
            }; -- match_fsm
          }; -- ru
          ru_ping = {
            match_and = {
              {
                match_fsm = {
                  host = "beta.mobsearch.yandex.ru";
                  case_insensitive = true;
                  surround = false;
                }; -- match_fsm
              };
              {
                match_fsm = {
                  URI = "/ping";
                  case_insensitive = true;
                  surround = false;
                }; -- match_fsm
              };
              {
                match_fsm = {
                  upgrade = "[a-z]";
                  case_insensitive = true;
                  surround = true;
                }; -- match_fsm
              };
            }; -- match_and
          }; -- ru_ping
        }; -- matcher_map
        outgoing_codes = "200,400";
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          keepalive_timeout = "100s";
          keepalive_requests = 1000;
          keepalive_drop_probability = 0.667;
          events = {
            stats = "report";
          }; -- events
          errordocument = {
            status = 204;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
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
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        active_check_reply = {
          default_weight = 100;
          use_header = false;
          use_body = true;
          use_dynamic_weight = false;
          weight_file = "./active-check-reply-weight-file";
          zero_weight_at_shutdown = true;
        }; -- active_check_reply
      }; -- http
    }; -- check
    s3 = {
      ips = {
        "127.0.0.1";
      }; -- ips
      ports = {
        30000;
      }; -- ports
      shared = {
        uuid = "s3";
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          compressor = {
            enable_compression = true;
            enable_decompression = true;
            compression_codecs = "gzip,br";
            report = {
              uuid = "s3";
              ranges = get_str_var("default_ranges");
              disable_signals = "requests,nka,ka";
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              errordocument = {
                status = 200;
                force_conn_close = false;
                content = "Here is your sitemap!";
              }; -- errordocument
            }; -- report
          }; -- compressor
        }; -- http
      }; -- shared
    }; -- s3
    http_section_80 = {
      ips = {
        "2a02:6b8::1:62";
        "2a02:6b8::1:63";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "4399850908810723262";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15220, "/place/db/www/logs/");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = false;
            no_keepalive_file = "./controls/keepalive_disabled";
            keepalive_timeout = "0ms";
            events = {
              stats = "report";
            }; -- events
            maxheaders = 100;
            accesslog = {
              log = get_log_path("access_log", 15220, "/place/db/www/logs/");
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
                response_headers_if = {
                  matcher = {
                    match_and = {
                      {
                        match_header = {
                          name = "location";
                          value = "http://yabs.yandex.ru.*";
                          value_case_insensitive = false;
                        }; -- match_header
                      };
                      {
                        match_response_codes = {
                          codes = { 302; };
                        }; -- match_response_codes
                      };
                    }; -- match_and
                  }; -- matcher
                  create_header = {
                    Authorization = get_str_env_var("TOKEN", "default-token");
                    ["Strict-Transport-Security"] = "max-age=600";
                  }; -- create_header
                  headers_forwarder = {
                    actions = {
                      {
                        request_header = "Origin";
                        response_header = "Access-Control-Allow-Origin";
                        erase_from_request = true;
                        erase_from_response = true;
                        weak = false;
                      };
                    }; -- actions
                    headers = {
                      create_func = {
                        ["X-Ja3"] = "ja3";
                        ["X-P0f"] = "p0f";
                        ["X-Req-Id"] = "reqid";
                        ["X-Source-Port-Y"] = "realport";
                        ["X-Start-Time"] = "starttime";
                        ["X-Yandex-RandomUID"] = "yuid";
                      }; -- create_func
                      create_func_weak = {
                        ["X-Forwarded-For"] = "realip";
                        ["X-Forwarded-For-Y"] = "realip";
                      }; -- create_func_weak
                      append_func = {
                        ["X-Req-Id"] = "reqid";
                        ["X-Source-Port-Y"] = "realport";
                        ["X-Start-Time"] = "starttime";
                        ["X-Yandex-RandomUID"] = "yuid";
                      }; -- append_func
                      append_func_weak = {
                        ["X-Forwarded-For"] = "realip";
                        ["X-Forwarded-For-Y"] = "realip";
                      }; -- append_func_weak
                      copy = {
                        ["X-Real-Ip"] = "X-Forwarded-For";
                      }; -- copy
                      copy_weak = {
                        ["Y-Real-Ip"] = "Y-Forwarded-For";
                      }; -- copy_weak
                      response_headers = {
                        create_weak = {
                          ["X-Content-Type-Options"] = "nosniff";
                          ["X-XSS-Protection"] = "1; mode=block";
                        }; -- create_weak
                        append_func = {
                          ["X-Start-Time"] = "starttime";
                        }; -- append_func
                        append_func_weak = {
                          ["X-Ip"] = "localip";
                        }; -- append_func_weak
                        append_weak = {
                          ["X-Content-Type-Options"] = "nosniff";
                          ["X-XSS-Protection"] = "1; mode=block";
                        }; -- append_weak
                        cookies = {
                          delete = ".*cookie1.*";
                          create = {
                            cookie2 = "value2";
                          }; -- create
                          create_weak = {
                            cookie3 = "value3";
                          }; -- create_weak
                          shared = {
                            uuid = "5262399783298041165";
                            regexp = {
                              slb_ping = {
                                priority = 38;
                                match_fsm = {
                                  URI = "/admin/info";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                stats_eater = {
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/slb_check.weights";
                                      to_upstream = {
                                        weight = 1.000;
                                        errordocument = {
                                          status = 200;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- to_upstream
                                      switch_off = {
                                        weight = -1.000;
                                        errordocument = {
                                          status = 204;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- switch_off
                                    }; -- rr
                                  }; -- balancer2
                                }; -- stats_eater
                              }; -- slb_ping
                              slb_ping_v2 = {
                                priority = 37;
                                match_fsm = {
                                  URI = "/admin/info_v2";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                stats_eater = {
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/slb_check.weights";
                                      to_upstream = {
                                        weight = 1.000;
                                        shared = {
                                          uuid = "backends";
                                        }; -- shared
                                      }; -- to_upstream
                                      switch_off = {
                                        weight = -1.000;
                                        errordocument = {
                                          status = 503;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- switch_off
                                    }; -- rr
                                  }; -- balancer2
                                }; -- stats_eater
                              }; -- slb_ping_v2
                              captcha = {
                                priority = 36;
                                match_fsm = {
                                  URI = "/x?(show|check)?captcha.*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
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
                                          balancer2 = {
                                            by_name_policy = {
                                              name = get_geo("antirobot_", "random");
                                              simple_policy = {};
                                            }; -- by_name_policy
                                            attempts = 1;
                                            rr = {
                                              antirobot_man = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  sd = {
                                                    endpoint_sets = {
                                                      {
                                                        cluster_name = "man";
                                                        endpoint_set_id = "prod-antirobot-yp-man";
                                                      };
                                                    }; -- endpoint_sets
                                                    proxy_options = {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "30ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = false;
                                                    }; -- proxy_options
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
                                                    }; -- weighted2
                                                  }; -- sd
                                                }; -- balancer2
                                              }; -- antirobot_man
                                              antirobot_sas = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  sd = {
                                                    endpoint_sets = {
                                                      {
                                                        cluster_name = "sas";
                                                        endpoint_set_id = "prod-antirobot-yp-sas";
                                                      };
                                                    }; -- endpoint_sets
                                                    proxy_options = {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "30ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = false;
                                                    }; -- proxy_options
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
                                                    }; -- weighted2
                                                  }; -- sd
                                                }; -- balancer2
                                              }; -- antirobot_sas
                                              antirobot_vla = {
                                                weight = 1.000;
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  sd = {
                                                    endpoint_sets = {
                                                      {
                                                        cluster_name = "vla";
                                                        endpoint_set_id = "prod-antirobot-yp-vla";
                                                      };
                                                    }; -- endpoint_sets
                                                    proxy_options = {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "30ms";
                                                      backend_timeout = "10s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = false;
                                                    }; -- proxy_options
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
                                                    }; -- weighted2
                                                  }; -- sd
                                                }; -- balancer2
                                              }; -- antirobot_vla
                                            }; -- rr
                                          }; -- balancer2
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
                              }; -- captcha
                              atomsearch = {
                                priority = 35;
                                match_and = {
                                  {
                                    match_fsm = {
                                      URI = "/((m)?search/)?atomsearch(/.*)?";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_method = {
                                      methods = { "post"; "put"; };
                                    }; -- match_method
                                  };
                                }; -- match_and
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
                                  regexp = {
                                    exp_testing = {
                                      priority = 2;
                                      match_fsm = {
                                        cgi = "(exp-testing=da|exp_confs=testing)";
                                        case_insensitive = true;
                                        surround = true;
                                      }; -- match_fsm
                                      headers = {
                                        create = {
                                          ["X-L7-EXP-Testing"] = "true";
                                        }; -- create
                                        shared = {
                                          uuid = "1042199279589293405";
                                          exp_getter = {
                                            trusted = false;
                                            file_switch = "./controls/expgetter.switch";
                                            service_name = "atomsearch";
                                            service_name_header = "Y-Service";
                                            exp_headers = "X-Yandex-ExpConfigVersion(-Pre)?|X-Yandex-ExpBoxes(-Pre)?|X-Yandex-ExpFlags(-Pre)?|X-Yandex-LogstatUID";
                                            uaas = {
                                              shared = {
                                                uuid = "2699981958704336253";
                                              }; -- shared
                                            }; -- uaas
                                            stats_eater = {
                                              balancer2 = {
                                                by_name_from_header_policy = {
                                                  header_name = "X-Test";
                                                  allow_zero_weights = true;
                                                  strict = true;
                                                  hints = {
                                                    {
                                                      hint = "pum";
                                                      backend = "purum";
                                                    };
                                                  }; -- hints
                                                  unique_policy = {};
                                                }; -- by_name_from_header_policy
                                                attempts = 2;
                                                rewind_limit = 1024;
                                                check_backends = {
                                                  quorum = 0.100;
                                                  name = "kek";
                                                }; -- check_backends
                                                leastconn = {
                                                  unpack(gen_proxy_backends({
                                                    { "ws33-340.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9cb1"; };
                                                    { "ws34-487.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9ffa"; };
                                                    { "ws35-290.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8493"; };
                                                    { "ws35-658.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8651"; };
                                                    { "ws40-413.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81d0"; };
                                                    { "ws40-449.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81e2"; };
                                                  }, {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "150ms";
                                                    backend_timeout = "5s";
                                                    fail_on_5xx = true;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = true;
                                                    allow_connection_upgrade = true;
                                                  }))
                                                }; -- leastconn
                                              }; -- balancer2
                                            }; -- stats_eater
                                          }; -- exp_getter
                                        }; -- shared
                                      }; -- headers
                                    }; -- exp_testing
                                    default = {
                                      priority = 1;
                                      headers = {
                                        create = {
                                          ["X-L7-EXP"] = "true";
                                        }; -- create
                                        shared = {
                                          uuid = "1042199279589293405";
                                        }; -- shared
                                      }; -- headers
                                    }; -- default
                                  }; -- regexp
                                }; -- rewrite
                              }; -- atomsearch
                              jsonproxy = {
                                priority = 34;
                                match_fsm = {
                                  URI = "/((m)?search/)?jsonproxy(/.*)?";
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
                                        cut_request_bytes = 512;
                                        checker = {
                                          shared = {
                                            uuid = "5685305157605435098";
                                            report = {
                                              uuid = "antirobot";
                                              ranges = get_str_var("default_ranges");
                                              just_storage = false;
                                              disable_robotness = true;
                                              disable_sslness = true;
                                              events = {
                                                stats = "report";
                                              }; -- events
                                              balancer2 = {
                                                by_name_policy = {
                                                  name = get_geo("antirobot_", "random");
                                                  simple_policy = {};
                                                }; -- by_name_policy
                                                attempts = 1;
                                                rr = {
                                                  antirobot_man = {
                                                    weight = 1.000;
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      hashing = {
                                                        unpack(gen_proxy_backends({
                                                          { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
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
                                                  }; -- antirobot_man
                                                  antirobot_sas = {
                                                    weight = 1.000;
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      hashing = {
                                                        unpack(gen_proxy_backends({
                                                          { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
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
                                                  }; -- antirobot_sas
                                                  antirobot_vla = {
                                                    weight = 1.000;
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      hashing = {
                                                        unpack(gen_proxy_backends({
                                                          { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
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
                                                  }; -- antirobot_vla
                                                }; -- rr
                                              }; -- balancer2
                                            }; -- report
                                          }; -- shared
                                        }; -- checker
                                        module = {
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
                                              regexp = {
                                                exp_testing = {
                                                  priority = 2;
                                                  match_fsm = {
                                                    cgi = "(exp-testing=da|exp_confs=testing)";
                                                    case_insensitive = true;
                                                    surround = true;
                                                  }; -- match_fsm
                                                  headers = {
                                                    create = {
                                                      ["X-L7-EXP-Testing"] = "true";
                                                    }; -- create
                                                    shared = {
                                                      uuid = "6516614686881964764";
                                                      exp_getter = {
                                                        trusted = false;
                                                        file_switch = "./controls/expgetter.switch";
                                                        uaas = {
                                                          shared = {
                                                            uuid = "2699981958704336253";
                                                          }; -- shared
                                                        }; -- uaas
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 2;
                                                          dynamic = {
                                                            max_pessimized_share = 0.100;
                                                            min_pessimization_coeff = 0.100;
                                                            weight_increase_step = 0.100;
                                                            history_interval = "10s";
                                                            backends_name = "common-antirobot/antirobot_vla";
                                                            active = {
                                                              request = "GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n";
                                                              delay = "10s";
                                                              weight_normalization_coeff = 0.500;
                                                              use_backend_weight = true;
                                                            }; -- active
                                                            unpack(gen_proxy_backends({
                                                              { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
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
                                                          }; -- dynamic
                                                        }; -- balancer2
                                                      }; -- exp_getter
                                                    }; -- shared
                                                  }; -- headers
                                                }; -- exp_testing
                                                default = {
                                                  priority = 1;
                                                  headers = {
                                                    create = {
                                                      ["X-L7-EXP"] = "true";
                                                    }; -- create
                                                    shared = {
                                                      uuid = "6516614686881964764";
                                                    }; -- shared
                                                  }; -- headers
                                                }; -- default
                                              }; -- regexp
                                            }; -- stats_eater
                                          }; -- rewrite
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- hasher
                              }; -- jsonproxy
                              jsonsearch = {
                                priority = 33;
                                match_fsm = {
                                  URI = "/(((m)?search/)?(suggest|brosearch|onewizard|logverifier)|msearch/jsonsearch|jsonproxy|(((search|mobilesearch)/)?searchapi))(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
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
                                          { "ws33-340.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9cb1"; };
                                          { "ws34-487.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9ffa"; };
                                          { "ws35-290.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8493"; };
                                          { "ws35-658.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8651"; };
                                          { "ws40-413.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81d0"; };
                                          { "ws40-449.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81e2"; };
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
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- rewrite
                              }; -- jsonsearch
                              touchsearch = {
                                priority = 32;
                                match_fsm = {
                                  URI = "/touchsearch(/.*)?";
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
                                        cut_request_bytes = 512;
                                        checker = {
                                          shared = {
                                            uuid = "5685305157605435098";
                                          }; -- shared
                                        }; -- checker
                                        module = {
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              hedged_delay = "10s";
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
                                                  { "ws33-340.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9cb1"; };
                                                  { "ws34-487.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9ffa"; };
                                                  { "ws35-290.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8493"; };
                                                  { "ws35-658.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8651"; };
                                                  { "ws40-413.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81d0"; };
                                                  { "ws40-449.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81e2"; };
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
                                              attempts_rate_limiter = {
                                                limit = 0.200;
                                                max_budget = 3.000;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- hasher
                              }; -- touchsearch
                              sport_push = {
                                priority = 31;
                                match_fsm = {
                                  URI = "/(sport_push_(un)?subscribe|olymp_push)(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                response_matcher = {
                                  buffer_size = 1024;
                                  on_response = {
                                    succ = {
                                      priority = 2;
                                      match_response_codes = {
                                        codes = { 200; 204; };
                                      }; -- match_response_codes
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 1;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
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
                                    }; -- succ
                                    redirect = {
                                      priority = 1;
                                      match_and = {
                                        {
                                          match_header = {
                                            name = "location";
                                            value = "http://yabs.yandex.ru.*";
                                            value_case_insensitive = false;
                                          }; -- match_header
                                        };
                                        {
                                          match_response_codes = {
                                            codes = { 302; };
                                          }; -- match_response_codes
                                        };
                                      }; -- match_and
                                      errordocument = {
                                        status = 200;
                                        content = "redirect";
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- redirect
                                  }; -- on_response
                                  module = {
                                    log_headers = {
                                      name_re = "X-Yandex-RandomUID";
                                      cookie_fields = "first_field,second_field";
                                      tcp_rst_on_error = {
                                        send_rst = false;
                                        threshold = {
                                          lo_bytes = 512;
                                          hi_bytes = 1024;
                                          recv_timeout = "1s";
                                          pass_timeout = "10s";
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 6;
                                              active = {
                                                delay = "1s";
                                                request = "GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n";
                                                steady = true;
                                                quorum = 495.000;
                                                hysteresis = 0.000;
                                                unpack(gen_proxy_backends({
                                                  { "man1-0755.search.yandex.net"; 29700; 250.000; "2a02:6b8:b000:6030:92e2:baff:fe6e:bf1a"; };
                                                  { "man1-7443.search.yandex.net"; 29700; 250.000; "2a02:6b8:b000:6067:e61d:2dff:fe04:3230"; };
                                                  { "sas1-3919.search.yandex.net"; 18460; 250.000; "2a02:6b8:b000:634:225:90ff:fe83:38d8"; };
                                                  { "sas1-6011.search.yandex.net"; 18460; 250.000; "2a02:6b8:b000:672:feaa:14ff:fe1d:faa8"; };
                                                  { "ws38-641.search.yandex.net"; 28700; 1.000; "2a02:6b8:0:c22::52d:ef48"; };
                                                  { "ws43-215.search.yandex.net"; 28700; 499.000; "2a02:6b8:0:2502::5f6c:d8d8"; };
                                                }, {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "150ms";
                                                  backend_timeout = "10s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = true;
                                                  https_settings = {
                                                    ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                                    ca_file = get_ca_cert_path("allCAs-beta.mobsearch.yandex.ru.pem", "");
                                                    sni_on = false;
                                                    verify_depth = 1;
                                                    sni_host = "yandex.ru";
                                                  }; -- https_settings
                                                }))
                                              }; -- active
                                            }; -- balancer2
                                          }; -- stats_eater
                                        }; -- threshold
                                      }; -- tcp_rst_on_error
                                    }; -- log_headers
                                  }; -- module
                                }; -- response_matcher
                              }; -- sport_push
                              test_click_module = {
                                priority = 30;
                                match_fsm = {
                                  header = {
                                    name = "X-Xxx";
                                    value = "xxx";
                                  }; -- header
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                click = {
                                  keys = "./click-keys";
                                  json_keys = "./click-keys.json";
                                  errordocument = {
                                    status = 200;
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- click
                              }; -- test_click_module
                              psuh = {
                                priority = 29;
                                match_fsm = {
                                  URI = "/psuh/(login|logout|projects/\\w+/topics|registrations|subscriptions|tags)(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                pinger = {
                                  lo = 0.500;
                                  hi = 0.700;
                                  delay = "1s";
                                  histtime = "4s";
                                  ping_request_data = "GET /ping HTTP/1.1\nHost: beta.mobseaerch.yandex.ru\r\n\r\n";
                                  admin_request_uri = "/ping";
                                  admin_ips = "5.45.193.144/29,5.45.208.88/30,5.45.228.160/30,5.45.229.168/30,5.45.232.160/30,5.45.240.168/30,5.45.243.16/30,5.45.247.144/29,5.45.247.176/29,5.255.192.64/30,5.255.194.12/30,5.255.195.144/30,5.255.196.168/30,5.255.197.168/30,5.255.200.160/30,5.255.252.160/30,37.9.73.160/30,37.140.136.88/29,37.140.137.104/29,77.88.35.168/30,77.88.54.168/30,84.201.159.176/28,87.250.226.0/25,87.250.226.128/25,87.250.228.0/24,87.250.234.0/24,95.108.180.40/29,95.108.237.0/25,95.108.237.128/25,100.43.92.144/28,141.8.136.128/25,141.8.154.200/29,141.8.155.104/29,185.32.186.8/29,213.180.202.160/30,213.180.223.16/30,2001:678:384:100::/64,2620:10f:d000:100::/64,2a02:6b8:0:300::/64,2a02:6b8:0:400::/64,2a02:6b8:0:800::/64,2a02:6b8:0:900::/64,2a02:6b8:0:d00::/64,2a02:6b8:0:e00::/64,2a02:6b8:0:1000::/64,2a02:6b8:0:1100::/64,2a02:6b8:0:1200::/64,2a02:6b8:0:1300::/64,2a02:6b8:0:1400::/64,2a02:6b8:0:1500::/64,2a02:6b8:0:1600::/64,2a02:6b8:0:1700::/64,2a02:6b8:0:1800::/64,2a02:6b8:0:1900::/64,2a02:6b8:0:1a00::/64,2a02:6b8:0:1b00::/64,2a02:6b8:0:1d00::/64,2a02:6b8:0:1e00::/64,2a02:6b8:0:1f00::/64,2a02:6b8:0:2000::/64,2a02:6b8:0:2200::/64,2a02:6b8:0:2c00::/64,2a02:6b8:0:3000::/64,2a02:6b8:0:3100::/64,2a02:6b8:0:3401::/64,2a02:6b8:0:3c00::/64,2a02:6b8:0:3d00::/64,2a02:6b8:0:3e00::/64,2a02:6b8:0:3f00::/64,2a02:6b8:0:4000::/64,2a02:6b8:0:4200::/64,2a02:6b8:0:4700::/64,2a02:6b8:b010:b000::/64";
                                  enable_tcp_check_file = "./controls/tcp_check_on";
                                  switch_off_file = "./controls/slb_check.weights";
                                  switch_off_key = "switch_off";
                                  admin_error_replier = {
                                    errordocument = {
                                      status = 503;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- admin_error_replier
                                  status_codes = {
                                    "2xx";
                                  }; -- status_codes
                                  status_codes_exceptions = {
                                    "204";
                                  }; -- status_codes_exceptions
                                  module = {
                                    threshold = {
                                      lo_bytes = 512;
                                      hi_bytes = 1024;
                                      recv_timeout = "1s";
                                      pass_timeout = "10s";
                                      on_pass_timeout_failure = {
                                        errordocument = {
                                          status = 200;
                                          base64 = "CAI=";
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_pass_timeout_failure
                                      stats_eater = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
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
                                              { "ws36-383.search.yandex.net"; 1026; 1.000; "2a02:6b8:0:2502::2509:55c1"; };
                                              { "ws36-628.search.yandex.net"; 1025; 1.000; "2a02:6b8:0:2502::2509:5642"; };
                                              { "ws39-438.search.yandex.net"; 1027; 1.000; "2a02:6b8:0:2502::2509:50dd"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "35s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                      }; -- stats_eater
                                    }; -- threshold
                                  }; -- module
                                }; -- pinger
                              }; -- psuh
                              swat_7451 = {
                                priority = 28;
                                match_fsm = {
                                  URI = "/swat7451";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                hasher = {
                                  mode = "subnet";
                                  shared = {
                                    uuid = "1282840050913886341";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      rr = {
                                        x = {
                                          weight = 1.000;
                                          shared = {
                                            uuid = "6922024133015752822";
                                            balancer2 = {
                                              active_policy = {
                                                unique_policy = {};
                                              }; -- active_policy
                                              attempts = 2;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "man";
                                                    endpoint_set_id = "rpslimiter-serval-man-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "150ms";
                                                  backend_timeout = "5s";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
                                                hashing = {};
                                              }; -- sd
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- x
                                        y = {
                                          weight = 1.000;
                                          shared = {
                                            uuid = "6922024133015752822";
                                          }; -- shared
                                        }; -- y
                                      }; -- rr
                                    }; -- balancer2
                                  }; -- shared
                                }; -- hasher
                              }; -- swat_7451
                              ban_push = {
                                priority = 27;
                                match_fsm = {
                                  URI = "/push_(get|set)_ban(/.*)?";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                tcp_rst_on_error = {
                                  send_rst = true;
                                  stats_eater = {
                                    icookie = {
                                      use_default_keys = true;
                                      domains = ".yandex.ru,.yandex.tr";
                                      trust_parent = false;
                                      trust_children = false;
                                      enable_set_cookie = true;
                                      enable_decrypting = true;
                                      decrypted_uid_header = "X-Yandex-ICookie";
                                      error_header = "X-Yandex-ICookie-Error";
                                      force_equal_to_yandexuid = false;
                                      force_generate_from_searchapp_uuid = false;
                                      enable_parse_searchapp_uuid = false;
                                      max_transport_age = 180;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
                                        rr = {
                                          randomize_initial_state = true;
                                          unpack(gen_proxy_backends({
                                            { "search-history.yandex.net"; 10000; 1.000; "2a02:6b8:0:3400::3:36"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "20ms";
                                            backend_timeout = "300ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- rr
                                        on_status_code = {
                                          ["400"] = {
                                            errordocument = {
                                              status = 404;
                                              force_conn_close = false;
                                            }; -- errordocument
                                          }; -- ["400"]
                                          ["404"] = {
                                            errordocument = {
                                              status = 404;
                                              force_conn_close = false;
                                            }; -- errordocument
                                          }; -- ["404"]
                                        }; -- on_status_code
                                      }; -- balancer2
                                    }; -- icookie
                                  }; -- stats_eater
                                }; -- tcp_rst_on_error
                              }; -- ban_push
                              awacs_1148 = {
                                priority = 26;
                                match_fsm = {
                                  URI = "/swat7451";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                hasher = {
                                  mode = "subnet";
                                  redirects = {
                                    actions = {
                                      {
                                        src = "//mir.trains.yandex.ru/x/*";
                                        forward = {
                                          dst = "http://yastatic.net/s3/travel/other-projects/mir/robots.txt";
                                          legacy_rstrip = true;
                                          dst_rewrites = {
                                            {
                                              regexp = "[.]xml$";
                                              rewrite = "";
                                            };
                                          }; -- dst_rewrites
                                          errordocument = {
                                            status = 503;
                                            force_conn_close = false;
                                            content = "Service unavailable";
                                          }; -- errordocument
                                        }; -- forward
                                      };
                                      {
                                        src = "//mir.trains.yandex.ru/y/*";
                                        redirect = {
                                          dst = "https://travel.yandex.ru/trains{query}";
                                          legacy_rstrip = true;
                                          code = 301;
                                          dst_rewrites = {};
                                        }; -- redirect
                                      };
                                      {
                                        src = "//adv.yandex.ru/price/media/*";
                                        redirect = {
                                          dst = "https://yandex.ru/adv/products/display/{path}#price";
                                          legacy_rstrip = true;
                                          code = 301;
                                          dst_rewrites = {
                                            {
                                              regexp = "[.]xml$";
                                              rewrite = "";
                                            };
                                          }; -- dst_rewrites
                                        }; -- redirect
                                      };
                                    }; -- actions
                                    shared = {
                                      uuid = "1282840050913886341";
                                    }; -- shared
                                  }; -- redirects
                                }; -- hasher
                              }; -- awacs_1148
                              test_antirobot_wrapper = {
                                priority = 25;
                                match_fsm = {
                                  URI = "/antirobot";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                hasher = {
                                  mode = "subnet";
                                  take_ip_from = "X-Forwarded-For-Y";
                                  h100 = {
                                    cutter = {
                                      bytes = 512;
                                      timeout = "0.1s";
                                      antirobot_wrapper = {
                                        cut_request = true;
                                        no_cut_request_file = "./controls/no_cut_request_file";
                                        cut_request_bytes = 512;
                                        stats_eater = {
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            hashing = {
                                              unpack(gen_proxy_backends({
                                                { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                                { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                                { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                                { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                                { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                                { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                                { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                                { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                                { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                                { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
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
                                            attempts_rate_limiter = {
                                              limit = 2.000;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- stats_eater
                                      }; -- antirobot_wrapper
                                    }; -- cutter
                                  }; -- h100
                                }; -- hasher
                              }; -- test_antirobot_wrapper
                              antirobot_009 = {
                                priority = 24;
                                match_fsm = {
                                  URI = "/antirobot_009";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                headers = {
                                  create_func = {
                                    ["X-Forwarded-For-Y"] = "realip";
                                  }; -- create_func
                                  hasher = {
                                    mode = "subnet";
                                    take_ip_from = "X-Forwarded-For-Y";
                                    subnet_v4_mask = 32;
                                    subnet_v6_mask = 64;
                                    h100 = {
                                      cutter = {
                                        bytes = 512;
                                        timeout = "0.1s";
                                        icookie = {
                                          use_default_keys = true;
                                          domains = ".yandex.ru,.yandex.ua,.yandex.uz,.yandex.by,.yandex.kz,.yandex.com,.yandex.com.tr,.yandex.com.ge,.yandex.fr,.yandex.az,.yandex.com.am,.yandex.co.il,.yandex.kg,.yandex.lt,.yandex.lv,.yandex.md,.yandex.tj,.yandex.tm,.yandex.ee,.yandex.eu,.yandex.fi,.yandex.pl,.ya.ru,.kinopoisk.ru";
                                          trust_parent = false;
                                          trust_children = false;
                                          enable_set_cookie = true;
                                          enable_decrypting = true;
                                          decrypted_uid_header = "X-Yandex-ICookie";
                                          error_header = "X-Yandex-ICookie-Error";
                                          headers = {
                                            create_func = {
                                              ["X-Yandex-Ja3"] = "ja3";
                                              ["X-Yandex-Ja4"] = "ja4";
                                            }; -- create_func
                                            antirobot = {
                                              cut_request = true;
                                              no_cut_request_file = "./controls/no_cut_request_file";
                                              file_switch = "./controls/disable_antirobot_module";
                                              cut_request_bytes = 512;
                                              checker = {
                                                report = {
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  balancer2 = {
                                                    by_name_policy = {
                                                      name = get_geo("antirobot_", "random");
                                                      simple_policy = {};
                                                    }; -- by_name_policy
                                                    attempts = 1;
                                                    rr = {
                                                      antirobot_man = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 2;
                                                          sd = {
                                                            endpoint_sets = {
                                                              {
                                                                cluster_name = "man";
                                                                endpoint_set_id = "prod-antirobot-yp-man";
                                                              };
                                                            }; -- endpoint_sets
                                                            proxy_options = {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "30ms";
                                                              backend_timeout = "100ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 0;
                                                              need_resolve = false;
                                                            }; -- proxy_options
                                                            hashing = {};
                                                          }; -- sd
                                                        }; -- balancer2
                                                      }; -- antirobot_man
                                                      antirobot_sas = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 2;
                                                          sd = {
                                                            endpoint_sets = {
                                                              {
                                                                cluster_name = "sas";
                                                                endpoint_set_id = "prod-antirobot-yp-sas";
                                                              };
                                                            }; -- endpoint_sets
                                                            proxy_options = {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "30ms";
                                                              backend_timeout = "100ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 0;
                                                              need_resolve = false;
                                                            }; -- proxy_options
                                                            hashing = {};
                                                          }; -- sd
                                                        }; -- balancer2
                                                      }; -- antirobot_sas
                                                      antirobot_vla = {
                                                        weight = 1.000;
                                                        balancer2 = {
                                                          unique_policy = {};
                                                          attempts = 2;
                                                          sd = {
                                                            endpoint_sets = {
                                                              {
                                                                cluster_name = "vla";
                                                                endpoint_set_id = "prod-antirobot-yp-vla";
                                                              };
                                                            }; -- endpoint_sets
                                                            proxy_options = {
                                                              resolve_timeout = "10ms";
                                                              connect_timeout = "30ms";
                                                              backend_timeout = "100ms";
                                                              fail_on_5xx = true;
                                                              http_backend = true;
                                                              buffering = false;
                                                              keepalive_count = 0;
                                                              need_resolve = false;
                                                            }; -- proxy_options
                                                            hashing = {};
                                                          }; -- sd
                                                        }; -- balancer2
                                                      }; -- antirobot_vla
                                                      antirobot_iva = {
                                                        weight = 1.000;
                                                        shared = {
                                                          uuid = "2168940542810593880";
                                                          balancer2 = {
                                                            unique_policy = {};
                                                            attempts = 2;
                                                            rr = {
                                                              antirobot_man = {
                                                                weight = 1.000;
                                                                balancer2 = {
                                                                  unique_policy = {};
                                                                  attempts = 1;
                                                                  sd = {
                                                                    endpoint_sets = {
                                                                      {
                                                                        cluster_name = "man";
                                                                        endpoint_set_id = "prod-antirobot-yp-man";
                                                                      };
                                                                    }; -- endpoint_sets
                                                                    proxy_options = {
                                                                      resolve_timeout = "10ms";
                                                                      connect_timeout = "30ms";
                                                                      backend_timeout = "100ms";
                                                                      fail_on_5xx = true;
                                                                      http_backend = true;
                                                                      buffering = false;
                                                                      keepalive_count = 0;
                                                                      need_resolve = false;
                                                                    }; -- proxy_options
                                                                    hashing = {};
                                                                  }; -- sd
                                                                }; -- balancer2
                                                              }; -- antirobot_man
                                                              antirobot_sas = {
                                                                weight = 1.000;
                                                                balancer2 = {
                                                                  unique_policy = {};
                                                                  attempts = 1;
                                                                  sd = {
                                                                    endpoint_sets = {
                                                                      {
                                                                        cluster_name = "sas";
                                                                        endpoint_set_id = "prod-antirobot-yp-sas";
                                                                      };
                                                                    }; -- endpoint_sets
                                                                    proxy_options = {
                                                                      resolve_timeout = "10ms";
                                                                      connect_timeout = "30ms";
                                                                      backend_timeout = "100ms";
                                                                      fail_on_5xx = true;
                                                                      http_backend = true;
                                                                      buffering = false;
                                                                      keepalive_count = 0;
                                                                      need_resolve = false;
                                                                    }; -- proxy_options
                                                                    hashing = {};
                                                                  }; -- sd
                                                                }; -- balancer2
                                                              }; -- antirobot_sas
                                                              antirobot_vla = {
                                                                weight = 1.000;
                                                                balancer2 = {
                                                                  unique_policy = {};
                                                                  attempts = 1;
                                                                  sd = {
                                                                    endpoint_sets = {
                                                                      {
                                                                        cluster_name = "vla";
                                                                        endpoint_set_id = "prod-antirobot-yp-vla";
                                                                      };
                                                                    }; -- endpoint_sets
                                                                    proxy_options = {
                                                                      resolve_timeout = "10ms";
                                                                      connect_timeout = "30ms";
                                                                      backend_timeout = "100ms";
                                                                      fail_on_5xx = true;
                                                                      http_backend = true;
                                                                      buffering = false;
                                                                      keepalive_count = 0;
                                                                      need_resolve = false;
                                                                    }; -- proxy_options
                                                                    hashing = {};
                                                                  }; -- sd
                                                                }; -- balancer2
                                                              }; -- antirobot_vla
                                                            }; -- rr
                                                          }; -- balancer2
                                                        }; -- shared
                                                      }; -- antirobot_iva
                                                      antirobot_myt = {
                                                        weight = 1.000;
                                                        shared = {
                                                          uuid = "2168940542810593880";
                                                        }; -- shared
                                                      }; -- antirobot_myt
                                                    }; -- rr
                                                  }; -- balancer2
                                                  refers = "antirobot";
                                                }; -- report
                                              }; -- checker
                                              module = {
                                                stats_eater = {
                                                  errordocument = {
                                                    status = 200;
                                                    force_conn_close = false;
                                                    content = "antirobot_009";
                                                  }; -- errordocument
                                                }; -- stats_eater
                                              }; -- module
                                            }; -- antirobot
                                          }; -- headers
                                        }; -- icookie
                                      }; -- cutter
                                    }; -- h100
                                  }; -- hasher
                                }; -- headers
                              }; -- antirobot_009
                              test_balance2_policies = {
                                priority = 23;
                                match_fsm = {
                                  URI = "/balancer2_policies";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  retry_policy = {
                                    watermark_policy = {
                                      lo = 0.050;
                                      hi = 0.100;
                                      params_file = "./controls/watermark_policy.params_file";
                                      shared = true;
                                      coeff = 0.100;
                                      unique_policy = {};
                                    }; -- watermark_policy
                                  }; -- retry_policy
                                  attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                      { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                      { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                      { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                      { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                      { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                      { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                      { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                      { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                      { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
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
                              }; -- test_balance2_policies
                              test_exp_module = {
                                priority = 22;
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
                                    no_remote_log_file = "./controls/remote_log.switch";
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
                                      no_remote_log_file = "./controls/remote_log.switch";
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
                              }; -- test_exp_module
                              test_aab_cookie_verify = {
                                priority = 21;
                                match_fsm = {
                                  URI = "/get";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                aab_cookie_verify = {
                                  aes_key_path = "./private.key";
                                  disable_antiadblock_file = "./controls/disable_antiadblock_file";
                                  cookie = "cycada";
                                  cookie_lifetime = "15d";
                                  ip_header = "X-Not-Real-Ip";
                                  antiadblock = {
                                    errordocument = {
                                      status = 500;
                                      force_conn_close = false;
                                      content = "Hello from aab_cookie_verify module";
                                    }; -- errordocument
                                  }; -- antiadblock
                                  default = {
                                    srcrwr = {
                                      id = "42";
                                      match_and = {
                                        {
                                          match_fsm = {
                                            host = "(.*[.])?hamster[.](.*)?";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                        };
                                        {
                                          match_source_ip = {
                                            source_mask = "2a02:6b8::/32,2620:10f:d000::/44,5.45.208.0/26,5.45.228.0/25";
                                          }; -- match_source_ip
                                        };
                                      }; -- match_and
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        return_last_5xx = true;
                                        status_code_blacklist = {
                                          "5xx";
                                        }; -- status_code_blacklist
                                        first_delay = "10ms";
                                        delay_multiplier = 1.100;
                                        delay_on_fast = true;
                                        max_random_delay = "1ms";
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                            { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                            { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                            { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                            { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                            { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                            { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                            { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                            { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                            { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
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
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- srcrwr
                                  }; -- default
                                }; -- aab_cookie_verify
                              }; -- test_aab_cookie_verify
                              test_cgi_hasher = {
                                priority = 20;
                                match_fsm = {
                                  URI = "/cgi_hasher";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                cgi_hasher = {
                                  parameters = {
                                    "xxx";
                                    "yyy";
                                  }; -- parameters
                                  randomize_empty_match = true;
                                  case_insensitive = false;
                                  mode = "priority";
                                  flags_getter = {
                                    service_name = "my-test-service";
                                    flags_path = "flags-path";
                                    flags_host = "flags-host";
                                    file_switch = "./disable-flags.txt";
                                    flags = {
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        rr = {
                                          unpack(gen_proxy_backends({
                                            { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
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
                                        }; -- rr
                                      }; -- balancer2
                                    }; -- flags
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      hashing = {
                                        unpack(gen_proxy_backends({
                                          { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                          { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                          { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                          { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                          { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                          { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                          { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                          { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                          { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                          { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
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
                                  }; -- flags_getter
                                }; -- cgi_hasher
                              }; -- test_cgi_hasher
                              test_rendezvous_hashing = {
                                priority = 19;
                                match_fsm = {
                                  URI = "/rendezvous_hashing";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                hasher = {
                                  mode = "subnet";
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 2;
                                    rendezvous_hashing = {
                                      weights_file = "weights_file";
                                      reload_duration = "1s";
                                      delay = "1s";
                                      request = "GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n";
                                      steady = true;
                                      unpack(gen_proxy_backends({
                                        { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                        { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                        { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                        { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                        { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                        { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                        { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                        { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                        { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                        { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
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
                                    }; -- rendezvous_hashing
                                  }; -- balancer2
                                }; -- hasher
                              }; -- test_rendezvous_hashing
                              test_hdrcgi = {
                                priority = 18;
                                match_fsm = {
                                  URI = "/hdrcgi";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                hdrcgi = {
                                  hdr_from_cgi = {
                                    ["X-Boo"] = "boo";
                                  }; -- hdr_from_cgi
                                  cgi_from_hdr = {
                                    creepy = "X-Creepy";
                                  }; -- cgi_from_hdr
                                  body_scan_limit = 123;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 2;
                                    rr = {
                                      unpack(gen_proxy_backends({
                                        { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                        { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                        { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                        { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                        { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                        { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                        { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                        { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                        { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                        { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
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
                                    }; -- rr
                                  }; -- balancer2
                                }; -- hdrcgi
                              }; -- test_hdrcgi
                              test_headerfunc_matcher = {
                                priority = 17;
                                match_fsm = {
                                  header = {
                                    name = "X-Strange-Header";
                                    value = get_str_env_var("TOKEN", "default_token");
                                  }; -- header
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "xxx";
                                  ranges = get_str_var("default_ranges");
                                  labels = {
                                    la = "papam";
                                    pum = "purum";
                                  }; -- labels
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  cryprox = {
                                    partner_token = "XXX";
                                    use_cryprox_matcher = {
                                      match_fsm = {
                                        path = ".*";
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    }; -- use_cryprox_matcher
                                    secrets_file = "./cryprox.txt";
                                    disable_file = "./xxx.txt";
                                    cryprox_backend = {
                                      errordocument = {
                                        status = 200;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- cryprox_backend
                                    service_backend = {
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
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
                                            { "ws39-272.search.yandex.net"; 1029; 1.000; "2a02:6b8:0:2502::2509:508a"; };
                                            { "ws39-386.search.yandex.net"; 1029; 1.000; "2a02:6b8:0:2502::2509:50c3"; };
                                            { "ws39-438.search.yandex.net"; 1034; 1.000; "2a02:6b8:0:2502::2509:50dd"; };
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
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- service_backend
                                  }; -- cryprox
                                }; -- report
                              }; -- test_headerfunc_matcher
                              test_l7_fast_upstream_macro = {
                                priority = 16;
                                match_fsm = {
                                  URI = "/%d0%bf%d1%80%d0%b8%d0%b2%d0%b5%d1%82";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "l7-fast";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "l7-fast_main";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 3;
                                                  connection_attempts = 5;
                                                  dynamic = {
                                                    max_pessimized_share = 0.100;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "l7-fast_main";
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 3;
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- test_l7_fast_upstream_macro
                              test_l7_fast_upstream_macro_2 = {
                                priority = 15;
                                match_fsm = {
                                  URI = "/proxy-last-5xx";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    return_last_5xx = true;
                                    status_code_blacklist = {
                                      "5xx";
                                    }; -- status_code_blacklist
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          return_last_5xx = true;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 4;
                                                  connection_attempts = 5;
                                                  return_last_5xx = true;
                                                  status_code_blacklist = {
                                                    "5xx";
                                                  }; -- status_code_blacklist
                                                  dynamic = {
                                                    max_pessimized_share = 0.100;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "l7-fast_main";
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = false;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          return_last_5xx = true;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 4;
                                                  connection_attempts = 5;
                                                  return_last_5xx = true;
                                                  status_code_blacklist = {
                                                    "5xx";
                                                  }; -- status_code_blacklist
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = false;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                  refers = "l7-fast";
                                }; -- report
                              }; -- test_l7_fast_upstream_macro_2
                              test_l7_fast_upstream_macro_3 = {
                                priority = 14;
                                match_fsm = {
                                  URI = "/proxy-first-5xx";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 5;
                                                  connection_attempts = 5;
                                                  dynamic = {
                                                    max_pessimized_share = 0.100;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "l7-fast_main";
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = false;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 5;
                                                  connection_attempts = 5;
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = false;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                  refers = "l7-fast";
                                }; -- report
                              }; -- test_l7_fast_upstream_macro_3
                              test_l7_fast_upstream_macro_4 = {
                                priority = 13;
                                match_fsm = {
                                  URI = "/rst-last-5xx";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 1;
                                    status_code_blacklist = {
                                      "5xx";
                                    }; -- status_code_blacklist
                                    rr = {
                                      weights_file = "./controls/dynamic_balancing_switch";
                                      dynamic_balancing_enabled = {
                                        weight = -1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 6;
                                                  connection_attempts = 5;
                                                  status_code_blacklist = {
                                                    "5xx";
                                                  }; -- status_code_blacklist
                                                  dynamic = {
                                                    max_pessimized_share = 0.100;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "l7-fast_main";
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = false;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_enabled
                                      dynamic_balancing_disabled = {
                                        weight = 1.000;
                                        balancer2 = {
                                          retry_policy = {
                                            unique_policy = {};
                                          }; -- retry_policy
                                          attempts = 2;
                                          status_code_blacklist = {
                                            "5xx";
                                          }; -- status_code_blacklist
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            main = {
                                              weight = 1.000;
                                              report = {
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  retry_policy = {
                                                    unique_policy = {};
                                                  }; -- retry_policy
                                                  attempts = 6;
                                                  connection_attempts = 5;
                                                  status_code_blacklist = {
                                                    "5xx";
                                                  }; -- status_code_blacklist
                                                  rr = {
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "10ms";
                                                      backend_timeout = "1500ms";
                                                      fail_on_5xx = false;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 3;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- rr
                                                  attempts_rate_limiter = {
                                                    limit = 0.100;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                                refers = "l7-fast_main";
                                              }; -- report
                                            }; -- main
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- dynamic_balancing_disabled
                                    }; -- rr
                                  }; -- balancer2
                                  refers = "l7-fast";
                                }; -- report
                              }; -- test_l7_fast_upstream_macro_4
                              test_l7_fast_sitemap_upstream_macro = {
                                priority = 12;
                                match_fsm = {
                                  URI = "/blogs/sitemap";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "blogs_sitemap";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  headers = {
                                    create = {
                                      Host = "my-bucket-id.s3.yandex.net";
                                    }; -- create
                                    shared = {
                                      uuid = "s3";
                                    }; -- shared
                                  }; -- headers
                                }; -- report
                              }; -- test_l7_fast_sitemap_upstream_macro
                              easy = {
                                priority = 11;
                                match_fsm = {
                                  path = ".*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "easy";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  headers = {
                                    create = {
                                      Host = "this.is.test";
                                    }; -- create
                                    headers = {
                                      create = {
                                        ["X-L7-EXP"] = "true";
                                      }; -- create
                                      exp_getter = {
                                        trusted = false;
                                        file_switch = "./controls/expgetter.switch";
                                        service_name = "mobsearch";
                                        service_name_header = "Y-Service";
                                        uaas = {
                                          shared = {
                                            uuid = "2699981958704336253";
                                          }; -- shared
                                        }; -- uaas
                                        rewrite = {
                                          actions = {
                                            {
                                              regexp = ".*";
                                              global = false;
                                              literal = false;
                                              case_insensitive = false;
                                              header_name = "Location";
                                              rewrite = "https://xxx.yandex-team.ru%{url}";
                                            };
                                          }; -- actions
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
                                            return_last_5xx = true;
                                            status_code_blacklist = {
                                              "5xx";
                                            }; -- status_code_blacklist
                                            status_code_blacklist_exceptions = {
                                              "503";
                                            }; -- status_code_blacklist_exceptions
                                            check_backends = {
                                              quorum = 0.350;
                                              name = "easy";
                                            }; -- check_backends
                                            dynamic = {
                                              max_pessimized_share = 0.200;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_vla";
                                              unpack(gen_proxy_backends({
                                                { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "20s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                                https_settings = {
                                                  ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                                  ca_file = get_ca_cert_path("allCAs.pem", "./");
                                                  sni_on = true;
                                                  verify_depth = 3;
                                                }; -- https_settings
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Service unavailable";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- rewrite
                                      }; -- exp_getter
                                    }; -- headers
                                  }; -- headers
                                }; -- report
                              }; -- easy
                              easy2 = {
                                priority = 10;
                                match_fsm = {
                                  host = "hey\\.yandex\\.ru";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "hey";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  headers = {
                                    create = {
                                      ["X-L7-EXP"] = "true";
                                    }; -- create
                                    exp_getter = {
                                      trusted = false;
                                      file_switch = "./controls/expgetter.switch";
                                      service_name = "mobsearch2";
                                      service_name_header = "Y-Service";
                                      uaas = {
                                        shared = {
                                          uuid = "2699981958704336253";
                                          report = {
                                            uuid = "expgetter";
                                            ranges = get_str_var("default_ranges");
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            stats_eater = {
                                              balancer2 = {
                                                by_name_policy = {
                                                  name = get_geo("bygeo_", "random");
                                                  simple_policy = {};
                                                }; -- by_name_policy
                                                attempts = 1;
                                                rr = {
                                                  bygeo_man = {
                                                    weight = 1.000;
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      connection_attempts = 5;
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "usersplit-1.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0b:3ae1:100:1101::1111"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "5ms";
                                                          backend_timeout = "10ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 1;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- bygeo_man
                                                  bygeo_sas = {
                                                    weight = 1.000;
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      connection_attempts = 5;
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "usersplit-2.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c08:6a21:100:47b::1111"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "5ms";
                                                          backend_timeout = "10ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 1;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- bygeo_sas
                                                  bygeo_vla = {
                                                    weight = 1.000;
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      connection_attempts = 5;
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "usersplit-3.gencfg-c.yandex.net"; 80; 1.000; "2a02:6b8:c0d:4fa5:10b:2909::1111"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "5ms";
                                                          backend_timeout = "10ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 1;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- bygeo_vla
                                                }; -- rr
                                                on_error = {
                                                  balancer2 = {
                                                    unique_policy = {};
                                                    attempts = 1;
                                                    rr = {
                                                      unpack(gen_proxy_backends({
                                                        { "uaas.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::120"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "20ms";
                                                        backend_timeout = "30ms";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 1;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- rr
                                                  }; -- balancer2
                                                }; -- on_error
                                              }; -- balancer2
                                            }; -- stats_eater
                                          }; -- report
                                        }; -- shared
                                      }; -- uaas
                                      geobase = {
                                        trusted = false;
                                        geo_host = "laas.yandex.ru";
                                        take_ip_from = "X-Forwarded-For-Y";
                                        laas_answer_header = "X-LaaS-Answered";
                                        file_switch = "./controls/disable_geobase.switch";
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
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          rr = {
                                            weights_file = "./controls/traffic_control.weights";
                                            hey_man = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "hey_to_man";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  dynamic = {
                                                    max_pessimized_share = 0.150;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "common-antirobot/antirobot_man";
                                                    active = {
                                                      request = "GET /ping.html HTTP/1.1\nHost: hey.yandex.ru\n\n";
                                                      delay = "10s";
                                                      weight_normalization_coeff = 0.000;
                                                      use_backend_weight = false;
                                                    }; -- active
                                                    unpack(gen_proxy_backends({
                                                      { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.150;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- report
                                            }; -- hey_man
                                            hey_sas = {
                                              weight = 30.000;
                                              report = {
                                                uuid = "hey_to_sas";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  dynamic = {
                                                    max_pessimized_share = 0.150;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "common-antirobot/antirobot_sas";
                                                    active = {
                                                      request = "GET /ping.html HTTP/1.1\nHost: hey.yandex.ru\n\n";
                                                      delay = "10s";
                                                      weight_normalization_coeff = 0.000;
                                                      use_backend_weight = false;
                                                    }; -- active
                                                    unpack(gen_proxy_backends({
                                                      { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.150;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- report
                                            }; -- hey_sas
                                            hey_vla = {
                                              weight = 1.000;
                                              report = {
                                                uuid = "hey_to_vla";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                balancer2 = {
                                                  unique_policy = {};
                                                  attempts = 2;
                                                  dynamic = {
                                                    max_pessimized_share = 0.150;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "common-antirobot/antirobot_vla";
                                                    active = {
                                                      request = "GET /ping.html HTTP/1.1\nHost: hey.yandex.ru\n\n";
                                                      delay = "10s";
                                                      weight_normalization_coeff = 0.000;
                                                      use_backend_weight = false;
                                                    }; -- active
                                                    unpack(gen_proxy_backends({
                                                      { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "100ms";
                                                      backend_timeout = "1s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                    }))
                                                  }; -- dynamic
                                                  attempts_rate_limiter = {
                                                    limit = 0.150;
                                                    coeff = 0.990;
                                                    switch_default = true;
                                                  }; -- attempts_rate_limiter
                                                }; -- balancer2
                                              }; -- report
                                            }; -- hey_vla
                                            hey_devnull = {
                                              weight = -1.000;
                                              report = {
                                                uuid = "requests_easy2_to_devnull";
                                                ranges = get_str_var("default_ranges");
                                                just_storage = false;
                                                disable_robotness = true;
                                                disable_sslness = true;
                                                events = {
                                                  stats = "report";
                                                }; -- events
                                                errordocument = {
                                                  status = 204;
                                                  force_conn_close = false;
                                                }; -- errordocument
                                              }; -- report
                                            }; -- hey_devnull
                                          }; -- rr
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                              content = "Service unavailable";
                                            }; -- errordocument
                                          }; -- on_error
                                          on_fast_error = {
                                            errordocument = {
                                              status = 502;
                                              content = "Bad gateway";
                                              force_conn_close = false;
                                            }; -- errordocument
                                          }; -- on_fast_error
                                        }; -- balancer2
                                      }; -- geobase
                                    }; -- exp_getter
                                  }; -- headers
                                }; -- report
                              }; -- easy2
                              easy3 = {
                                priority = 9;
                                match_and = {
                                  {
                                    match_fsm = {
                                      host = "hey3\\.yandex\\.ru";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_fsm = {
                                      header = {
                                        name = "X-Bububu";
                                        value = ".*";
                                      }; -- header
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_not = {
                                      match_fsm = {
                                        header = {
                                          name = "X-Lalala";
                                          value = ".*";
                                        }; -- header
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    }; -- match_not
                                  };
                                }; -- match_and
                                report = {
                                  uuid = "easy3";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      hey2_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy3_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.200;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_man";
                                              unpack(gen_proxy_backends({
                                                { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = true;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- hey2_man
                                      hey2_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy3_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.200;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_sas";
                                              unpack(gen_proxy_backends({
                                                { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = true;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- hey2_sas
                                      hey2_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy3_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.200;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_vla";
                                              unpack(gen_proxy_backends({
                                                { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = true;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- hey2_vla
                                      hey2_devnull = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "requests_easy3_to_devnull";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          errordocument = {
                                            status = 204;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- report
                                      }; -- hey2_devnull
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- easy3
                              easy4 = {
                                priority = 8;
                                match_or = {
                                  {
                                    match_fsm = {
                                      host = "hey4\\.yandex\\.ru";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  };
                                  {
                                    match_method = {
                                      methods = { "post"; };
                                    }; -- match_method
                                  };
                                }; -- match_or
                                report = {
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      hey2_man = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.500;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_man";
                                              unpack(gen_proxy_backends({
                                                { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                          refers = "requests_easy3_to_man";
                                        }; -- report
                                      }; -- hey2_man
                                      hey2_sas = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.500;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_sas";
                                              unpack(gen_proxy_backends({
                                                { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                          refers = "requests_easy3_to_sas";
                                        }; -- report
                                      }; -- hey2_sas
                                      hey2_vla = {
                                        weight = 1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            dynamic = {
                                              max_pessimized_share = 0.500;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_vla";
                                              unpack(gen_proxy_backends({
                                                { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                          refers = "requests_easy3_to_vla";
                                        }; -- report
                                      }; -- hey2_vla
                                      hey2_devnull = {
                                        weight = -1.000;
                                        report = {
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          errordocument = {
                                            status = 201;
                                            force_conn_close = false;
                                          }; -- errordocument
                                          refers = "requests_easy3_to_devnull";
                                        }; -- report
                                      }; -- hey2_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 201;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                  refers = "easy3";
                                }; -- report
                              }; -- easy4
                              easy5 = {
                                priority = 7;
                                match_fsm = {
                                  host = "easy5\\.yandex\\.ru";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                balancer2 = {
                                  by_name_policy = {
                                    name = get_geo("easy5_", "random");
                                    unique_policy = {};
                                  }; -- by_name_policy
                                  attempts = 3;
                                  rr = {
                                    weights_file = "./controls/traffic_control.weights";
                                    easy5_man = {
                                      weight = 1.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        dynamic = {
                                          max_pessimized_share = 0.500;
                                          min_pessimization_coeff = 0.100;
                                          weight_increase_step = 0.100;
                                          history_interval = "10s";
                                          backends_name = "common-antirobot/antirobot_man";
                                          unpack(gen_proxy_backends({
                                            { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                            https_settings = {
                                              ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                              ca_file = get_ca_cert_path("allCAs.pem", "./");
                                              sni_on = false;
                                              verify_depth = 3;
                                            }; -- https_settings
                                          }))
                                        }; -- dynamic
                                        attempts_rate_limiter = {
                                          limit = 0.500;
                                          coeff = 0.990;
                                          switch_default = true;
                                        }; -- attempts_rate_limiter
                                      }; -- balancer2
                                    }; -- easy5_man
                                    easy5_sas = {
                                      weight = 1.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        dynamic = {
                                          max_pessimized_share = 0.500;
                                          min_pessimization_coeff = 0.100;
                                          weight_increase_step = 0.100;
                                          history_interval = "10s";
                                          backends_name = "common-antirobot/antirobot_sas";
                                          unpack(gen_proxy_backends({
                                            { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                            https_settings = {
                                              ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                              ca_file = get_ca_cert_path("allCAs.pem", "./");
                                              sni_on = false;
                                              verify_depth = 3;
                                            }; -- https_settings
                                          }))
                                        }; -- dynamic
                                        attempts_rate_limiter = {
                                          limit = 0.500;
                                          coeff = 0.990;
                                          switch_default = true;
                                        }; -- attempts_rate_limiter
                                      }; -- balancer2
                                    }; -- easy5_sas
                                    easy5_vla = {
                                      weight = 1.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        dynamic = {
                                          max_pessimized_share = 0.500;
                                          min_pessimization_coeff = 0.100;
                                          weight_increase_step = 0.100;
                                          history_interval = "10s";
                                          backends_name = "common-antirobot/antirobot_vla";
                                          unpack(gen_proxy_backends({
                                            { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "1s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                            https_settings = {
                                              ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                              ca_file = get_ca_cert_path("allCAs.pem", "./");
                                              sni_on = false;
                                              verify_depth = 3;
                                            }; -- https_settings
                                          }))
                                        }; -- dynamic
                                        attempts_rate_limiter = {
                                          limit = 0.500;
                                          coeff = 0.990;
                                          switch_default = true;
                                        }; -- attempts_rate_limiter
                                      }; -- balancer2
                                    }; -- easy5_vla
                                    easy5_devnull = {
                                      weight = -1.000;
                                      errordocument = {
                                        status = 204;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- easy5_devnull
                                  }; -- rr
                                  on_error = {
                                    errordocument = {
                                      status = 201;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- on_error
                                }; -- balancer2
                              }; -- easy5
                              easy6 = {
                                priority = 6;
                                match_fsm = {
                                  host = "easy6\\.yandex\\.ru";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                rewrite = {
                                  actions = {
                                    {
                                      regexp = ".*";
                                      split = "url";
                                      global = false;
                                      literal = false;
                                      rewrite = "\"xxx\"";
                                      case_insensitive = true;
                                    };
                                    {
                                      global = true;
                                      split = "cgi";
                                      regexp = "\\.";
                                      literal = false;
                                      rewrite = "yyy";
                                      case_insensitive = false;
                                    };
                                    {
                                      regexp = "/";
                                      global = false;
                                      literal = true;
                                      rewrite = "\\";
                                      split = "path";
                                      case_insensitive = true;
                                    };
                                  }; -- actions
                                  rewrite = {
                                    actions = {
                                      {
                                        global = true;
                                        literal = true;
                                        regexp = "123";
                                        rewrite = "456";
                                        case_insensitive = true;
                                        header_name = "X-Random-Header";
                                      };
                                    }; -- actions
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
                                            rewrite = "https://xxx.yandex-team.ru%{url}";
                                          };
                                          {
                                            regexp = ".*";
                                            global = false;
                                            literal = false;
                                            case_insensitive = true;
                                            header_name = "X-Location";
                                            rewrite = "https://xxx.yandex-team.ru%{url}";
                                          };
                                        }; -- actions
                                        errordocument = {
                                          status = 301;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- rewrite
                                    }; -- headers
                                  }; -- rewrite
                                }; -- rewrite
                              }; -- easy6
                              easy7 = {
                                priority = 5;
                                match_fsm = {
                                  host = "easy7\\.yandex\\.ru";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "easy7";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      easy7_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy7_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 1;
                                            dynamic = {
                                              max_pessimized_share = 0.500;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_man";
                                              unpack(gen_proxy_backends({
                                                { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- easy7_man
                                      easy7_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy7_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 1;
                                            dynamic = {
                                              max_pessimized_share = 0.500;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_sas";
                                              unpack(gen_proxy_backends({
                                                { "sas1-0281.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:151:225:90ff:fe83:8d4"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- easy7_sas
                                      easy7_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy7_to_vla";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 1;
                                            dynamic = {
                                              max_pessimized_share = 0.500;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "common-antirobot/antirobot_vla";
                                              unpack(gen_proxy_backends({
                                                { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = true;
                                              }))
                                            }; -- dynamic
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- easy7_vla
                                      easy7_devnull = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "requests_easy7_to_devnull";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          errordocument = {
                                            status = 204;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- report
                                      }; -- easy7_devnull
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 201;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- report
                              }; -- easy7
                              easy8 = {
                                priority = 4;
                                match_fsm = {
                                  host = "easy8\\.yandex\\.ru";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "easy8";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  shared = {
                                    uuid = "backends";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 1;
                                      rr = {
                                        weights_file = "./controls/traffic_control.weights";
                                        easy8_man = {
                                          weight = 1.000;
                                          report = {
                                            uuid = "requests_easy8_to_man";
                                            ranges = get_str_var("default_ranges");
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = "count_backends";
                                              fast_attempts = "count_backends";
                                              fast_503 = true;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "man";
                                                    endpoint_set_id = "rpslimiter-serval-man-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "100ms";
                                                  backend_timeout = "1s";
                                                  fail_on_5xx = false;
                                                  http_backend = true;
                                                  buffering = true;
                                                  keepalive_count = 4;
                                                  need_resolve = false;
                                                  status_code_blacklist = {
                                                    "503";
                                                  }; -- status_code_blacklist
                                                  backend_read_timeout = "1s";
                                                  client_read_timeout = "3s";
                                                  allow_connection_upgrade = true;
                                                  backend_write_timeout = "2s";
                                                  client_write_timeout = "4s";
                                                }; -- proxy_options
                                                dynamic = {
                                                  max_pessimized_share = 0.500;
                                                  min_pessimization_coeff = 0.100;
                                                  weight_increase_step = 0.100;
                                                  history_interval = "10s";
                                                  backends_name = "common-rpslimiter/rpslimiter-serval-man-sd";
                                                }; -- dynamic
                                              }; -- sd
                                              attempts_rate_limiter = {
                                                limit = 0.500;
                                                coeff = 0.990;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- report
                                        }; -- easy8_man
                                      }; -- rr
                                      on_error = {
                                        errordocument = {
                                          status = 201;
                                          force_conn_close = false;
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- shared
                                }; -- report
                              }; -- easy8
                              easy9 = {
                                priority = 3;
                                match_or = {
                                  {
                                    match_and = {
                                      {
                                        match_fsm = {
                                          host = "easy9\\.yandex\\.ru";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          path = "/xxx/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_and
                                  };
                                  {
                                    match_and = {
                                      {
                                        match_fsm = {
                                          host = "easy9\\.yandex\\.com";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                      {
                                        match_fsm = {
                                          path = "/yyy/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                      };
                                    }; -- match_and
                                  };
                                  {
                                    match_not = {
                                      match_fsm = {
                                        header = {
                                          name = "X";
                                          value = ".*";
                                        }; -- header
                                        case_insensitive = true;
                                        surround = false;
                                      }; -- match_fsm
                                    }; -- match_not
                                  };
                                }; -- match_or
                                report = {
                                  uuid = "easy9";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 2;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      easy9_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy9_to_man";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = "count_backends";
                                            fast_attempts = "count_backends";
                                            fast_503 = true;
                                            check_backends = {
                                              quorum = 0.350;
                                              name = "easy9#man";
                                            }; -- check_backends
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "man";
                                                  endpoint_set_id = "rpslimiter-serval-man-sd";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = false;
                                                status_code_blacklist = {
                                                  "503";
                                                }; -- status_code_blacklist
                                              }; -- proxy_options
                                              dynamic = {
                                                max_pessimized_share = 0.500;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "common-rpslimiter/rpslimiter-serval-man-sd";
                                              }; -- dynamic
                                            }; -- sd
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- easy9_man
                                      easy9_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_easy9_to_sas";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = "count_backends";
                                            fast_attempts = "count_backends";
                                            fast_503 = true;
                                            check_backends = {
                                              quorum = 0.350;
                                              name = "easy9#sas";
                                            }; -- check_backends
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "sas";
                                                  endpoint_set_id = "rpslimiter-serval-sas-sd";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "100ms";
                                                backend_timeout = "1s";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = false;
                                                status_code_blacklist = {
                                                  "503";
                                                }; -- status_code_blacklist
                                              }; -- proxy_options
                                              dynamic = {
                                                max_pessimized_share = 0.500;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "common-rpslimiter/rpslimiter-serval-sas-sd";
                                              }; -- dynamic
                                            }; -- sd
                                            attempts_rate_limiter = {
                                              limit = 0.500;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- easy9_sas
                                    }; -- rr
                                    on_error = {
                                      errordocument = {
                                        status = 201;
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- on_error
                                  }; -- balancer2
                                }; -- report
                              }; -- easy9
                              easy10 = {
                                priority = 2;
                                match_not = {
                                  match_not = {
                                    match_fsm = {
                                      path = ".*";
                                      case_insensitive = true;
                                      surround = false;
                                    }; -- match_fsm
                                  }; -- match_not
                                }; -- match_not
                                report = {
                                  uuid = "easy10";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  headers_forwarder = {
                                    actions = {
                                      {
                                        request_header = "X-Test";
                                        response_header = "X-Test";
                                        erase_from_request = false;
                                        erase_from_response = true;
                                        weak = false;
                                      };
                                    }; -- actions
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 3;
                                      return_last_5xx = true;
                                      status_code_blacklist = {
                                        "5xx";
                                      }; -- status_code_blacklist
                                      status_code_blacklist_exceptions = {
                                        "503";
                                      }; -- status_code_blacklist_exceptions
                                      check_backends = {
                                        quorum = 0.350;
                                        name = "easy10";
                                      }; -- check_backends
                                      dynamic = {
                                        max_pessimized_share = 0.200;
                                        min_pessimization_coeff = 0.100;
                                        weight_increase_step = 0.100;
                                        history_interval = "10s";
                                        backends_name = "common-antirobot/antirobot_vla";
                                        unpack(gen_proxy_backends({
                                          { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "20s";
                                          fail_on_5xx = false;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- dynamic
                                      attempts_rate_limiter = {
                                        limit = 0.200;
                                        coeff = 0.990;
                                        switch_default = true;
                                      }; -- attempts_rate_limiter
                                      on_error = {
                                        errordocument = {
                                          status = 504;
                                          force_conn_close = false;
                                          content = "Service unavailable";
                                        }; -- errordocument
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- headers_forwarder
                                }; -- report
                              }; -- easy10
                              default = {
                                priority = 1;
                                regexp = {
                                  exp_testing = {
                                    priority = 2;
                                    match_fsm = {
                                      cgi = "(exp-testing=da|exp_confs=testing)";
                                      case_insensitive = true;
                                      surround = true;
                                    }; -- match_fsm
                                    headers = {
                                      create = {
                                        ["X-L7-EXP-Testing"] = "true";
                                      }; -- create
                                      shared = {
                                        uuid = "4635249079502574784";
                                        exp_getter = {
                                          trusted = false;
                                          file_switch = "./controls/expgetter.switch";
                                          uaas = {
                                            shared = {
                                              uuid = "2699981958704336253";
                                            }; -- shared
                                          }; -- uaas
                                          stats_eater = {
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              check_backends = {
                                                quorum = 0.350;
                                                name = "mobile_heroism";
                                              }; -- check_backends
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
                                                  { "ws33-340.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9cb1"; };
                                                  { "ws34-487.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:f12::b29a:9ffa"; };
                                                  { "ws35-290.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8493"; };
                                                  { "ws35-658.search.yandex.net"; 20752; 683.000; "2a02:6b8:0:160b::b29a:8651"; };
                                                  { "ws40-413.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81d0"; };
                                                  { "ws40-449.search.yandex.net"; 20752; 1293.000; "2a02:6b8:0:2502::258c:81e2"; };
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
                                          }; -- stats_eater
                                        }; -- exp_getter
                                      }; -- shared
                                    }; -- headers
                                  }; -- exp_testing
                                  default = {
                                    priority = 1;
                                    headers = {
                                      create = {
                                        ["X-L7-EXP"] = "true";
                                      }; -- create
                                      shared = {
                                        uuid = "4635249079502574784";
                                      }; -- shared
                                    }; -- headers
                                  }; -- default
                                }; -- regexp
                              }; -- default
                            }; -- regexp
                          }; -- shared
                        }; -- cookies
                      }; -- response_headers
                    }; -- headers
                  }; -- headers_forwarder
                }; -- response_headers_if
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15220 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15220;
      }; -- ports
      shared = {
        uuid = "4399850908810723262";
      }; -- shared
    }; -- http_section_15220
    https_section_443 = {
      ips = {
        "2a02:6b8::1:62";
        "2a02:6b8::1:63";
      }; -- ips
      ports = {
        443;
      }; -- ports
      shared = {
        uuid = "8057385718451102696";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15221, "/place/db/www/logs/");
          ssl_sni = {
            force_ssl = true;
            events = {
              stats = "report";
              reload_ocsp_response = "reload_ocsp";
              reload_ticket_keys = "reload_ticket";
            }; -- events
            max_send_fragment = 1024;
            ja3_enabled = true;
            contexts = {
              default = {
                priority = 1;
                timeout = "100800s";
                disable_sslv3 = true;
                disable_tlsv1_3 = false;
                ciphers = get_str_var("default_ciphers");
                log = get_log_path("ssl_sni", 15221, "/place/db/www/logs/");
                priv = get_private_cert_path("beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer/priv");
                cert = get_public_cert_path("allCAs-beta.mobsearch.yandex.ru.pem", "/dev/shm/balancer");
                ticket_keys_list = {
                  {
                    priority = 3;
                    keyfile = get_private_cert_path("1st.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 2;
                    keyfile = get_private_cert_path("2nd.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
                  };
                  {
                    priority = 1;
                    keyfile = get_private_cert_path("3rd.beta.mobsearch.yandex.ru.key", "/dev/shm/balancer/priv");
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
                log = get_log_path("access_log", 15221, "/place/db/www/logs/");
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
                  response_headers_if = {
                    if_has_header = "X-Yandex-STS";
                    erase_if_has_header = true;
                    create_header = {
                      ["Strict-Transport-Security"] = "max-age=600";
                    }; -- create_header
                    headers_forwarder = {
                      actions = {
                        {
                          request_header = "Origin";
                          response_header = "Access-Control-Allow-Origin";
                          erase_from_request = true;
                          erase_from_response = true;
                          weak = false;
                        };
                      }; -- actions
                      headers = {
                        create_func = {
                          ["X-Req-Id"] = "reqid";
                          ["X-Source-Port-Y"] = "realport";
                          ["X-Start-Time"] = "starttime";
                          ["X-Yandex-RandomUID"] = "yuid";
                        }; -- create_func
                        create_func_weak = {
                          ["X-Forwarded-For"] = "realip";
                          ["X-Forwarded-For-Y"] = "realip";
                        }; -- create_func_weak
                        response_headers = {
                          create_weak = {
                            ["X-Content-Type-Options"] = "nosniff";
                            ["X-XSS-Protection"] = "1; mode=block";
                          }; -- create_weak
                          cookies = {
                            delete = ".*cookie1.*";
                            create = {
                              cookie2 = "value2";
                            }; -- create
                            create_weak = {
                              cookie3 = "value3";
                            }; -- create_weak
                            cookie_policy = {
                              uuid = "very-test";
                              default_yandex_policies = "off";
                              headers = {
                                create = {
                                  ["x-rpslimiter-geo"] = get_geo("", "unknown");
                                }; -- create
                                rps_limiter = {
                                  skip_on_error = true;
                                  disable_file = "./controls/rps_limiter_disabled";
                                  namespace = "beta.mobsearch.yandex.ru";
                                  checker = {
                                    balancer2 = {
                                      by_name_policy = {
                                        name = get_geo("rpslimiter_", "random");
                                        unique_policy = {};
                                      }; -- by_name_policy
                                      attempts = 1;
                                      rr = {
                                        rpslimiter_man = {
                                          weight = 1.000;
                                          report = {
                                            uuid = "rpslimiter-man";
                                            ranges = get_str_var("default_ranges");
                                            outgoing_codes = "429";
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "man";
                                                    endpoint_set_id = "rpslimiter-serval-man-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "50ms";
                                                  backend_timeout = "200ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
                                                rr = {};
                                              }; -- sd
                                              attempts_rate_limiter = {
                                                limit = 0.150;
                                                coeff = 0.990;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- report
                                        }; -- rpslimiter_man
                                        rpslimiter_sas = {
                                          weight = 1.000;
                                          report = {
                                            uuid = "rpslimiter-sas";
                                            ranges = get_str_var("default_ranges");
                                            outgoing_codes = "429";
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "sas";
                                                    endpoint_set_id = "rpslimiter-serval-sas-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "50ms";
                                                  backend_timeout = "200ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
                                                rr = {};
                                              }; -- sd
                                              attempts_rate_limiter = {
                                                limit = 0.150;
                                                coeff = 0.990;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- report
                                        }; -- rpslimiter_sas
                                        rpslimiter_vla = {
                                          weight = 1.000;
                                          report = {
                                            uuid = "rpslimiter-vla";
                                            ranges = get_str_var("default_ranges");
                                            outgoing_codes = "429";
                                            just_storage = false;
                                            disable_robotness = true;
                                            disable_sslness = true;
                                            events = {
                                              stats = "report";
                                            }; -- events
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              sd = {
                                                endpoint_sets = {
                                                  {
                                                    cluster_name = "vla";
                                                    endpoint_set_id = "rpslimiter-serval-vla-sd";
                                                  };
                                                }; -- endpoint_sets
                                                proxy_options = {
                                                  resolve_timeout = "10ms";
                                                  connect_timeout = "50ms";
                                                  backend_timeout = "200ms";
                                                  fail_on_5xx = true;
                                                  http_backend = true;
                                                  buffering = false;
                                                  keepalive_count = 0;
                                                  need_resolve = false;
                                                }; -- proxy_options
                                                rr = {};
                                              }; -- sd
                                              attempts_rate_limiter = {
                                                limit = 0.150;
                                                coeff = 0.990;
                                                switch_default = true;
                                              }; -- attempts_rate_limiter
                                            }; -- balancer2
                                          }; -- report
                                        }; -- rpslimiter_vla
                                        rpslimiter_myt = {
                                          weight = 1.000;
                                          shared = {
                                            uuid = "703037500452280184";
                                            balancer2 = {
                                              unique_policy = {};
                                              attempts = 2;
                                              rr = {
                                                rpslimiter_man = {
                                                  weight = 1.000;
                                                  report = {
                                                    ranges = get_str_var("default_ranges");
                                                    outgoing_codes = "429";
                                                    just_storage = false;
                                                    disable_robotness = true;
                                                    disable_sslness = true;
                                                    events = {
                                                      stats = "report";
                                                    }; -- events
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "man";
                                                            endpoint_set_id = "rpslimiter-serval-man-sd";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "50ms";
                                                          backend_timeout = "200ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        rr = {};
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.150;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                    refers = "rpslimiter-man";
                                                  }; -- report
                                                }; -- rpslimiter_man
                                                rpslimiter_sas = {
                                                  weight = 1.000;
                                                  report = {
                                                    ranges = get_str_var("default_ranges");
                                                    outgoing_codes = "429";
                                                    just_storage = false;
                                                    disable_robotness = true;
                                                    disable_sslness = true;
                                                    events = {
                                                      stats = "report";
                                                    }; -- events
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "sas";
                                                            endpoint_set_id = "rpslimiter-serval-sas-sd";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "50ms";
                                                          backend_timeout = "200ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        rr = {};
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.150;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                    refers = "rpslimiter-sas";
                                                  }; -- report
                                                }; -- rpslimiter_sas
                                                rpslimiter_vla = {
                                                  weight = 1.000;
                                                  report = {
                                                    ranges = get_str_var("default_ranges");
                                                    outgoing_codes = "429";
                                                    just_storage = false;
                                                    disable_robotness = true;
                                                    disable_sslness = true;
                                                    events = {
                                                      stats = "report";
                                                    }; -- events
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 1;
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "vla";
                                                            endpoint_set_id = "rpslimiter-serval-vla-sd";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "50ms";
                                                          backend_timeout = "200ms";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        rr = {};
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.150;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                    refers = "rpslimiter-vla";
                                                  }; -- report
                                                }; -- rpslimiter_vla
                                              }; -- rr
                                            }; -- balancer2
                                          }; -- shared
                                        }; -- rpslimiter_myt
                                        rpslimiter_iva = {
                                          weight = 1.000;
                                          shared = {
                                            uuid = "703037500452280184";
                                          }; -- shared
                                        }; -- rpslimiter_iva
                                      }; -- rr
                                    }; -- balancer2
                                  }; -- checker
                                  module = {
                                    shared = {
                                      uuid = "5262399783298041165";
                                    }; -- shared
                                  }; -- module
                                }; -- rps_limiter
                              }; -- headers
                            }; -- cookie_policy
                          }; -- cookies
                        }; -- response_headers
                      }; -- headers
                    }; -- headers_forwarder
                  }; -- response_headers_if
                }; -- report
              }; -- accesslog
            }; -- http
          }; -- ssl_sni
        }; -- errorlog
      }; -- shared
    }; -- https_section_443
    https_section_15221 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15221;
      }; -- ports
      shared = {
        uuid = "8057385718451102696";
      }; -- shared
    }; -- https_section_15221
  }; -- ipdispatch
}
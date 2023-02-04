default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function _call_func_providers(overridable_func_names)
  for _, func_name in pairs(overridable_func_names) do
    local func_provider_path = _G[func_name .. "_provider"]
    if func_provider_path ~= nil then
      local env = {}
      setmetatable(env, {__index = _G})
      local provider, err = loadfile(func_provider_path, nil, env)
      if provider == nil then
        error(string.format('Failed to import provider "%s": %s', func_provider_path, err))
      end
      ok, rv = pcall(provider)
      if ok then
        if type(rv) ~= 'function' then
          error(string.format('Provider "%s" must return a function, not %s.', func_provider_path, type(rv)))
        end
        _G["do_" .. func_name] = rv
      else
        error(string.format('Provider "%s" failed: %s', func_provider_path, rv))
      end
    end
  end
end


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


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


function do_get_workers()
  -- actual get_workers() implementation, can be overridden
  value = _G["workers"]
  if value == nil then
    error('Variable "workers" is not specified.')
  end
  int_value = tonumber(value)
  if int_value == nil then
    error('Could not cast variable "workers" to a number.')
  end
  return int_value
end


function get_workers()
  value = do_get_workers()
  if type(value) ~= 'number' then
    error(string.format('Provided get_workers() implementation must return a number, not %s.', type(value)))
  end
  if value < 0 or value % 1 ~= 0 then
    error(string.format('Provided get_workers() implementation must return a non-negative integer, not %s', value))
  end
  return value
end


_call_func_providers({
  "get_workers";
})


instance = {
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  pinger_required = true;
  tcp_listen_queue = 128;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  state_directory = "/dev/shm/balancer-state";
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("port"), "/place/db/www/logs");
  pinger_log = get_log_path("pinger_log", get_port_var("port"), "/place/db/www/logs/");
  dynamic_balancing_log = get_log_path("dynamic_balancing_log", get_port_var("port"), "/place/db/www/logs/");
  admin_addrs = {
    {
      ip = "127.0.0.1";
      port = get_port_var("port");
    };
    {
      ip = "::1";
      port = get_port_var("port");
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
  }; -- unistat
  sd = {
    client_name = "unknown-awacs-l7-balancer";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  addrs = {
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 443;
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
        get_port_var("port");
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
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 80, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 80, "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                ["awacs-balancer-health-check"] = {
                  priority = 3;
                  match_fsm = {
                    URI = "/awacs-balancer-health-check";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  errordocument = {
                    status = 200;
                    force_conn_close = false;
                  }; -- errordocument
                }; -- ["awacs-balancer-health-check"]
                slbping = {
                  priority = 2;
                  match_fsm = {
                    url = "/ping";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "1683180570280210434";
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
                              status = 503;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- switch_off
                        }; -- rr
                      }; -- balancer2
                    }; -- stats_eater
                  }; -- shared
                }; -- slbping
                default = {
                  priority = 1;
                  regexp_host = {
                    domain = {
                      priority = 2;
                      pattern = "(http_and_https_0)|(http_and_https_1)|(http_and_https_2)|(http_and_https_3)|(http_and_https_4)|(http_and_https_5)|(http_and_https_6)|(http_and_https_7)|(http_and_https_8)|(http_and_https_9)|(http_and_https_10)|(http_and_https_11)|(http_and_https_12)|(http_and_https_13)|(http_and_https_14)|(http_and_https_15)|(http_and_https_16)|(http_and_https_17)|(http_and_https_18)|(http_and_https_19)|(http_and_https_20)|(http_and_https_21)|(http_and_https_22)|(http_and_https_23)|(http_and_https_24)|(http_and_https_25)|(http_and_https_26)|(http_and_https_27)|(http_and_https_28)|(http_and_https_29)|(http_and_https_30)|(http_and_https_31)|(http_and_https_32)|(http_and_https_33)|(http_and_https_34)|(http_and_https_35)|(http_and_https_36)|(http_and_https_37)|(http_and_https_38)|(http_and_https_39)|(http_and_https_40)|(http_and_https_41)|(http_and_https_42)|(http_and_https_43)|(http_and_https_44)|(http_and_https_45)|(http_and_https_46)|(http_and_https_47)|(http_and_https_48)|(http_and_https_49)|(http_and_https_50)|(http_and_https_51)|(http_and_https_52)|(http_and_https_53)|(http_and_https_54)|(http_and_https_55)|(http_and_https_56)|(http_and_https_57)|(http_and_https_58)|(http_and_https_59)|(http_and_https_60)|(http_and_https_61)|(http_and_https_62)|(http_and_https_63)|(http_and_https_64)|(http_and_https_65)|(http_and_https_66)|(http_and_https_67)|(http_and_https_68)|(http_and_https_69)|(http_and_https_70)|(http_and_https_71)|(http_and_https_72)|(http_and_https_73)|(http_and_https_74)|(http_and_https_75)|(http_and_https_76)|(http_and_https_77)|(http_and_https_78)|(http_and_https_79)|(http_and_https_80)|(http_and_https_81)|(http_and_https_82)|(http_and_https_83)|(http_and_https_84)|(http_and_https_85)|(http_and_https_86)|(http_and_https_87)|(http_and_https_88)|(http_and_https_89)|(http_and_https_90)|(http_and_https_91)|(http_and_https_92)|(http_and_https_93)|(http_and_https_94)|(http_and_https_95)|(http_and_https_96)|(http_and_https_97)|(http_and_https_98)|(http_and_https_99)|(http_and_https_100)|(shadow_http_and_https)(:80)?";
                      case_insensitive = true;
                      shared = {
                        uuid = "7006341551556367040";
                        regexp = {
                          configsearch = {
                            priority = 3;
                            match_fsm = {
                              URI = "/touchsearch(/.*)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "uuid1";
                              refers = "uuid2";
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
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
                                      file_switch = "./controls/touchsearch_disable_antirobot_module";
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
                                                  { "antirobot.man.yandex.ru"; 80; 1.000; "2a02:6b8::4"; };
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
                                        stats_eater = {
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            fast_attempts = 1;
                                            fast_503 = true;
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
                                                { "mobile_heroism.yandex.ru"; 80; 1.000; "2a02:6b8::3"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "10ms";
                                                backend_timeout = "1s";
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
                                        }; -- stats_eater
                                      }; -- module
                                    }; -- antirobot
                                  }; -- cutter
                                }; -- h100
                              }; -- hasher
                            }; -- report
                          }; -- configsearch
                          easy = {
                            priority = 2;
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
                                    use_on_error_for_non_idempotent = true;
                                    return_last_5xx = true;
                                    status_code_blacklist = {
                                      "5xx";
                                    }; -- status_code_blacklist
                                    status_code_blacklist_exceptions = {
                                      "503";
                                    }; -- status_code_blacklist_exceptions
                                    sd = {
                                      endpoint_sets = {
                                        {
                                          cluster_name = "man";
                                          endpoint_set_id = "xxx";
                                        };
                                      }; -- endpoint_sets
                                      proxy_options = {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "20s";
                                        fail_on_5xx = false;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = false;
                                        https_settings = {
                                          ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                          ca_file = get_ca_cert_path("allCAs.pem", "./");
                                          sni_on = true;
                                          verify_depth = 3;
                                        }; -- https_settings
                                        watch_client_close = true;
                                      }; -- proxy_options
                                      dynamic = {
                                        max_pessimized_share = 0.200;
                                        min_pessimization_coeff = 0.100;
                                        weight_increase_step = 0.100;
                                        history_interval = "10s";
                                        backends_name = "xxx";
                                      }; -- dynamic
                                    }; -- sd
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
                                    on_fast_error = {
                                      errordocument = {
                                        status = 509;
                                        content = "Nobody knows";
                                        force_conn_close = false;
                                      }; -- errordocument
                                    }; -- on_fast_error
                                  }; -- balancer2
                                }; -- rewrite
                              }; -- headers
                            }; -- report
                          }; -- easy
                          jsonproxy = {
                            priority = 1;
                            match_fsm = {
                              URI = "/((m)?search/)?jsonproxy(/.*)?";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            report = {
                              uuid = "uuid2";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              report = {
                                refers = "uuid2,uuid1";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
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
                                          report = {
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
                                                    { "antirobot_iss_prestable.yandex.ru"; 80; 1.000; "2a02:6b8::5"; };
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
                                            refers = "antirobot";
                                          }; -- report
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
                                              cachalot = {
                                                collection = "abc";
                                                cacher = {
                                                  errordocument = {
                                                    status = 200;
                                                    force_conn_close = false;
                                                  }; -- errordocument
                                                }; -- cacher
                                                balancer2 = {
                                                  timeout_policy = {
                                                    timeout = "1s";
                                                    by_name_policy = {
                                                      name = "xxx";
                                                      allow_zero_weights = true;
                                                      strict = true;
                                                      unique_policy = {};
                                                    }; -- by_name_policy
                                                  }; -- timeout_policy
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
                                                      { "mobile_heroism.yandex.ru"; 80; 1.000; "2a02:6b8::3"; };
                                                    }, {
                                                      resolve_timeout = "10ms";
                                                      connect_timeout = "150ms";
                                                      backend_timeout = "5s";
                                                      fail_on_5xx = true;
                                                      http_backend = true;
                                                      buffering = false;
                                                      keepalive_count = 0;
                                                      need_resolve = true;
                                                      switched_backend_timeout = "123ms";
                                                      backend_read_timeout = "10s";
                                                      client_read_timeout = "8s";
                                                      backend_write_timeout = "12s";
                                                      client_write_timeout = "18s";
                                                    }))
                                                  }; -- weighted2
                                                  on_error = {
                                                    errordocument = {
                                                      status = 504;
                                                      force_conn_close = false;
                                                    }; -- errordocument
                                                  }; -- on_error
                                                }; -- balancer2
                                              }; -- cachalot
                                            }; -- stats_eater
                                          }; -- rewrite
                                        }; -- module
                                      }; -- antirobot
                                    }; -- cutter
                                  }; -- h100
                                }; -- hasher
                              }; -- report
                            }; -- report
                          }; -- jsonproxy
                        }; -- regexp
                      }; -- shared
                    }; -- domain
                    domain2 = {
                      priority = 1;
                      pattern = "(http_and_https_1000)(:80)?";
                      case_insensitive = true;
                      shared = {
                        uuid = "7006341551556367040";
                      }; -- shared
                    }; -- domain2
                  }; -- regexp_host
                }; -- default
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
    https_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        443;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 443, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          ja3_enabled = true;
          contexts = {
            ["adm-nanny.yandex-team.ru"] = {
              priority = 2;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              ca = get_ca_cert_path("clientCAs.pem", "./");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("adm-nanny.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-adm-nanny.yandex-team.ru.pem", "/dev/shm/balancer");
              client = {
                verify_peer = true;
                verify_depth = 3;
                verify_once = true;
                fail_if_no_peer_cert = false;
              }; -- client
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(http_and_https_0)|(http_and_https_1)|(http_and_https_2)|(http_and_https_3)|(http_and_https_4)|(http_and_https_5)|(http_and_https_6)|(http_and_https_7)|(http_and_https_8)|(http_and_https_9)|(http_and_https_10)|(http_and_https_11)|(http_and_https_12)|(http_and_https_13)|(http_and_https_14)|(http_and_https_15)|(http_and_https_16)|(http_and_https_17)|(http_and_https_18)|(http_and_https_19)|(http_and_https_20)|(http_and_https_21)|(http_and_https_22)|(http_and_https_23)|(http_and_https_24)|(http_and_https_25)|(http_and_https_26)|(http_and_https_27)|(http_and_https_28)|(http_and_https_29)|(http_and_https_30)|(http_and_https_31)|(http_and_https_32)|(http_and_https_33)|(http_and_https_34)|(http_and_https_35)|(http_and_https_36)|(http_and_https_37)|(http_and_https_38)|(http_and_https_39)|(http_and_https_40)|(http_and_https_41)|(http_and_https_42)|(http_and_https_43)|(http_and_https_44)|(http_and_https_45)|(http_and_https_46)|(http_and_https_47)|(http_and_https_48)|(http_and_https_49)|(http_and_https_50)|(http_and_https_51)|(http_and_https_52)|(http_and_https_53)|(http_and_https_54)|(http_and_https_55)|(http_and_https_56)|(http_and_https_57)|(http_and_https_58)|(http_and_https_59)|(http_and_https_60)|(http_and_https_61)|(http_and_https_62)|(http_and_https_63)|(http_and_https_64)|(http_and_https_65)|(http_and_https_66)|(http_and_https_67)|(http_and_https_68)|(http_and_https_69)|(http_and_https_70)|(http_and_https_71)|(http_and_https_72)|(http_and_https_73)|(http_and_https_74)|(http_and_https_75)|(http_and_https_76)|(http_and_https_77)|(http_and_https_78)|(http_and_https_79)|(http_and_https_80)|(http_and_https_81)|(http_and_https_82)|(http_and_https_83)|(http_and_https_84)|(http_and_https_85)|(http_and_https_86)|(http_and_https_87)|(http_and_https_88)|(http_and_https_89)|(http_and_https_90)|(http_and_https_91)|(http_and_https_92)|(http_and_https_93)|(http_and_https_94)|(http_and_https_95)|(http_and_https_96)|(http_and_https_97)|(http_and_https_98)|(http_and_https_99)|(http_and_https_100)|(shadow_http_and_https)|(http_and_https_1000)";
              }; -- servername
            }; -- ["adm-nanny.yandex-team.ru"]
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              ca = get_ca_cert_path("clientCAs.pem", "./");
              log = get_log_path("ssl_sni", 443, "/place/db/www/logs");
              priv = get_private_cert_path("adm-nanny.yandex-team.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-adm-nanny.yandex-team.ru.pem", "/dev/shm/balancer");
              client = {
                verify_peer = true;
                verify_depth = 3;
                verify_once = true;
                fail_if_no_peer_cert = false;
              }; -- client
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.adm-nanny.yandex-team.ru.key", "/dev/shm/balancer/priv");
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
              log = get_log_path("access_log", 443, "/place/db/www/logs");
              report = {
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                regexp = {
                  ["awacs-balancer-health-check"] = {
                    priority = 3;
                    match_fsm = {
                      URI = "/awacs-balancer-health-check";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    errordocument = {
                      status = 200;
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- ["awacs-balancer-health-check"]
                  slbping = {
                    priority = 2;
                    match_fsm = {
                      url = "/ping";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "1683180570280210434";
                    }; -- shared
                  }; -- slbping
                  default = {
                    priority = 1;
                    regexp_host = {
                      domain = {
                        priority = 2;
                        pattern = "(http_and_https_0)|(http_and_https_1)|(http_and_https_2)|(http_and_https_3)|(http_and_https_4)|(http_and_https_5)|(http_and_https_6)|(http_and_https_7)|(http_and_https_8)|(http_and_https_9)|(http_and_https_10)|(http_and_https_11)|(http_and_https_12)|(http_and_https_13)|(http_and_https_14)|(http_and_https_15)|(http_and_https_16)|(http_and_https_17)|(http_and_https_18)|(http_and_https_19)|(http_and_https_20)|(http_and_https_21)|(http_and_https_22)|(http_and_https_23)|(http_and_https_24)|(http_and_https_25)|(http_and_https_26)|(http_and_https_27)|(http_and_https_28)|(http_and_https_29)|(http_and_https_30)|(http_and_https_31)|(http_and_https_32)|(http_and_https_33)|(http_and_https_34)|(http_and_https_35)|(http_and_https_36)|(http_and_https_37)|(http_and_https_38)|(http_and_https_39)|(http_and_https_40)|(http_and_https_41)|(http_and_https_42)|(http_and_https_43)|(http_and_https_44)|(http_and_https_45)|(http_and_https_46)|(http_and_https_47)|(http_and_https_48)|(http_and_https_49)|(http_and_https_50)|(http_and_https_51)|(http_and_https_52)|(http_and_https_53)|(http_and_https_54)|(http_and_https_55)|(http_and_https_56)|(http_and_https_57)|(http_and_https_58)|(http_and_https_59)|(http_and_https_60)|(http_and_https_61)|(http_and_https_62)|(http_and_https_63)|(http_and_https_64)|(http_and_https_65)|(http_and_https_66)|(http_and_https_67)|(http_and_https_68)|(http_and_https_69)|(http_and_https_70)|(http_and_https_71)|(http_and_https_72)|(http_and_https_73)|(http_and_https_74)|(http_and_https_75)|(http_and_https_76)|(http_and_https_77)|(http_and_https_78)|(http_and_https_79)|(http_and_https_80)|(http_and_https_81)|(http_and_https_82)|(http_and_https_83)|(http_and_https_84)|(http_and_https_85)|(http_and_https_86)|(http_and_https_87)|(http_and_https_88)|(http_and_https_89)|(http_and_https_90)|(http_and_https_91)|(http_and_https_92)|(http_and_https_93)|(http_and_https_94)|(http_and_https_95)|(http_and_https_96)|(http_and_https_97)|(http_and_https_98)|(http_and_https_99)|(http_and_https_100)|(shadow_http_and_https)(:443)?";
                        case_insensitive = true;
                        shared = {
                          uuid = "7006341551556367040";
                        }; -- shared
                      }; -- domain
                      domain2 = {
                        priority = 1;
                        pattern = "(http_and_https_1000)(:443)?";
                        case_insensitive = true;
                        shared = {
                          uuid = "7006341551556367040";
                        }; -- shared
                      }; -- domain2
                    }; -- regexp_host
                  }; -- default
                }; -- regexp
                refers = "service_total";
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}
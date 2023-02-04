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
  config_check = {
    quorums_file = "./controls/backend_check_quorums";
  }; -- config_check
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
  cpu_limiter = {
    active_check_subnet_default = true;
    disable_file = "./controls/cpu_limiter_disabled";
    active_check_subnet_file = "./controls/active_check_subnets_list";
  }; -- cpu_limiter
  addrs = {
    {
      ip = "*";
      port = 11111;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 11112;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  sd = {
    client_name = "awacs-l7-balancer(easy-mode-v2.test.yandex.net:balancer)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = get_str_var("sd_cache_dir", "./sd_cache");
  }; -- sd
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
        11111;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 11111, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 11111, "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              matcher_map = {
                molly = {
                  match_fsm = {
                    cgi = ".*everybodybecoolthisis=(crasher|molly).*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                }; -- molly
              }; -- matcher_map
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              cookie_policy = {
                uuid = "service_total";
                default_yandex_policies = "stable";
                regexp = {
                  ["awacs-balancer-health-check"] = {
                    priority = 4;
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
                    priority = 3;
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
                  antirobot_captcha = {
                    priority = 2;
                    match_fsm = {
                      URI = "/x?(show|check)?captcha.*";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "2057491396122981757";
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
                                              endpoint_set_id = "httpbin";
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
                                              endpoint_set_id = "httpbin";
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
                                              endpoint_set_id = "httpbin";
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
                    }; -- shared
                  }; -- antirobot_captcha
                  default = {
                    priority = 1;
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
                                uuid = "1723673756573565505";
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
                                          sd = {
                                            endpoint_sets = {
                                              {
                                                cluster_name = "man";
                                                endpoint_set_id = "httpbin";
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
                                                endpoint_set_id = "httpbin";
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
                                                endpoint_set_id = "httpbin";
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
                                }; -- report
                              }; -- shared
                            }; -- checker
                            module = {
                              regexp = {
                                default = {
                                  priority = 1;
                                  regexp_host = {
                                    ["by_dc.easy-mode.yandex.net"] = {
                                      priority = 3;
                                      pattern = "(by_dc\\.easy-mode\\.yandex\\.net)(:11111)?";
                                      case_insensitive = true;
                                      regexp = {
                                        by_dc = {
                                          priority = 1;
                                          match_fsm = {
                                            path = ".*";
                                            case_insensitive = true;
                                            surround = false;
                                          }; -- match_fsm
                                          report = {
                                            uuid = "by_dc";
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
                                                by_dc_man = {
                                                  weight = 1.000;
                                                  report = {
                                                    uuid = "requests_by_dc_to_man";
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
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "man";
                                                            endpoint_set_id = "httpbin";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "20ms";
                                                          backend_timeout = "50ms";
                                                          fail_on_5xx = false;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        dynamic = {
                                                          max_pessimized_share = 0.200;
                                                          min_pessimization_coeff = 0.100;
                                                          weight_increase_step = 0.100;
                                                          history_interval = "10s";
                                                          backends_name = "httpbin-man";
                                                        }; -- dynamic
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.200;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- report
                                                }; -- by_dc_man
                                                by_dc_sas = {
                                                  weight = 1.000;
                                                  report = {
                                                    uuid = "requests_by_dc_to_sas";
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
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "sas";
                                                            endpoint_set_id = "httpbin";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "20ms";
                                                          backend_timeout = "50ms";
                                                          fail_on_5xx = false;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        dynamic = {
                                                          max_pessimized_share = 0.200;
                                                          min_pessimization_coeff = 0.100;
                                                          weight_increase_step = 0.100;
                                                          history_interval = "10s";
                                                          backends_name = "httpbin-sas";
                                                        }; -- dynamic
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.200;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- report
                                                }; -- by_dc_sas
                                                by_dc_vla = {
                                                  weight = 1.000;
                                                  report = {
                                                    uuid = "requests_by_dc_to_vla";
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
                                                      sd = {
                                                        endpoint_sets = {
                                                          {
                                                            cluster_name = "vla";
                                                            endpoint_set_id = "httpbin";
                                                          };
                                                        }; -- endpoint_sets
                                                        proxy_options = {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "20ms";
                                                          backend_timeout = "50ms";
                                                          fail_on_5xx = false;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = false;
                                                        }; -- proxy_options
                                                        dynamic = {
                                                          max_pessimized_share = 0.200;
                                                          min_pessimization_coeff = 0.100;
                                                          weight_increase_step = 0.100;
                                                          history_interval = "10s";
                                                          backends_name = "httpbin-vla";
                                                        }; -- dynamic
                                                      }; -- sd
                                                      attempts_rate_limiter = {
                                                        limit = 0.200;
                                                        coeff = 0.990;
                                                        switch_default = true;
                                                      }; -- attempts_rate_limiter
                                                    }; -- balancer2
                                                  }; -- report
                                                }; -- by_dc_vla
                                                by_dc_devnull = {
                                                  weight = -1.000;
                                                  report = {
                                                    uuid = "requests_by_dc_to_devnull";
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
                                                }; -- by_dc_devnull
                                              }; -- rr
                                            }; -- balancer2
                                          }; -- report
                                        }; -- by_dc
                                      }; -- regexp
                                    }; -- ["by_dc.easy-mode.yandex.net"]
                                    ["flat.easy-mode.yandex.net"] = {
                                      priority = 2;
                                      pattern = "(flat\\.easy-mode\\.yandex\\.net)(:11111)?";
                                      case_insensitive = true;
                                      shared = {
                                        uuid = "1374075909425755349";
                                        regexp = {
                                          flat = {
                                            priority = 1;
                                            match_fsm = {
                                              path = ".*";
                                              case_insensitive = true;
                                              surround = false;
                                            }; -- match_fsm
                                            report = {
                                              uuid = "flat";
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
                                                sd = {
                                                  endpoint_sets = {
                                                    {
                                                      cluster_name = "sas";
                                                      endpoint_set_id = "httpbin";
                                                    };
                                                    {
                                                      cluster_name = "man";
                                                      endpoint_set_id = "httpbin";
                                                    };
                                                    {
                                                      cluster_name = "vla";
                                                      endpoint_set_id = "httpbin";
                                                    };
                                                  }; -- endpoint_sets
                                                  proxy_options = {
                                                    resolve_timeout = "10ms";
                                                    connect_timeout = "20ms";
                                                    backend_timeout = "50ms";
                                                    fail_on_5xx = false;
                                                    http_backend = true;
                                                    buffering = false;
                                                    keepalive_count = 0;
                                                    need_resolve = false;
                                                    switched_backend_timeout = "31536000s";
                                                  }; -- proxy_options
                                                  dynamic = {
                                                    max_pessimized_share = 0.200;
                                                    min_pessimization_coeff = 0.100;
                                                    weight_increase_step = 0.100;
                                                    history_interval = "10s";
                                                    backends_name = "httpbin-man#httpbin-sas#httpbin-vla";
                                                  }; -- dynamic
                                                }; -- sd
                                                attempts_rate_limiter = {
                                                  limit = 0.200;
                                                  coeff = 0.990;
                                                  switch_default = true;
                                                }; -- attempts_rate_limiter
                                              }; -- balancer2
                                            }; -- report
                                          }; -- flat
                                        }; -- regexp
                                      }; -- shared
                                    }; -- ["flat.easy-mode.yandex.net"]
                                    ["test.yandex.ru"] = {
                                      priority = 1;
                                      pattern = "(test\\.yandex\\.ru)(:11111)?";
                                      case_insensitive = true;
                                      shared = {
                                        uuid = "1374075909425755349";
                                      }; -- shared
                                    }; -- ["test.yandex.ru"]
                                  }; -- regexp_host
                                }; -- default
                              }; -- regexp
                            }; -- module
                          }; -- antirobot
                        }; -- cutter
                      }; -- h100
                    }; -- hasher
                  }; -- default
                }; -- regexp
              }; -- cookie_policy
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
        11112;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 11112, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          ja3_enabled = true;
          contexts = {
            ["test.yandex.ru"] = {
              priority = 2;
              timeout = "100800s";
              disable_sslv3 = true;
              disable_tlsv1_3 = false;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 11112, "/place/db/www/logs");
              priv = get_private_cert_path("test.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-test.yandex.ru.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(test\\.yandex\\.ru)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.test.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.test.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.test.yandex.ru.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["test.yandex.ru"]
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              disable_tlsv1_3 = false;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 11112, "/place/db/www/logs");
              priv = get_private_cert_path("test.yandex.ru.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-test.yandex.ru.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.test.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.test.yandex.ru.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.test.yandex.ru.key", "/dev/shm/balancer/priv");
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
              log = get_log_path("access_log", 11112, "/place/db/www/logs");
              report = {
                ranges = get_str_var("default_ranges");
                matcher_map = {
                  molly = {
                    match_fsm = {
                      cgi = ".*everybodybecoolthisis=(crasher|molly).*";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                  }; -- molly
                }; -- matcher_map
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                cookie_policy = {
                  uuid = "service_total";
                  default_yandex_policies = "stable";
                  regexp = {
                    ["awacs-balancer-health-check"] = {
                      priority = 4;
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
                      priority = 3;
                      match_fsm = {
                        url = "/ping";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      shared = {
                        uuid = "1683180570280210434";
                      }; -- shared
                    }; -- slbping
                    antirobot_captcha = {
                      priority = 2;
                      match_fsm = {
                        URI = "/x?(show|check)?captcha.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      shared = {
                        uuid = "2057491396122981757";
                      }; -- shared
                    }; -- antirobot_captcha
                    default = {
                      priority = 1;
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
                                  uuid = "1723673756573565505";
                                }; -- shared
                              }; -- checker
                              module = {
                                regexp = {
                                  default = {
                                    priority = 1;
                                    regexp_host = {
                                      ["test.yandex.ru"] = {
                                        priority = 1;
                                        pattern = "(test\\.yandex\\.ru)(:11112)?";
                                        case_insensitive = true;
                                        shared = {
                                          uuid = "1374075909425755349";
                                        }; -- shared
                                      }; -- ["test.yandex.ru"]
                                    }; -- regexp_host
                                  }; -- default
                                }; -- regexp
                              }; -- module
                            }; -- antirobot
                          }; -- cutter
                        }; -- h100
                      }; -- hasher
                    }; -- default
                  }; -- regexp
                }; -- cookie_policy
                refers = "service_total";
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}
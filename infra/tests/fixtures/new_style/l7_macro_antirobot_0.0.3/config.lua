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
  addrs = {
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
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
  cpu_limiter = {
    active_check_subnet_default = true;
    disable_file = "./controls/cpu_limiter_disabled";
    active_check_subnet_file = "./controls/active_check_subnets_list";
  }; -- cpu_limiter
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
    client_name = "awacs-l7-balancer(namespace-id:balancer)";
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
              input_size_ranges = "32,64,128,256,512,1024,4096,8192,16384,131072,524288,1048576,2097152";
              output_size_ranges = "512,1024,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608";
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
                  }; -- slbping
                  antirobot_captcha = {
                    priority = 2;
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
                          headers = {
                            create = {
                              ["X-Antirobot-Req-Group"] = "taxi-frontend-yandex";
                            }; -- create
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
                                              endpoint_set_id = "antirobot-man-yp";
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
                                              endpoint_set_id = "antirobot-sas-yp";
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
                                              endpoint_set_id = "antirobot-vla-yp";
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
                          }; -- headers
                        }; -- cutter
                      }; -- h100
                    }; -- report
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
                          headers = {
                            create = {
                              ["X-Antirobot-Req-Group"] = "taxi-frontend-yandex";
                              ["X-Antirobot-Service-Y"] = "zen";
                            }; -- create
                            headers = {
                              create_func_weak = {
                                ["X-Forwarded-For-Y"] = "realip";
                              }; -- create_func_weak
                              antirobot = {
                                cut_request = true;
                                no_cut_request_file = "./controls/no_cut_request_file";
                                file_switch = "./controls/disable_antirobot_module";
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
                                                  endpoint_set_id = "antirobot-man-yp";
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
                                                  endpoint_set_id = "antirobot-sas-yp";
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
                                                  endpoint_set_id = "antirobot-vla-yp";
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
                                }; -- checker
                                module = {
                                  regexp = {
                                    default = {
                                      priority = 1;
                                      report = {
                                        uuid = "default";
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
                                            ["X-Real-Scheme"] = "scheme";
                                            ["X-Req-Id"] = "reqid";
                                          }; -- create_func
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 2;
                                            connection_attempts = 2;
                                            check_backends = {
                                              quorum = 0.350;
                                              name = "default";
                                            }; -- check_backends
                                            dynamic = {
                                              max_pessimized_share = 0.200;
                                              min_pessimization_coeff = 0.100;
                                              weight_increase_step = 0.100;
                                              history_interval = "10s";
                                              backends_name = "test_sas";
                                              unpack(gen_proxy_backends({
                                                { "test.sas.yp-c.yandex.net"; 80; 1.000; "2a02:6b8:c1c:320:0:4ac4:41a5:0"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "70ms";
                                                backend_timeout = "10s";
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
                                            on_error = {
                                              errordocument = {
                                                status = 504;
                                                force_conn_close = false;
                                                content = "Service unavailable";
                                              }; -- errordocument
                                            }; -- on_error
                                          }; -- balancer2
                                        }; -- headers
                                      }; -- report
                                    }; -- default
                                  }; -- regexp
                                }; -- module
                              }; -- antirobot
                            }; -- headers
                          }; -- headers
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
  }; -- ipdispatch
}
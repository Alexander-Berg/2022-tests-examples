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


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
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
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  state_directory = "/dev/shm/balancer-state";
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15220, "/place/db/www/logs");
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
  addrs = {
    {
      port = 15220;
      ip = "127.0.0.4";
    };
    {
      ip = "*";
      port = 80;
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
        admin = {};
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
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
        errordocument = {
          status = 204;
          force_conn_close = false;
        }; -- errordocument
      }; -- http
    }; -- stats_storage
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
                default = {
                  priority = 1;
                  cookie_hasher = {
                    cookie = "yandexuid";
                    file_switch = "./controls/disable_cookie_hasher";
                    balancer2 = {
                      unique_policy = {};
                      attempts = 2;
                      dynamic_hashing = {
                        max_pessimized_share = 0.800;
                        min_pessimization_coeff = 0.100;
                        weight_increase_step = 0.100;
                        history_interval = "10s";
                        backends_name = "test";
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
                      }; -- dynamic_hashing
                    }; -- balancer2
                  }; -- cookie_hasher
                }; -- default
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}
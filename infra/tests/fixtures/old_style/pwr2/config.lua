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


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


instance = {
  workers = 1;
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
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
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 16100;
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
    local_ips = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
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
        balancer2 = {
          unique_policy = {};
          attempts = 2;
          pwr2 = {
            combined_criterion = {
              {
                weight = 0.500;
                fail_rate_criterion = {
                  history_time = "2000ms";
                }; -- fail_rate_criterion
              };
              {
                weight = 0.300;
                load_factor_criterion = {};
              };
              {
                weight = 0.300;
                request_duration_criterion = {
                  history_time = "2000ms";
                  slow_reply_time = "1000ms";
                }; -- request_duration_criterion
              };
              {
                weight = 0.100;
                backend_weight_criterion = {};
              };
              {
                weight = 0.100;
                combined_criterion = {
                  {
                    weight = 0.600;
                    load_factor_criterion = {};
                  };
                  {
                    weight = 0.400;
                    backend_weight_criterion = {};
                  };
                }; -- combined_criterion
              };
            }; -- combined_criterion
            {
              weight = 100.000;
              errordocument = {
                status = 200;
                force_conn_close = false;
              }; -- errordocument
            };
          }; -- pwr2
        }; -- balancer2
      }; -- http
    }; -- local_ips
  }; -- ipdispatch
}
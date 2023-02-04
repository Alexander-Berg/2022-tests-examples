enable_ips = enable_ips and enable_ips == "true"
enable_ip = enable_ip and enable_ip == "true"
enable_ports = enable_ports and enable_ports == "true"
enable_port = enable_port and enable_port == "true"

disable_ipv6 = false
disable_ipv4 = false
if enable_ips and enable_ip then
    ips = { "::1" }
    ip = "127.0.0.1"
elseif enable_ips then
    ips = { "::1", "127.0.0.1" }
else
    ip = "::1"
    disable_ipv4 = true
end

disable_electric_port = false
disable_light_port = false
disable_orchestra_port = false
if enable_ports and enable_port then
    ports = { electric_port, light_port }
    the_port = orchestra_port
elseif enable_ports then
    ports = { electric_port, light_port, orchestra_port }
else
    the_port = electric_port
    disable_light_port = true
    disable_orchestra_port = true
end

instance = {
    addrs = {
        { ip = "::1"; port = electric_port; disabled = disable_ipv6 or disable_electric_port; };
        { ip = "::1"; port = light_port; disabled = disable_ipv6 or disable_light_port; };
        { ip = "::1"; port = orchestra_port; disabled = disable_ipv6 or disable_orchestra_port; };
        { ip = "127.0.0.1"; port = electric_port; disabled = disable_ipv4 or disable_electric_port; };
        { ip = "127.0.0.1"; port = light_port; disabled = disable_ipv4 or disable_light_port; };
        { ip = "127.0.0.1"; port = orchestra_port; disabled = disable_ipv4 or disable_orchestra_port; };
        { ip = "localhost"; port = port; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; };
    }; -- admin_addrs

    thread_mode = thread_mode; set_no_file = false;

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
            ip = ip;
            port = the_port;
            ips = ips;
            ports = ports;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "OK";
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

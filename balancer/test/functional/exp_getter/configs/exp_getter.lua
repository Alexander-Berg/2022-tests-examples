instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    };

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
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                exp_getter = {
                    trusted = trusted;
                    file_switch = file_switch;
                    service_name_header = service_name_header;
                    service_name = service_name;
                    exp_headers = exp_headers;
                    service_name_to_backend_header = service_name_to_backend_header;
                    processing_time_header = true;
                    headers_size_limit = headers_size_limit;
                    uaas = {
                        proxy = {
                            host = "localhost"; port = uaas_backend_port;
                            backend_timeout = uaas_backend_timeout;
                            connect_timeout = "1s"; resolve_timeout = "1s";
                        }; -- proxy
                    }; -- uaas
                    proxy = {
                        host = "localhost"; port = backend_port;
                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                        resolve_timeout = "0.3s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; -- exp_getter
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance

function replace_uaas_proxy_with_errordocument()
    uaas = instance.ipdispatch.test.http.exp_getter.uaas;
    uaas.proxy = nil;
    uaas["errordocument"] = {
        status = 200;
        remain_headers = remain_headers;
        content = "ok\n";
    };
end

if errordocument_in_uaas then
    replace_uaas_proxy_with_errordocument()
end



function dumpvar(data)
    -- cache of tables already printed, to avoid infinite recursive loops
    local tablecache = {}
    local buffer = ""
    local padder = "    "

    local function _dumpvar(d, depth)
        local t = type(d)
        local str = tostring(d)
        if (t == "table") then
            if (tablecache[str]) then
                -- table already dumped before, so we dont
                -- dump it again, just mention it
                buffer = buffer.."<"..str..">\n"
            else
                tablecache[str] = (tablecache[str] or 0) + 1
                buffer = buffer.."("..str..") {\n"
                for k, v in pairs(d) do
                    buffer = buffer..string.rep(padder, depth+1).."["..k.."] => "
                    _dumpvar(v, depth+1)
                end
                buffer = buffer..string.rep(padder, depth).."}\n"
            end
        elseif (t == "number") then
            buffer = buffer.."("..t..") "..str.."\n"
        else
            buffer = buffer.."("..t..") \""..str.."\"\n"
        end
    end
    _dumpvar(data, 0)
    return buffer
end

print(dumpvar(instance));

function get_workers()
  local handle = io.popen('./awacslet get_workers_count')
  local output = handle:read("*a")
  handle:close()

  if string.match(output, '^%d+\n?$') == nil then
     error("failed to get read workers count from awacslet: output is " .. output)
  end
  workers_count = tonumber(output)
  if workers_count == 0 then
     error("failed to get read workers count from awacslet: workers_count == 0")
  end
  return workers_count
end

return get_workers

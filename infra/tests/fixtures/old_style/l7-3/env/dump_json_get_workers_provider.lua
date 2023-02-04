function get_workers()
  local json = require("json")
  local file, err = io.open("dump.json", "r")

  if file == nil then
    return 1
  end

  local porto_properties = json.decode(file:read("*a"));
  io.close(file)

  default_constraint = constraint or "cpu_limit"

  cpu_limit = porto_properties["container"]["constraints"][default_constraint]
  if cpu_limit == nil or cpu_limit == "" then
    return 1
  end
  cpu_limit = tonumber(string.match(cpu_limit, "%d+.%d+"))
  return math.ceil(cpu_limit)
end

return get_workers

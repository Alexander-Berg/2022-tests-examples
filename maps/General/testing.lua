local P = {}

-- Private unique identifier to store mocked call invocation info
local callid = {}

-- Mock class prototype
--
-- Tracks access to any field and call as a function/method
P.Mock = {
   __index = function(tbl, key)
      local nested = getmetatable(tbl):new()
      tbl[key] = nested

      return nested
   end,
   __call = function(tbl)
      return tbl[callid]
   end
}

function P.Mock:new(obj)
   return setmetatable(obj or {}, self)
end

local __matcher_factory = {
   __call = function(self, ...)
      return self:new(...)
   end
}

P.equals = setmetatable({}, __matcher_factory)

local matchers = {}

function P.ismatcher(v)
  return matchers[v] == true
end

function P.register_matcher(v)
   assert(not matchers[v], "Matcher is already registered")
   matchers[v] = true
end

function P.equals:new(value)
  local object = {proto = value}
  setmetatable(object, self)
  self.__index = self

  return object
end

function P.equals:matches(value)
  if type(self.proto) ~= 'table' then
    if value ~= self.proto then
      return false, {"values are not equal: " .. tostring(value) .. ' != ' .. tostring(self.proto)}
    end

    return true
  end

  local mt = getmetatable(self.proto)
  if mt then
    assert(type(mt.__eq) == 'function')
    if value ~= self.proto then
       return false, {
          "objects with user-defined comparator are not equal"
             .. tostring(value) .. ' != ' .. tostring(self.proto)}
    end

    return true
  end

  local value_type = type(value)
  if value_type ~= 'table' then
    return false, {"compared value is not a table: " .. value_type}
  end
  for k, v in pairs(self.proto) do
    local nested = value[k]
    if nested == nil then
      return false, {"property is missing from the value: [" .. tostring(k) .. "]"}
    end

    if not P.ismatcher(v) then
       v = P.equals:new(v)
    end

    local result, err = v:matches(nested)
    if not result then
       table.insert(err, 1, "values of property [" .. tostring(k) .. '] do not match')
       return result, err
    end
  end
  for k, _ in pairs(value) do
     if not self.proto[k] then
        return false, {"unexpected property [" .. tostring(k) .. "]"}
     end
  end

  return true
end

P.register_matcher(P.equals)

P.not_nil = {}

function P.not_nil:matches(v)
   if v == nil then
      return false, "value is nil"
   end

   return true
end

P.register_matcher(P.not_nil)

P.size_is = setmetatable({}, __matcher_factory)

function P.size_is:new(value)
   local object = setmetatable({proto = value}, self)
   self.__index = self

   return object
end

P.called = {
   times = setmetatable({}, __matcher_factory)
}

local __called_mt = {
   __call = function(_, stub)
      return stub[callid]
   end
}

setmetatable(P.called, __called_mt)

function P.called.times:new(matcher)
   if not P.ismatcher(matcher) then
      matcher = P.equals(matcher)
   end

   local object = setmetatable({matcher = matcher}, self)
   self.__index = self

   return object
end

function P.called.times:matches(mock)
   local called_times = #mock[callid]
   assert(called_times, "Only stubs/mocks can be tested")

   local res, err = self.matcher:matches(called_times)
   if not res then
      table.insert(err, 1, "unexpected number of calls")
      return res, err
   end

   return true
end

P.register_matcher(P.called.times)

P.assert = {
  __call = assert,
  that = function(value, matcher)
    local result, err = matcher:matches(value)
    if not result then
      local message = "Assertion failed: match failure"
      for i = 1, #err do
        message = message .. '\n' .. string.rep(' ', tostring(i)) .. tostring(err[i])
      end

      error(message, 2)
    end
  end
}

setmetatable(P.assert, P.assert)

-- Callable stub
--
-- Captures call arguments which can be later accessed via `called` function.
-- `action` is optional, mock object returned as fallback.
function P.stub(action)
   if not action then
      local retval = P.Mock:new()
      action = function()
         return retval
      end
   end

   local stub = { [callid] = {} }
   local function capture(...)
      table.insert(stub[callid], {...})
      return action(...)
   end

   return setmetatable(
      stub, {__call = function(_, ...) return capture(...) end})
end

local nameid = {}
local activeid = {}
local registry = {}

function P.describe(suite_name, fn)
   assert(type(suite_name) == 'string')
   assert(type(fn) == 'function')

   local suite = {[nameid] = suite_name}
   registry[suite_name] = suite
   registry[activeid] = suite
   local _, err = pcall(fn)
   if err then
      error("Invalid test suite specification: " .. err)
      os.exit(1)
   end
end

function P.it(name, fn)
   local suite = registry[activeid]
   suite[name] = fn
   -- Test framework is expected to provide the implementation during initial test discovery
   -- If not set, assume actual testing is in progress
   if _G.register_test then
      _G.register_test(suite[nameid], name)
   end
end

function P.run(suite_name, test_name)
   local test = assert(
      registry[suite_name][test_name],
      "Test not found - " .. tostring(suite_name) .. '::' .. tostring(test_name))
   test()
end

return P

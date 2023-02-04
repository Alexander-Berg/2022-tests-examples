local test = require('testing')
local describe, it = test.describe, test.it
local assert = test.assert

local http_client = test.Mock:new{
   connect = function()
      return true
   end,

   request = test.stub()
}

local default = {}
local samples = {
   api_key = "0xdeadbeef"
}

package.loaded['ngx'] = test.Mock:new{
   req = {
      get_uri_args = function()
         return {
            api_key = samples.api_key
         }
      end,
      set_header = test.stub()
   },
   var = {
      request_uri = "/access?api_key=" .. samples.api_key,
      http_x_real_ip = "1.1.1.1",
      referer = "https://localhost"
   },
   get_phase = function()
      return "init_worker"
   end,
   worker = {
      pid = 42
   },
   timer = {
      every = test.stub()
   },

   HTTP_OK = 200,
   HTTP_FORBIDDEN = 403,
   HTTP_BAD_REQUEST = 400,
   HTTP_NOT_FOUND = 404,
   HTTP_INTERNAL_SERVER_ERROR = 500,
   status = default
}

package.loaded['resty.http'] = test.Mock:new{
   new = function()
      return http_client
   end
}

local ngx = require 'ngx'
local apiteka = require 'apiteka-nginx-plugin'
local plugin = apiteka.ApitekaPlugin
local json = require 'cjson'

local function req_mock(status, body)
   return test.stub(function()
         return {
            status = status,
            reason = '',
            read_body = function()
               return body
            end
         }
   end)
end

describe('apiteka', function()
   it('grant access if agent responds OK', function()
         http_client.request = req_mock(ngx.HTTP_OK)
         local instance = plugin:new()
         instance:access()

         local calls = test.called(http_client.request)
         assert(#calls == 1)

         local request_args = calls[1][2]
         assert(request_args.path == '/access')
         local params = json.decode(request_args.body)
         assert(params.path_query == ngx.var.request_uri)
         assert(params.ip == ngx.var.http_x_real_ip)
         assert(params.referer == ngx.var.referer)
         assert(ngx.status == default)
   end)

   it('access response details passed to request headers', function()
         http_client.request = req_mock(
            ngx.HTTP_OK,
            '{"X-Ya-Aptk-Provider-Features": "some features"}'
         )

         local instance = plugin:new()
         instance:access()
         -- Success
         assert(ngx.status == default)
         -- Expect /access response contents set to request headers
         local ngx_req_headers = test.called(ngx.req.set_header)
         assert(#ngx_req_headers == 1)
         assert(
            table.concat(ngx_req_headers[1], '=') == "X-Ya-Aptk-Provider-Features=some features"
         )
   end)

   it('write FORBIDDEN status if agent responds FORBIDDEN', function()
         http_client.request = req_mock(ngx.HTTP_FORBIDDEN)
         plugin:new():access()
         assert.that(http_client.request, test.called.times(1))
         assert(ngx.status == ngx.HTTP_FORBIDDEN)
   end)

   it('grant access if agent responds server error', function()
         http_client.request = req_mock(ngx.HTTP_INTERNAL_SERVER_ERROR)

         plugin:new():access()
         assert.that(http_client.request, test.called.times(1))
         assert(ngx.status == default)
   end)

   it('write BAD_REQUEST status if agent responds BAD_REQUEST', function()
         http_client.request = req_mock(ngx.HTTP_BAD_REQUEST)
         plugin:new():access()
         assert.that(http_client.request, test.called.times(1))
         assert(ngx.status == ngx.HTTP_BAD_REQUEST)
   end)

   it('grant access if agent responds with other client error', function()
         http_client.request = req_mock(ngx.HTTP_NOT_FOUND)
         plugin:new():access()
         assert.that(http_client.request, test.called.times(1))
         assert(ngx.status == default)
   end)

   it('dry run always grants access', function()
         http_client.request = req_mock(ngx.HTTP_FORBIDDEN)

         plugin:new{ dry_run = true }:access()
         assert.that(http_client.request, test.called.times(1))
         assert(ngx.status == default)
   end)

   it('surprising api_key query parameter format', function()
         http_client.request = req_mock(ngx.HTTP_FORBIDDEN)

         local instance = plugin:new()
         for _, substitution in ipairs{
            {"array", "value"},
            {key = 'dict', value = 'value'},
            function() end, false, nil}
         do
            ngx.req.get_uri_args = function()
               return { api_key = substitution }
            end

            instance.reject = test.stub()
            assert(pcall(instance.access, instance))
            assert.that(instance.reject, test.called.times(1))
         end
   end)

   it('null response is treated as server error', function()
         http_client.request = test.stub(function()
               return nil, "Error stub"
         end)

         plugin:new():access()
         assert(ngx.status == default)
   end)

end)

describe('plugin usage counters', function()
   local function process_timers()
      for _, v in ipairs(test.called(ngx.timer.every)) do
         v[2]()
      end
   end

   local function process_response(instance, counters)
      ngx.headers[apiteka.USAGE_COUNTERS_HEADER] = counters
      instance:handle_response()
   end

   local function process_batch(instance, batch)
      for _, v in ipairs(batch) do
         process_response(instance, v)
      end
   end

   local function get_counters_report(index)
      return test.called(http_client.request)[index][2]
   end

   http_client.request = req_mock(ngx.HTTP_OK)

   it('disabled plugin doesn\'t make requests, sets timers or processes usage counters', function()
         ngx.timer.every = test.stub()

         local instance = plugin:new{disabled_by_file = true}
         instance:access()
         instance:handle_response()

         assert.that(http_client.request, test.called.times(0))
         assert.that(ngx.timer.every, test.called.times(0))
         assert(ngx.status == default)
   end)

   local instance = plugin:new()
   it('usage counters are reported by the timer handler', function()
         local counters = json.encode{counter = 1}
         process_response(instance, counters)

         assert.that(http_client.request, test.called.times(0))
         process_timers()

         local params = get_counters_report(1)
         assert.that(json.decode(params.body), test.equals{[samples.api_key] = {counters}})
         assert.that(params.query, test.equals{sender = ngx.worker.pid, seq = test.not_nil})
         assert.that(params.path, test.equals('/counters/update'))
   end)

   it('usage counters are accumulated before sending', function()
         local reports = {}
         for i = 1, 3 do
            table.insert(reports, json.encode{counter = i})
         end

         process_batch(instance, reports)

         assert.that(http_client.request, test.called.times(0))
         process_timers()

         assert.that(json.decode(get_counters_report(1).body),
                     test.equals{[samples.api_key] = reports})
   end)

   it('usage counters batching', function()
         local first_batch = {json.encode{counter = 1}}

         process_batch(instance, first_batch)
         process_timers()

         local second_batch = {json.encode{counter = 2}, json.encode{counter = 3}}
         process_batch(instance, second_batch)
         process_timers()

         assert.that(http_client.request, test.called.times(2))
         local first_report = get_counters_report(1)
         assert.that(json.decode(first_report.body),
                     test.equals{[samples.api_key] = first_batch})

         local second_report = get_counters_report(2)
         assert.that(json.decode(second_report.body),
                     test.equals{[samples.api_key] = second_batch})
         assert.that(second_report.query, test.equals{
                        sender = first_report.query.sender,
                        seq = first_report.query.seq + 1
         })
   end)

   it('retry batch if send failed', function()
         http_client.request = req_mock(ngx.HTTP_INTERNAL_SERVER_ERROR)

         local batch = {}
         for i = 1, 3 do
            table.insert(batch, json.encode{counter = i})
         end

         for _, v in ipairs(batch) do
            process_response(instance, v)
            process_timers()
         end

         http_client.request = req_mock(ngx.HTTP_OK)
         for _ = 1, 3 do
            process_timers()
         end

         assert.that(http_client.request, test.called.times(2))

         local first = get_counters_report(1)
         assert.that(json.decode(first.body),
                     test.equals{[samples.api_key] = {batch[1]}})

         local second = get_counters_report(2)
         assert.that(json.decode(second.body),
                     test.equals{[samples.api_key] = {batch[2], batch[3]}})
         assert.that(second.query.seq, test.equals(first.query.seq + 1))
   end)
end)

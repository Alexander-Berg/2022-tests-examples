require 'uri'
require 'net/http'
uri = URI.parse('https://staff.yandex-team.ru/')

connection = Net::HTTP.new(uri.host, uri.port)
connection.set_debug_output $stdout
if uri.scheme == 'https'
  connection.use_ssl = true
  connection.verify_mode = OpenSSL::SSL::VERIFY_PEER
end

request = Net::HTTP::Get.new(uri.request_uri)
response = connection.request(request)

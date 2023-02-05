require 'uri'
require 'net/http'
uri = URI.parse('https://staff.yandex-team.ru/')

ca_file = '/usr/share/yandex-internal-root-ca/YandexInternalRootCA.crt'

connection = Net::HTTP.new(uri.host, uri.port)
connection.set_debug_output $stdout
if uri.scheme == 'https'
  connection.use_ssl = true
  connection.verify_mode = OpenSSL::SSL::VERIFY_NONE
  connection.ca_file = ca_file
end

request = Net::HTTP::Get.new(uri.request_uri)
response = connection.request(request)

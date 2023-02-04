set -e
set -x
CN=ec.yandex.net
SAN='DNS:ec.yandex.net'

./gen_ec_certs.sh $CN $SAN
openssl x509 -in ./certs/$CN.crt -out ../certs/allCAs-$CN.pem -outform PEM
openssl ec -in ./certs/$CN.key -out ../certs/$CN.pem -outform PEM

#!/bin/sh

openssl pkcs12 -export -inkey yakassa_mws.key -in yakassa_mws.cer -out autoru_yakassa_mws.pkcs12

#openssl pkcs7 -print_certs -in ymca.p7b -inform DER -out ymca.pem
#keytool -import -v -trustcacerts -alias endeca-ca -file ymca.pem -keystore ymca.jks
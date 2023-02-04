#!/bin/sh

# key pass 12345

# openssl genrsa -des3 -out private_key.pem 2048
# openssl req -new -key private_key.pem -out server2.csr
# openssl x509 -req -days 365 -in server2.csr -signkey private_key.pem -out certificate2.pem

openssl smime -sign -signer certificate.pem -inkey private_key.pem -nocerts -outform PEM -nodetach -in xml_rq_checkOrder01.xml -out checkOrder.p7m
openssl smime -verify -inform PEM -nointern -certfile certificate.pem -CAfile certificate.pem -in checkOrder.p7m

openssl smime -sign -signer certificate2.pem -inkey private_key.pem -nocerts -outform PEM -nodetach -in xml_rq_checkOrder01.xml -out checkOrder2.p7m
openssl smime -verify -inform PEM -nointern -certfile certificate2.pem -CAfile certificate2.pem -in checkOrder2.p7m
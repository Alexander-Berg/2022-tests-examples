import base64
import datetime
import errno
import os
import pytz

from asn1crypto import crl
from asn1crypto import pem
from unittest import mock

from infra.rtc.certman import certificate

# Generated with
# openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 \
# -keyout privateKey.key -out certificate.pem
TEST_CERT_PEM = b"""-----BEGIN CERTIFICATE-----
MIIDWjCCAkKgAwIBAgIJAPvXnJGzypZYMA0GCSqGSIb3DQEBCwUAMEIxCzAJBgNV
BAYTAlhYMRUwEwYDVQQHDAxEZWZhdWx0IENpdHkxHDAaBgNVBAoME0RlZmF1bHQg
Q29tcGFueSBMdGQwHhcNMTgwNzAyMTA1NzU5WhcNMTkwNzAyMTA1NzU5WjBCMQsw
CQYDVQQGEwJYWDEVMBMGA1UEBwwMRGVmYXVsdCBDaXR5MRwwGgYDVQQKDBNEZWZh
dWx0IENvbXBhbnkgTHRkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA
xf/sVOY4tD2KkZZ047r+dC58WK3wpFfPx8z7zQV8Ih5pOuoP3FdLKXCfFXGC8/vf
ez+xXetKt4xMZ+kHxN4nDNk8E64tOhYVXs4jGioeIphS6mQ4e/8sNzF3TDHZ2Su+
Xf5Zxv9FsB6c5VMenR5njtEJmM2n2RyGYRqdHLjb7pR9sjx9ZJuRGHMopwTLLWUZ
f/AOlmYPzOCgV0T0CUz72UulWMHz84uJFNIrk2Kcdr7I2Be66BfKLnGyCopguDFc
ClWFtqoMZuZrFnLi0FTduKKEvG2jaRkZoWLEFcRbwMd5i6zg9A0Uqeokvpnjk4SS
DFu3pKb6DJUgJ+CfRexY5QIDAQABo1MwUTAdBgNVHQ4EFgQUgQxukVBBEsM2n6SX
AMLOmmgYCMcwHwYDVR0jBBgwFoAUgQxukVBBEsM2n6SXAMLOmmgYCMcwDwYDVR0T
AQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAj5OTlAwV2pBITTXvLaa40ItW
s5hu8gJnqx/HhZ+w1cKreFTVHLDGZd8MMqtGgemYuUT4EE8uPsJaQiMdqaWXOcEQ
4/4NDnNo6ZzXoHOO0H//mo696iSlfEFAvx8LJPyoCVM9Q7M1H9SJZfPZ/NipQESc
eJvZNxKC3GRCqbvwCVrpU2Phwzp2fJnJmvUBWNcoYbodWMbzGdkdG2GCoq7XoFiq
I5pcT1xEbKOZ05fPb0WQLDuHd8klhlS4qKTNpFj3I61RXI9XZzjkZzg1eWI0i0/u
Y9RQlb1ti4YZDMAYR9g4taNT1C3GmFu6FU9hTK5KRtP5m6+5dv+gxa8I96HnIA==
-----END CERTIFICATE-----
"""
TEST_NOT_BEFORE = datetime.datetime(2018, 7, 2, 10, 57, 59, tzinfo=pytz.FixedOffset(180))
TEST_NOT_AFTER = datetime.datetime(2019, 7, 2, 10, 57, 59, tzinfo=pytz.FixedOffset(180))

TEST_PK_PEM = b"""-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCvIaOssauCmYF2
XJ0596ijfoIHL2u66btpZ3+zQ3060xEFihRb4FJrMceha7cRu+PikvL2AzeaKVls
809qMcWz2YnLTeaDOP9GdvdF12PkRN7hxhSKzbR4f4p7wrIzu2zNz5XhKK688fLx
pjq9w54cNcwaYRpKGWGtJNifRT0JhIh5oFeq/oby572Fg4ouWA8DRptkwHXP1IwP
mR8BHJsAD5K4ro53M1DKySB3MeH6QYFYelL9dhD/l+GtZLaK4wt8c+4pphQr8ROX
Avc7NvQfJtzXW5QNEDhwTe1OSqOqf+nk0+o9VWiFNIUT6aAAPTA2dmzNwXanocwR
7nkxxTbNAgMBAAECggEAbAtk3vX9yeTKaitZFJomADfcIHKxE+9/H9gqgH3ylIgC
yFgXl1cdgeSENKIvn1E7llzyZ7Q0aXd5JC43oFx0jDB5QmQdAnf3hZF/nCb2Ca4z
kzaRFdEyteR5m0DSj/fheFfJL8/ZfdgG+e6D7u7wbJcEBIJDpdNi318MBbbBoUyN
Ub/Z9HTnHQ1x/hupkyzypTPWtIevQNfLrOxwtmyC6tCCEPghszthWk/LsQFHw7Sf
/aGiHG0sUHkMqGRE+IsUxSk8dDfcE+XJ15NK9HEgr7zFEs036gJcOe+bxbyCVx23
uV1Xv1zSHCzCDgjMUZM9PCjgv7VLXErzhFZnnOWCQQKBgQDYkXAZ8jbM/CkaUtQp
Sa3eeED5FJi6wfeoU9DzhUlyJCuEnXbb1WBD1sQMG29xeeUcqR4KMWSyE6um5wJG
nbMLGPXsyHZP4eHxPrUsjDM+m6ttWMVXssOgJ/NpyuJYLb/SgL9JWLI1i7lY6sNx
mJHh034H4in2N4wlMLkQg5lJzwKBgQDPBMZNZZ2CiYIQB1dLhmi1i0jtU+no/jeU
lW/YmOrOWK2SgPcmJnPLRVx37+NO8AXayxunIA4S2xJwqRnrPGiMFoMpaV10LuW2
4EZHcYvu2OYudXRu283sSeZHm5Er216kdxgQ22PFtbb0RdnvBelqDALZKkMLN4QT
leAijkdIowKBgCE/KpOBILKVX4YAAzwXaI7HrUnPxKafEnMP8vr/kkfoj5m7Rrz2
4+FmPhCRwakNCoQ83jS4YnoIFQj5W9sVaKAig9aG68rVpYcQlmWxXEiXQ1j1EM6m
zetvVqn+EINy+ojJZRRQfoND4P9qgviUcIjXm/h/2utGmcg306FaYpT3AoGAcjTl
HW+wvIIV0bfrDcxh2b20htFjWtlcWkkyweBisWG0p/0j29Uczog+YiBiW3sJD9I7
ODDz8q0O+D4iAWd7GcWVqHPrTZxNuFz0CJf4pdTc78Z2bG4wpWmDc/+7z+Eezkax
bSNLYJhGDW63syikw49KxvmMsRR4dcGaPBYX91cCgYEAr3QofHlIAu/xSQOCxz24
ZWxpfxakBe36v2gxKX1qNb5Z00+leQXdpjzH136/iYTsOCrpWZIxqH+mRFydeQW/
C8yeGyEDIL760DX8QaR3SkmY7v8Re1F7OukrJvt5U73fuvFDX6DIh1etK915x3IA
kq7fG0Cz+pSjCdBqOMdEiwg=
-----END PRIVATE KEY-----
"""
# Certificate acquired from production host and
# then manually reissued (so it is invalid and must be present in CRL)
HOST_PEM = b"""-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCvIaOssauCmYF2
XJ0596ijfoIHL2u66btpZ3+zQ3060xEFihRb4FJrMceha7cRu+PikvL2AzeaKVls
809qMcWz2YnLTeaDOP9GdvdF12PkRN7hxhSKzbR4f4p7wrIzu2zNz5XhKK688fLx
pjq9w54cNcwaYRpKGWGtJNifRT0JhIh5oFeq/oby572Fg4ouWA8DRptkwHXP1IwP
mR8BHJsAD5K4ro53M1DKySB3MeH6QYFYelL9dhD/l+GtZLaK4wt8c+4pphQr8ROX
Avc7NvQfJtzXW5QNEDhwTe1OSqOqf+nk0+o9VWiFNIUT6aAAPTA2dmzNwXanocwR
7nkxxTbNAgMBAAECggEAbAtk3vX9yeTKaitZFJomADfcIHKxE+9/H9gqgH3ylIgC
yFgXl1cdgeSENKIvn1E7llzyZ7Q0aXd5JC43oFx0jDB5QmQdAnf3hZF/nCb2Ca4z
kzaRFdEyteR5m0DSj/fheFfJL8/ZfdgG+e6D7u7wbJcEBIJDpdNi318MBbbBoUyN
Ub/Z9HTnHQ1x/hupkyzypTPWtIevQNfLrOxwtmyC6tCCEPghszthWk/LsQFHw7Sf
/aGiHG0sUHkMqGRE+IsUxSk8dDfcE+XJ15NK9HEgr7zFEs036gJcOe+bxbyCVx23
uV1Xv1zSHCzCDgjMUZM9PCjgv7VLXErzhFZnnOWCQQKBgQDYkXAZ8jbM/CkaUtQp
Sa3eeED5FJi6wfeoU9DzhUlyJCuEnXbb1WBD1sQMG29xeeUcqR4KMWSyE6um5wJG
nbMLGPXsyHZP4eHxPrUsjDM+m6ttWMVXssOgJ/NpyuJYLb/SgL9JWLI1i7lY6sNx
mJHh034H4in2N4wlMLkQg5lJzwKBgQDPBMZNZZ2CiYIQB1dLhmi1i0jtU+no/jeU
lW/YmOrOWK2SgPcmJnPLRVx37+NO8AXayxunIA4S2xJwqRnrPGiMFoMpaV10LuW2
4EZHcYvu2OYudXRu283sSeZHm5Er216kdxgQ22PFtbb0RdnvBelqDALZKkMLN4QT
leAijkdIowKBgCE/KpOBILKVX4YAAzwXaI7HrUnPxKafEnMP8vr/kkfoj5m7Rrz2
4+FmPhCRwakNCoQ83jS4YnoIFQj5W9sVaKAig9aG68rVpYcQlmWxXEiXQ1j1EM6m
zetvVqn+EINy+ojJZRRQfoND4P9qgviUcIjXm/h/2utGmcg306FaYpT3AoGAcjTl
HW+wvIIV0bfrDcxh2b20htFjWtlcWkkyweBisWG0p/0j29Uczog+YiBiW3sJD9I7
ODDz8q0O+D4iAWd7GcWVqHPrTZxNuFz0CJf4pdTc78Z2bG4wpWmDc/+7z+Eezkax
bSNLYJhGDW63syikw49KxvmMsRR4dcGaPBYX91cCgYEAr3QofHlIAu/xSQOCxz24
ZWxpfxakBe36v2gxKX1qNb5Z00+leQXdpjzH136/iYTsOCrpWZIxqH+mRFydeQW/
C8yeGyEDIL760DX8QaR3SkmY7v8Re1F7OukrJvt5U73fuvFDX6DIh1etK915x3IA
kq7fG0Cz+pSjCdBqOMdEiwg=
-----END PRIVATE KEY-----

-----BEGIN CERTIFICATE-----
MIIEIDCCAwigAwIBAgITbwABGEi3X6rZwvmq6gABAAEYSDANBgkqhkiG9w0BAQsF
ADBVMRIwEAYKCZImiZPyLGQBGRYCcnUxFjAUBgoJkiaJk/IsZAEZFgZ5YW5kZXgx
EjAQBgoJkiaJk/IsZAEZFgJsZDETMBEGA1UEAxMKWWFuZGV4UkNDQTAeFw0xODA0
MTExOTQyMDVaFw0xOTA0MTExOTQyMDVaMCIxIDAeBgNVBAMTF3ZsYTEtNjA1Ni55
cC55YW5kZXgubmV0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAryGj
rLGrgpmBdlydOfeoo36CBy9ruum7aWd/s0N9OtMRBYoUW+BSazHHoWu3Ebvj4pLy
9gM3milZbPNPajHFs9mJy03mgzj/Rnb3Rddj5ETe4cYUis20eH+Ke8KyM7tszc+V
4SiuvPHy8aY6vcOeHDXMGmEaShlhrSTYn0U9CYSIeaBXqv6G8ue9hYOKLlgPA0ab
ZMB1z9SMD5kfARybAA+SuK6OdzNQyskgdzHh+kGBWHpS/XYQ/5fhrWS2iuMLfHPu
KaYUK/ETlwL3Ozb0Hybc11uUDRA4cE3tTkqjqn/p5NPqPVVohTSFE+mgAD0wNnZs
zcF2p6HMEe55McU2zQIDAQABo4IBGjCCARYwCwYDVR0PBAQDAgWgMB0GA1UdDgQW
BBTXe6JrIZ2l2oBUvx0mx80PjU3LjzAfBgNVHSMEGDAWgBQ/ya7bq876RgGIApvQ
9KMuwahxqDBABgNVHR8EOTA3MDWgM6Axhi9odHRwOi8vY3Jscy55YW5kZXgucnUv
WWFuZGV4UkNDQS9ZYW5kZXhSQ0NBLmNybDA9BgkrBgEEAYI3FQcEMDAuBiYrBgEE
AYI3FQiG3KEmhZPVVYf9gS2D+ecCg5XSBByD+40Qg9anTwIBZAIBIzAdBgNVHSUE
FjAUBggrBgEFBQcDAgYIKwYBBQUHAwEwJwYJKwYBBAGCNxUKBBowGDAKBggrBgEF
BQcDAjAKBggrBgEFBQcDATANBgkqhkiG9w0BAQsFAAOCAQEADFxcSJdV1iFHq95l
2S6Id1erWxnyfnQsvoHBLfbUPHCN0Jocgqp5bC5HDY8tXRmx6txM4yTt/XDU3T1H
NdPPJIcjISnwknN4dbn9YJ2c9tiq2WLQZRwwWPLrEh0Nm+pE2n8awbBQmwyVodPW
VSH2oaMEyFmGIG4jV8IWMLjoYMPYwNeC0Tznvu8CTIoaJpB8NVUFeKpWmEHyISMV
H8W/4EBB+7fc+1LIsGn0jfoZs1e+r4YCa7paUgeGlYhF36FGWaj/gRgyJzlcmZjN
E91Lji1ww42PjDoWZa1c7mkETe5i7XbITL+gUlmt8eFQXSju+VF91qvHvUq2R+ZH
0JVf+w==
-----END CERTIFICATE-----

-----BEGIN CERTIFICATE-----
MIIFZTCCA02gAwIBAgIKEzQkDgAAAAAAEzANBgkqhkiG9w0BAQ0FADAfMR0wGwYD
VQQDExRZYW5kZXhJbnRlcm5hbFJvb3RDQTAeFw0xMzEwMDMxOTAzNTNaFw0xODEw
MDMxOTEzNTNaMFsxEjAQBgoJkiaJk/IsZAEZFgJydTEWMBQGCgmSJomT8ixkARkW
BnlhbmRleDESMBAGCgmSJomT8ixkARkWAmxkMRkwFwYDVQQDExBZYW5kZXhJbnRl
cm5hbENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy6Sab1PCbISk
GSAUpr6JJKLXlf4O+cBhjALfQn2QpPL/cDjZ2+MPXuAUgE8KT+/mbAGA2rJID0KY
RjDSkByxnhoX8jwWsmPYXoAmOMPkgKRG9/ZefnMrK4oVhGgLmxnpbEkNbGh88cJ1
OVzgD5LVHSpDqm7iEuoUPOJCWXQ51+rZ0Lw9zBEU8v3yXXI345iWpLj92pOQDH0G
Tqr7BnQywxcgb5BYdywayacIT7UTJZk7832m5k7Oa3qMIKKXHsx26rNVUVBfpzph
OFvqkLetOKHk7827NDKr3I3OFXzQk4gy6tagv8PZNp+XGOBWfYkbLfI4xbTnjHIW
n5q1gfKPOQIDAQABo4IBZTCCAWEwEAYJKwYBBAGCNxUBBAMCAQEwIwYJKwYBBAGC
NxUCBBYEFPsCldaHl535SpdYTUxdSiLspKGTMB0GA1UdDgQWBBSP3TKDCRNT3ZEa
Zumz1DzFtPJnSDBZBgNVHSAEUjBQME4GBFUdIAAwRjBEBggrBgEFBQcCARY4aHR0
cDovL2NybHMueWFuZGV4LnJ1L2Nwcy9ZYW5kZXhJbnRlcm5hbENBL3BvbGljaWVz
Lmh0bWwwGQYJKwYBBAGCNxQCBAweCgBTAHUAYgBDAEEwCwYDVR0PBAQDAgGGMA8G
A1UdEwEB/wQFMAMBAf8wHwYDVR0jBBgwFoAUq7nF/6Hv5lMdMzkihNF21DdOLWow
VAYDVR0fBE0wSzBJoEegRYZDaHR0cDovL2NybHMueWFuZGV4LnJ1L1lhbmRleElu
dGVybmFsUm9vdENBL1lhbmRleEludGVybmFsUm9vdENBLmNybDANBgkqhkiG9w0B
AQ0FAAOCAgEASB7PbqnZdbjNYdiitSKpGszqx4Ao9eROViznbUyhqf9mdoe/MqyR
d6yR574UYJKzelBM8Mh6AiUnVkbcqdtsrEUT1OO12Pcmq12Ej5lxOTKQ7A2WhKqE
k8ezsMzgOsE7B7FxOA/Cy01xtgGWb247bUblhlVokGcco1s6aajRpgDUSJ/vNAA+
LsPgCEsIvUVxvXX69PMoKalmrF80zmnkB84XZD48F7Wl/ryDcXVONzhdy1b7jlLA
fiGwKOInx6KNTBiVCSJKsFM6sofgHbgPKAOf4cAlE6yE6jEa5Y9U8YJ8GbXXwKXn
l7mfi/fvRSdn8jGdwqSRFuiRwMuw78iGfx6xXp1KaLJURdE7cZ2b1r2FzSI6GN0v
UiAIIONSFj1T23MLRaI3Gb+Solll8cPtb/jSyua3cDufXh6Nk7OfclQbQSzFYn2c
9PWFbFbcdPUMnF5Z+s4huYnBKlHZd7vY98zS13dUUd24iF0GrHgbNJx1vWFRBQzh
nU3oeNl2EwHQpTLCsiXjl+cXDh55jgNfyco22FVXNKrDHu5Dcrs4zo8iqqOMrZ2O
/D2wvqw8ic9v9JQV4GcwOxFrQr7PBQx1CS3VwgP7avOtgSNQBzF4yUtGNaCuCjE5
piBHDpWy17xFwXP9s488GK0+IZi2OnlpoRAXmqYKI36I3UHJC5Ah1gU=
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIFGTCCAwGgAwIBAgIQJMM7ZIy2SYxCBgK7WcFwnjANBgkqhkiG9w0BAQ0FADAf
MR0wGwYDVQQDExRZYW5kZXhJbnRlcm5hbFJvb3RDQTAeFw0xMzAyMTExMzQxNDNa
Fw0zMzAyMTExMzUxNDJaMB8xHTAbBgNVBAMTFFlhbmRleEludGVybmFsUm9vdENB
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAgb4xoQjBQ7oEFk8EHVGy
1pDEmPWw0Wgw5nX9RM7LL2xQWyUuEq+Lf9Dgh+O725aZ9+SO2oEs47DHHt81/fne
5N6xOftRrCpy8hGtUR/A3bvjnQgjs+zdXvcO9cTuuzzPTFSts/iZATZsAruiepMx
SGj9S1fGwvYws/yiXWNoNBz4Tu1Tlp0g+5fp/ADjnxc6DqNk6w01mJRDbx+6rlBO
aIH2tQmJXDVoFdrhmBK9qOfjxWlIYGy83TnrvdXwi5mKTMtpEREMgyNLX75UjpvO
NkZgBvEXPQq+g91wBGsWIE2sYlguXiBniQgAJOyRuSdTxcJoG8tZkLDPRi5RouWY
gxXr13edn1TRDGco2hkdtSUBlajBMSvAq+H0hkslzWD/R+BXkn9dh0/DFnxVt4XU
5JbFyd/sKV/rF4Vygfw9ssh1ZIWdqkfZ2QXOZ2gH4AEeoN/9vEfUPwqPVzL0XEZK
r4s2WjU9mE5tHrVsQOZ80wnvYHYi2JHbl0hr5ghs4RIyJwx6LEEnj2tzMFec4f7o
dQeSsZpgRJmpvpAfRTxhIRjZBrKxnMytedAkUPguBQwjVCn7+EaKiJfpu42JG8Mm
+/dHi+Q9Tc+0tX5pKOIpQMlMxMHw8MfPmUjC3AAd9lsmCtuybYoeN2IRdbzzchJ8
l1ZuoI3gH7pcIeElfVSqSBkCAwEAAaNRME8wCwYDVR0PBAQDAgGGMA8GA1UdEwEB
/wQFMAMBAf8wHQYDVR0OBBYEFKu5xf+h7+ZTHTM5IoTRdtQ3Ti1qMBAGCSsGAQQB
gjcVAQQDAgEAMA0GCSqGSIb3DQEBDQUAA4ICAQAVpyJ1qLjqRLC34F1UXkC3vxpO
nV6WgzpzA+DUNog4Y6RhTnh0Bsir+I+FTl0zFCm7JpT/3NP9VjfEitMkHehmHhQK
c7cIBZSF62K477OTvLz+9ku2O/bGTtYv9fAvR4BmzFfyPDoAKOjJSghD1p/7El+1
eSjvcUBzLnBUtxO/iYXRNo7B3+1qo4F5Hz7rPRLI0UWW/0UAfVCO2fFtyF6C1iEY
/q0Ldbf3YIaMkf2WgGhnX9yH/8OiIij2r0LVNHS811apyycjep8y/NkG4q1Z9jEi
VEX3P6NEL8dWtXQlvlNGMcfDT3lmB+tS32CPEUwce/Ble646rukbERRwFfxXojpf
C6ium+LtJc7qnK6ygnYF4D6mz4H+3WaxJd1S1hGQxOb/3WVw63tZFnN62F6/nc5g
6T44Yb7ND6y3nVcygLpbQsws6HsjX65CoSjrrPn0YhKxNBscF7M7tLTW/5LK9uhk
yjRCkJ0YagpeLxfV1l1ZJZaTPZvY9+ylHnWHhzlq0FzcrooSSsp4i44DB2K7O2ID
87leymZkKUY6PMDa4GkDJx0dG4UXDhRETMf+NkYgtLJ+UIzMNskwVDcxO4kVL+Hi
Pj78bnC5yCw8P5YylR45LdxLzLO68unoXOyFz1etGXzszw8lJI9LNubYxk77mK8H
LpuQKbSbIERsmR+QqQ==
-----END CERTIFICATE-----
"""
# cat YandexRCCA.crl | base64
RCCA_CRL_BASE64 = """MIICoDCCAYgCAQEwDQYJKoZIhvcNAQELBQAwVTESMBAGCgmSJomT8ixkARkWAnJ1MRYwFAYKCZIm
iZPyLGQBGRYGeWFuZGV4MRIwEAYKCZImiZPyLGQBGRYCbGQxEzARBgNVBAMTCllhbmRleFJDQ0EX
DTE5MDgzMDA1MjcwMFoXDTE5MDkwNjE3NDcwMFowgZwwMgITbwAAAA266IH4TU1IzwABAAAADRcN
MTgwOTEyMTEyNDM3WjAMMAoGA1UdFQQDCgEFMDICE28AAAAONJhg4lNEgo8AAQAAAA4XDTE4MDkx
MjExMjQzM1owDDAKBgNVHRUEAwoBBTAyAhNvAAAADDIHpYnKrtVyAAEAAAAMFw0xNjEyMDcxNjI0
MjVaMAwwCgYDVR0VBAMKAQWgYDBeMB8GA1UdIwQYMBaAFD/JrturzvpGAYgCm9D0oy7BqHGoMBAG
CSsGAQQBgjcVAQQDAgEBMAsGA1UdFAQEAgJVkzAcBgkrBgEEAYI3FQQEDxcNMTkwOTA2MDUzNzAw
WjANBgkqhkiG9w0BAQsFAAOCAQEABSRw7MnE49wDWb5YAUm+HPFWapYNEcF91N8qxclQ6AuuX5kP
DD2EDvoUpHF/GRll7IBrmJAth+1olFPrt2KNx3iw1AeVPvHPi0Q+64Zmif9O/NG79QzK+aVaKVR7
eWkwf8Q2rtvBlR1fWQZnyjMIH2ex5rno8gxcPfkTOKY3hsU5AeeDnjhC8rK3L66vOom2YvRPROV/
n1c7Vy+GZiA5u/CvnfvpSkXd9PzfY3/mKvZeBnOcFsm+4FZJLDXGWNjsUJXNC9M8+zMmETGaEz33
Y62DbNyaJvY5APGOibUGoisAhvGVDhdDrdMOwZPUde3Uc4oOfmP0zyRAUkU7b1sfxw=="""

HOST_CNAME = 'vla1-6056.yp.yandex.net'
HOST_NOT_BEFORE = datetime.datetime(2018, 4, 11, hour=19, minute=42, second=5, tzinfo=pytz.FixedOffset(180))
HOST_NOT_AFTER = datetime.datetime(2019, 4, 11, hour=19, minute=42, second=5, tzinfo=pytz.FixedOffset(180))


def test_cert_from_pem():
    f = certificate.cert_from_pem
    _, err = f('')
    assert err is not None
    _, err = f(TEST_PK_PEM)
    assert err is not None
    cert, err = f(TEST_CERT_PEM)
    assert err is None
    assert pem.armor(u'CERTIFICATE', cert.dump(force=True)) == TEST_CERT_PEM


def test_pk_from_pem():
    f = certificate.pk_from_pem
    _, err = f('')
    assert err is not None
    _, err = f(TEST_CERT_PEM)
    assert err is not None
    pk, err = f(TEST_PK_PEM)
    assert err is None
    assert pem.armor(u'PRIVATE KEY', pk.dump(force=True)) == TEST_PK_PEM


def test_validate_certificate():
    f = certificate.validate_certificate
    now = datetime.datetime.utcnow().replace(tzinfo=pytz.UTC)
    issuer = {'domain_component': ['ru', 'yandex', 'ld'], 'common_name': 'GoogleRCCA'}
    c, _ = certificate.cert_from_pem(HOST_PEM)
    # Test invalid common name
    err = f(c, 'sas1-2323.search.yandex.net', now, certificate.RTC_ISSUER)
    assert err is not None
    assert err.startswith('unexpected subject')
    # Test invalid issuer
    err = f(c, HOST_CNAME, now, issuer)
    assert err is not None
    assert err.startswith('unexpected issuer')
    # Test invalid not before
    err = f(c, HOST_CNAME, datetime.datetime(2000, 1, 1, tzinfo=pytz.UTC),
            certificate.RTC_ISSUER)
    assert err is not None
    assert err.startswith('certificate is not valid yet')
    # Test invalid not after
    err = f(c, HOST_CNAME, datetime.datetime(2020, 1, 1, tzinfo=pytz.UTC),
            certificate.RTC_ISSUER)
    assert err is not None
    assert err.startswith('certificate is expired')
    # Test valid
    err = f(c, HOST_CNAME, datetime.datetime(2019, 1, 1, tzinfo=pytz.UTC),
            certificate.RTC_ISSUER)
    assert err is None


def test_crl_from_bytes():
    # Test invalid bytes
    cert_list, err = certificate.crl_from_bytes('THIS IS NOT A CRL')
    assert cert_list is None
    assert err is not None
    # Test valid CRL
    cert_list, err = certificate.crl_from_bytes(base64.b64decode(RCCA_CRL_BASE64))
    assert err is None
    assert isinstance(cert_list, crl.CertificateList)


def test_fetch_crl():
    fetch_crl = certificate.fetch_crl
    # Test stat failed
    stat_func = mock.Mock()
    stat_func.side_effect = EnvironmentError(errno.EACCES, 'No access')
    revoked, err = fetch_crl('', 'cache-path.crl', 10, stat_func)
    assert err is not None and err == '[Errno 13] No access'
    stat_func.assert_called_once_with('cache-path.crl')


def test_status_is_update_required():
    status = certificate.Status(days_left=20, error_msg='')
    assert status.is_update_required(None, 7, 14, 'example.com')
    assert status.is_update_required(None, 21, 356, 'example.com')

    # rest passed to certificate.cert_needs_reissue(), require own tests
    status = certificate.Status(days_left=101, error_msg='')
    assert not status.is_update_required(None, 7, 356, 'example.com')


def test_status_is_update_required__cert_needs_reissue__called():
    # Details in https://a.yandex-team.ru/arc/diff/trunk/arcadia/infra/rtc/certman/certificate.py?rev=8237130&prevRev=8237129
    status = certificate.Status(days_left=10, error_msg='')

    with mock.patch.object(certificate, 'cert_needs_reissue') as mocked_func:
        status.is_update_required(None, 7, 14, 'example.com')
        mocked_func.assert_not_called()

        status.is_update_required(None, 0, 14, 'example.com')
        mocked_func.assert_called_once()


def test_status_mtime_roundtrip(tmp_path):
    state_file = os.path.join(tmp_path, 'state')
    state_mtime = 1624010792.0

    with mock.patch('time.time', return_value=state_mtime):
        # mtime updated when `error_msg` is being set
        status = certificate.Status(days_left=10, error_msg='')

    status.dump_to_file(state_file)

    # classmethod load_from_file set mtime from file mtime
    assert state_mtime == certificate.Status.load_from_file(state_file).mtime

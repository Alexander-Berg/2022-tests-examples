class MockYavClient(object):
    @staticmethod
    def get_version(*_, **__):
        return {
            'version': 'v1',
            'value': {
                '1974C54600010024F4F5_certificate': b"""-----BEGIN CERTIFICATE-----
MIIE1jCCA76gAwIBAgIKGXTFRgABACT09TANBgkqhkiG9w0BAQUFADBSMRUwEwYK
CZImiZPyLGQBGRYFbG9jYWwxGDAWBgoJkiaJk/IsZAEZFghsZHRlc3RjYTEfMB0G
A1UEAxMWVGVzdC1ZYW5kZXhJbnRlcm5hbC1DYTAeFw0xOTAzMTIxMjUwNTBaFw0y
MTAzMTExMjUwNTBaMIGlMQswCQYDVQQGEwJSVTEbMBkGA1UECBMSUnVzc2lhbiBG
ZWRlcmF0aW9uMQ8wDQYDVQQHEwZNb3Njb3cxEzARBgNVBAoTCllhbmRleCBMTEMx
DDAKBgNVBAsTA0lUTzEiMCAGA1UEAxMZdGVzdDEudGVzdC55YW5kZXgtdGVhbS5y
dTEhMB8GCSqGSIb3DQEJARYScGtpQHlhbmRleC10ZWFtLnJ1MIIBIjANBgkqhkiG
9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyNt2mR5J52zyDmP1S04hFTnqwaQv7KXyqUWQ
GV04ffc0t/ErwkSiQF6vlkmNo8qrlWbNdNS7Nrwo4hGvxFShd1NKRWiWZ4qbnv2z
kuhLiAnCAOn7uAOvXmHV3nffJNWkhE50Mf5qRtTW2eBSgk/kN63pr6AvoNs6WT/5
KXnU+TgXGS5hyLYacdi6ZxTKbeP1B7xmU1jBU4galCXJs7dypWQx8HgmWuUdT3MT
afteGscltyhd68bI0LF5Q1QjztOnISCqtr76ZMBZqgZwkJPR+de09YSPM4+3dIsS
ve69upzWDDRsGaIhynstVcvnQOBfZ4QAknfl4xOsXBW5+jpHwQIDAQABo4IBWDCC
AVQwDgYDVR0PAQH/BAQDAgWgMBMGA1UdJQQMMAoGCCsGAQUFBwMBMD8GA1UdEQQ4
MDaCGXRlc3QxLnRlc3QueWFuZGV4LXRlYW0ucnWCGXRlc3QyLnRlc3QueWFuZGV4
LXRlYW0ucnUwHQYDVR0OBBYEFBOE7qV0RqEppdRiLXRn2WIG0Iy5MB8GA1UdIwQY
MBaAFCq4DlsFaxRSg71HKJnM9uWoZ6K7MFAGA1UdHwRJMEcwRaBDoEGGP2h0dHA6
Ly90ZXN0bGFiLW4yLWEubGQueWFuZGV4LnJ1L2NkcC9UZXN0LVlhbmRleEludGVy
bmFsLUNhLmNybDA9BgkrBgEEAYI3FQcEMDAuBiYrBgEEAYI3FQiGpfs9hIK+QYO5
nSiEt9w8h6SRB1eG/pJMgu+JeQIBZAIBAzAbBgkrBgEEAYI3FQoEDjAMMAoGCCsG
AQUFBwMBMA0GCSqGSIb3DQEBBQUAA4IBAQCUqCVfAOx3Q5OsGofSpH7RpwfAoa2+
8kscOk/wx/Z9BNs9tJ9bRc892Wom4jvcirda/+YpJsPYEIqQVl7UrBaT0VNsMUm1
y7Iu83AnXGzwPCRqQqfBgF6v32G0pFodLXkpb6R3i9owYJNKdt8AHrRPtXqfC1EB
K2TKHw2GEKn+GyRejR0fnqMbi4XJZrUAVDraZdV+ZZ0TzQe58dVbWfYx19nZ8x0f
HIVTYZWgDtvkT4INyhACDaNnaEYIVmE4c5EyxjgpwS+9CnbwM9UUn+Tu7Ycy1mVB
6W0a4f3jMzGRGMa9h11NI+Qx6kxC/RrOQr5mwrttwS7DrZwyxKrkbiY3
-----END CERTIFICATE-----
""",
                '1974C54600010024F4F5_private_key': 'abc',
            }
        }

    @staticmethod
    def delete_user_role_from_secret(*_, **__):
        return

    @staticmethod
    def get_owners(*_, **__):
        return [{'login': 'robot-awacs-certs'}]

    @staticmethod
    def get_readers(*_, **__):
        return [{'login': 'robot-awacs-certs'}]

    @staticmethod
    def remove_secret(*_, **__):
        return

    @staticmethod
    def create_secret(*_, **__):
        return 'new_secret_id'

    @staticmethod
    def create_secret_version(*_, **__):
        return 'new_secret_ver'

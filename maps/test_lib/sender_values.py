import base64

INTERNAL_EMAIL = "robot-b2bgeo@yandex-team.ru"
WELCOME_MAILING_ID = "WKL6BYI3-QWL"
WELCOME_MAILING_ID_TR = "GQ2MGZK3-HCH1"
INTERNAL_MAILING_ID = "OQ6EHBJ3-RRM1"

SENDER_BASIC_AUTH_USER = "sender_username"
SENDER_BASIC_AUTH_HEADER = "Basic " + base64.b64encode((SENDER_BASIC_AUTH_USER + ":").encode()).decode()

EMAIL_SIMULATE_INTERNAL_ERROR = 'email@simulate_sender_internal_error.com'

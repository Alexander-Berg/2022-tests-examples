MONGODB_URI = ['mongodb://127.0.0.1/']

MONGODB_DB_NAME = 'lacmus2_unittest'

REDIS = dict(
    USE_SENTINEL=False,
    HOST='127.0.0.1',
    PORT=6379,
    DBNUM=7,
)

WSGI_LISTEN_ADDRESS = ('127.0.0.1', 4000)

ACCESS_CONTROL_ALLOW_ORIGIN = ' '.join([
  "http://127.0.0.1:4001",
])

DEBUG = 1
TESTING = 1

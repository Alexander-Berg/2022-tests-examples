from cab.settings import *

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
    }
}

# no WARNINGS in tests
import logging
logging.disable(logging.ERROR)

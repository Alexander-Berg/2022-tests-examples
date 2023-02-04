import warnings

from gevent import monkey

with warnings.catch_warnings():
    warnings.simplefilter("ignore")
    monkey.patch_all()

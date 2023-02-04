import os
import tempfile
import errno


def get_sock_path(name='test.sock'):
    return os.path.join(tempfile.gettempdir(), name)


def unlink(filepath):
    try:
        return os.remove(filepath)
    except OSError as e:
        if e.errno != errno.ENOENT:
            raise

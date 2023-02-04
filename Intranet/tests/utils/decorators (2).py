from contextlib import contextmanager


@contextmanager
def disconnect_signal(signal, receivers):
    for receiver in receivers:
        signal.disconnect(**receiver)

    yield

    for receiver in receivers:
        signal.connect(**receiver)

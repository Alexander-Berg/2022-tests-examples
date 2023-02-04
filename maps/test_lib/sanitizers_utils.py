from library.python.sanitizers import asan_is_on, msan_is_on


def is_sanitizer_on():
    return asan_is_on() or msan_is_on()


def sanitizer_aware_timeout(timeout, sanitizer_timeout=None):
    if not is_sanitizer_on():
        return timeout
    if sanitizer_timeout is not None:
        return sanitizer_timeout
    # From https://github.com/google/sanitizers/wiki/AddressSanitizer
    # "The average slowdown of the instrumented program is ~2x"
    return 2.5 * timeout

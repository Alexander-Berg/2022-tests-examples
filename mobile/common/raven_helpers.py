import six
from raven import breadcrumbs

__all__ = ('raven_tune',)


def logging_handler(logger, level, msg, args, kwargs):
    message = msg if isinstance(msg, six.string_types) else unicode(msg)
    return message.startswith('tskv')


def raven_tune(max_breadcrumbs):
    def make_buffer(enabled=True):
        if enabled:
            return breadcrumbs.BreadcrumbBuffer(limit=max_breadcrumbs)
        return breadcrumbs.make_buffer(False)

    breadcrumbs.make_buffer = make_buffer
    breadcrumbs.register_logging_handler(logging_handler)

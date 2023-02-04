import logging

logger = logging.getLogger(__name__)


class ChangeLoggingMixin(object):
    def save(self, **kwargs):
        logger.info('Model {model_name} was changed: {content}'.format(
            model_name=type(self).__name__, content=self.to_json()))

        return super(ChangeLoggingMixin, self).save(**kwargs)

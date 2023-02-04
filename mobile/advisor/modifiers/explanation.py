import logging

from yaphone.utils import geo

DEFAULT_LINGUISTICS_CASE = 'nominative'
FALLBACK_LANGUAGE = 'en'

logger = logging.getLogger(__name__)


class RegionExplanation(object):
    def __init__(self, region_id, language):
        self.language = str(language)
        self.region_id = region_id

    @property
    def linguistics(self):
        try:
            return geo.geobase_lookuper.get_linguistics(self.region_id, self.language)
        except RuntimeError:
            return None

    @property
    def linguistics_fallback(self):
        try:
            return geo.geobase_lookuper.get_linguistics(self.region_id, FALLBACK_LANGUAGE)
        except RuntimeError:
            return None

    @property
    def preposition(self):
        preposition = getattr(self.linguistics, 'preposition', None)
        if not preposition:
            preposition = 'in'
        return preposition.decode('utf-8')

    @property
    def prepositional(self):
        prepositional = getattr(self.linguistics, 'prepositional', None)
        if not prepositional:
            prepositional = getattr(self.linguistics_fallback, DEFAULT_LINGUISTICS_CASE, None)
        if not prepositional:
            raise ExplanationError('No prepositional linguistics for region',
                                   region=self.region_id, language=self.language)
        return prepositional.decode('utf-8')

    def __getattr__(self, item):
        attr = (getattr(self.linguistics, item, None) or
                getattr(self.linguistics_fallback, item, None) or
                getattr(self.linguistics_fallback, DEFAULT_LINGUISTICS_CASE, None))
        if not attr:
            raise ExplanationError('No %s linguistics for region' % item,
                                   region=self.region_id, language=self.language)
        return attr.decode('utf-8')


class ExplanationError(Exception):
    def __init__(self, message, **extra):
        super(ExplanationError, self).__init__(message)
        self.extra = extra

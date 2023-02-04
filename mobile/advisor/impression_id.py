from bson.objectid import ObjectId
from copy import copy
from rest_framework.exceptions import ValidationError

from yaphone.advisor.project.version import VERSION

FIELD_PARTS_SEPARATOR = ';'
ID_FIELDS_SEPARATOR = ':'

EXTERNAL_RECOMMENDERS_MAP = [
    ('facebook', '1'),
    ('direct', '2'),
]
EMPTY_EXTERNAL_RECOMMENDERS_CODE = '0'
DEFAULT_CONTENT_TYPE = 'apps'

ext_recommender_name_to_code = dict(EXTERNAL_RECOMMENDERS_MAP)
# noinspection PyTypeChecker
ext_recommender_code_to_name = dict(map(reversed, EXTERNAL_RECOMMENDERS_MAP))


class ImpressionID(object):
    def __init__(self, app='', experiment='', algorithm_code='',
                 position=(0, 0, 0), promo_enforcement_fields=(0, 0, 0),
                 external_recommenders=None, content_type=DEFAULT_CONTENT_TYPE):
        self.application = app
        self.experiment = experiment
        self.view_id = ObjectId()
        self.position = position
        self.promo_enforcement = promo_enforcement_fields
        self.external_recommenders = external_recommenders or []
        self.algorithm_code = algorithm_code
        self.clid = ''
        self.client = None
        self.jafar_version = ''
        self.content_type = content_type

    def to_dict(self):
        dict_repr = {
            key: getattr(self, key) for key in
            ('experiment', 'view_id', 'position', 'promo_enforcement', 'algorithm_code')
        }
        dict_repr['external_recommenders'] = copy(self.external_recommenders)
        return dict_repr

    def __str__(self):
        # external recommenders should keep order and be unique
        external_recommenders_codes = []
        for ext_recommender_name in self.external_recommenders:
            code = ext_recommender_name_to_code.get(ext_recommender_name, None)
            if code is not None and code not in external_recommenders_codes:
                external_recommenders_codes.append(code)
        # if no external recommender then special value EMPTY_EXTERNAL_RECOMMENDERS_CODE should be used.
        # for more info see https://wiki.yandex-team.ru/yandexmobile/advisor/impressionid/#formatimpressionid
        if not external_recommenders_codes:
            external_recommenders_codes.append(EMPTY_EXTERNAL_RECOMMENDERS_CODE)

        return ID_FIELDS_SEPARATOR.join([
            self.application,
            FIELD_PARTS_SEPARATOR.join((self.experiment, self.algorithm_code or '')),
            str(self.view_id),
            FIELD_PARTS_SEPARATOR.join(map(str, self.position)),
            FIELD_PARTS_SEPARATOR.join(map(str, self.promo_enforcement)),
            FIELD_PARTS_SEPARATOR.join(external_recommenders_codes),
            FIELD_PARTS_SEPARATOR.join([VERSION, self.jafar_version]),
            self.clid,
            str(self.client.user_agent.app_version) if self.client and self.client.user_agent else '',
            FIELD_PARTS_SEPARATOR.join([
                self.client.profile.country if self.client else '',
                self.client.profile.current_country if self.client else ''
            ]),
            self.content_type
        ])

    @classmethod
    def from_string(cls, impression_id_string):
        try:
            impression_fields = impression_id_string.split(ID_FIELDS_SEPARATOR)

            application = impression_fields[0]
            experiments_string = impression_fields[1]
            view_id_string = impression_fields[2]
            position_string = impression_fields[3]
            promo_enforcement_string = impression_fields[4]
            external_recommenders_string = impression_fields[5]
            content_type = impression_fields[6] if len(impression_fields) > 6 else DEFAULT_CONTENT_TYPE

            experiment, algorithm_code = experiments_string.split(FIELD_PARTS_SEPARATOR)
            promo_enforcement_options = promo_enforcement_string.split(FIELD_PARTS_SEPARATOR)

            external_recommenders = []
            for ext_recommender_code in external_recommenders_string.split(FIELD_PARTS_SEPARATOR):
                ext_recommender_name = ext_recommender_code_to_name.get(ext_recommender_code, None)
                if ext_recommender_name:
                    external_recommenders.append(ext_recommender_name)

            impression_id = cls(
                app=application,
                experiment=experiment,
                algorithm_code=algorithm_code,
                position=map(int, position_string.split(FIELD_PARTS_SEPARATOR)),
                promo_enforcement_fields=map(int, promo_enforcement_options[:3]),
                external_recommenders=external_recommenders,
                content_type=content_type
            )

            impression_id.view_id = ObjectId(view_id_string)
            return impression_id
        except (ValueError, KeyError, IndexError):
            raise ImpressionIDFormatError


# for test purposes
def impression_id_is_valid(impression_id_string):
    try:
        ImpressionID.from_string(impression_id_string)
        return True
    except ImpressionIDFormatError:
        return False


def impression_id_validator(impression_id_string):
    try:
        ImpressionID.from_string(impression_id_string)
    except ImpressionIDFormatError:
        raise ValidationError('Not valid impression_id')


class ImpressionIDFormatError(Exception):
    pass

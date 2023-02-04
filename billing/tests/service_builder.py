import re
from billing.apikeys.apikeys import mapper


class NameConverter:

    _re_camelto_first = re.compile('(.)([A-Z][a-z]+)')
    _re_camelto_all = re.compile('([a-z0-9])([A-Z])')

    @classmethod
    def camel_to_lowlineseparated(cls, s):
        s1 = cls._re_camelto_first.sub(r'\1_\2', s)
        return cls._re_camelto_all.sub(r'\1_\2', s1).lower()

    @classmethod
    def camel_to_spaceseparated(cls, s):
        s1 = cls._re_camelto_first.sub(r'\1 \2', s)
        return cls._re_camelto_all.sub(r'\1 \2', s1)


class AbstractServiceBuilder:

    _service = None

    @classmethod
    def create_service(cls, service_id):
        if cls._service is None:
            cls._service = mapper.Service()
            cls._service.id = service_id

    def set_name(self):
        name = self.__class__.__name__
        if name.endswith('Builder'):
            name = name[:-7]
        self._service.cc = f"autotest_{NameConverter.camel_to_lowlineseparated(name)}"
        self._service.name = f"[AUTOTEST] {NameConverter.camel_to_spaceseparated(name)}"
        self._service.eng_name = self._service.name

    def set_key_params(self):
        self._service.default_link_config.banned = False
        self._service.default_link_config.approved = True

    def set_units(self):
        self._service.units = ["hits"]

    def set_reasons(self):
        self._service.unlock_reasons = [17, 18]
        self._service.lock_reasons = [30, 31]

    def set_mail(self):
        self._service.mail_to = "666@devnull.yandex.ru"

    def set_attachable_in_ui(self):
        self._service.attachable_in_ui = False

    def create_limit_configs(self):
        pass

    def save_service(self):
        self._service._created = True
        self._service.save()

    def get_service(self):
        return self._service


class RegularServiceBuilder(AbstractServiceBuilder):
    pass


class AutoApprovedServiceBuilder(AbstractServiceBuilder):
    pass


class ManuallyApprovedServiceBuilder(AbstractServiceBuilder):

    def set_key_params(self):
        super().set_key_params()
        self._service.default_link_config.approved = False
        self._service.default_link_config.inactive_reason_id = 26


class MultiCounterServiceBuilder(AbstractServiceBuilder):
    def set_units(self):
        self._service.units = ["hits", "bytes"]


class AttachableServiceBuilder(AbstractServiceBuilder):
    def set_attachable_in_ui(self):
        self._service.attachable_in_ui = True


class QuestionnaireServiceBuilder(AttachableServiceBuilder):
    def set_key_params(self):
        self._service.default_link_config.questionnaire_id = 1029384756


class LimitedServiceBuilder(AbstractServiceBuilder):

    def create_limit_configs(self):
        limit_config = mapper.LimitConfig()
        limit_config.service_id = self._service.id
        limit_config.name = f'autotest_{self._service.cc}'
        limit_config.unit_id = 1
        limit_config.cron_string = '0 21 * * * *'
        limit_config.limit = 3000
        limit_config.lock_reason = 1
        limit_config.save()


class MultiLimitedServiceBuilder(AbstractServiceBuilder):
    def set_units(self):
        self._service.units = ["hits", "bytes"]

    def create_limit_configs(self):
        limit_config1 = mapper.LimitConfig()
        limit_config1.service_id = self._service.id
        limit_config1.name = f'autotest_{self._service.cc}_hits'
        limit_config1.unit_id = 1
        limit_config1.cron_string = '0 21 * * * *'
        limit_config1.limit = 3000
        limit_config1.lock_reason = 1
        limit_config1.save()

        limit_config2 = mapper.LimitConfig()
        limit_config2.service_id = self._service.id
        limit_config2.name = f'autotest_{self._service.cc}_bytes'
        limit_config2.unit_id = 2
        limit_config2.cron_string = '0 21 * * * *'
        limit_config2.limit = 3000
        limit_config2.lock_reason = 1
        limit_config2.save()


class ServiceBuilderDirector:

    _service_id = 666665

    @classmethod
    def next_service_id(cls):
        cls._service_id += 1
        return cls._service_id

    @classmethod
    def remove_test_objects(cls):
        mapper.LimitConfig.objects.filter(name__contains='autotest_').delete()
        mapper.Service.objects.filter(cc__contains='autotest_').delete()

    def __init__(self, builder_class):
        self._builder = builder_class()

    def build(self):
        """
        :rtype : mapper.Service
        """
        self._builder.create_service(self.next_service_id())
        self._builder.set_name()
        self._builder.set_key_params()
        self._builder.set_units()
        self._builder.set_reasons()
        self._builder.set_mail()
        self._builder.set_attachable_in_ui()
        self._builder.save_service()
        self._builder.create_limit_configs()
        return self._builder.get_service()

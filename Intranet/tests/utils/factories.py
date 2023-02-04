import factory

from intranet.crt.constants import CA_NAME


class AbcService(factory.DjangoModelFactory):
    external_id = factory.Sequence(lambda n: n)
    name = factory.Sequence(lambda n: f'service_name_{n}')
    slug = factory.Sequence(lambda n: f'service_slug_{n}')

    class Meta:
        model = 'django_abc_data.AbcService'


class User(factory.DjangoModelFactory):
    username = factory.Sequence(lambda n: f'user{n}')

    class Meta:
        model = 'users.CrtUser'


class CrtGroup(factory.DjangoModelFactory):
    external_id = factory.Sequence(lambda n: n)
    name = factory.Sequence(lambda n: f'group_name_{n}')
    url = factory.Sequence(lambda n: f'group_url_{n}')
    abc_service = factory.SubFactory(AbcService)
    is_deleted = False

    class Meta:
        model = 'users.CrtGroup'

    @classmethod
    def _setup_next_sequence(cls):
        return 1


class CertificateType(factory.DjangoModelFactory):
    name = 'host'

    class Meta:
        model = 'core.CertificateType'
        django_get_or_create = ('name', )


class Certificate(factory.DjangoModelFactory):
    requester = factory.SubFactory(User)
    type = factory.SubFactory(CertificateType)
    requested_by_csr = False

    ca_name = CA_NAME.CERTUM_TEST_CA

    class Meta:
        model = 'core.Certificate'


class HostToApprove(factory.DjangoModelFactory):
    host = factory.Sequence(lambda n: 'yandex-{}.ru'.format(n))

    class Meta:
        model = 'core.HostToApprove'


class HostValidationCode(factory.DjangoModelFactory):
    host = factory.SubFactory(HostToApprove)
    certificate = factory.SubFactory(Certificate)
    code = factory.Sequence(lambda n: 'c{}'.format(n))

    class Meta:
        model = 'core.HostValidationCode'


class HostToApproveHistory(factory.DjangoModelFactory):
    host = factory.SubFactory(HostToApprove)
    certificate = factory.SubFactory(Certificate)
    action = 'added'
    validation_code = factory.Sequence(lambda n: 'c{}'.format(n))

    class Meta:
        model = 'core.HostToApproveHistory'

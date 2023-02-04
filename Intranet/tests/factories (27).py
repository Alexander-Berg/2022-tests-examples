import factory

from staff.lib.testing import StaffFactory


class EmailFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'emails.Email'


class EmailRedirectionFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'emails.EmailRedirection'

    from_person = factory.SubFactory(StaffFactory)
    to_person = factory.SubFactory(StaffFactory)

from typing import Any, Sequence

import factory
import faker
from factory import post_generation
from factory.django import DjangoModelFactory

from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission
from django.contrib.contenttypes import models as contenttypes_models
from django.contrib.contenttypes.models import ContentType

from ..models import Group, PermissionPreset, ServiceAccount

User = get_user_model()
fake = faker.Faker()


class UserFactory(DjangoModelFactory):
    username = factory.Sequence(lambda n: f"{fake.user_name()}-{n}")
    email = factory.Faker("email")
    yauid = factory.Faker('pyint', min_value=100000, max_value=9999999)

    @post_generation
    def password(self, create: bool, extracted: Sequence[Any], **kwargs):
        password = (
            extracted
            if extracted
            else factory.Faker(
                "password",
                length=42,
                special_chars=True,
                digits=True,
                upper_case=True,
                lower_case=True,
            ).evaluate(None, None, extra={"locale": None})
        )
        self.set_password(password)

    class Meta:
        model = User
        django_get_or_create = ('username', 'email', 'yauid',)


class LabUserFactory(UserFactory):

    @post_generation
    def set_permissions(self, create: bool, extracted: Sequence[Any], **kwargs):
        content_type = ContentType.objects.get_for_model(User)
        can_view_permission = Permission.objects.get(
            codename='can_view_in_lab',
            content_type=content_type,
        )
        can_edit_permission = Permission.objects.get(
            codename='can_edit_in_lab',
            content_type=content_type,
        )

        self.user_permissions.set([
            can_view_permission,
            can_edit_permission,
        ])


class GroupFactory(DjangoModelFactory):
    name = factory.Sequence(lambda n: f"{fake.street_name()}-{n}")

    class Meta:
        model = Group


class PermissionFactory(DjangoModelFactory):
    name = factory.Sequence(lambda n: f"{fake.word().lower()}-permission-{n}")
    content_type = factory.Iterator(contenttypes_models.ContentType.objects.all())
    codename = factory.Sequence(lambda n: f"permission-{n}")

    class Meta:
        model = Permission


class PermissionPresetFactory(DjangoModelFactory):
    name = factory.Sequence(lambda n: f"{fake.color_name().lower()}-preset-{n}")

    class Meta:
        model = PermissionPreset


class ServiceAccountFactory(DjangoModelFactory):
    name = factory.Sequence(lambda n: "ServiceAccount Name %03d" % n)

    class Meta:
        model = ServiceAccount

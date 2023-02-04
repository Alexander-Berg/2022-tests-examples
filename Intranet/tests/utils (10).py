import factory

from staff.person_avatar.models import AvatarMetadata


class AvatarMetadataFactory(factory.DjangoModelFactory):
    class Meta:
        model = AvatarMetadata

import pytest
from unittest.mock import patch

from django.conf import settings

from staff.lib.testing import StaffFactory, DepartmentFactory
from staff.person_avatar.models import AvatarMetadata
from staff.person_avatar.tests.utils import AvatarMetadataFactory
from staff.preprofile.tests.utils import PreprofileFactory

from staff.person_profile.controllers.photos import PhotosCtl


@pytest.mark.django_db
@patch('staff.lib.requests.get')
@patch('staff.person_profile.controllers.photos.TVM_HEADERS')
def test_delete_all_photos(tvm_headers_patch, get_patch):
    staff = StaffFactory()
    other_staff = StaffFactory()
    pre_profile = PreprofileFactory(login=staff.login, department=DepartmentFactory())
    other_pre_profile = PreprofileFactory(login=other_staff.login, department=DepartmentFactory())

    image_types = ('-main', '-avatar', '')
    removing_avatars_ids = [staff.login] + [
        AvatarMetadataFactory(person=staff).id,
        AvatarMetadataFactory(preprofile=pre_profile).id,
    ]
    keep_avatars = [
        AvatarMetadataFactory(person=other_staff),
        AvatarMetadataFactory(preprofile=other_pre_profile),
    ]

    target = PhotosCtl(staff)

    target.delete_all_photos()

    patch_args = [x[0] for x in get_patch.call_args_list]

    for image_name in removing_avatars_ids:
        for image_type in image_types:
            assert (f'{settings.AVATAR_STORAGE_URL}/delete-staff/{image_name}{image_type}',) in patch_args

    expected_patch_kwargs = {'headers': tvm_headers_patch, 'timeout': settings.AVATAR_UPLOAD_TIMEOUT}
    assert all([x[1] == expected_patch_kwargs for x in get_patch.call_args_list])
    assert get_patch.call_count == len(removing_avatars_ids) * len(image_types)

    remaining_ids = sorted(AvatarMetadata.objects.all().values_list('id', flat=True))
    assert remaining_ids == sorted([x.id for x in keep_avatars]), 'Wrong avatars removed'

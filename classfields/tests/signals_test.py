from unittest import mock

from django.contrib.admin.models import DELETION, LogEntryManager
from django.db.models.signals import post_delete

from parts.models import PartInfo

USER_ID = 999


class Test:
    @mock.patch.object(LogEntryManager, "log_action")
    @mock.patch("parts.signals.current_user")
    @mock.patch("parts.signals.ContentType")
    def test_handle_deletion_billing(self, content_type_klass, current_user, log_action: mock.MagicMock):
        part_content_id = 12
        content_type = mock.MagicMock()
        content_type.id = content_type.pk = part_content_id
        content_type_klass.objects.get_for_model.return_value = content_type

        user_id = 8888
        current_user.return_value = mock.MagicMock()
        current_user.return_value.id = user_id
        wanted = mock.MagicMock()
        wanted.id = wanted.pk = 9999
        post_delete.send(sender=PartInfo, instance=wanted)
        log_action.assert_called()
        assert log_action.call_args == mock.call(
            user_id=user_id,
            content_type_id=part_content_id,
            object_id=wanted.id,
            object_repr=repr(wanted),
            action_flag=DELETION,
        )

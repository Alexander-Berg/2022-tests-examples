from mock import patch

from staff.groups.models import Group

from staff.departments.controllers.group import DepartmentGroupCtl


def test_proxy_functiona():
    pass


class CallCounter(object):

    def __init__(self):
        self.called = 0

    def __call__(self, *args, **kwargs):
        self.called += 1


def test_save_descendants(company):
    """
    Проверяем что при сохранении группы через контроллер пересохраняются все её потомки
    """
    for group in Group.objects.all():
        ctl = DepartmentGroupCtl(group_instance=group)
        descendants_count = ctl.get_descendants().count()

        save_calls_counter = CallCounter()

        with patch('mptt.models.MPTTModel.save', side_effect=save_calls_counter):
            ctl.save()

        assert save_calls_counter.called == descendants_count + 1

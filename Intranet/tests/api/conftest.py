import pytest
import vcr
import yatest

from django.conf import settings
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings

from intranet.audit.src.core import models
from intranet.audit.src.files.models import File
from intranet.audit.src.users.models import StatedPerson, User


@pytest.fixture
def test_vcr():
    path = yatest.common.source_path('intranet/audit/vcr_cassettes')
    return vcr.VCR(cassette_library_dir=path)


@pytest.fixture
def author(db):
    return User.objects.create(
        login='test login',
        uid=123,
        last_name='test last name',
        first_name='test name',
    )


@pytest.fixture
def process(db, author):
    return models.Process.objects.create(
        process_type=models.Process.TYPES.root,
        name='process_one',
        author=author
    )


@pytest.fixture
def process_two(db, author):
    return models.Process.objects.create(
        process_type=models.Process.TYPES.root,
        name='process_two',
        author=author
    )


@pytest.fixture
def process_three(db, author):
    return models.Process.objects.create(
        process_type=models.Process.TYPES.root,
        name='someothername',
        author=author
    )


@pytest.fixture
def sub_process(db, author, process_three):
    return models.Process.objects.create(
        process_type=models.Process.TYPES.subprocess,
        name='subprocess_one',
        author=author,
        parent=process_three,
    )


@pytest.fixture
def sub_process_two(db, author, process_three):
    return models.Process.objects.create(
        process_type=models.Process.TYPES.subprocess,
        name='subprocess_two',
        author=author,
        parent=process_three,
    )


@pytest.fixture
def sub_process_three(db, author, process_three):
    return models.Process.objects.create(
        process_type=models.Process.TYPES.subprocess,
        name='subprocess_three',
        author=author,
        parent=process_three,
    )


@pytest.fixture
def risk(db, author):
    return models.Risk.objects.create(
        number='risk number',
        name='risk',
        author=author,
    )


@pytest.fixture
def control(db, author):
    return models.Control.objects.create(
        number='control number',
        name='control',
        author=author,
    )


@pytest.fixture
def control_plan(db, process, risk, control, author):
    control_plan_obj = models.ControlPlan(
        control=control,
        method=models.ControlPlan.METHODS.manual,
        control_type=models.ControlPlan.TYPES.warning,
        author=author,
        frequency=models.ControlPlan.FREQUENCIES.adhoc,
        comment='comment for one',
    )
    control_plan_obj.save()
    control_plan_obj.process.add(process)
    control_plan_obj.risk.add(risk)
    return control_plan_obj


@pytest.fixture
def control_plan_two(db, process_two, risk, control, author):
    control_plan_obj = models.ControlPlan(
        control=control,
        method=models.ControlPlan.METHODS.auto,
        control_type=models.ControlPlan.TYPES.warning,
        author=author,
        frequency=models.ControlPlan.FREQUENCIES.adhoc,
        comment='comment for two',
    )
    control_plan_obj.save()
    control_plan_obj.process.add(process_two)
    control_plan_obj.risk.add(risk)
    return control_plan_obj


@pytest.fixture
def control_plan_with_subprocess(db, sub_process_two, risk, control, author):
    control_plan_obj = models.ControlPlan(
        control=control,
        method=models.ControlPlan.METHODS.auto,
        control_type=models.ControlPlan.TYPES.warning,
        author=author,
        frequency=models.ControlPlan.FREQUENCIES.adhoc,
        comment='comment for two',
    )
    control_plan_obj.save()
    control_plan_obj.process.add(sub_process_two)
    control_plan_obj.risk.add(risk)
    return control_plan_obj


@pytest.fixture
def control_plan_three(db, process, risk, control, author):
    control_plan_obj = models.ControlPlan(
        control=control,
        method=models.ControlPlan.METHODS.manual,
        control_type=models.ControlPlan.TYPES.warning,
        author=author,
        frequency=models.ControlPlan.FREQUENCIES.adhoc,
    )
    control_plan_obj.save()
    control_plan_obj.process.add(process)
    control_plan_obj.risk.add(risk)
    return control_plan_obj


@pytest.fixture
def control_step(db, author, control_test):
    return models.ControlStep.objects.create(
        author=author,
        step='some step',
        control_test=control_test,
        comment='some comment',
    )


@pytest.fixture
def control_test(db, author, control_plan):
    return models.ControlTest.objects.create(
        author=author,
        control_plan=control_plan,
        comment='some comment',
    )


@pytest.fixture
def account(db, author):
    return models.Account.objects.create(
        name='account',
        author=author,
    )


@pytest.fixture
def business_unit(db, author):
    return models.BusinessUnit.objects.create(
        name='business unit',
        author=author,
    )


@pytest.fixture
def deficiency(db, author):
    return models.Deficiency.objects.create(
        short_description='some description',
        author=author,
    )


@pytest.fixture
def deficiency_group(db, author):
    return models.DeficiencyGroup.objects.create(
        author=author,
    )


@pytest.fixture
def ipe(db, author):
    return models.IPE.objects.create(
        name='name',
        author=author,
    )


@pytest.fixture
def legal(db, author):
    return models.Legal.objects.create(
        name='legal',
        author=author,
    )


@pytest.fixture
def service(db, author):
    return models.Service.objects.create(
        name='service',
        author=author,
    )


@pytest.fixture
def system(db, author):
    return models.System.objects.create(
        name='system',
        author=author,
    )


@pytest.fixture
@override_settings(DEFAULT_FILE_STORAGE='django.core.files.storage.FileSystemStorage')
def file(db, author):
    return File.objects.create(
        file=SimpleUploadedFile('some_name.txt', b'some text data'),
        author=author,
    )


@pytest.fixture
def assertion(db, author):
    return models.Assertion.objects.create(
        name='assertion name',
        author=author,
    )


@pytest.fixture
def default_queries_count():
    """
    Число sql запросов при обращении
    к любой ручке в тестах будет минимум 4:
    1. SAVEPOINT;
    2. RELEASE SAVEPOINT;
    3. создание пользователя;
    4. извлечение его из БД;

    При написании тестов на количество запросов следует это учитывать:
    django_assert_num_queries(default_queries_count+число_запросов_в_ручке).
    """
    return 4


@pytest.fixture
def controltestipe(ipe, control_test, author):
    return models.ControlTestIPE.objects.create(
        ipe=ipe,
        control_test=control_test,
        author=author
    )


@pytest.fixture
def stated_person(ipe, control_test, author):
    return StatedPerson.objects.create(
        uid=settings.TEST_USER_UID,
        login='volozh',
        first_name='Аркадий',
        last_name='Волож',
        department='Yandex',
        department_slug='ya',
        position='VIP',
    )

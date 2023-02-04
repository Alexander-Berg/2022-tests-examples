import datetime
import hashlib
import uuid

import factory

from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.models import Permission, Group
from django.utils import timezone

from intranet.femida.src.actionlog.models import SNAPSHOT_REASONS
from intranet.femida.src.candidates.models import Consideration
from intranet.femida.src.candidates.choices import (
    CANDIDATE_STATUSES,
    CANDIDATE_RESPONSIBLE_ROLES,
    SUBMISSION_SOURCES,
    SUBMISSION_STATUSES,
    VERIFICATION_STATUSES,
    VERIFICATION_RESOLUTIONS,
    CONSIDERATION_ISSUE_LEVELS,
    CONSIDERATION_STATUSES,
)
from intranet.femida.src.communications.choices import (
    EXTERNAL_MESSAGE_TYPES,
    MESSAGE_STATUSES,
    MESSAGE_TYPES,
)
from intranet.femida.src.core.models import Currency, Country
from intranet.femida.src.hire_orders.choices import HIRE_ORDER_STATUSES, HIRE_ORDER_RESOLUTIONS
from intranet.femida.src.interviews.choices import (
    APPLICATION_STATUSES,
    APPLICATION_RESOLUTIONS,
    INTERVIEW_ROUND_TYPES,
    INTERVIEW_TYPES,
    INTERVIEW_STATES,
)
from intranet.femida.src.interviews.models import Interview
from intranet.femida.src.offers.choices import (
    CONTRACT_TYPES,
    INTERNAL_EMPLOYEE_TYPES,
    OFFER_STATUSES,
)
from intranet.femida.src.publications.choices import (
    PUBLICATION_LANGUAGES,
    PUBLICATION_STATUSES,
    PUBLICATION_TYPES,
)
from intranet.femida.src.staff.choices import DEPARTMENT_ROLES
from intranet.femida.src.utils.datetime import shifted_now
from intranet.femida.src.vacancies.choices import (
    VACANCY_STATUSES,
    VACANCY_ROLES,
    VACANCY_PRO_LEVELS,
)

from intranet.femida.tests.utils import get_forms_constructor_data
from intranet.femida.tests import raw_template


here = 'intranet.femida.tests.factories'

User = get_user_model()


class UserFactory(factory.DjangoModelFactory):

    class Meta:
        model = settings.AUTH_USER_MODEL

    username = factory.Sequence(lambda n: 'user{}'.format(n))
    first_name = factory.Sequence(lambda n: 'First{}'.format(n))
    first_name_en = factory.Sequence(lambda n: 'FirstEn{}'.format(n))
    last_name = factory.Sequence(lambda n: 'Last{}'.format(n))
    last_name_en = factory.Sequence(lambda n: 'LastEn{}'.format(n))
    email = factory.LazyAttribute(lambda obj: '{}@yandex-team.ru'.format(obj.username))
    phone = factory.Sequence(lambda n: '+7000000{}'.format(n))
    work_phone = factory.Sequence(lambda n: int('100{}'.format(n)))
    department = factory.SubFactory(f'{here}.DepartmentFactory')


class AttachmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'attachments.Attachment'

    uploader = factory.SubFactory(f'{here}.UserFactory')
    name = factory.Sequence(lambda n: 'file{}'.format(n))
    attached_file = factory.django.FileField(filename='file.pdf', data=b'data')


class CategoryFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'problems.Category'

    name = factory.Sequence(lambda n: 'category{}'.format(n))


class PresetFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'problems.Preset'

    name = factory.Sequence(lambda n: 'preset{}'.format(n))
    created_by = factory.SubFactory(f'{here}.UserFactory')


class ProblemFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'problems.Problem'

    summary = factory.Sequence(lambda n: 'summary{}'.format(n))
    description = factory.Sequence(lambda n: 'description{}'.format(n))
    solution = factory.Sequence(lambda n: 'solution{}'.format(n))
    created_by = factory.SubFactory(f'{here}.UserFactory')


class PresetProblemFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'problems.PresetProblem'

    preset = factory.SubFactory(f'{here}.PresetFacotry')
    problem = factory.SubFactory(f'{here}.ProblemFactory')


class ComplaintFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'problems.Complaint'

    problem = factory.SubFactory(f'{here}.ProblemFactory')
    created_by = factory.SubFactory(f'{here}.UserFactory')


class CategorySubscriptionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'notifications.CategorySubscription'

    category = factory.SubFactory(f'{here}.Category')
    user = factory.SubFactory(f'{here}.User')


class CandidateFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.Candidate'

    first_name = factory.Sequence(lambda n: 'First{}'.format(n))
    middle_name = factory.Sequence(lambda n: 'Middle{}'.format(n))
    last_name = factory.Sequence(lambda n: 'Last{}'.format(n))
    created_by = factory.SubFactory(f'{here}.UserFactory')


class CandidateContactFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateContact'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    account_id = factory.Sequence(lambda n: 'Account_Id_{}'.format(n))


class CandidateCostsSetFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateCostsSet'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    created_by = factory.SubFactory(f'{here}.UserFactory')


class CandidateCostFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateCost'

    candidate_costs_set = factory.SubFactory(f'{here}.CandidateCostsSetFactory')
    cost_group = 'expectation'
    type = 'total'
    value = 100500
    currency = factory.LazyAttribute(lambda obj: currency())
    taxed = True
    comment = 'comment'


class CandidateEducationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateEducation'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')


class CandidateJobFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateJob'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')


class CandidateProfessionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateProfession'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    profession = factory.SubFactory(f'{here}.ProfessionFactory')
    professional_sphere = factory.LazyAttribute(lambda obj: obj.profession.professional_sphere)


class CandidateCityFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateCity'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    city = factory.SubFactory(f'{here}.CityFactory')


class CandidateSkillFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateSkill'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    skill = factory.SubFactory(f'{here}.SkillFactory')
    confirmed_by = []


class CandidateTagFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateTag'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    tag = factory.SubFactory(f'{here}.TagFactory')


class CandidateScoringFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateScoring'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    scoring_category = factory.SubFactory(f'{here}.ScoringCategoryFactory')
    scoring_value = factory.Sequence(lambda n: n)
    version = '1'


class CandidateResponsibleFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateResponsible'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    user = factory.SubFactory(f'{here}.UserFactory')


class CandidateAttachmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateAttachment'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    attachment = factory.SubFactory(f'{here}.AttachmentFactory')


class DuplicationCaseFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.DuplicationCase'

    first_candidate = factory.SubFactory(f'{here}.CandidateFactory')
    second_candidate = factory.SubFactory(f'{here}.CandidateFactory')


class VerificationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.Verification'

    created_by = factory.SubFactory(f'{here}.UserFactory')
    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    application = factory.SubFactory(
        factory=f'{here}.ApplicationFactory',
        candidate=factory.SelfAttribute('..candidate'),
    )
    uuid = factory.LazyAttribute(lambda obj: uuid.uuid4())


class MessageFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'communications.Message'

    application = factory.SubFactory(f'{here}.ApplicationFactory')
    candidate = factory.LazyAttribute(lambda obj: obj.application.candidate)
    author = factory.SubFactory(f'{here}.UserFactory')
    text = factory.Sequence(lambda n: 'Text{}'.format(n))


class ReminderFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'communications.Reminder'

    user = factory.SubFactory(f'{here}.UserFactory')
    message = factory.SubFactory(f'{here}.MessageFactory')
    remind_at = factory.LazyAttribute(lambda n: shifted_now(days=1))


class MessageAttachmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'communications.MessageAttachment'

    message = factory.SubFactory(f'{here}.MessageFactory')
    attachment = factory.SubFactory(f'{here}.AttachmentFactory')


class MessageTemplateFactory(factory.DjangoModelFactory):

    category_name = factory.Sequence(lambda n: 'Category{}'.format(n))
    name = factory.Sequence(lambda n: 'Name{}'.format(n))
    subject = factory.Sequence(lambda n: 'Subject{}'.format(n))
    text = factory.Sequence(lambda n: 'Text{}'.format(n))

    class Meta:
        model = 'communications.MessageTemplate'


class SubmissionFormFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.SubmissionForm'

    title = factory.Sequence(lambda n: 'Title{}'.format(n))


class ReferenceFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.Reference'

    created_by = factory.SubFactory(f'{here}.UserFactory')
    processed_by = factory.SubFactory(f'{here}.UserFactory')
    startrek_key = factory.Sequence(lambda n: 'REFERENCE-{}'.format(n))


class RotationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.Rotation'

    created_by = factory.SubFactory(f'{here}.UserFactory')
    startrek_rotation_key = factory.Sequence(lambda n: 'ROTATION-{}'.format(n))
    startrek_myrotation_key = factory.Sequence(lambda n: 'MYROTATION-{}'.format(n))


class SubmissionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.CandidateSubmission'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    form = factory.SubFactory(f'{here}.SubmissionFormFactory')
    reference = factory.SubFactory(f'{here}.ReferenceFactory')
    rotation = factory.SubFactory(f'{here}.RotationFactory')
    source = SUBMISSION_SOURCES.form
    forms_data = factory.LazyAttribute(lambda obj: {})


class VacancySkillFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.VacancySkill'

    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    skill = factory.SubFactory(f'{here}.SkillFactory')


class VacancyCityFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.VacancyCity'

    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    city = factory.SubFactory(f'{here}.CityFactory')


class VacancyLocationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.VacancyLocation'

    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    location = factory.SubFactory(f'{here}.LocationFactory')


class VacancyMembershipFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.VacancyMembership'

    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    member = factory.SubFactory(f'{here}.UserFactory')


class VacancyFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.Vacancy'

    name = factory.Sequence(lambda n: 'Name{}'.format(n))
    created_by = factory.SubFactory(f'{here}.UserFactory')
    profession = factory.SubFactory(f'{here}.ProfessionFactory')
    professional_sphere = factory.LazyAttribute(lambda obj: obj.profession.professional_sphere)
    budget_position_id = factory.Sequence(lambda n: n)
    status = VACANCY_STATUSES.in_progress
    department = factory.SubFactory(f'{here}.DepartmentFactory')
    publication_title = factory.Sequence(lambda n: 'Title{}'.format(n))
    publication_content = factory.Sequence(lambda n: 'Publication{} __with__ %%WF%%'.format(n))
    raw_issue_data = '{"fields": {}}'
    value_stream = factory.SubFactory(f'{here}.ValueStreamFactory')


class VacancyGroupFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.VacancyGroup'

    created_by = factory.SubFactory(f'{here}.UserFactory')
    name = factory.Sequence(lambda n: 'VacancyGroup{}'.format(n))
    is_active = True


class VacancyGroupMembershipFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.VacancyGroupMembership'

    vacancy_group = factory.SubFactory(f'{here}.VacancyGroupFactory')
    member = factory.SubFactory(f'{here}.UserFactory')


class PublicationSubscriptionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'vacancies.PublicationSubscription'

    created_by = factory.SubFactory(f'{here}.UserFactory')
    pro_level_min = VACANCY_PRO_LEVELS.intern
    pro_level_max = VACANCY_PRO_LEVELS.expert
    only_active = True
    department = factory.SubFactory(f'{here}.DepartmentFactory')
    external_url = 'https://yandex.ru/jobs/vacancies/some/url/'
    sha1 = factory.Sequence(
        lambda n: hashlib.sha1(str(n).encode('utf-8')).hexdigest(),
    )


class ConsiderationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.Consideration'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    created_by = factory.SubFactory(f'{here}.UserFactory')


class ConsiderationResponsibleFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.ConsiderationResponsible'

    consideration = factory.SubFactory(f'{here}.ConsiderationFactory')
    user = factory.SubFactory(f'{here}.UserFactory')


class ConsiderationIssueFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.ConsiderationIssue'

    consideration = factory.SubFactory(f'{here}.ConsiderationFactory')
    type = factory.Sequence(lambda n: f'Type{n}')
    level = CONSIDERATION_ISSUE_LEVELS.danger


class ApplicationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'interviews.Application'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    consideration = factory.SubFactory(
        factory=f'{here}.ConsiderationFactory',
        candidate=factory.SelfAttribute('..candidate'),
    )
    created_by = factory.SubFactory(f'{here}.UserFactory')
    vacancy = factory.SubFactory(f'{here}.VacancyFactory')


class InterviewFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'interviews.Interview'

    application = factory.SubFactory(f'{here}.ApplicationFactory')
    consideration = factory.SubFactory(f'{here}.ConsiderationFactory')
    candidate = factory.LazyAttribute(lambda obj: obj.application.candidate)
    interviewer = factory.SubFactory(f'{here}.UserFactory')
    created_by = factory.SubFactory(f'{here}.UserFactory')
    section = factory.Sequence(lambda n: 'Section{}'.format(n))


class InterviewRoundFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'interviews.InterviewRound'

    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    consideration = factory.SubFactory(
        factory=f'{here}.ConsiderationFactory',
        candidate=factory.SelfAttribute('..candidate'),
    )
    created_by = factory.SubFactory(f'{here}.UserFactory')
    office = factory.SubFactory(f'{here}.OfficeFactory')
    type = INTERVIEW_ROUND_TYPES.onsite
    message = factory.SubFactory(
        factory=f'{here}.MessageFactory',
        candidate=factory.SelfAttribute('..candidate'),
        application=None,
        author=factory.SelfAttribute('..created_by'),
    )


class AssignmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'interviews.Assignment'

    problem = factory.SubFactory(f'{here}.ProblemFactory')
    interview = factory.SubFactory(f'{here}.InterviewFactory')


class ChallengeFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.Challenge'

    consideration = factory.SubFactory(f'{here}.ConsiderationFactory')
    submission = factory.SubFactory(f'{here}.SubmissionFactory')
    candidate = factory.LazyAttribute(lambda obj: obj.application.candidate)
    application = factory.SubFactory(f'{here}.ApplicationFactory')
    reviewed_by = factory.SubFactory(f'{here}.UserFactory')


class CommentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'comments.Comment'

    text = factory.Sequence(lambda n: 'Text{}'.format(n))
    created_by = factory.SubFactory(f'{here}.UserFactory')


class PositionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.Position'

    name_ru = factory.Sequence(lambda n: 'Должность{}'.format(n))
    name_en = factory.Sequence(lambda n: 'Position{}'.format(n))


class OfferFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.Offer'

    application = factory.SubFactory(f'{here}.ApplicationFactory')
    vacancy = factory.LazyAttribute(lambda obj: obj.application.vacancy)
    candidate = factory.LazyAttribute(lambda obj: obj.application.candidate)
    department = factory.SubFactory(f'{here}.DepartmentFactory')
    join_at = factory.LazyAttribute(lambda obj: timezone.now().date() + datetime.timedelta(days=30))
    contract_type = CONTRACT_TYPES.fixed_term
    contract_term = 3
    office = factory.SubFactory(f'{here}.OfficeFactory')
    full_name = factory.Sequence(lambda n: 'FullName{}'.format(n))
    creator = factory.SubFactory(f'{here}.UserFactory')
    org = factory.SubFactory(f'{here}.OrganizationFactory')
    payment_currency = factory.LazyAttribute(lambda obj: currency())
    position = factory.SubFactory(f'{here}.PositionFactory')
    schemes_data = factory.RelatedFactory(
        factory=f'{here}.OfferSchemesDataFactory',
        factory_related_name='offer',
    )


class InternalOfferFactory(OfferFactory):

    employee_type = INTERNAL_EMPLOYEE_TYPES.rotation

    class Meta:
        model = 'offers.InternalOffer'


class RawTemplateFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.RawTemplate'

    name = factory.Sequence(lambda n: 'Template{}'.format(n))
    text = raw_template.text


class OfferProfileFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.OfferProfile'

    offer = factory.SubFactory(f'{here}.OfferFactory')
    first_name = factory.Sequence(lambda n: 'First{}'.format(n))
    last_name = factory.Sequence(lambda n: 'Last{}'.format(n))


class OfferAttachmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.OfferAttachment'

    offer = factory.SubFactory(f'{here}.OfferFactory')
    attachment = factory.SubFactory(f'{here}.AttachmentFactory')

    # TODO: удалить после релиза FEMIDA-3368
    offer_profile = factory.SubFactory(f'{here}.OfferProfileFactory')


class BrochureFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.Brochure'

    name = factory.Sequence(lambda n: 'Brochure{}'.format(n))
    attachment = factory.SubFactory(f'{here}.AttachmentFactory')


class OfferSchemesDataFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.OfferSchemesData'

    offer = factory.SubFactory(f'{here}.OfferFactory')


class PreprofileFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.Preprofile'

    id = factory.Sequence(lambda n: n)


class PreprofileAttachmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.PreprofileAttachment'

    preprofile = factory.SubFactory(f'{here}.PreprofileFactory')
    attachment = factory.SubFactory(f'{here}.AttachmentFactory')


class OfferLinkFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'offers.Link'

    offer = factory.SubFactory(f'{here}.OfferFactory')
    preprofile = factory.SubFactory(f'{here}.PreprofileFactory')
    expiration_time = factory.LazyAttribute(lambda obj: shifted_now(days=7))


class OrganizationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.Organization'

    name = factory.Sequence(lambda n: 'Org{}'.format(n))
    startrek_id = factory.Sequence(lambda n: 'StartrekOrg{}'.format(n))


class OfficeFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.Office'

    city = factory.SubFactory(f'{here}.CityFactory')


class DepartmentFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.Department'

    # Избегаем коллизий по id с департаментами, у которых id фиксированы в настройках
    # См. settings.YANDEX_DEPARTMENT_ID, BOOTCAMP_BACK_DEPARTMENT_ID
    id = factory.Sequence(lambda n: n + 10000)
    group_id = factory.Sequence(lambda n: n)
    url = factory.Sequence(lambda n: f'dep_{n}')
    tags = factory.Sequence(lambda n: list())
    name = factory.Sequence(lambda n: f"Department_{n}")


class DepartmentUserFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.DepartmentUser'

    department = factory.SubFactory(f'{here}.DepartmentFactory')
    user = factory.SubFactory('test.factories.UserFactory')


class DepartmentAdaptationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.DepartmentAdaptation'

    department = factory.SubFactory(f'{here}.DepartmentFactory')


class ServiceFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.Service'

    group_id = factory.Sequence(lambda n: n)
    slug = factory.Sequence(lambda n: f'slug_{n}')
    url = factory.LazyAttribute(lambda obj: f'svc_{obj.slug}')


class TagFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'core.Tag'

    name = factory.Sequence(lambda n: 'Tag{}'.format(n))


class ScoringCategoryFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'candidates.ScoringCategory'

    name_ru = factory.Sequence(lambda n: 'Разновидность скоринга{}'.format(n))
    name_en = factory.Sequence(lambda n: 'ScoringCategory{}'.format(n))
    is_active = True


class CityFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'core.City'

    slug = factory.Sequence(lambda n: f'city-{n}')
    name_ru = factory.Sequence(lambda n: 'Город{}'.format(n))
    name_en = factory.Sequence(lambda n: 'City{}'.format(n))
    country = factory.LazyAttribute(lambda obj: country())


class LocationFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'core.Location'

    geo_id = factory.Sequence(lambda n: n)
    name_ru = factory.Sequence(lambda n: 'Локация{}'.format(n))
    name_en = factory.Sequence(lambda n: 'Location{}'.format(n))


class ProfessionalSphereFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'professions.ProfessionalSphere'

    name = factory.Sequence(lambda n: 'Sphere{}'.format(n))


class PublicProfessionalSphereFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'public_professions.PublicProfessionalSphere'


class ProfessionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'professions.Profession'

    slug = factory.Sequence(lambda n: f'profession-{n}')
    name = factory.Sequence(lambda n: 'Profession{}'.format(n))
    professional_sphere = factory.SubFactory(f'{here}.ProfessionalSphereFactory')


class PublicProfessionFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'public_professions.PublicProfession'

    slug = factory.Sequence(lambda n: f'public-profession-{n}')
    name_en = factory.Sequence(lambda n: 'PublicProfession{}'.format(n))
    name_ru = factory.Sequence(lambda n: 'ПубличнаяПрофессия{}'.format(n))
    professional_sphere = factory.SubFactory(f'{here}.ProfessionalSphereFactory')
    public_professional_sphere = factory.SubFactory(f'{here}.PublicProfessionalSphereFactory')

    @factory.post_generation
    def professions(self, create, professions, **kwargs):
        """
        Позволяет передавать professions=[profession, profession2, ...] в конструктор
        """
        if create and professions:
            for profession in professions:
                self.professions.add(profession)


class SkillFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'skills.Skill'

    name = factory.Sequence(lambda n: 'Skill{}'.format(n))


class SwitchFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'waffle.Switch'


class FlagFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'waffle.Flag'


class LogRecordFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'actionlog.LogRecord'

    user = factory.SubFactory(f'{here}.UserFactory')


class SnapshotFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'actionlog.Snapshot'

    log_record = factory.SubFactory(f'{here}.LogRecordFactory')
    reason = SNAPSHOT_REASONS.addition


class HireOrderFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'hire_orders.HireOrder'
        exclude = ('post_generation_hook',)

    created_by = factory.SubFactory(f'{here}.UserFactory')
    recruiter = factory.SubFactory(f'{here}.UserFactory')
    candidate = factory.SubFactory(f'{here}.CandidateFactory')
    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    application = factory.SubFactory(
        factory=f'{here}.ApplicationFactory',
        candidate=factory.SelfAttribute('..candidate'),
        vacancy=factory.SelfAttribute('..vacancy'),
    )
    offer = factory.SubFactory(
        factory=f'{here}.OfferFactory',
        application=factory.SelfAttribute('..application'),
        candidate=factory.SelfAttribute('..candidate'),
        vacancy=factory.SelfAttribute('..vacancy'),
    )
    raw_data = factory.LazyAttribute(lambda obj: {})

    @factory.post_generation
    def post_generation_hook(self, create, extracted, **kwargs):
        if self.vacancy:
            self.vacancy.add_recruiter(self.recruiter)


class HireOrderHistoryFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'hire_orders.HireOrderHistory'


class PublicServiceFactory(factory.DjangoModelFactory):

    slug = factory.Sequence(lambda n: f'service-{n}')
    name_ru = factory.Sequence(lambda n: 'Сервис{}'.format(n))
    name_en = factory.Sequence(lambda n: 'Service{}'.format(n))

    class Meta:
        model = 'services.PublicService'


class PublicationFactory(factory.DjangoModelFactory):

    title = factory.Sequence(lambda n: 'Publication{}'.format(n))
    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    public_service = factory.SubFactory(f'{here}.PublicServiceFactory')
    lang = PUBLICATION_LANGUAGES.en

    class Meta:
        model = 'publications.Publication'


class ExternalPublicationFactory(PublicationFactory):

    type = PUBLICATION_TYPES.external


class PublishedExternalPublicationFactory(ExternalPublicationFactory):

    status = PUBLICATION_STATUSES.published


class PublicationFacetFactory(factory.DjangoModelFactory):

    lang = PUBLICATION_LANGUAGES.en
    facet = factory.Sequence(lambda n: 'facet{}'.format(n))
    value = factory.Sequence(lambda n: 'slug-{}'.format(n))
    publication_ids = []

    class Meta:
        model = 'publications.PublicationFacet'


class PublicationSuggestFactory(factory.DjangoModelFactory):

    lang = PUBLICATION_LANGUAGES.en
    text = factory.Sequence(lambda n: 'PublicationSuggest{}'.format(n))

    class Meta:
        model = 'publications.PublicationSuggest'

    @factory.post_generation
    def facets(self, create, facets, **kwargs):
        if create and facets:
            for facet in facets:
                self.facets.add(facet)


class ValueStreamFactory(factory.DjangoModelFactory):

    staff_id = factory.Sequence(lambda n: n)

    class Meta:
        model = 'staff.ValueStream'

    @factory.post_generation
    def post_generation_hook(self, create, extracted, **kwargs):
        ServiceFactory(slug=self.slug)


class VacancyWorkModeFactory(factory.DjangoModelFactory):

    vacancy = factory.SubFactory(f'{here}.VacancyFactory')
    work_mode = factory.SubFactory(f'{here}.WorkModeFactory')

    class Meta:
        model = 'vacancies.VacancyWorkMode'


class WorkModeFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'core.WorkMode'

    slug = factory.Sequence(lambda n: f'workMode_{n}')
    name_ru = factory.Sequence(lambda n: f'Режим работы {n}')
    name_en = factory.Sequence(lambda n: f'Work mode {n}')


class GeographyFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.Geography'


class CertificationFactory(factory.DjangoModelFactory):

    consideration = factory.SubFactory(
        factory=f'{here}.ConsiderationFactory',
    )
    created_by = factory.SubFactory(f'{here}.UserFactory')

    is_published = factory.Sequence(lambda n: False)
    private_uuid = factory.Sequence(lambda _: str(uuid.uuid4()))
    public_uuid = factory.Sequence(lambda _: str(uuid.uuid4()))

    class Meta:
        model = 'certifications.Certification'


def language_tag_sequence(n):
    def letter(n):
        if n < 26:
            return chr(ord('a') + n)
        else:
            return chr(ord('A') + n - 26)

    n1 = n % 52
    n2 = n // 52
    return letter(n1) + letter(n2)


class LanguageTagFactory(factory.DjangoModelFactory):

    tag = factory.Sequence(language_tag_sequence)
    native = factory.Sequence(lambda n: "native-{}".format(language_tag_sequence(n)))
    name_en = factory.Sequence(lambda n: "lang-{}".format(language_tag_sequence(n)))
    name_ru = factory.Sequence(lambda n: "язык-{}".format(language_tag_sequence(n)))

    class Meta:
        model = 'core.LanguageTag'


class CandidateLanguageTagFactory(factory.DjangoModelFactory):

    tag = factory.SubFactory(factory=f'{here}.LanguageTagFactory')
    candidate = factory.SubFactory(factory=f'{here}.CandidateFactory')

    class Meta:
        model = 'candidates.CandidateLanguageTag'


#######################


def currency(code='RUB'):
    assert len(code) == 3
    defaults = {
        'name_ru': f'Валюта-{code}',
        'name_en': f'Currency-{code}',
    }
    return Currency.objects.get_or_create(code=code, defaults=defaults)[0]


def country(code='RU'):
    assert len(code) == 2
    defaults = {
        'name_ru': code,
        'name_en': code,
    }
    return Country.objects.get_or_create(code=code, defaults=defaults)[0]


def create_user(**kwargs):
    return UserFactory.create(**kwargs)


def create_superuser(**kwargs):
    kwargs['is_superuser'] = True
    kwargs['username'] = 'superuser'
    return create_user(**kwargs)


def get_superuser(**kwargs):
    return User.objects.get(username='superuser')


def create_user_with_perm(perm_codename, **kwargs):
    if perm_codename is None:
        return create_user(**kwargs)
    perm, _created = Permission.objects.get_or_create(codename=perm_codename)
    user = create_user(**kwargs)
    user.user_permissions.add(perm)
    return user


def create_recruiter(**kwargs):
    return create_user_with_perm('recruiter_perm', **kwargs)


def create_interviewer(group_id, perm, **kwargs):
    group, _created = Group.objects.get_or_create(id=group_id, name=f'Group-{group_id}')
    user = create_user_with_perm(perm, **kwargs)
    user.groups.add(group)
    return user


def create_aa_interviewer(aa_type, perm=None, **kwargs):
    perm = perm or f'aa_{aa_type}_perm'
    return create_interviewer(
        group_id=settings.AA_TYPE_TO_GROUP_ID[aa_type],
        perm=perm,
        **kwargs,
    )


def create_interview_reviewer(**kwargs):
    kwargs.setdefault('group_id', settings.DEFAULT_INTERVIEW_REVIEWER_GROUP_ID)
    return create_interviewer(
        perm='screening_reviewer_perm',
        **kwargs
    )


def create_moderator(**kwargs):
    perm, _created = Permission.objects.get_or_create(codename='change_complaint')
    user = create_user(**kwargs)
    user.user_permissions.add(perm)
    return user


def create_problem(**kwargs):
    problem = ProblemFactory.create()
    problem.categories.add(CategoryFactory.create())
    return problem


def create_problem_with_moderators(**kwargs):
    problem = create_problem()
    for _ in range(2):
        CategorySubscriptionFactory.create(
            category=problem.categories.first(),
            user=create_moderator(),
        )
    return problem


def create_candidate_with_responsibles(**kwargs):
    main_recruiter = kwargs.pop('main_recruiter', create_recruiter())
    recruiter = kwargs.pop('recruiter', create_recruiter())
    candidate = CandidateFactory(**kwargs)
    CandidateResponsibleFactory(
        candidate=candidate,
        user=recruiter,
        role=CANDIDATE_RESPONSIBLE_ROLES.recruiter,
    )
    CandidateResponsibleFactory(
        candidate=candidate,
        user=main_recruiter,
        role=CANDIDATE_RESPONSIBLE_ROLES.main_recruiter,
    )
    return candidate


def create_consideration_with_responsibles(**kwargs):
    main_recruiter = kwargs.pop('main_recruiter', create_recruiter())
    recruiter = kwargs.pop('recruiter', create_recruiter())
    consideration = ConsiderationFactory(**kwargs)
    ConsiderationResponsibleFactory(
        consideration=consideration,
        user=recruiter,
        role=CANDIDATE_RESPONSIBLE_ROLES.recruiter,
    )
    ConsiderationResponsibleFactory(
        consideration=consideration,
        user=main_recruiter,
        role=CANDIDATE_RESPONSIBLE_ROLES.main_recruiter,
    )
    return consideration


def create_candidate_with_consideration(**kwargs):
    candidate = create_candidate_with_responsibles(**kwargs)
    status_map = {
        CANDIDATE_STATUSES.in_progress: Consideration.STATES.in_progress,
        CANDIDATE_STATUSES.closed: Consideration.STATES.archived,
    }
    with_responsibles_map = {
        CANDIDATE_STATUSES.in_progress: False,
        CANDIDATE_STATUSES.closed: True,
    }
    if with_responsibles_map[candidate.status]:
        create_consideration_with_responsibles(
            candidate=candidate,
            state=status_map[candidate.status],
            **{k: v for k, v in kwargs.items() if k in ['main_recruiter', 'recruiter']},
        )
    else:
        ConsiderationFactory(
            candidate=candidate,
            state=status_map[candidate.status],
        )
    return candidate


def create_heavy_candidate(**kwargs):
    candidate = create_candidate_with_consideration(**kwargs)
    CandidateContactFactory.create(candidate=candidate)
    CandidateEducationFactory.create(candidate=candidate)
    CandidateJobFactory.create(candidate=candidate)
    CandidateProfessionFactory.create(candidate=candidate)
    CandidateCityFactory.create(candidate=candidate)
    CandidateSkillFactory.create(candidate=candidate)
    CandidateTagFactory.create(candidate=candidate)
    CandidateAttachmentFactory.create(candidate=candidate)
    return candidate


def create_completed_consideration(candidate=None):
    candidate = candidate or CandidateFactory.create()
    consideration = ConsiderationFactory.create(candidate=candidate)
    application = ApplicationFactory.create(candidate=candidate, consideration=consideration)
    InterviewFactory.create(
        candidate=candidate,
        consideration=consideration,
        application=application,
        state=Interview.STATES.finished,
        finished=timezone.now(),
    )
    return consideration


def create_interview(**kwargs):
    candidate = kwargs.get('candidate')
    application = kwargs.get('application')
    consideration = kwargs.get('consideration')

    if application is not None:
        assert not consideration or application.consideration == consideration
        assert not candidate or application.candidate == candidate
        assert not candidate or application.consideration.candidate == candidate

    if consideration is not None:
        assert not candidate or consideration.candidate == candidate

    if application is None:
        if consideration is None:
            if candidate is None:
                candidate = CandidateFactory.create()
            consideration = ConsiderationFactory.create(candidate=candidate)
        application = ApplicationFactory.create(
            consideration=consideration,
            candidate=consideration.candidate,
        )

    kwargs['application'] = application
    kwargs['consideration'] = application.consideration
    kwargs['candidate'] = application.candidate
    return InterviewFactory.create(**kwargs)


def create_interview_with_assignments(**kwargs):
    interview = create_interview(**kwargs)
    problem = create_problem()
    AssignmentFactory.create(interview=interview, problem=problem)
    return interview


def create_finished_final_interview(**kwargs):
    return create_interview(
        type=INTERVIEW_TYPES.final,
        state=Interview.STATES.finished,
        **kwargs,
    )


def create_preset_with_problems(problems_count=2):
    preset = PresetFactory.create()
    for i in range(problems_count):
        problem = create_problem()
        PresetProblemFactory.create(preset=preset, problem=problem)
    return preset


def create_vacancy(**kwargs):
    vacancy = VacancyFactory.create(**kwargs)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.main_recruiter)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.hiring_manager)
    return vacancy


def create_active_vacancy(**kwargs):
    kwargs['status'] = VACANCY_STATUSES.in_progress
    return create_vacancy(**kwargs)


def create_application(**kwargs):
    if 'vacancy' not in kwargs:
        kwargs['vacancy'] = create_vacancy()
    return ApplicationFactory.create(**kwargs)


def create_heavy_vacancy(**kwargs):
    vacancy = VacancyFactory.create(**kwargs)
    VacancySkillFactory.create(vacancy=vacancy)
    VacancyCityFactory.create(vacancy=vacancy)
    VacancyLocationFactory.create(vacancy=vacancy)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.head)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.responsible)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.main_recruiter)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.recruiter)
    VacancyMembershipFactory.create(vacancy=vacancy, role=VACANCY_ROLES.hiring_manager)
    VacancyWorkModeFactory.create(vacancy=vacancy)

    profession = ProfessionFactory.create()
    vacancy.profession = profession
    vacancy.professional_sphere = profession.professional_sphere

    return vacancy


def factory_maker(cls, *args1, **kwargs1):
    def model_factory(*args2, **kwargs2):
        args = args1 or args2
        return cls(*args, **{**kwargs2, **kwargs1})
    return model_factory


def make_constant_factory(any_factory, **kwargs):
    obj = any_factory(**kwargs)

    def constant(**_kwargs):
        return obj
    return constant


def make_vacancy_with_status_factory(status):
    return factory_maker(VacancyFactory, status=status)


def make_employee_candidate_factory(**kwargs):
    candidate_factory = factory_maker(CandidateFactory, **kwargs)

    def employee_candidate_factory():
        user = create_user()
        return candidate_factory(login=user.username)
    return employee_candidate_factory


def make_consideration_factory_with_status(state=CONSIDERATION_STATUSES.in_progress):
    return factory_maker(ConsiderationFactory, state=state)


def make_regular_interview_with_state_factory(interviewer, state=INTERVIEW_STATES.assigned):
    return factory_maker(
        InterviewFactory,
        interviewer=interviewer,
        type=INTERVIEW_TYPES.regular,
        state=state
    )


def make_rotating_employee(
    employee_candidate_factory=make_employee_candidate_factory(),
    rotation_factory=RotationFactory
):
    candidate = employee_candidate_factory()
    employee = User.objects.get(username=candidate.login)
    rotation = rotation_factory(created_by=employee)
    return candidate, rotation


def create_simple_vacancy_with_candidate(
        vacancy_factory=VacancyFactory,
        candidate_factory=CandidateFactory,
        consideration_factory=ConsiderationFactory,
        regular_interview_factories=[InterviewFactory],
        aa_interviewer=None,
        hr_screeneer=None,
        vacancy_members=[],
):
    """ Создаем простую вакансию с кандидатом """
    vacancy = vacancy_factory()
    candidate = candidate_factory()
    consideration = consideration_factory(candidate=candidate)
    application = create_application(
        consideration=consideration,
        vacancy=vacancy,
        candidate=candidate
    )
    for regular_interview_factory in regular_interview_factories:
        regular_interview_factory(
            application=application,
            candidate=candidate,
            consideration=consideration
        )
    if aa_interviewer:
        InterviewFactory(
            candidate=candidate,
            type=INTERVIEW_TYPES.aa,
            state=INTERVIEW_STATES.finished,
            consideration=consideration,
            interviewer=aa_interviewer,
            application=None
        )
    if hr_screeneer:
        InterviewFactory(
            candidate=candidate,
            type=INTERVIEW_TYPES.hr_screening,
            state=INTERVIEW_STATES.finished,
            consideration=consideration,
            interviewer=hr_screeneer,
            application=None
        )
    for role, member in vacancy_members:
        VacancyMembershipFactory(vacancy=vacancy, member=member, role=role)
    return vacancy, consideration


def create_publication(**kwargs):
    return create_heavy_vacancy(is_published=True, **kwargs)


def create_heavy_application(**kwargs):
    if 'vacancy' not in kwargs:
        kwargs['vacancy'] = create_heavy_vacancy()
    if 'candidate' not in kwargs:
        kwargs['candidate'] = create_candidate_with_responsibles()
    return create_application(**kwargs)


def create_vacancy_group(**kwargs):
    recruiters = kwargs.pop('recruiters', [])
    vacancy_group = VacancyGroupFactory.create(**kwargs)
    for recruiter in recruiters:
        VacancyGroupMembershipFactory.create(member=recruiter, vacancy_group=vacancy_group)
    return vacancy_group


def create_submission(**kwargs):
    forms_data = get_forms_constructor_data(1, kwargs.pop('candidate_data', None))
    candidate_data = forms_data['params']
    kwargs['forms_data'] = forms_data
    kwargs['first_name'] = candidate_data['cand_name']
    kwargs['last_name'] = candidate_data['cand_surname']
    kwargs['phone'] = candidate_data['cand_phone']
    kwargs['email'] = candidate_data['cand_email']
    kwargs['comment'] = candidate_data['cand_info']

    source = kwargs.get('source', SUBMISSION_SOURCES.form)
    vacancy = create_vacancy(status=VACANCY_STATUSES.in_progress)
    if source == SUBMISSION_SOURCES.form and 'form' not in kwargs:
        submission_form = SubmissionFormFactory.create()
        submission_form.vacancies.add(vacancy)
        kwargs['form'] = submission_form
    elif source == SUBMISSION_SOURCES.publication and 'publication' not in kwargs:
        publication = PublicationFactory(vacancy=vacancy)
        kwargs['publication'] = publication
    elif source == SUBMISSION_SOURCES.reference and 'reference' not in kwargs:
        reference = ReferenceFactory()
        reference.vacancies.add(vacancy)
        kwargs['reference'] = reference
    elif source == SUBMISSION_SOURCES.rotation and 'rotation' not in kwargs:
        rotation = RotationFactory()
        rotation.vacancies.add(vacancy)
        kwargs['rotation'] = rotation

    return SubmissionFactory.create(**kwargs)


def create_offer(**kwargs):
    RawTemplateFactory.create()
    budget_position_id = kwargs.pop('budget_position_id', 1000)
    if 'application' not in kwargs:
        kwargs['vacancy'] = create_heavy_vacancy(
            budget_position_id=budget_position_id,
            status=VACANCY_STATUSES.in_progress,
        )
        kwargs['application'] = create_application(
            vacancy=kwargs['vacancy'],
            status=APPLICATION_STATUSES.in_progress,
        )
        kwargs['candidate'] = kwargs['application'].candidate

    offer = OfferFactory(**kwargs)
    create_department_chief(offer.department)
    return offer


def create_departments_tree(depth=1, **kwargs):
    assert depth > 0
    ancestors = kwargs.pop('ancestors', [])
    departments = []
    for i in range(depth):
        department = DepartmentFactory(ancestors=ancestors[:], **kwargs)
        departments.append(department)
        ancestors.append(department.id)
    return departments


def create_department_chief(department):
    return DepartmentUserFactory.create(
        department=department,
        user=create_user(),
        role=DEPARTMENT_ROLES.chief,
        is_direct=True,
        is_closest=True,
    )


def create_message(**kwargs):
    type_ = kwargs.get('type')
    if type_ and type_ in EXTERNAL_MESSAGE_TYPES:
        return MessageFactory.create(**kwargs)

    consideration = ConsiderationFactory.create()
    application = ApplicationFactory.create(
        candidate=consideration.candidate,
        consideration=consideration,
    )
    return MessageFactory.create(
        candidate=consideration.candidate,
        application=application,
        **kwargs
    )


def create_note(**kwargs):
    return create_message(type=MESSAGE_TYPES.note, **kwargs)


def create_scheduled_message(**kwargs):
    kwargs.setdefault('schedule_time', shifted_now(days=1))
    return MessageFactory.create(
        type=EXTERNAL_MESSAGE_TYPES.outcoming,
        status=MESSAGE_STATUSES.scheduled,
        **kwargs
    )


def create_waffle_switch(name, active=True):
    return SwitchFactory(
        name=name,
        active=active,
    )


def create_waffle_flag(name, **params):
    return FlagFactory(
        name=name,
        **params
    )


def create_actual_verification(**kwargs):
    kwargs.setdefault('resolution', VERIFICATION_RESOLUTIONS.hire)
    kwargs.setdefault('expiration_date', shifted_now(months=3))
    return VerificationFactory(
        status=VERIFICATION_STATUSES.closed,
        **kwargs
    )


def create_hire_order_for_status(status=HIRE_ORDER_STATUSES.new, **kwargs):
    kwargs['status'] = status
    kwargs.setdefault('recruiter', create_recruiter())
    kwargs.setdefault('created_by', create_user())

    if status == HIRE_ORDER_STATUSES.new:
        return HireOrderFactory(
            candidate=None,
            vacancy=None,
            application=None,
            offer=None,
            **kwargs
        )
    if status == HIRE_ORDER_STATUSES.candidate_prepared:
        return HireOrderFactory(
            vacancy=None,
            application=None,
            offer=None,
            **kwargs
        )
    if status == HIRE_ORDER_STATUSES.vacancy_on_approval:
        return HireOrderFactory(
            application=None,
            offer=None,
            vacancy__status=VACANCY_STATUSES.on_approval,
            vacancy__startrek_key='JOB-1',
            **kwargs
        )
    if status == HIRE_ORDER_STATUSES.vacancy_prepared:
        return HireOrderFactory(
            offer=None,
            vacancy__status=VACANCY_STATUSES.in_progress,
            vacancy__startrek_key='JOB-1',
            **kwargs
        )
    if status == HIRE_ORDER_STATUSES.offer_on_approval:
        return HireOrderFactory(
            application__resolution=APPLICATION_RESOLUTIONS.offer_agreement,
            vacancy__status=VACANCY_STATUSES.offer_processing,
            vacancy__startrek_key='JOB-1',
            offer__status=OFFER_STATUSES.on_approval,
            **kwargs
        )
    if status in (HIRE_ORDER_STATUSES.verification_sent, HIRE_ORDER_STATUSES.verification_on_check):
        hire_order = HireOrderFactory(
            application__resolution=APPLICATION_RESOLUTIONS.offer_agreement,
            vacancy__status=VACANCY_STATUSES.offer_processing,
            vacancy__startrek_key='JOB-1',
            offer__status=OFFER_STATUSES.on_approval,
            **kwargs
        )
        VerificationFactory(
            candidate=hire_order.candidate,
            application=hire_order.application,
            status=(
                VERIFICATION_STATUSES.new
                if status == HIRE_ORDER_STATUSES.verification_sent
                else VERIFICATION_STATUSES.on_check
            ),
        )
        return hire_order
    if status == HIRE_ORDER_STATUSES.offer_sent:
        hire_order = HireOrderFactory(
            application__resolution=APPLICATION_RESOLUTIONS.offer_agreement,
            vacancy__status=VACANCY_STATUSES.offer_processing,
            vacancy__startrek_key='JOB-1',
            offer__status=OFFER_STATUSES.sent,
            **kwargs
        )
        OfferLinkFactory(offer=hire_order.offer)
        create_actual_verification(
            candidate=hire_order.candidate,
            application=hire_order.application,
        )
        return hire_order
    offer_post_processing_statuses = (
        HIRE_ORDER_STATUSES.offer_accepted,
        HIRE_ORDER_STATUSES.preprofile_approved,
        HIRE_ORDER_STATUSES.preprofile_ready,
    )
    if status in offer_post_processing_statuses:
        return HireOrderFactory(
            application__status=APPLICATION_STATUSES.closed,
            application__resolution=APPLICATION_RESOLUTIONS.offer_accepted,
            vacancy__status=VACANCY_STATUSES.offer_accepted,
            vacancy__startrek_key='JOB-1',
            offer__status=OFFER_STATUSES.accepted,
            offer__newhire_id=1,
            offer__startrek_hr_key='HR-1',
            **kwargs
        )
    if status == HIRE_ORDER_STATUSES.closed:
        return HireOrderFactory(
            application__status=APPLICATION_STATUSES.closed,
            application__resolution=APPLICATION_RESOLUTIONS.offer_accepted,
            vacancy__status=VACANCY_STATUSES.closed,
            vacancy__startrek_key='JOB-1',
            offer__status=OFFER_STATUSES.closed,
            resolution=HIRE_ORDER_RESOLUTIONS.hired,
            **kwargs
        )

    assert 0, f'unsupported status: {status}'


def create_n_references_with_m_closed_offers(ref_user, counts, status, offers_deltas=[]):
    total, offers = counts
    references = []
    for _ in range(total):
        references.append(ReferenceFactory(created_by=ref_user, status=status))
        candidate = CandidateFactory()
        SubmissionFactory(reference=references[-1], candidate=candidate,
                          status=SUBMISSION_STATUSES.draft, source=SUBMISSION_SOURCES.reference)
    for i, reference in enumerate(references[:offers]):
        created_delta, closed_delta = offers_deltas[i] if offers_deltas else (1, 1)
        OfferFactory(candidate=reference.submission.candidate, status=OFFER_STATUSES.closed,
                     created=reference.created + datetime.timedelta(days=created_delta),
                     closed_at=reference.created + datetime.timedelta(days=closed_delta))

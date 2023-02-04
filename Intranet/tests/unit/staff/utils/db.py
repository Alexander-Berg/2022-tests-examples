from intranet.femida.tests import factories as f
from intranet.femida.src.staff.choices import DEPARTMENT_ROLES
from intranet.femida.src.vacancies.choices import VACANCY_ROLES


DR = DEPARTMENT_ROLES
VR = VACANCY_ROLES


def create_departments():
    return [f.DepartmentFactory.create(id=_id) for _id in (1001, 1002, 1003)]


def create_users():
    usernames = ['chief1', 'chief2', 'chief3', 'hr1', 'hr2']
    return [f.UserFactory.create(username=username) for username in usernames]


def create_vacancies(departments):
    return [f.create_vacancy(id=_id, department=dep) for _id, dep in zip((1, 2, 3), departments)]


def _build_db(data):
    departments = create_departments()
    user_map = {u.username: u for u in create_users()}
    vacancy_map = {v.id: v for v in create_vacancies(departments)}

    for vacancy_id, role, username, is_direct, is_closest, vacancy_roles in data:
        vacancy = vacancy_map[vacancy_id]
        user = user_map[username]
        du = f.DepartmentUserFactory(
            department=vacancy.department,
            role=role,
            user=user,
            is_direct=is_direct,
            is_closest=is_closest,
        )
        for vacancy_role in vacancy_roles:
            f.VacancyMembershipFactory(
                vacancy=vacancy,
                role=vacancy_role,
                member=user,
                department_user=du,
            )


def state_without_last_head(*args, **kwargs):
    data = (
        # (vacancy_id, du_role, username, is_direct, is_closest, vacancy_roles)
        (1, DR.chief, 'chief1', True, True, (VR.auto_observer, VR.head)),

        (2, DR.chief, 'chief1', False, False, (VR.auto_observer,)),
        (2, DR.chief, 'chief2', True, True, (VR.auto_observer, VR.head)),
        (2, DR.hr_partner, 'hr1', True, True, (VR.auto_observer,)),
        (2, DR.hr_partner, 'hr2', True, True, (VR.auto_observer,)),

        (3, DR.chief, 'chief1', False, False, (VR.auto_observer,)),
        (3, DR.chief, 'chief2', False, True, (VR.auto_observer, VR.head)),
        (3, DR.hr_partner, 'hr1', False, True, (VR.auto_observer,)),
        (3, DR.hr_partner, 'hr2', False, True, (VR.auto_observer,)),
    )
    _build_db(data)


def state_with_last_head():
    data = (
        # (vacancy, du_role, username, is_direct, is_closest, vacancy_roles)
        (1, DR.chief, 'chief1', True, True, (VR.auto_observer, VR.head)),

        (2, DR.chief, 'chief1', False, False, (VR.auto_observer,)),
        (2, DR.chief, 'chief2', True, True, (VR.auto_observer, VR.head)),
        (2, DR.hr_partner, 'hr1', True, True, (VR.auto_observer,)),
        (2, DR.hr_partner, 'hr2', True, True, (VR.auto_observer,)),

        (3, DR.chief, 'chief1', False, False, (VR.auto_observer,)),
        (3, DR.chief, 'chief2', False, False, (VR.auto_observer,)),
        (3, DR.hr_partner, 'hr1', False, True, (VR.auto_observer,)),
        (3, DR.hr_partner, 'hr2', False, True, (VR.auto_observer,)),
        (3, DR.chief, 'chief3', True, True, (VR.auto_observer, VR.head)),
    )
    _build_db(data)

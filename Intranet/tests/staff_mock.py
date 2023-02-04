from typing import Dict

from staff.workspace_management import entities


class StaffMock(entities.Staff):
    def __init__(self) -> None:
        self._login_to_person_id: Dict[str, int] = {}

    def set_person_id_for_login(self, login: str, person_id: int) -> None:
        self._login_to_person_id[login] = person_id

    def get_person_id(self, login: str) -> int:
        return self._login_to_person_id[login]

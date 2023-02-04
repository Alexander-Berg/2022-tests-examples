class FakeNewhireAPI:

    @classmethod
    def create_preprofile(cls, *args, **kwargs):
        return {'id': 1}, 200

    @classmethod
    def check_login(cls, *args, **kwargs):
        return {'error': None}, 200

    @classmethod
    def get_preprofile(cls, *args, **kwargs):
        return {
            'femida_offer_id': 1,
            'hdrfs_ticket': 'HDRFS-1',
            'join_at': '2018-08-01',
            'candidate_type': 'NEW_EMPLOYEE',
            'login': 'username',
            'modified_at': '2018-07-24T14:28:23',
            'status': 'NEW',
            'supply_ticket': None,
            'organization_id': 1,
            'department_id': 1,
            'recruiter_login': 'recruiter',
            'chief_login': 'boss',
            'position': 'Разработчик',
        }, 200

    @classmethod
    def update_preprofile(cls, *args, **kwargs):
        return {}, 200

    @classmethod
    def accept_preprofile(cls, *args, **kwargs):
        return {}, 200

    @classmethod
    def cancel_preprofile(cls, *args, **kwargs):
        return {}, 200


class FakeBPRegistryAPI:

    @classmethod
    def create_transaction(cls, *args, **kwargs):
        return '1'

    @classmethod
    def create_bp_assignment(cls, *args, **kwargs):
        return {
            'id': 'adc06440-2490-12ea-b9d4-5d77dc9a2bc9',
        }


class FakeOebsHireAPI:

    @classmethod
    def create_person(cls, *args, **kwargs):
        return 1

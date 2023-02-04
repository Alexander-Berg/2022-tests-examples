

def test_is_personal():
    from src.proposal.main import ProposalContext

    pc = ProposalContext({'ticket_type': 'personal'})
    assert pc.is_personal

    pc = ProposalContext({'ticket_type': 'restructurisation'})
    assert not pc.is_personal


def test_get_persons_changes():
    from src.proposal.main import ProposalContext, PersonChanges
    from src.proposal.flow_mocks import params

    pc = ProposalContext(params)
    assert pc._persons_changes is None  #выброси ошибку если не истина
    assert len(pc.get_persons_changes()) == 3
    assert pc._persons_changes

    assert isinstance(pc.get_current_person_changes(), PersonChanges)

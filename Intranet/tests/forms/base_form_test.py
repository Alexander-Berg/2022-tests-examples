import sform
from staff.proposal.forms.base import ProposalBaseForm


class SomeProposalForm(ProposalBaseForm):
    required_int_field = sform.IntegerField(state=sform.REQUIRED)
    char_field = sform.CharField(max_length=10)
    choices = sform.MultipleChoiceField(choices=((1, 'one'), (2, 'two')))
    readonly_bool_field = sform.BooleanField(state=sform.READONLY)


def test_readonly_when_locked():
    form = SomeProposalForm(base_initial={'locked_proposal': True})
    data = form.data_as_dict()
    for field in form.fields:
        assert data[field]['readonly'] is True

    form = SomeProposalForm(initial={'locked': True, 'some': 'other data'})
    data = form.data_as_dict()
    for field in form.fields:
        assert data[field]['readonly'] is True


def test_readonly_when_applied():
    form = SomeProposalForm(initial={'applied_at': '2018-06-18T22:33:44'})
    data = form.data_as_dict()
    for field in form.fields:
        assert data[field]['readonly'] is True

from django.db.models.signals import post_save

from ok.approvements.models import (
    Approvement,
    ApprovementStage,
    save_approvement_history,
    save_approvement_stage_history,
)

from tests.utils.decorators import disconnect_signal


disable_auto_history = disconnect_signal(
    signal=post_save,
    receivers=[
        {
            'receiver': save_approvement_history,
            'sender': Approvement,
            'dispatch_uid': 'save_approvement_history',
        },
        {
            'receiver': save_approvement_stage_history,
            'sender': ApprovementStage,
            'dispatch_uid': 'save_approvement_stage_history',
        },
    ],
)


def generate_stages_data(*stages, n=None, d=False):
    """
    :param n: need_approvals
    :param d: is_with_deputies
    """
    result = {
        'stages': [],
        'need_approvals': n or len(stages),
        'is_with_deputies': d,
    }
    for stage in stages:
        if isinstance(stage, str):
            result['stages'].append({'approver': stage})
        else:
            result['stages'].append(stage)
    return result

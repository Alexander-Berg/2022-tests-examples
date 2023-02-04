from intranet.femida.src.candidates.choices import CHALLENGE_RESOLUTIONS
from intranet.femida.src.notifications import challenges as notifications

from intranet.femida.tests import factories as f


def test_challenge_estimated():
    instance = f.ChallengeFactory(
        resolution=CHALLENGE_RESOLUTIONS.hire,
        comment='Итог',
        candidate=f.create_candidate_with_consideration(),
    )
    initiator = instance.candidate.responsibles.first()

    notification = notifications.ChallengeEstimatedNotification(instance, initiator)
    notification.send()

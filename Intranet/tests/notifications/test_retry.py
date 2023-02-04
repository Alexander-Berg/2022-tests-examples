from post_office import models as post_models

from review.notifications import (
    models,
    retry,
)


def test_email_retries():
    email = post_models.Email.objects.create(
        status=post_models.STATUS.failed,
        from_email='something@somewhere.why',
    )
    for _ in range(models.RETRIES_NUMBER):
        retry.retry_failed()
        email.refresh_from_db()
        assert email.status == post_models.STATUS.queued
        email.status = post_models.STATUS.failed
        email.save()
    retry.retry_failed()
    email.refresh_from_db()
    assert email.status == post_models.STATUS.failed

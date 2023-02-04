import django_celery_results


def test_dcr_version():
    error = (
        'Greater versions of django-celery-results may not be compatible '
        'with django-celery-monitoring'
    )
    assert django_celery_results.__version__ == '1.1.2', error

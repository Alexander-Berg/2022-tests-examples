# coding: utf-8


import logging

from django.conf import settings
from django.core.cache import cache
from django.db import models
from django.db.models import signals

from idm.core.models import System

cache_key = 'allowed_test_emails'
log = logging.getLogger(__name__)


class AllowedEmail(models.Model):
    """
    Модель для хранения email'ов, на которые разрешена отправка с тестинга
    """
    email = models.EmailField(unique=True)
    comment = models.TextField(blank=True)
    added_at = models.DateTimeField(auto_now_add=True)

    @classmethod
    def get_test_env_allowed_emails(cls):
        """
        Вернуть список (set, на самом деле) email'ов, на которые можно отправлять письма в тестинге
        :rtype: set
        """
        allowed_test_email = cache.get(cache_key)
        if allowed_test_email is None:
            # email'ы, указанные как системные
            systems_emails = sum([system.get_emails() for system in System.objects.all().only('emails')], [])
            # email'ы, попавшие в список разрешенных для отправки с тестинга
            excluded_emails = list(cls.objects.all().values_list('email', flat=True))

            allowed_test_email = set(
                systems_emails
                + excluded_emails
            )

            cache.set(cache_key, allowed_test_email, timeout=24 * 60 * 60)

        return allowed_test_email


class AllowedPhone(models.Model):
    """
    Модель для хранения телефонных номеров, на которые разрешена отправка с тестинга
    """
    mobile_phone = models.CharField(max_length=15)
    comment = models.TextField(blank=True)
    added_at = models.DateTimeField(auto_now_add=True)

    @classmethod
    def check_phone(cls, phone):
        return cls.objects.filter(mobile_phone=phone).exists()


def _drop_test_emails_cache(sender, *args, **kwargs):
    cache.delete(cache_key)
    log.info('dropped allowed test emails by signal from %s', sender)


signals.post_save.connect(_drop_test_emails_cache, sender=System)
signals.post_save.connect(_drop_test_emails_cache, sender=AllowedEmail)

# coding: utf-8
from __future__ import unicode_literals
from django.db import models


class Article(models.Model):
    slug = models.CharField(max_length=50)
    name = models.CharField(max_length=255)
    score = models.IntegerField(default=0)

    def __unicode__(self):
        return 'slug=%s, name=%s, score=%d' % (self.slug, self.name, self.score)

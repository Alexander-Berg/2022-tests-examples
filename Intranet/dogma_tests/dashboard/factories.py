# coding: utf-8



import factory
import socket

from intranet.dogma.dogma.core import models


class NodeFactory(factory.DjangoModelFactory):
    hostname = factory.LazyAttribute(lambda _: socket.getfqdn())

    class Meta:
        model = models.Node


class SourceFactory(factory.DjangoModelFactory):

    class Meta:
        model = models.Source


class RepoFactory(factory.DjangoModelFactory):
    source = factory.SubFactory(SourceFactory)

    class Meta:
        model = models.Repo


class CloneFactory(factory.DjangoModelFactory):
    node = factory.SubFactory(NodeFactory)
    repo = factory.SubFactory(RepoFactory)

    class Meta:
        model = models.Clone

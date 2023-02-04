# -*- coding: utf-8 -*-

import xml.etree.ElementTree as ET

import pytest
from hamcrest import assert_that, contains_inanyorder, only_contains
from sqlalchemy import create_engine, Table, Column, String, MetaData
from sqlalchemy.sql import select, text
from sqlalchemy.orm import sessionmaker

from balance.webcontrols.sources import BaseSource, SqlExpressionSource


@pytest.fixture()
def animals():
    engine = create_engine("sqlite:///:memory:")
    metadata = MetaData(bind=engine)
    animals = Table("t_animals", metadata,
        Column("name", String),
        Column("says", String),
    )
    metadata.create_all()
    conn = engine.connect()
    conn.execute(animals.insert().values(name="Pluto", says="bark"))
    conn.execute(animals.insert().values(name="Donald", says="quack"))
    conn.execute(animals.insert().values(name="Babe", says="oink"))
    conn.execute(animals.insert().values(name="Daffy", says="quack"))
    yield animals


@pytest.fixture()
def session(animals):
    engine = animals.metadata.bind
    Session = sessionmaker(bind=engine)
    session = Session()
    yield session


class TestSqlExpressionSource(object):

    def test_simple(self, animals, session):
        SqlExpressionSource(
            "ducks",
            statement=select([animals]).where(animals.c.says == "quack"),
        )

        ducks_src_inst = BaseSource.get("ducks", sourceparams="filter={}")

        result = ducks_src_inst.source(session)
        assert_that(
            [r.attrib["name"] for r in result], contains_inanyorder("Donald", "Daffy"),
        )

    def test_parametrized(self, animals, session):
        SqlExpressionSource(
            "animals",
            statement=select([animals]).where(text("says = :x")),
        )

        dogs_src_inst = BaseSource.get(
            "animals",
            sourceparams='filter={"x": "bark"}',
        )

        result = dogs_src_inst.source(session)
        assert_that(
            [r.attrib["name"] for r in result], only_contains("Pluto"),
        )

    def test_parametrized_fails_wo_parameter(self, animals, session):
        """
        Reproducing BALANCE-31518
        """
        SqlExpressionSource(
            "animals",
            statement=select([animals]).where(text("says = :x")),
        )

        src_inst = BaseSource.get(
            "animals",
            sourceparams='filter={}',  # no parameter "x", should fail
        )

        with pytest.raises(Exception, match="value is required for bind parameter"):
            src_inst.source(session)

    def test_parametrized_with_default(self, animals, session):
        """
        Fixing BALANCE-31518
        """
        SqlExpressionSource(
            "animals",
            statement=select([animals]).where(text("says = :x")),
            default_bind_params={"x": "oink"},
        )

        src_inst = BaseSource.get(
            "animals",
            sourceparams='filter={}',  # no parameter "x", should use default "oink"
        )

        result = src_inst.source(session)
        assert_that(
            [r.attrib["name"] for r in result], only_contains("Babe"),
        )

        src_inst2 = BaseSource.get(
            "animals",
            sourceparams='filter={"x": "quack"}',  # still works if "x" set
        )

        result = src_inst2.source(session)
        assert_that(
            [r.attrib["name"] for r in result], contains_inanyorder("Donald", "Daffy"),
        )

    def test_parametrized_with_old_format(self, animals, session):
        """
        Fixing BALANCE-31518 again, because some javascript still sends filter in
        old formats
        """
        SqlExpressionSource(
            "animals",
            statement=select([animals]).where(text("says = :x")),
            default_bind_params={"x": "oink"},
        )

        src_inst = BaseSource.get(
            "animals",
            sourceparams='filter=x%3D%3D%22bark%22',  # x=="bark"
        )

        result = src_inst.source(session)
        assert_that(
            [r.attrib["name"] for r in result], only_contains("Pluto"),
        )

    def test_parametrized_with_after_parse(self, animals, session):
        src_base = SqlExpressionSource(
            "animals",
            statement=select([animals]).where(text("says = :x")),
        )

        # If user defines such callback, it's invoked after bind_params are
        # extracted from sourceparams. It gives you chance to mess with keys and
        # values of bind_params before SQL is executed
        @src_base.after_parse
        def _nameisnotimportant(bind_params):
            if "xxx" in bind_params:
                bind_params["x"] = bind_params["xxx"]

        src_inst = BaseSource.get(
            "animals",
            sourceparams='filter={"xxx": "quack"}',
        )

        result = src_inst.source(session)
        assert_that(
            [r.attrib["name"] for r in result], contains_inanyorder("Donald", "Daffy"),
        )


# vim:ts=4:sts=4:sw=4:tw=88:et:

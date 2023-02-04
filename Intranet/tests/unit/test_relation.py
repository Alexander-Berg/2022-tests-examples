# coding: utf-8

import unittest

from lxml import etree as ET

from tests import helpers

from at.aux_ import Relations


class TestRelations(unittest.TestCase):
    def testSerializeOne(self):
        sample = ET.XML('<member>'\
                        '<uid>1120000000017474</uid>'\
                        '<feed_id>4611686018427387909</feed_id>'\
                        '<role>50</role>'\
                        '<add_date>1450695014</add_date>'\
                        '<role_type>MEMBER</role_type>'\
                        '<mutual>0</mutual></member>')
        relation = Relations.Relation(\
                1120000000017474, 4611686018427387909, 50, 1450695014, 0)
        diff = helpers.xml_diff(sample, relation.to_xml(), consider_order=False)
        assert not diff, diff

    def testSerializeMulti(self):
        sample = ET.XML('<aux><moderators><aux>'\
                '<count>15</count><members>'\
                        '<member>'\
                        '<uid>1120000000017474</uid>'\
                        '<feed_id>4611686018427387909</feed_id>'\
                        '<role>60</role>'\
                        '<add_date>1450695014</add_date>'\
                        '<role_type>MODERATOR</role_type>'\
                        '<mutual>0</mutual></member>'\
                        '<member>'\
                        '<uid>1120000000017474</uid>'\
                        '<feed_id>4611686018427387909</feed_id>'\
                        '<role>50</role>'\
                        '<role_type>MEMBER</role_type>'\
                        '<add_date>1450695014</add_date>'\
                        '<mutual>0</mutual></member>'\
                        '</members></aux></moderators>'\
                        '<invited><aux>'\
                                '<count>1</count><members/>'\
                        '</aux></invited>'\
                        '</aux>'
                        )
        data = {
                'moderators': (15, Relations.RelationsList([
                        Relations.Relation(1120000000017474, 4611686018427387909, 60, 1450695014, 0),
                        Relations.Relation(1120000000017474, 4611686018427387909, 50, 1450695014, 0),
                        ])),
                'invited': (1, Relations.RelationsList())
                }
        diff = helpers.xml_diff(sample, Relations.serialize_grouplist(data), consider_order=False)
        assert not diff, diff

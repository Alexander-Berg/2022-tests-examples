from dataclasses import dataclass

from rest_framework import serializers
from wiki.utils.context_bound_serializer import ContextBoundSerializer


@dataclass
class Context:
    user: str


class ContextSerializer(ContextBoundSerializer[Context], serializers.Serializer):
    dummy = serializers.SerializerMethodField()
    dummy_b = serializers.CharField(max_length=128)

    def get_dummy(self, obj):
        """
        вернуть список супертэгов похожих на переданную строчку
        """
        return self.get_context().user + '_123'


class SerializerB(serializers.Serializer):
    q = serializers.CharField(max_length=128)


class SubclassContextSerializer(SerializerB, ContextSerializer):
    pass


def test_context_serializer():
    c = SubclassContextSerializer(data={'dummy_b': '123', 'q': '345'}, context={'user': 'neofelis'})

    assert c.is_valid()
    assert c.data['dummy'] == 'neofelis_123'

import decimal

from billing.dcsaap.backend.core.utils.check import format_material_thresholds


class TestMaterialThreshold:
    @staticmethod
    def cleanup_threshold(threshold):
        result = {}
        for key, value in threshold.items():
            if value is not None:
                result[key] = value
        return result

    def format_thresholds(self, material_threshold):
        thresholds = format_material_thresholds(material_threshold)
        return list(map(self.cleanup_threshold, thresholds))

    def test_invalid_threshold(self):
        # invalid expression
        material_threshold = '2a+b'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1
        assert thresholds[0]['error'] == 'invalid expression: unexpected variable, expression: 2a+b'

        # invalid variables count
        material_threshold = '2+2'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1
        assert thresholds[0]['error'] == 'expression has invalid count of variables: 0'

        material_threshold = '2*a+b'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1
        assert thresholds[0]['error'] == 'expression has invalid count of variables: 2'

        # no threshold specified
        material_threshold = 'INvalid'  # avoiding parsing `in` as token
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1
        assert thresholds[0]['error'] == 'no threshold specified'

        # invalid min threshold
        material_threshold = 'a:100f,1000f'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1
        assert thresholds[0]['error'] == 'invalid min threshold'

        # invalid max threshold
        material_threshold = 'a:1000f'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1
        assert thresholds[0]['error'] == 'invalid max threshold'

    def test_multiple(self):
        material_threshold = 'a:1 b:2 c:3'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 3

        expected = [
            {'name': 'a', 'expression': 'a', 'threshold_max': decimal.Decimal(1)},
            {'name': 'b', 'expression': 'b', 'threshold_max': decimal.Decimal(2)},
            {'name': 'c', 'expression': 'c', 'threshold_max': decimal.Decimal(3)},
        ]
        assert thresholds == expected

    def test_min_max(self):
        material_threshold = 'amount:100,1000'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1

        expected = [
            {
                'name': 'amount',
                'expression': 'amount',
                'threshold_min': decimal.Decimal(100),
                'threshold_max': decimal.Decimal(1000),
            }
        ]
        assert thresholds == expected

    def test_expression(self):
        material_threshold = '(amount*30)/10000:100000'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1

        expected = [
            {
                'name': 'amount',
                'expression': '(amount*30)/10000',
                'threshold_max': decimal.Decimal(100000),
            }
        ]
        assert thresholds == expected

    def test_unit(self):
        material_threshold = 'amount:100000:"английские фунты"'
        thresholds = self.format_thresholds(material_threshold)
        assert len(thresholds) == 1

        expected = [
            {
                'name': 'amount',
                'expression': 'amount',
                'threshold_max': decimal.Decimal(100000),
                'units': 'английские фунты',
            }
        ]
        assert thresholds == expected

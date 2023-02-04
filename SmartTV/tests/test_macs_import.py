import pytest

from smarttv.droideka.proxy.models import ValidIdentifier
from smarttv.droideka.proxy.identifiers import IdentifiersImporter
from smarttv.droideka.tests.helpers import to_in_memory_uploaded_file


@pytest.mark.django_db
class TestUploadIdentifiers:
    test_mac_1 = '7E:11:6C:5F:B3:54'
    test_mac_2 = '7a:21:15:f1:4f:53'
    test_mac_3_incorrect = 'aa:bb'

    def _assert_first_and_second_mac_saved(self):
        assert ValidIdentifier.objects.count() == 2
        saved = list(ValidIdentifier.objects.all())

        assert saved[0].type == ValidIdentifier.ETHERNET_MAC
        assert saved[1].type == ValidIdentifier.ETHERNET_MAC

        assert saved[0].value == self.test_mac_1.lower()
        assert saved[1].value == self.test_mac_2

    @pytest.mark.parametrize('type', ValidIdentifier.TYPES)
    def test_check_identifiers_type_valid(self, type):
        IdentifiersImporter.check_identifiers_type(type)

    @pytest.mark.parametrize('type', [type.upper() for type in ValidIdentifier.TYPES] + ['unknown-type', '', None])
    def test_check_identifiers_type_invalid(self, type):
        with pytest.raises(IdentifiersImporter.InvalidMacType):
            IdentifiersImporter.check_identifiers_type(type)

    @pytest.mark.parametrize('file, expected_result', [
        (to_in_memory_uploaded_file(f'{ValidIdentifier.ETHERNET_MAC}\n{test_mac_1}'),
         [{'type': ValidIdentifier.ETHERNET_MAC, 'value': test_mac_1}]),

        (to_in_memory_uploaded_file(
            f'{ValidIdentifier.ETHERNET_MAC}\n{test_mac_1}\n{test_mac_2}'),
         [{'type': ValidIdentifier.ETHERNET_MAC, 'value': test_mac_1},
          {'type': ValidIdentifier.ETHERNET_MAC, 'value': test_mac_2}])
    ])
    def test_read_file(self, file, expected_result):
        assert IdentifiersImporter.read_file(file) == expected_result

    def test_create_models_ok(self):
        raw_identifiers = [
            {'type': ValidIdentifier.ETHERNET_MAC, 'value': self.test_mac_1},
            {'type': ValidIdentifier.ETHERNET_MAC, 'value': self.test_mac_2}
        ]
        actual_result = IdentifiersImporter.create_models(raw_identifiers)

        assert len(actual_result) == 2
        assert actual_result[0].type == ValidIdentifier.ETHERNET_MAC
        assert actual_result[1].type == ValidIdentifier.ETHERNET_MAC

        assert actual_result[0].value == self.test_mac_1.lower()
        assert actual_result[1].value == self.test_mac_2

    @pytest.mark.parametrize('input_list', [[], (), None])
    def test_create_models_from_empty_list_exception_raised(self, input_list):
        with pytest.raises(IdentifiersImporter.NoMacs):
            IdentifiersImporter.create_models(input_list)

    def test_save_identifier_ok(self):
        assert ValidIdentifier.objects.count() == 0

        IdentifiersImporter.save_identifiers([ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC,
                                                               value=self.test_mac_2)])

        assert ValidIdentifier.objects.count() == 1
        saved = ValidIdentifier.objects.first()
        assert saved.type == ValidIdentifier.ETHERNET_MAC
        assert saved.value == self.test_mac_2

    def test_save_multiple_identifiers_ok(self):
        assert ValidIdentifier.objects.count() == 0
        IdentifiersImporter.save_identifiers([
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_1.lower()),
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_2),
        ])

        self._assert_first_and_second_mac_saved()

    def test_save_existing_identifier_ignored(self):
        assert ValidIdentifier.objects.count() == 0
        ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_1.lower()).save()
        assert ValidIdentifier.objects.count() == 1

        IdentifiersImporter.save_identifiers([
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_1.lower()),
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_2),
        ])

        self._assert_first_and_second_mac_saved()

    def test_save_duplicate_identifiers_ignored(self):
        IdentifiersImporter.save_identifiers([
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_1.lower()),
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_1.lower()),
            ValidIdentifier(type=ValidIdentifier.ETHERNET_MAC, value=self.test_mac_2),
        ])

        self._assert_first_and_second_mac_saved()

    def test_import_from_string(self):
        lines = [self.test_mac_1, self.test_mac_2, self.test_mac_3_incorrect]
        result = IdentifiersImporter.import_from_string(lines)
        assert result.imported == 2
        assert result.imported_unique == 2
        assert result.skipped == 1
        assert len(result.warnings) == 1

    def test_import_from_string_multiple_times(self):
        lines = [self.test_mac_1, self.test_mac_2]
        IdentifiersImporter.import_from_string(lines)

        # при повторном импорте данные не вставляются в базу второй раз
        result = IdentifiersImporter.import_from_string(lines)
        assert result.imported_unique == 0
        assert result.imported == 2

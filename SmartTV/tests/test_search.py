import pytest
import os
from copy import deepcopy
from smarttv.droideka.proxy.views.search import SearchView, FIELD_ENTITY_DATA, FIELD_RELATED_OBJECT, \
    FIELD_RELATED_OBJECT_TYPE, FIELD_AGE_LIMIT, RelatedObjectType, FIELD_BASE_INFO, FIELD_PARENT_COLLECTION, AliceSearchV7, \
    process_parent_collection
from smarttv.droideka.utils import PlatformInfo, PlatformType
from smarttv.droideka.proxy.api.vs import client
import mock


class TestVideoResultRequestParams:

    def test_filter_persons_param_added_for_old_clients(self):
        params = {'need_people': False}
        request_params = client.update_params(params)
        assert 'scheme_Local/VideoExtraItems/FilterUnusedObjectsForDevices=1' in request_params['rearr']
        assert 'need_people' not in params

    def test_filter_persons_param_added_for_new_clients(self):
        params = {'need_people': True}
        request_params = client.update_params(params)
        assert 'scheme_Local/VideoExtraItems/FilterUnusedObjectsForDevices=1' not in request_params['rearr']
        assert 'need_people' not in params


class TestNeedPeople:

    @pytest.mark.parametrize('version, need_people', [
        ('1.0', False),
        ('1.1', False),
        (None, False),
        ('1.2', False),
        ('1.2.2147483646', False),
        ('1.2.2147483647', True),
        ('1.3', True),
    ])
    def test_need_people(self, version, need_people):
        platform_info = PlatformInfo(PlatformType.ANDROID, None, version, None, None)
        assert bool(SearchView.need_people(platform_info)) == need_people


class TestGetDataFromSearch:

    CORRECT_ASSOC_OBJECT = {FIELD_RELATED_OBJECT_TYPE: RelatedObjectType.ASSOC.value}

    @pytest.mark.parametrize('expected_result, input', [
        (None, {}),
        (None, {FIELD_ENTITY_DATA: {}}),
        (None, {FIELD_ENTITY_DATA: {FIELD_RELATED_OBJECT: None}}),
        (None, {FIELD_ENTITY_DATA: {FIELD_RELATED_OBJECT: []}}),
        (None, {FIELD_ENTITY_DATA: {FIELD_RELATED_OBJECT: [{FIELD_RELATED_OBJECT_TYPE: 'unknown_value'}]}}),
        (deepcopy(CORRECT_ASSOC_OBJECT), {FIELD_ENTITY_DATA: {FIELD_RELATED_OBJECT: [
            deepcopy(CORRECT_ASSOC_OBJECT)]}}),
    ])
    def test_get_association_object(self, expected_result, input):
        assert SearchView.get_association_object(input) == expected_result


class TestFilterEntityDataByAgeRestriction:
    @pytest.mark.parametrize('input_response, restriction_age, expected_result', [
        ({}, 0, {}),
        ({FIELD_ENTITY_DATA: {}}, 0, {FIELD_ENTITY_DATA: {}}),
        ({FIELD_ENTITY_DATA: {FIELD_BASE_INFO: {FIELD_AGE_LIMIT: 18}}}, 6, {}),
        ({FIELD_ENTITY_DATA: {FIELD_BASE_INFO: {FIELD_AGE_LIMIT: 12}}}, 12, {FIELD_ENTITY_DATA: {FIELD_BASE_INFO: {FIELD_AGE_LIMIT: 12}}}),
        ({FIELD_ENTITY_DATA: {FIELD_BASE_INFO: {FIELD_AGE_LIMIT: 6}}}, 12, {FIELD_ENTITY_DATA: {FIELD_BASE_INFO: {FIELD_AGE_LIMIT: 6}}}),
        ({FIELD_ENTITY_DATA: {FIELD_BASE_INFO: {}, FIELD_RELATED_OBJECT: ['s1'], FIELD_PARENT_COLLECTION: 's2'}}, 0, {}),
        ({FIELD_ENTITY_DATA: {FIELD_RELATED_OBJECT: ['o1']}}, 6, {})
    ])
    def test_age_restriction(self, input_response, restriction_age, expected_result):
        SearchView.filter_entity_data_by_age_restriction(input_response, restriction_age)
        assert input_response == expected_result


class TestSearchLoggingEnabledByDataCenter:
    """
    Проверяет включение/выключение логирования запросов в поиск в зависимости
    от значений переменных окружения
    """
    @pytest.fixture
    def dc_enabled(self, mocker, settings):
        mocker.patch.dict(os.environ, {
            'DEPLOY_NODE_DC': 'sas',
        })
        settings.LOG_UE2E_SEARCH_REQUESTS_DC = 'sas,man,vla'

    @pytest.fixture
    def dc_disabled(self, mocker, settings):
        mocker.patch.dict(os.environ, {
            'DEPLOY_NODE_DC': 'man',
        })
        settings.LOG_UE2E_SEARCH_REQUESTS_DC = 'sas,vla'

    @pytest.fixture
    def dc_no_values(self, mocker, settings):
        mocker.patch.dict(os.environ, {
            'DEPLOY_NODE_DC': '',
        })
        settings.LOG_UE2E_SEARCH_REQUESTS_DC = ''

    def test_dc_enabled(self, dc_enabled):
        view = AliceSearchV7()
        assert view.is_log_enabled() is True

    def test_dc_disabled(self, dc_disabled):
        view = AliceSearchV7()
        assert view.is_log_enabled() is False

    def test_dc_no_values(self, dc_no_values):
        view = AliceSearchV7()
        assert view.is_log_enabled() is False


class TestSearchLoggingEnabledBySampling:
    """
    Проверяет включение логирования для процента запросов
    """
    def test_default_is_100_percent_enabled(self):
        # по-умолчанию каждый запрос логируется
        view = AliceSearchV7()
        assert view.is_log_enabled_sampled() is True

    def test_0_percent_means_disabled(self, settings):
        view = AliceSearchV7()
        settings.LOG_UE2E_SEARCH_SAMPLING_RATE = '0'
        assert view.is_log_enabled_sampled() is False

    def test_randint_works(self, settings, mocker):
        """
        Если sampling rate = 5, а randint(1,10) выдал 6, то запрос не логируется
        """
        view = AliceSearchV7()
        settings.LOG_UE2E_SEARCH_SAMPLING_RATE = '5'
        mocker.patch('smarttv.droideka.proxy.views.search.random.randint', return_value=6)
        assert view.is_log_enabled_sampled() is False


class TestRemoveTracksParentCollectionFromSearchResponse:

    @mock.patch('smarttv.droideka.proxy.views.search.fill_parent_collection_more_url', mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.search.filter_parent_collection_for_module', mock.Mock(side_effect=None))
    def test_remove_tracks_search_response(self):
        response = {'entity_data': {'parent_collection': {'type': 'tracks'}}}
        process_parent_collection(response, None, None, True, None)

        assert response == {'entity_data': {}}

    @mock.patch('smarttv.droideka.proxy.views.search.fill_parent_collection_more_url', mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.search.filter_parent_collection_for_module', mock.Mock(side_effect=None))
    def test_remove_tracks_parent_collection_response(self):
        response = {'parent_collection': {'type': 'tracks'}}
        process_parent_collection(response, None, None, False, None)

        assert response == {}

    @mock.patch('smarttv.droideka.proxy.views.search.fill_parent_collection_more_url', mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.search.filter_parent_collection_for_module', mock.Mock(side_effect=None))
    def test_do_not_remove_parent_collection_search_response(self):
        response = {'entity_data': {'parent_collection': {'type': 'movies'}}}
        process_parent_collection(response, None, None, True, None)

        assert response == {'entity_data': {'parent_collection': {'type': 'movies'}}}

    @mock.patch('smarttv.droideka.proxy.views.search.fill_parent_collection_more_url', mock.Mock(side_effect=None))
    @mock.patch('smarttv.droideka.proxy.views.search.filter_parent_collection_for_module', mock.Mock(side_effect=None))
    def test_do_not_remove_tracks_parent_collection_response(self):
        response = {'parent_collection': {'type': 'movie'}}
        process_parent_collection(response, None, None, False, None)

        assert response == {'parent_collection': {'type': 'movie'}}

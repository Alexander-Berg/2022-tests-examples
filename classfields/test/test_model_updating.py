import pytest
import os
import sys
from datetime import datetime

parent_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.append(parent_dir)

import api.models
from api.models import PricePredictor, ModelContainer


MODEL_DIRNAME = "test_model"
NEW_MODEL_PATHS_PATCH = {
    "SELL_APARTMENT_MSC": MODEL_DIRNAME + '/{version}.cbm'
}


@pytest.fixture
def old_model_path():
    """Путь на диске до текущей модели"""
    if not os.path.exists(MODEL_DIRNAME):
        os.mkdir(MODEL_DIRNAME)
    return f"{MODEL_DIRNAME}/old_temp.cbm"


@pytest.fixture
def new_model_path():
    """Путь на диске до загружаемой модели"""
    if not os.path.exists(MODEL_DIRNAME):
        os.mkdir(MODEL_DIRNAME)
    version = datetime.today().strftime("_%Y_%m_%d_%H_%M")
    return f'{MODEL_DIRNAME}/{version}.cbm'


@pytest.fixture
def s3_helper_test():
    """Помощник чтения конфигов из локального тестового конфига"""
    from vsml_common.data_helpers.s3 import S3Helper
    from vsml_common.data_helpers.datasource import DataSourceHelper
    data_sources_path = "local-vertis-datasources/datasources_test.json"
    data_sources = DataSourceHelper(data_sources_path, project_prefix="realty.mds")
    s3_helper = S3Helper(data_sources.get("s3.url"),
                         data_sources.get("s3.bucket"),
                         data_sources.get("s3.key.id"),
                         data_sources.get("s3.key.secret"),
                         silent=False)
    return s3_helper


@pytest.fixture
def s3_helper_prod():
    """Помощник чтения конфигов из локального продового конфига"""
    from vsml_common.data_helpers.s3 import S3Helper
    from vsml_common.data_helpers.datasource import DataSourceHelper
    data_sources_path = "local-vertis-datasources/datasources.json"
    data_sources = DataSourceHelper(data_sources_path, project_prefix="realty.mds")
    s3_helper = S3Helper(data_sources.get("s3.url"),
                         data_sources.get("s3.bucket"),
                         data_sources.get("s3.key.id"),
                         data_sources.get("s3.key.secret"),
                         silent=False)
    return s3_helper


def get_price_predictor(model_s3_path, s3_helper, old_model_path):
    """Получение PricePredictor с одной моделью из model_s3_path"""
    # скачиваем модель в old_model_path
    s3_helper.download_file(model_s3_path, old_model_path)
    # заменяем путь модели в PricePredictor на old_model_path
    old_model_paths_patch = {
        "SELL_APARTMENT_MSC": old_model_path
    }
    # создаем PricePredictor с загруженной моделью
    price_predictor = PricePredictor(model_paths=old_model_paths_patch)
    return price_predictor


@pytest.fixture
def old_price_predictor(s3_helper_test, old_model_path):
    """Получение PricePredictor с одной неактуальной моделью"""
    return get_price_predictor("price-estimator/sell_apartment_msk_2021_03_17.cbm", s3_helper_test, old_model_path)


@pytest.fixture
def new_price_predictor(s3_helper_test, old_model_path):
    """Получение PricePredictor с одной актуальной моделью"""
    return get_price_predictor("price-estimator/sell_apartment_msk/best.cbm", s3_helper_test, old_model_path)


@pytest.fixture
def light_price_predictor(mocker):
    """Получение PricePredictor со всеми моделями, но вместо контейнеров просто строки"""
    # заменяем класс ModelContainer на str
    # чтобы ускорить выполнение конструктора PricePredictor
    mocker.patch.object(api.models, "ModelContainer", str)
    # создаем PricePredictor с пропатченными "контейнерами-строками"
    price_predictor = PricePredictor()
    return price_predictor


@pytest.fixture
def full_price_predictor():
    """Получение полного PricePredictor"""
    price_predictor = PricePredictor()
    return price_predictor


def test_loading_new_model(old_price_predictor, s3_helper_test, new_model_path, old_model_path, mocker):
    """Обновление устаревшей модели"""
    assert old_price_predictor.models["SELL_APARTMENT_MSC"].model_path == old_model_path

    mocker.patch.object(old_price_predictor, "model_paths", NEW_MODEL_PATHS_PATCH)
    result = old_price_predictor.reload_model(model_name="SELL_APARTMENT_MSC", s3_helper=s3_helper_test)
    assert result
    assert old_price_predictor.models["SELL_APARTMENT_MSC"].model_path == new_model_path

    assert len(os.listdir(MODEL_DIRNAME)) == 1
    os.remove(new_model_path)
    assert len(os.listdir(MODEL_DIRNAME)) == 0


def test_loading_same_model(new_price_predictor, s3_helper_test, old_model_path, mocker):
    """Обновление актуальной модели"""
    assert new_price_predictor.models["SELL_APARTMENT_MSC"].model_path == old_model_path

    mocker.patch.object(new_price_predictor, "model_paths", NEW_MODEL_PATHS_PATCH)
    result = new_price_predictor.reload_model(model_name="SELL_APARTMENT_MSC", s3_helper=s3_helper_test)
    assert not result
    assert new_price_predictor.models["SELL_APARTMENT_MSC"].model_path == old_model_path

    assert len(os.listdir(MODEL_DIRNAME)) == 1
    os.remove(old_model_path)
    assert len(os.listdir(MODEL_DIRNAME)) == 0


def test_remove_old_model(old_price_predictor, s3_helper_test, new_model_path, old_model_path, mocker):
    """Удаление устаревшей модели на диске после обновления"""
    assert old_price_predictor.models["SELL_APARTMENT_MSC"].model_path == old_model_path

    mocker.patch.object(old_price_predictor, "model_paths", NEW_MODEL_PATHS_PATCH)
    result = old_price_predictor.reload_model(model_name="SELL_APARTMENT_MSC", s3_helper=s3_helper_test)
    assert result
    assert not os.path.isfile(old_model_path)

    assert len(os.listdir(MODEL_DIRNAME)) == 1
    os.remove(new_model_path)
    assert len(os.listdir(MODEL_DIRNAME)) == 0


def test_all_models_exist(full_price_predictor):
    """Определение всех необходимых моделей"""
    assert len(full_price_predictor.models) == 16
    for model_name, model in full_price_predictor.models.items():
        assert isinstance(model, ModelContainer)
    model_paths = [model.model_path for model_name, model in full_price_predictor.models.items()]
    # NOTE: есть одна повторяющаяся модель (см ниже)
    assert len(set(model_paths)) == 15
    # NOTE: модель для sell_villages отдельно для модерации не переобучается, используется одна единственная
    assert sum([
        model.model_path == './model/regular/sell_villages.cbm'
        for model_name, model in full_price_predictor.models.items()
    ]) == 2


def test_init_model_paths_exist():
    """Проверка всех необходимых моделей на диске"""
    from api.models import MODEL_PATHS
    assert len(MODEL_PATHS) == 16
    for model_name, model_path in MODEL_PATHS.items():
        assert os.path.isfile(model_path.format(version=""))


def test_model_s3_paths_exist(s3_helper_prod):
    """Проверка всех необходимых моделей в продовом S3"""
    from api.models import MODEL_S3_PATHS
    assert len(MODEL_S3_PATHS) == 14
    temp_model_path = f"{MODEL_DIRNAME}/temp.cbm"
    from botocore.exceptions import ClientError
    for model_name, model_s3_path in MODEL_S3_PATHS.items():
        try:
            s3_helper_prod.download_file(model_s3_path, temp_model_path)
        except ClientError as e:
            os.remove(temp_model_path)
            assert False, str(e)
        model_container = ModelContainer(temp_model_path)
        assert model_container.model_path == temp_model_path

    os.remove(temp_model_path)


def test_model_paths_matching(light_price_predictor):
    """Соответствие списка путей в S3 и списка путей на диске"""
    from api.models import MODEL_PATHS
    model_s3_paths = light_price_predictor.model_s3_paths
    assert len(set(model_s3_paths) - set(MODEL_PATHS)) == 0
    assert len(model_s3_paths) == 14
    assert len(set(model_s3_paths)) == 14
    assert len(set(MODEL_PATHS) - set(model_s3_paths)) == 2




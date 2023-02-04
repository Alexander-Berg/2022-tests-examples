import pytest
from ads_pytorch.nirvana.model_factory import ModelFactory


def test_forbid_init():
    with pytest.raises(ValueError):
        class MyFactory(ModelFactory):
            def __init__(self, *args, **kwargs):
                print("CALLED")
                super(MyFactory, self).__init__(*args, **kwargs)

            def create_loss(self):
                pass

            def create_model(self):
                pass

            def create_optimizer(self, model):
                pass


def test_attributes():
    class MyFactory(ModelFactory):
        def create_loss(self):
            pass

        def create_model(self):
            pass

        def create_optimizer(self, model):
            pass

    obj = MyFactory(model_config_path="./ahaha", all_files={"1": "2"})
    assert obj.model_config_path == "./ahaha"
    assert obj.all_files == {"1": "2"}


def test_post_init():
    class MyModelFactory(ModelFactory):
        def __model_factory_post_init__(self):
            self.x = "ahaha"

    obj = MyModelFactory(model_config_path="./f1.json", all_files={"1": "2"})
    assert obj.x == "ahaha"

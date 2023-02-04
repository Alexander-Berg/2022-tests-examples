from maps.garden.libs_server.infrastructure_clients import abc


class DictLikeObject(dict):
    def __getattr__(self, attr_name):
        return self.get(attr_name)


TVM_TICKET = "123"

TVM2_RESPONSE = {
    str(abc.ABC_TVM_ID): TVM_TICKET
}

SEDEM_RESPONSE = DictLikeObject({
    "service_config": DictLikeObject({
        "abc_slug": "yet-another-map-slug"
    })
})

ABC_RESPONSE = [{
    'person': {
        'login': 'vasya'
    }
}, {
    'person': {
        'login': 'petya'
    }
}, {
    'person': {
        'login': 'kolya'
    }
}]


class FakeAbcLib():
    def __init__(self, get_tvm_service_ticket):
        assert get_tvm_service_ticket() == TVM_TICKET

    def members(self, service_slug, role__code):
        if service_slug == "yet-another-map-slug" and role__code == abc.MODULE_DEVELOPER_ABC_ROLE:
            return ABC_RESPONSE
        else:
            raise NotImplementedError()


def _get_sedem_configuration(service_name):
    if service_name in ("maps-garden-yet-another-map"):
        return SEDEM_RESPONSE
    else:
        raise NotImplementedError()


SERVER_SETTINGS = {
    "tvm_client": {
        "client_id": 1,
        "token": "test_token",
    },
    "sedem": {
        "token": "test_token"
    }
}


def test_simple(mocker):
    sedem_patch = mocker.patch("maps.garden.libs_server.infrastructure_clients.abc.MachineApi")
    sedem_patch().configuration_get = _get_sedem_configuration
    mocker.patch("maps.garden.libs_server.infrastructure_clients.abc.ABC", FakeAbcLib)
    tvm_lib = mocker.patch("maps.garden.libs.auth.auth_client.TVM2")
    tvm_lib().get_service_tickets.return_value = TVM2_RESPONSE

    abc_client = abc.Client(SERVER_SETTINGS)
    assert abc_client.get_module_followers("yet_another_map") == ['vasya', 'petya', 'kolya']

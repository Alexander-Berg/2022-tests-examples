import pytest

from infra.deploy_monitoring_controller.controller import StandaloneController


def create_controller(yp_instance):
    return StandaloneController({
        "Controller": {
            "YpClient": {
                "Address": yp_instance.yp_client_grpc_address,
                "EnableSsl": False
            },
            "LeadingInvader": {
                "Path": "//home",
                "Proxy": yp_instance.create_yt_client().config["proxy"]["url"]
            }
        },
        "Unistat": {
            "Geo": "sas-test"
        },
        "YtObjectStorage": {
            "TableFolderPath": "//tmp/deploy_monitoring_controller_test",
            "Proxy": yp_instance.create_yt_client().config["proxy"]["url"],
        }
    })


@pytest.mark.usefixtures("ctl_env")
class TestDeployMonitoringController(object):
    def test_sync(self, ctl_env):
        pass
        # yp_client = ctl_env.yp_client
        # controller = create_controller(ctl_env.yp_instance)

        # TODO(test)

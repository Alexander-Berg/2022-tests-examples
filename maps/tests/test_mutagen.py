from maps.garden.sdk.extensions.mutagen import create_region_vendor_mutagen


class FakeGraphBuilder:
    def __init__(self):
        self.resources = []
        self.tasks = []

    def add_resource(self, resource):
        self.resources.append(resource.name)

    def add_task(self, demands, creates, task):
        self.tasks.append(task)


class FakeResource:
    def __init__(self, name):
        self.name = name


def test_make_mutagen():
    # simple smoke-test to check that function can be called
    graph_builder = FakeGraphBuilder()
    create_region_vendor_mutagen(graph_builder, "russia", "yandex")


def test_region_vendor_mutagen():
    graph_builder = FakeGraphBuilder()
    mutagen = create_region_vendor_mutagen(graph_builder, "russia", "yandex")
    mutagen.add_resource(FakeResource("mutable_resource"))
    assert "mutable_resource_russia_yandex" in graph_builder.resources

    mutagen = create_region_vendor_mutagen(graph_builder, "russia", "yandex", external_resources=["immutable_resource"])
    mutagen.add_resource(FakeResource("immutable_resource"))
    assert "immutable_resource" in graph_builder.resources

    mutagen.add_resource(FakeResource("mutable"))
    assert "mutable_russia_yandex" in graph_builder.resources

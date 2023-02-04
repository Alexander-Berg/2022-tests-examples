import dataclasses

from tasklet.api.v2 import data_model_pb2


@dataclasses.dataclass(frozen=True, order=True)
class Tasklet:
    ID: str
    Name: str
    NS: str


@dataclasses.dataclass(frozen=True, order=True)
class Build:
    ID: str
    Tasklet: str
    TaskletID: str
    NS: str
    Revision: int

    @classmethod
    def from_meta_obj(cls, meta, **kwargs) -> "Build":
        build = dict(
            ID=meta["id"],
            Tasklet=meta["tasklet"],
            TaskletID=meta["tasklet_id"],
            NS=meta["namespace"],
            Revision=int(meta.get("revision", 0)),
        )
        build.update(kwargs)
        return Build(**build)


@dataclasses.dataclass(frozen=True, order=True)
class Label:
    ID: str
    Name: str
    Target: str  # build ID


@dataclasses.dataclass(frozen=True, order=True)
class ConfiguredTasklet:
    namespace: data_model_pb2.NamespaceMeta
    tasklet: Tasklet
    build: Build
    label: Label

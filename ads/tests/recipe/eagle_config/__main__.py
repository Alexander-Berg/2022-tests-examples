from library.python.testing import recipe
from yatest.common import build_path, source_path, work_path, execute


def start(args):
    """recipe entry point (start services)."""
    execute(
        [
            build_path("ads/bsyeti/eagle/config/codegen/codegen"),
            source_path("ads/bsyeti/eagle/config/clients.pb"),
            work_path("clients_generated.pb"),
        ]
    )


def stop(_):
    """recipe entry point (stop services)."""
    pass


if __name__ == "__main__":
    recipe.declare_recipe(start, stop)

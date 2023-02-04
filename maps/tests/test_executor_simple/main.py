import click
import time


@click.command()
@click.option("--module-binary", required=True, type=click.Path(exists=True), help="Module binary file")
@click.option("--environment-settings", type=click.Path(), help="File to read environment settings from")
@click.option("--logfile", default=None, help="Path to local log file to be uploaded to logs_storage")
@click.option("--task-key", help="Task identifier in collection tasks")
def main(
        module_binary,
        environment_settings,
        logfile,
        task_key,
):
    time.sleep(1)
    exit(0)

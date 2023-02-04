from yt import wrapper as yt

from billing.hot.tests.config import config


class Client:
    def __init__(self, yt_client: yt.YtClient, cfg: config.YtConfig) -> None:
        self.client = yt_client
        self.tables = cfg.tables

    def insert_rows(self, table_name: str, values: list[dict], atomicity: str = None) -> None:
        self.client.insert_rows(
            self.tables[table_name],
            values,
            atomicity=atomicity,
            raw=False,
            require_sync_replica=True
        )

    def delete_rows(self, table_name: str, values: list[dict], atomicity: str = None) -> None:
        self.client.delete_rows(
            self.tables[table_name],
            values,
            atomicity=atomicity,
            raw=False,
            require_sync_replica=True
        )

    def select_rows_dynamic(self, query):
        return yt.dynamic_table_commands.select_rows(query, client=self.client)

from library.python.vault_client import instances as client


class SecretProvider:
    def __new__(cls, *args, **kwargs) -> 'SecretProvider':
        if not hasattr(cls, 'instance'):
            cls.instance = object.__new__(cls)
        return cls.instance

    def __init__(self) -> None:
        self.client = client.Production(decode_files=True)

    def get_secret(self, secret_uuid: str, secret_key: str):
        secret_info = self.client.get_version(secret_uuid)
        return secret_info['value'][secret_key]

import unittest.mock


class AsyncContextManager:
    def __init__(self, /, return_value):
        self._return_value = return_value
        self._aenter_call_store = unittest.mock.Mock()
        self._aexit_call_store = unittest.mock.Mock()

    async def __aenter__(self):
        self._aenter_call_store()
        return self._return_value

    async def __aexit__(self, type, value, traceback):
        self._aexit_call_store(type, value, traceback)
        pass

    def assert_entered_once(self):
        self._aenter_call_store.assert_called_once_with()

    def assert_exited_once(self):
        self._aexit_call_store.assert_called_once()


class AsyncContextManagerMock(unittest.mock.Mock):
    """
    Ну, мокать контекст менеджеры это всегда боль.
    Этот мок должен уменьшать количество боли и если очень надо то предоставляет assert_entered* и assert_exit* методы.
    Идея такая:
    1. Возьмём за основу NonCallableMock, потому что кому придёт в голову вызывать контекстный менеджер?
    2. Будем вести себя как обычный мок, но aenter и aexit определены! Это асинхронные методы.
    3. Для ассертов схитрим - запомним совершенные вызовы в специально созданных моках. Для ассертов будем использовать
       ассерты этих же моков

    Проблематика
    В обычных моках aenter не определен.
    Казалось бы в AsyncMock определен aenter. Вот только тебе необходимо
    чередовать AsyncMock и обычный Mock. Рассмотрим пример.

    ```
    async with client_manager.acquire() as client:
        async with client.download_file('file.txt') as file_reader:
            data = await file_reader.read(100)
            data += await file_reader.read(200)
    ```
    Предположим, что acquire() вызывает поход в sd, поэтому его нельзя не мокать.

    Итоговый мок выглядит так.

    ```
    mocker.patch.object(
        ClientManager,
        'acquire',
        mocker.Mock(
            return_value=mocker.AsyncMock(  # acquire() ctx manager
                __aenter__=mocker.AsyncMock(
                    return_value=mocker.Mock(  # client
                        download_file=mocker.Mock(
                            return_value=mocker.AsyncMock(  # download_file() ctx manager
                                __aenter__=mocker.AsyncMock(
                                    return_value=mocker.AsyncMock()  # file_reader. Это уже не обязательно.
                                )
                            )
                        )
                    )
                )
            )
        )
    )
    ```

    А вот как это выглядит с AsyncContextManagerMock

    ```
    mocker.patch.object(
        ClientManager,
        'acquire',
        AsyncContextManagerMock(  # acquire() ctx manager
            return_value=mocker.Mock(  # client
                download_file=AsyncContextManagerMock(  # download_file() ctx manager
                    return_value=mocker.AsyncMock()  # file_reader. Это уже не обязательно.
                )
            )
        )
    )
    ```

    Огонь?
    """

    def __init__(self, /, return_value, **kwargs):
        ctx_manager = AsyncContextManager(return_value)
        super().__init__(return_value=ctx_manager, **kwargs)
        self.ctx_result = return_value
        self._ctx_manager = ctx_manager

    def __getattr__(self, name):
        if name.startswith('assert_') and hasattr(self._ctx_manager, name):
            return getattr(self._ctx_manager, name)
        return super().__getattr__(name)

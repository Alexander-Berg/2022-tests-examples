import pytest
import asyncio
from ads_pytorch.tools.progress import (
    ProgressEntity,
    ProgressCallable,
    StringProgressLogger,
    ProgressLogger
)


################################################
#                   ENTITY                     #
################################################


def test_progress_entity():
    with pytest.raises(Exception):
        e = ProgressEntity()
    e = ProgressEntity('myname')
    with pytest.raises(Exception):
        e = ProgressEntity(format='{}')
    e = ProgressEntity(name="ahaha", format="{}")
    e2 = ProgressEntity(name="ahaha", format="{}", value=105)
    assert e.name == "ahaha"
    assert e.format == "{}"
    assert e.value is None

    assert e2.name == "ahaha"
    assert e2.format == "{}"
    assert e2.value == 105


class BufferedProgressLogger(StringProgressLogger):
    def __init__(self, *args, **kwargs):
        super(BufferedProgressLogger, self).__init__(*args, **kwargs)
        self.printed_strings = []

    def _log(self, string: str):
        self.printed_strings.append(string)


class X(ProgressCallable):
    def __init__(self):
        self._call_counter = 0

    def get_progress(self):
        self._call_counter += 1
        return [
            ProgressEntity(name="OOPS", format="{:>14}", value=self._call_counter),
            ProgressEntity(name="BEEP", format="{:>14}", value=self._call_counter ** 2)
        ]


################################################
#            STRING LOGGER IMPL                #
################################################


@pytest.mark.asyncio
async def test_string_logger():
    progress = BufferedProgressLogger(separator="| ")
    obj = X()
    for i in range(2):
        await progress.log_progress(obj.get_progress())

    assert progress.printed_strings == [
        "| ".join(["{:>14}"] * 2).format("OOPS", "BEEP"),
        "| ".join(["{:>14}"] * 2).format(1, 1),
        "| ".join(["{:>14}"] * 2).format(2, 4)
    ]


@pytest.mark.asyncio
async def test_string_logger_reprint_header_after_object_set_change():
    progress = BufferedProgressLogger(separator="| ")

    async def _add_and_print(obj):
        obj2 = X()
        await progress.log_progress(obj.get_progress() + obj2.get_progress())
        await progress.log_progress(obj.get_progress() + obj2.get_progress())

    obj = X()
    await progress.log_progress(obj.get_progress())
    await _add_and_print(obj)
    await progress.log_progress(obj.get_progress())

    assert progress.printed_strings == [
        "| ".join(["{:>14}"] * 2).format("OOPS", "BEEP"),
        "| ".join(["{:>14}"] * 2).format(1, 1),
        "| ".join(["{:>14}"] * 4).format("OOPS", "BEEP", "OOPS", "BEEP"),
        "| ".join(["{:>14}"] * 4).format(2, 4, 1, 1),
        "| ".join(["{:>14}"] * 4).format(3, 9, 2, 4),
        "| ".join(["{:>14}"] * 2).format("OOPS", "BEEP"),
        "| ".join(["{:>14}"] * 2).format(4, 16)
    ]


################################################
#              PROGRESS LOGGER                 #
################################################


@pytest.mark.asyncio
async def test_automatic_progress_logging():
    progress = BufferedProgressLogger(separator="| ")
    logger = ProgressLogger([progress], frequency=0.099)
    obj = X()
    logger.register(obj)
    for _ in range(2):
        async with logger:
            await asyncio.sleep(0.5)
            logger.raise_on_error()
        prev_len = len(progress.printed_strings)
        assert prev_len >= 5
        await asyncio.sleep(0.11)
        assert prev_len == len(progress.printed_strings)


@pytest.mark.asyncio
async def test_automatic_several_loggers():
    subloggers = [BufferedProgressLogger(separator="| ") for _ in range(3)]
    logger = ProgressLogger(subloggers, frequency=0.099)
    obj = X()
    logger.register(obj)
    async with logger:
        await asyncio.sleep(0.5)
        logger.raise_on_error()
    for sublogger in subloggers:
        prev_len = len(sublogger.printed_strings)
        assert prev_len >= 5
        await asyncio.sleep(0.11)
        assert prev_len == len(sublogger.printed_strings)


class MyTestExc(Exception):
    pass


class DeadProgressLogger(StringProgressLogger):
    def _log(self, string: str):
        raise MyTestExc("OPS")


@pytest.mark.parametrize("call_raise", [True, False])
@pytest.mark.asyncio
async def test_logger_die_on_raise(call_raise):
    subloggers = [BufferedProgressLogger(separator="| "), DeadProgressLogger()]
    logger = ProgressLogger(subloggers, frequency=0.099)
    obj = X()
    logger.register(obj)
    with pytest.raises(MyTestExc):
        async with logger:
            await asyncio.sleep(0.5)
            if call_raise:
                logger.raise_on_error()

    # Test that again entering logger is forbidden
    with pytest.raises(MyTestExc):
        async with logger:
            pass

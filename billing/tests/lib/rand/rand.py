import random
import secrets
import uuid as uid

MAX_INT32 = (1 << 31) - 1
MAX_INT64 = (1 << 63) - 1
MAX_UINT64 = (1 << 64) - 1


def int32(*, from_num: int = 0) -> int:
    return random.randint(from_num, MAX_INT32)


def int64(*, from_num: int = 0) -> int:
    return random.randint(from_num, MAX_INT64)


def uint64(*, from_num: int = 0) -> int:
    return random.randint(from_num, MAX_UINT64)


def uuid() -> str:
    return str(uid.uuid4())


def hex(length: int) -> str:
    return secrets.token_hex(length)

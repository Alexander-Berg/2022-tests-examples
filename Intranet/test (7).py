#!/usr/bin/python

import asyncio
import sys

from tasha.core.tasha_worker.redis_settings import create_redis_pool


async def main():
    redis = await create_redis_pool()
    await redis.enqueue_job('unban_user', sys.argv[1])


if __name__ == '__main__':
    asyncio.get_event_loop().run_until_complete(main())

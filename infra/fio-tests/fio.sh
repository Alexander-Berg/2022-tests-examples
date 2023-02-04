#!/bin/bash
exec ./rootfs/usr/bin/fio --client=ip6::: "$@"

def kernel_version():
    with open('/proc/sys/kernel/osrelease') as f:
        version = f.read().strip()

    version, build = version.split('-')
    maj, min, patch = version.split('.')

    return (int(maj), int(min), int(patch), build)

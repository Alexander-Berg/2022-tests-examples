apt-get clean
find var/cache/apt/archives -iname '*.deb' -delete
find var/lib/apt/lists -type f -delete
find var/cache/apt -iname '*.bin' -delete

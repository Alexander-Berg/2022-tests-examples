# cleanup tmp
find tmp -mindepth 1 -delete

# cleanup logs
find var/log -iname '*.gz' -delete
find var/log -iname '*.1' -delete
find var/log -type f -print0 | xargs -0 -t tee < /dev/null

# cleanup history
> root/.bash_history

# cleanup cache
rm -rf root/.cache

# cleanup tmp apt configs
rm -rf etc/apt/apt.conf.d/80-retries-env-tmp

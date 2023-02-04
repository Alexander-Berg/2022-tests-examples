#!/bin/bash

REMOTE_WRITE_TARGETS="
http://vminsert-main-sas-int.slb.vertis.yandex.net:2250/insert/1/prometheus
http://vminsert-main-vla-int.slb.vertis.yandex.net:2254/insert/1/prometheus
"

REMOTE_WRITE_LABELS="prom_cluster=testing"
RW_MAX_DISK_USAGE_PER_URL="500000000"
MEMORY_ALLOWED_PERCENT="60"

SCRAPE_FILE="/etc/scrape.yml"

TMP_DIR="/dev/shm/vmagent-remotewrite-data"

error() {
    echo "error: ${1}"
    exit 1
}

/usr/bin/vmagent-mkconfig.sh || error "Config generation failed"

# compose remote write targets
rw_targets=
for rw_target in ${REMOTE_WRITE_TARGETS}; do
        rw_targets="${rw_targets} -remoteWrite.url=${rw_target}"
done

#compose remote write labels
rw_labels=
for rw_label in ${REMOTE_WRITE_LABELS}; do
        rw_labels="${rw_labels} -remoteWrite.label=${rw_label}"
done

MYHOSTNAME=$(hostname -f)
CGROUPS=$(curl -sf "https://c.yandex-team.ru/api-cached/hosts2groups/$(hostname -f)") || error "Can't get current group from conductor"
CGROUP=$(echo ${CGROUPS} | awk '{print $NF}')
CHOSTS_RAW=$(curl -sf "https://c.yandex-team.ru/api-cached/groups2hosts/${CGROUP}") || error "Can't get group hosts from conductor"
CHOSTS=$(echo ${CHOSTS_RAW} | sort)

CHOSTNUM=$(echo ${CHOSTS} | wc -w)

MYID=0
for CHOST in ${CHOSTS}; do
        [ "$(hostname -f)" == "${CHOST}" ] && break
        MYID=$((MYID+1))
done

# run vmagent
/usr/bin/vmagent -enableTCP6 \
	-promscrape.config=${SCRAPE_FILE} \
	-promscrape.streamParse=true \
        -promscrape.dropOriginalLabels=true \
        -promscrape.consul.waitTime=110s \
	-promscrape.consulSDCheckInterval=60s \
	-promscrape.discovery.concurrency=30 \
	-promscrape.suppressScrapeErrors=true \
	-promscrape.suppressDuplicateScrapeTargetErrors=true \
	-promscrape.cluster.membersCount=${CHOSTNUM} \
	-promscrape.cluster.replicationFactor=${CHOSTNUM} \
	-promscrape.cluster.memberNum=${MYID} \
	${rw_targets} \
	${rw_labels} \
	-remoteWrite.maxDiskUsagePerURL=${RW_MAX_DISK_USAGE_PER_URL} \
	-remoteWrite.tmpDataPath=${TMP_DIR} \
	-remoteWrite.showURL=true \
	-remoteWrite.queues=10 \
	-memory.allowedPercent=${MEMORY_ALLOWED_PERCENT}

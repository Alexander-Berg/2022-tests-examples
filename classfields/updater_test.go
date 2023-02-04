package haproxy

import (
	"github.com/YandexClassifieds/go-common/log/logrus"
	"io/ioutil"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"
)

func TestHaproxy(t *testing.T) {
	log := logrus.New("INFO")

	haproxyS3Config := "http://s3.mds.yandex.net/adm/files/tmp/haproxy-config-for-golang-tests.cfg"
	haproxyCheckInterval := 2 * time.Second

	h := NewUpdater(haproxyS3Config, haproxyCheckInterval, log)

	// 1 - Download
	needUpdate, etag, err := h.checkOrDownloadNewConfig()
	if err != nil {
		t.Fatal(err)
	}
	if needUpdate == false {
		t.Errorf("Error, no update required at startup")
	}
	if etag == "" {
		t.Errorf("Error, empty etag after download new chaproxy config")
	}

	// 2 - backup
	err = h.backupOldConfig()
	if err != nil {
		t.Fatal(err)
	}

	// 3 - validate
	err = h.validateNewConfig()
	if err != nil {
		t.Errorf("Config validation error")
		t.Fatal(err)
	}

	// 4 - copy config
	err = h.copyNewHaproxyConfig()
	if err != nil {
		t.Fatal(err)
	}

	time.Sleep(1 * time.Second) // not so fast !

	// 5 - get haproxy PID
	pid := getHaproxyPid(t)

	// 6 - stop haproxy process
	err = h.stopOldHaproxy()
	if err != nil {
		t.Fatal(err)
	}

	time.Sleep(2 * time.Second) // wait for haproxy process

	h.etag = etag
	oldPid := pid

	// 6.1 repeat without changes
	needUpdate, etag, err = h.checkOrDownloadNewConfig()
	if err != nil {
		t.Fatal(err)
	}
	if etag == "" {
		t.Errorf("Error, empty etag after download new chaproxy config")
	}
	if etag != h.etag {
		t.Errorf("Error, etag changed")
	}
	if needUpdate == true {
		t.Errorf("Error, update required without changing the config")
	}

	// 6.2 check new pid
	pid = getHaproxyPid(t)
	if oldPid == pid {
		t.Errorf("Error, pid has not changed")
	}
}

func getHaproxyPid(t *testing.T) int {
	// 5 - get haproxy PID
	piddata, err := ioutil.ReadFile(haproxyPidFile)
	if err != nil {
		t.Errorf("Error while reading the file")
	}

	pid, err := strconv.Atoi(strings.TrimSuffix(string(piddata), "\n"))
	if err != nil {
		t.Errorf("Unable to read and parse haproxy pid file")
	}

	// Look for the pid in the process list.
	_, err = os.FindProcess(pid)
	if err != nil {
		t.Errorf("Cannot find haproxy process by PID")
	}

	return pid
}

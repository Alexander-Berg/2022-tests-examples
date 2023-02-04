package tests

import (
	"a.yandex-team.ru/infra/wall-e/agent/kexec_helper/lib"
	"github.com/stretchr/testify/assert"
	"testing"
)

var saltJSON = `{"kernel": {
"bootVersion": {
"error": "",
"transitionTime": "2020-09-16T06:43:33.765768Z",
"version": "4.19.143-36"
},
"needReboot": {
"message": "Ready to reboot into 4.19.143-36",
"status": "True",
"transitionTime": "2020-09-16T14:47:49.108906Z"
},
"ready": {
"message": "spec.version='4.19.143-36' != status.version='4.19.143-cfsdbg+'",
"status": "False",
"transitionTime": "2020-09-16T14:47:49.108893Z"
},
"version": "4.19.143-cfsdbg+"
}}
`

var wrongJSON = `{}`
var testCmd = "-l --reuse-cmdline --initrd /boot/initrd.img-4.19.119-30.2 /boot/vmlinuz-4.19.119-30.2"
var testKern = kexeclib.KernelMessage{NeedReboot: true, KernelVersion: "4.19.119-30.2"}
var testFlags = kexeclib.FlagSet{Job: kexeclib.UPGRADE, Dry: false}

func TestGetKernelVersion(t *testing.T) {
	//test correct ya-salt JSON
	tkm, err := kexeclib.GetKernelVersion(saltJSON)
	if err != nil {
		t.Fatal("can't parse valid ya-salt JSON")
	}
	assert.Equal(t, tkm.NeedReboot, true)
	assert.Equal(t, tkm.KernelVersion, "4.19.143-36")
}

func TestGetKernelVersionException(t *testing.T) {
	_, err := kexeclib.GetKernelVersion(wrongJSON)
	if err == nil {
		t.Fatal("parsed incorrect JSON format")
	}
}

func TestKexecCmdline(t *testing.T) {
	assert.Equal(t, kexeclib.KexecCmdline(&testKern), testCmd)
}

func TestIsNeedKernReboot(t *testing.T) {
	assert.Equal(t, kexeclib.IsNeedKernReboot(&testKern, testFlags), true)
}

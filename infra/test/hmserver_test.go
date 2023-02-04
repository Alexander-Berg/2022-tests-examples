package test

import (
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"a.yandex-team.ru/infra/hostctl/units"
	"a.yandex-team.ru/library/go/test/assertpb"
	"testing"
)

func TestRendering_HMServer(t *testing.T) {
	stages := map[string]struct {
		host    string
		stage   string
		version string
		content string
	}{
		"PRESTABLE": {
			host:    "man1-3720.search.yandex.net",
			stage:   "absent",
			version: "",
			content: "[Service]\nEnvironment=HM_REMOTES=saltstack,sysdev,core\nEnvironment=HM_ROLE=man\n",
		}, "SAS": {
			host:    "stout21.search.yandex.net",
			stage:   "hostman",
			version: "1.4-7367789",
			content: "[Service]\nEnvironment=HM_REMOTES=saltstack,sysdev,core\nEnvironment=HM_ROLE=sas\n",
		}, "VLA": {
			host:    "vla1-0474.search.yandex.net",
			stage:   "hostman",
			version: "1.4-7367789",
			content: "[Service]\nEnvironment=HM_REMOTES=saltstack,sysdev,core\nEnvironment=HM_ROLE=vla\n",
		}, "MAN": {
			host:    "man0-0136.search.yandex.net",
			stage:   "hostman",
			version: "1.4-7367789",
			content: "[Service]\nEnvironment=HM_REMOTES=saltstack,sysdev,core\nEnvironment=HM_ROLE=man\n",
		}, "MSK": {
			host:    "myt1-0074.search.yandex.net",
			stage:   "hostman",
			version: "1.4-7367789",
			content: "[Service]\nEnvironment=HM_REMOTES=saltstack,sysdev,core\nEnvironment=HM_ROLE=msk\n",
		}}
	for stage, stageData := range stages {
		t.Run(stage, func(t *testing.T) {
			f, err := units.Dir.Open("yandex-hm-server.yaml")
			if err != nil {
				t.Error(err)
				return
			}
			result, err := units.RenderOn(f, stage, stageData.host)
			if err != nil {
				t.Fatalf("Failed to render unit on %s host_info: %s", stageData.host, err)
			}
			expectedSlotMeta := &pb.SlotMeta{
				Annotations: map[string]string{
					"stage":    stageData.stage,
					"filename": "test",
				},
			}
			expectedRevMeta := &pb.RevisionMeta{
				Kind:    "SystemService",
				Version: stageData.version,
			}
			assertpb.Equal(t, expectedRevMeta, result.RevMeta())
			assertpb.Equal(t, expectedSlotMeta, result.SlotMeta())
			expectedSpec := &pb.SystemServiceSpec{
				Packages: []*pb.SystemPackage{{
					Name:    "yandex-hm-server",
					Version: stageData.version,
				}},
				Files: []*pb.ManagedFile{{
					Path:    "/etc/systemd/system/yandex-hm-server.service.d/30-settings.conf",
					Content: stageData.content,
				}},
			}
			assertpb.Equal(t, expectedSpec, result.Spec())
		})
	}
}

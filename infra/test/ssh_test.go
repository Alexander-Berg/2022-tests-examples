package test

import (
	pb "a.yandex-team.ru/infra/hostctl/proto"
	"a.yandex-team.ru/infra/hostctl/units"
	"a.yandex-team.ru/library/go/test/assertpb"
	"testing"
)

func TestRendering_ssh(t *testing.T) {
	f, err := units.Dir.Open("ssh.yaml")
	if err != nil {
		t.Error(err)
	}
	result, err := units.RenderOn(f, "PRESTABLE", "man1-3720.search.yandex.net")
	if err != nil {
		t.Error(err)
	}
	expectedSlotMeta := &pb.SlotMeta{
		Annotations: map[string]string{
			"stage":    "default",
			"filename": "test",
			"env.noop": "True",
		},
	}
	expectedRevMeta := &pb.RevisionMeta{
		Kind:    "SystemService",
		Version: "default",
	}
	assertpb.Equal(t, expectedRevMeta, result.RevMeta())
	assertpb.Equal(t, expectedSlotMeta, result.SlotMeta())
	expectedSpec := &pb.SystemServiceSpec{}
	assertpb.Equal(t, expectedSpec, result.Spec())
}

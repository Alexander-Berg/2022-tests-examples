package clickHouse

import (
	"github.com/stretchr/testify/require"
	"reflect"
	"testing"
)

func Test_ParseToShards(t *testing.T) {
	tests := []struct {
		name  string
		dc    string
		hosts string
		want  []Shard
	}{
		{"empty", "sas", "", []Shard{}},
		{"comma", "sas", ",", []Shard{}},
		{"dot-comma", "sas", ";", []Shard{}},
		{"comma-dot-comma", "sas", ",,;", []Shard{}},
		{"single shard", "sas", "sas-a,vla-b",
			[]Shard{{Replicas: []string{"sas-a", "vla-b"}}}},
		{"two shards", "sas", "sas-a,vla-b;sas-c,vla-d",
			[]Shard{{Replicas: []string{"sas-a", "vla-b"}}, {Replicas: []string{"sas-c", "vla-d"}}}},
		{"two unsymmetrical shards", "sas", "sas-a;sas-c,vla-d",
			[]Shard{{Replicas: []string{"sas-a"}}, {Replicas: []string{"sas-c", "vla-d"}}}},
		{"real world", "vla", "sas-erw3qsc4esyao1q0.db.yandex.net:9440,vla-t5bmrxokt984p5gw.db.yandex.net:9440;sas-amhyn8scy4fvlcqi.db.yandex.net:9440,vla-ozaodsadw42w701r.db.yandex.net:9440;sas-grqbg232aggg4908.db.yandex.net:9440,vla-ffpfr7xwa0hqguhr.db.yandex.net:9440;sas-jef2z1xeqoqcjf0j.db.yandex.net:9440,vla-3yk2brkquj30tk8n.db.yandex.net:9440;sas-ygyqu77147fnk2b5.db.yandex.net:9440,vla-o9atsiggbcvxl1n4.db.yandex.net:9440",
			[]Shard{
				{Replicas: []string{"vla-t5bmrxokt984p5gw.db.yandex.net:9440", "sas-erw3qsc4esyao1q0.db.yandex.net:9440"}},
				{Replicas: []string{"vla-ozaodsadw42w701r.db.yandex.net:9440", "sas-amhyn8scy4fvlcqi.db.yandex.net:9440"}},
				{Replicas: []string{"vla-ffpfr7xwa0hqguhr.db.yandex.net:9440", "sas-grqbg232aggg4908.db.yandex.net:9440"}},
				{Replicas: []string{"vla-3yk2brkquj30tk8n.db.yandex.net:9440", "sas-jef2z1xeqoqcjf0j.db.yandex.net:9440"}},
				{Replicas: []string{"vla-o9atsiggbcvxl1n4.db.yandex.net:9440", "sas-ygyqu77147fnk2b5.db.yandex.net:9440"}},
			}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := ParseToShards(tt.dc, tt.hosts)
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ParseToShards() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_ParseToShards_Random(t *testing.T) {
	got1 := ParseToShards("sas", "vla-a,myt-b;vla-c,myt-d")
	for i := 0; i < 100; i++ {
		got2 := ParseToShards("sas", "vla-a,myt-b;vla-c,myt-d")
		if !reflect.DeepEqual(got1, got2) {
			return
		}
	}
	require.FailNow(t, "failed to assert random parsing shards")
}

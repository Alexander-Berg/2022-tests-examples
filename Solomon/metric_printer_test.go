package utils

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestAddCommonLabels(t *testing.T) {
	tests := []struct {
		name string
		line string
		lbls map[string]string
	}{
		{name: "Common labels",
			line: "common labels: {host='', lbl='q'}",
			lbls: map[string]string{"host": "", "lbl": "q"},
		},
		{name: "Empty common labels",
			line: "common labels: {}",
			lbls: map[string]string{},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			lbls := make(map[string]string)
			addCommonLabels(test.line, lbls)

			assert.Equal(t, test.lbls, lbls)
		})
	}
}

func TestAddLabels(t *testing.T) {
	tests := []struct {
		name string
		line string
		lbls map[string]string
	}{
		{name: "Add labels",
			line: "   IGAUGE alert.evaluation.status{alertId='a9a00891de0e5311defa394a2ec09373c58679f1', parentId='coremon_ready_shard_count', projectId='solomon'} [1]",
			lbls: map[string]string{
				"sensor":    "alert.evaluation.status",
				"alertId":   "a9a00891de0e5311defa394a2ec09373c58679f1",
				"parentId":  "coremon_ready_shard_count",
				"projectId": "solomon",
			},
		},
		{name: "Add only metric labels",
			line: "   IGAUGE alert.evaluation.status{} [1]",
			lbls: map[string]string{
				"sensor": "alert.evaluation.status",
			},
		},
		{name: "Add another labels",
			line: "COUNTER channel.notification.status{channelId='fake-juggler', projectId='logbroker', status='OBSOLETE'} [1310]",
			lbls: map[string]string{
				"sensor":    "channel.notification.status",
				"channelId": "fake-juggler",
				"status":    "OBSOLETE",
				"projectId": "logbroker",
			},
		},
		{name: "Add labels HIST_RATE",
			line: "HIST_RATE auth.elapsedTimeMs{authType='TvmService'} [{16: 0, 32: 0, 64: 0, 128: 0, 256: 0, 512: 0, 1024: 0, 2048: 0, 4096: 0, 8192: 0, 16384: 0, 32768: 0, inf: 0}]",
			lbls: map[string]string{
				"sensor":   "auth.elapsedTimeMs",
				"authType": "TvmService",
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			lbls := make(map[string]string)
			addLabels(test.line, lbls)
			assert.Equal(t, test.lbls, lbls)
		})
	}
}

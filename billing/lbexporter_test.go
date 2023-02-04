package entities

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestEventBatch_MarshalJSON(t *testing.T) {
	tests := []struct {
		name     string
		input    ExportEventBatchMeta
		expected []byte
		wantErr  bool
	}{
		{
			name: "non-empty info",
			input: ExportEventBatchMeta{
				EventBatch: EventBatch{
					Type:       "test_type",
					Dt:         time.Date(2005, 04, 30, 23, 17, 52, 0, time.UTC),
					ExternalID: "8ea94e16",
					Info:       []byte("[1, 2, 3]"),
					EventCount: 10,
				},
				ID: 1,
			},
			expected: []byte(
				`{"dt":1114903072,"event_count":10,"external_id":"8ea94e16","id":1,"info":[1,2,3],"type":"test_type"}`,
			),
			wantErr: false,
		},
		{
			name: "empty info",
			input: ExportEventBatchMeta{
				EventBatch: EventBatch{
					Type:       "test_type",
					Dt:         time.Date(2005, 04, 30, 23, 17, 52, 0, time.UTC),
					ExternalID: "8ea94e16",
					Info:       nil,
					EventCount: 10,
				},
				ID: 2,
			},
			expected: []byte(
				`{"dt":1114903072,"event_count":10,"external_id":"8ea94e16","id":2,"info":null,"type":"test_type"}`,
			),
			wantErr: false,
		},
	}
	for _, tt := range tests {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			if actual, err := json.Marshal(tt.input); tt.wantErr {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
				assert.Equal(t, string(tt.expected), string(actual))
			}
		})
	}
}

func TestEventBatch_UnmarshalJSON(t *testing.T) {
	type args struct {
		data []byte
	}
	tests := []struct {
		name     string
		args     args
		expected ExportEventBatchMeta
		wantErr  bool
	}{
		{
			name: "sample",
			args: args{[]byte(
				`{"dt":1114903072,"event_count":10,"external_id":"8ea94e16","id":1,"info":[1,2,3],"type":"test_type"}`,
			)},
			expected: ExportEventBatchMeta{
				EventBatch: EventBatch{
					Type:       "test_type",
					Dt:         time.Date(2005, 04, 30, 23, 17, 52, 0, time.UTC),
					ExternalID: "8ea94e16",
					Info:       []byte("[1,2,3]"),
					EventCount: 10,
				},
				ID: 1,
			},
			wantErr: false,
		},
		{
			name: "null info",
			args: args{[]byte(
				`{"dt":1114903072,"event_count":10,"external_id":"8ea94e16","id":2,"info":null,"type":"test_type"}`,
			)},
			expected: ExportEventBatchMeta{
				EventBatch: EventBatch{
					Type:       "test_type",
					Dt:         time.Date(2005, 04, 30, 23, 17, 52, 0, time.UTC),
					ExternalID: "8ea94e16",
					Info:       nil,
					EventCount: 10,
				},
				ID: 2,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		tt := tt
		t.Run(tt.name, func(t *testing.T) {
			var e ExportEventBatchMeta
			if err := json.Unmarshal(tt.args.data, &e); tt.wantErr {
				assert.Error(t, err)
			} else {
				assert.NoError(t, err)
				assert.Equal(t, tt.expected, e)
			}
		})
	}
}

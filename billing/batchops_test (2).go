package entities

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBatchWriteRequest_MarshalJSON(t *testing.T) {
	v1 := "v1"
	v2 := "v2"
	v3 := "v3"
	e := BatchWriteRequest{
		EventType:  "batch",
		ExternalID: "1234",
		Dt:         time.Unix(1708091866, 0).UTC(),
		Info:       []byte(`["some","info"]`),
		Events: []EventAttributes{{
			Loc: LocationAttributes{
				Namespace: "ns",
				Type:      "et",
				Attributes: map[string]*string{
					"a1": &v1,
					"a2": &v2,
				},
			},
			Type:   "debit",
			Dt:     time.Unix(1608091866, 0).UTC(),
			Amount: "123.45",
			Info:   []byte(`{"some":"info"}`),
		}},
		States: []StateAttributes{{
			Loc: LocationAttributes{
				Namespace: "ns",
				Type:      "st",
				Attributes: map[string]*string{
					"a1": &v2,
					"a2": &v3,
				},
			},
			State: []byte(`{"some":"json"}`),
		}},
		Locks: []LockAction{
			{
				Loc: LocationAttributes{
					Namespace: "ns",
					Type:      "lt1",
					Attributes: map[string]*string{
						"a1": &v1,
					},
				},
				UID:  "123",
				Mode: "validate",
			},
			{
				Loc: LocationAttributes{
					Namespace: "ns",
					Type:      "lt2",
					Attributes: map[string]*string{
						"a2": &v2,
					},
				},
				UID:  "321",
				Mode: "remove",
			},
		},
	}

	res, err := json.Marshal(e)
	require.NoError(t, err)

	// nolint: lll
	assert.Equal(t, `{"dt":1708091866,"event_type":"batch","events":[{"amount":"123.45","dt":1608091866,"info":{"some":"info"},"loc":{"a1":"v1","a2":"v2","type":"et","namespace":"ns"},"type":"debit"}],"external_id":"1234","info":["some","info"],"locks":[{"loc":{"a1":"v1","type":"lt1","namespace":"ns"},"uid":"123","mode":"validate"},{"loc":{"a2":"v2","type":"lt2","namespace":"ns"},"uid":"321","mode":"remove"}],"states":[{"loc":{"a1":"v2","a2":"v3","type":"st","namespace":"ns"},"state":{"some":"json"}}]}`,
		string(res))
}

func TestBatchWriteRequest_UnmarshalJSON(t *testing.T) {
	// nolint: lll
	d := []byte(`{"event_type":"batch","external_id":"1234","info":["some","info"],"dt":1708091866,"events":[{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"type":"debit","dt":1608091866,"amount":"123.45","info":{"some":"info"}}],"states":[{"loc":{"namespace":"ns","type":"st","a1":"v2","a2":"v3"},"state":{"some":"json"}}],"locks":[{"loc":{"namespace":"ns","type":"lt1","a1":"v1"},"uid":"123","mode":"validate"},{"loc":{"namespace":"ns","type":"lt2","a2":"v2"},"uid":"321","mode":"remove"}]}`)
	var r BatchWriteRequest
	err := json.Unmarshal(d, &r)
	require.NoError(t, err)

	v1 := "v1"
	v2 := "v2"
	v3 := "v3"
	assert.Equal(t, BatchWriteRequest{
		EventType:  "batch",
		ExternalID: "1234",
		Dt:         time.Unix(1708091866, 0).UTC(),
		Info:       []byte(`["some","info"]`),
		Events: []EventAttributes{{
			Loc: LocationAttributes{
				Namespace: "ns",
				Type:      "et",
				Attributes: map[string]*string{
					"a1": &v1,
					"a2": &v2,
				},
			},
			Type:   "debit",
			Dt:     time.Unix(1608091866, 0).UTC(),
			Amount: "123.45",
			Info:   []byte(`{"some":"info"}`),
		}},
		States: []StateAttributes{{
			Loc: LocationAttributes{
				Namespace: "ns",
				Type:      "st",
				Attributes: map[string]*string{
					"a1": &v2,
					"a2": &v3,
				},
			},
			State: []byte(`{"some":"json"}`),
		}},
		Locks: []LockAction{
			{
				Loc: LocationAttributes{
					Namespace: "ns",
					Type:      "lt1",
					Attributes: map[string]*string{
						"a1": &v1,
					},
				},
				UID:  "123",
				Mode: "validate",
			},
			{
				Loc: LocationAttributes{
					Namespace: "ns",
					Type:      "lt2",
					Attributes: map[string]*string{
						"a2": &v2,
					},
				},
				UID:  "321",
				Mode: "remove",
			},
		},
	}, r)
}

func TestBatchWriteRequest_UnmarshalJSON_Empty(t *testing.T) {
	d := []byte(`{"event_type":"batch","external_id":"1234","dt":123456}`)
	var r BatchWriteRequest
	err := json.Unmarshal(d, &r)
	require.NoError(t, err)

	assert.Equal(t, BatchWriteRequest{
		EventType:  "batch",
		ExternalID: "1234",
		Dt:         time.Unix(123456, 0).UTC(),
	}, r)
}

func TestBatchWriteRequest_UnmarshalJSON_NoEventType(t *testing.T) {
	d := []byte(`{"event_type":"batch","dt": 666}`)
	var r BatchWriteRequest
	err := json.Unmarshal(d, &r)
	assert.EqualError(t, err, "no required 'external_id' attribute")
}

func TestBatchWriteRequest_UnmarshalJSON_NoExternalID(t *testing.T) {
	d := []byte(`{"external_id":"1234","dt": 666}`)
	var r BatchWriteRequest
	err := json.Unmarshal(d, &r)
	assert.EqualError(t, err, "no required 'event_type' attribute")
}

func TestBatchWriteRequest_UnmarshalJSON_NoDt(t *testing.T) {
	d := []byte(`{"external_id":"1234","event_type":"batch"}`)
	var r BatchWriteRequest
	err := json.Unmarshal(d, &r)
	assert.EqualError(t, err, "no required 'dt' attribute")
}

func TestBatchWriteRequest_UnmarshalJSON_InvalidParts(t *testing.T) {
	d := []byte(`{"event_type":"batch","external_id":"1234","states":[{"state":{"some":"json"}}]}`)
	var r BatchWriteRequest
	err := json.Unmarshal(d, &r)
	assert.EqualError(t, err, "no required 'loc' attribute")
}

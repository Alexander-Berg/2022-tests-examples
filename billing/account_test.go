package entities

import (
	"encoding/json"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestEventDetails_MarshalJSON(t *testing.T) {
	e := EventDetails{
		Type:      "type",
		Dt:        time.Unix(1234567890, 0),
		Amount:    "123",
		EventType: "456",
		EventID:   "789",
		EventInfo: []byte(`{"recipient":"kositsyn-pa"}`),
		Info:      []byte(`[{"some": "thing"}, "is", ["VERY-VERY-WRONG"]]`),
	}
	res, err := json.Marshal(e)
	require.NoError(t, err)

	// nolint: lll
	assert.Equal(t, `{"amount":"123","dt":1234567890,"event_id":"789","event_info":{"recipient":"kositsyn-pa"},"event_type":"456","info":[{"some":"thing"},"is",["VERY-VERY-WRONG"]],"type":"type"}`, string(res))
}

func TestEventDetails_MarshalJSON_EmptyInfo(t *testing.T) {
	e := EventDetails{
		Type:      "kreditorka",
		Dt:        time.Unix(123454321, 0),
		Amount:    "12312",
		EventType: "t",
		EventID:   "d",
		EventInfo: nil,
		Info:      nil,
	}
	res, err := json.Marshal(e)
	require.NoError(t, err)

	// nolint: lll
	assert.Equal(t, `{"amount":"12312","dt":123454321,"event_id":"d","event_info":null,"event_type":"t","info":null,"type":"kreditorka"}`, string(res))
}

func TestEventAttributes_MarshalJSON(t *testing.T) {
	a1 := "v1"
	a2 := "v2"
	e := EventAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		Type:   "debit",
		Dt:     time.Unix(1608091866, 0).UTC(),
		Amount: "123.45",
		Info:   []byte(`{"some":"info"}`),
	}
	res, err := json.Marshal(e)
	require.NoError(t, err)

	// nolint: lll
	assert.Equal(t, `{"amount":"123.45","dt":1608091866,"info":{"some":"info"},"loc":{"a1":"v1","a2":"v2","type":"et","namespace":"ns"},"type":"debit"}`, string(res))
}

func TestEventAttributes_UnmarshalJSON(t *testing.T) {
	// nolint: lll
	d := []byte(`{"loc":{"namespace":"some_namespace","type":"et","a1":"v1","a2":"v2"},"type":"debit","dt":1608091866,"amount":"123.45","info":{"some":"info"}}`)
	var e EventAttributes
	err := json.Unmarshal(d, &e)
	require.NoError(t, err)

	a1 := "v1"
	a2 := "v2"
	assert.Equal(t, EventAttributes{
		Loc: LocationAttributes{
			Namespace:  "some_namespace",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		Type:   "debit",
		Dt:     time.Unix(1608091866, 0).UTC(),
		Amount: "123.45",
		Info:   []byte(`{"some":"info"}`),
	}, e)
}

func TestEventAttributes_UnmarshalJSON_InvalidLoc(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","a1":"v1","a2":"v2"},"type":"debit","dt":1608091866,"amount":"123.45"}`)
	var e EventAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'type' attribute")
}

func TestEventAttributes_UnmarshalJSON_NoLoc(t *testing.T) {
	d := []byte(`{"type":"debit","dt":1608091866,"amount":"123.45"}`)
	var e EventAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'loc' attribute")
}

func TestEventAttributes_UnmarshalJSON_NoType(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"t","a1":"v1","a2":"v2"},"dt":1608091866,"amount":"123.45"}`)
	var e EventAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'type' attribute")
}

func TestEventAttributes_UnmarshalJSON_NoDt(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"t","a1":"v1","a2":"v2"},"type":"debit","amount":"123.45"}`)
	var e EventAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'dt' attribute")
}

func TestEventAttributes_UnmarshalJSON_NoAmount(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"t","a1":"v1","a2":"v2"},"type":"debit","dt":11}`)
	var e EventAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'amount' attribute")
}

func TestDtRequest_MarshalJSON(t *testing.T) {
	a1 := "v1"
	a2 := "v2"
	e := DtRequestAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		Dt: time.Unix(1608091866, 0).UTC(),
	}
	res, err := json.Marshal(e)
	require.NoError(t, err)

	// nolint: lll
	assert.Equal(t, `{"dt":1608091866,"loc":{"a1":"v1","a2":"v2","type":"et","namespace":"ns"}}`, string(res))
}

func TestDtRequest_UnmarshalJSON(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"dt":1608091866}`)
	var e DtRequestAttributes
	err := json.Unmarshal(d, &e)
	require.NoError(t, err)

	a1 := "v1"
	a2 := "v2"
	assert.Equal(t, DtRequestAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		Dt: time.Unix(1608091866, 0).UTC(),
	}, e)
}

func TestDtRequest_UnmarshalJSON_NoLoc(t *testing.T) {
	d := []byte(`{"dt":1608091866}`)
	var e DtRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'loc' attribute")
}

func TestDtRequest_UnmarshalJSON_NoDt(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"}}`)
	var e DtRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'dt' attribute")
}

func TestDtRequest_UnmarshalJSON_InvalidLoc(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","a1":"v1","a2":"v2"},"dt":1608091866}`)
	var e DtRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'type' attribute")
}

func TestPeriodRequest_MarshalJSON(t *testing.T) {
	a1 := "v1"
	a2 := "v2"
	e := PeriodRequestAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		DtFrom: time.Unix(1608091866, 0).UTC(),
		DtTo:   time.Unix(1708091866, 0).UTC(),
	}
	res, err := json.Marshal(e)
	require.NoError(t, err)

	// nolint: lll
	assert.Equal(t, `{"dt_from":1608091866,"dt_to":1708091866,"loc":{"a1":"v1","a2":"v2","type":"et","namespace":"ns"}}`, string(res))
}

func TestPeriodRequest_UnmarshalJSON(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"dt_from":1608091866,"dt_to":1708091866}`)
	var e PeriodRequestAttributes
	err := json.Unmarshal(d, &e)
	require.NoError(t, err)

	a1 := "v1"
	a2 := "v2"
	assert.Equal(t, PeriodRequestAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		DtFrom: time.Unix(1608091866, 0).UTC(),
		DtTo:   time.Unix(1708091866, 0).UTC(),
	}, e)
}

func TestPeriodRequest_UnmarshalJSON_NoLoc(t *testing.T) {
	d := []byte(`{"dt_from":1608091866,"dt_to":1608091866}`)
	var e PeriodRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'loc' attribute")
}

func TestPeriodRequest_UnmarshalJSON_NoDtFrom(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"dt_to":1608091866}`)
	var e PeriodRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'dt_from' attribute")
}

func TestPeriodRequest_UnmarshalJSON_NoDtTo(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"dt_from":1608091866}`)
	var e PeriodRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'dt_to' attribute")
}

func TestPeriodRequest_UnmarshalJSON_InvalidLoc(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","a1":"v1","a2":"v2"},"dt_from":1608091866,"dt_to":1708091866}`)
	var e PeriodRequestAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'type' attribute")
}

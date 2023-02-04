package entities

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestStateAttributes_UnmarshalJSON(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"state":{"some":["weird",666]}}`)
	var e StateAttributes
	err := json.Unmarshal(d, &e)
	if err != nil {
		t.Fatal(err)
	}

	a1 := "v1"
	a2 := "v2"
	assert.Equal(t, StateAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		State: []byte(`{"some":["weird",666]}`),
	}, e)
}

func TestStateAttributes_UnmarshalJSON_NullState(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"t","a1":"v1"},"state":null}`)
	var e StateAttributes
	err := json.Unmarshal(d, &e)
	if err != nil {
		t.Fatal(err)
	}

	v := "v1"
	assert.Equal(t, StateAttributes{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "t",
			Attributes: map[string]*string{"a1": &v},
		},
		State: []byte("null"),
	}, e)
}

func TestStateAttributes_UnmarshalJSON_InvalidLoc(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","a1":"v1","a2":"v2"},"state":"aaa"}`)
	var e StateAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'type' attribute")
}

func TestStateAttributes_UnmarshalJSON_NoLoc(t *testing.T) {
	d := []byte(`{"state":"aaa"}`)
	var e StateAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'loc' attribute")
}

func TestStateAttributes_UnmarshalJSON_NoState(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"t","a1":"v1"}}`)
	var e StateAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'state' attribute")
}

func TestStateAttributes_UnmarshalJSON_NoNamespace(t *testing.T) {
	d := []byte(`{"loc":{"type":"t","a1":"v1"},"state":"aaa"}`)
	var e StateAttributes
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'namespace' attribute")
}

func TestStateAttributes_MarshalJSON(t *testing.T) {
	val := "value"
	a := StateAttributes{
		Loc: LocationAttributes{
			Namespace: "ns",
			Type:      "stype",
			Attributes: map[string]*string{
				"val": &val,
			},
		},
		State: []byte(`{"some": ["statez"]}`),
	}

	res, err := json.Marshal(a)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, string(res), `{"loc":{"val":"value","type":"stype","namespace":"ns"},"state":{"some":["statez"]}}`)
}

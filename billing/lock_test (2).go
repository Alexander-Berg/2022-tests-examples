package entities

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestLockAction_UnmarshalJSON(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"uid":"11-11-11","mode":"remove"}`)
	var e LockAction
	err := json.Unmarshal(d, &e)
	if err != nil {
		t.Fatal(err)
	}

	a1 := "v1"
	a2 := "v2"
	assert.Equal(t, LockAction{
		Loc: LocationAttributes{
			Namespace:  "ns",
			Type:       "et",
			Attributes: map[string]*string{"a1": &a1, "a2": &a2},
		},
		UID:  "11-11-11",
		Mode: "remove",
	}, e)
}

func TestLockAction_UnmarshalJSON_InvalidLoc(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","a1":"v1","a2":"v2"},"uid":"11-11-11","mode":"remove"}`)
	var e LockAction
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'type' attribute")
}

func TestLockAction_UnmarshalJSON_NoLoc(t *testing.T) {
	d := []byte(`{"uid":"11-11-11","mode":"remove"}`)
	var e LockAction
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'loc' attribute")
}

func TestLockAction_UnmarshalJSON_NoUID(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"mode":"remove"}`)
	var e LockAction
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'uid' attribute")
}

func TestLockAction_UnmarshalJSON_NoMode(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"uid":"11-11-11"}`)
	var e LockAction
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "no required 'mode' attribute")
}

func TestLockAction_UnmarshalJSON_InvalidMode(t *testing.T) {
	d := []byte(`{"loc":{"namespace":"ns","type":"et","a1":"v1","a2":"v2"},"uid":"11-11-11","mode":"take"}`)
	var e LockAction
	err := json.Unmarshal(d, &e)
	assert.EqualError(t, err, "invalid lock mode")
}

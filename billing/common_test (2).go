package entities

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestLocationAttributes_UnmarshalJSON(t *testing.T) {
	str := `{"namespace": "namespace", "type": "abc", "attr1": "val1", "attr2": "val2"}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if err != nil {
		t.Fatal(err)
	}

	a1 := "val1"
	a2 := "val2"
	assert.Equal(t, loc, LocationAttributes{
		Namespace:  "namespace",
		Type:       "abc",
		Attributes: map[string]*string{"attr1": &a1, "attr2": &a2},
	})
}

func TestLocationAttributes_UnmarshalJSON_EmptyNamespace_String(t *testing.T) {
	str := `{"namespace": "", "type": "abc", "attr": "val"}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if err != nil {
		t.Fatal(err)
	}

	a := "val"
	assert.Equal(t, loc, LocationAttributes{
		Namespace:  "",
		Type:       "abc",
		Attributes: map[string]*string{"attr": &a},
	})
}

func TestLocationAttributes_UnmarshalJSON_NoNamespace(t *testing.T) {
	str := `{"type": "abc", "attr1": "val1"}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), "no required 'namespace' attribute")
	}
}

func TestLocationAttributes_UnmarshalJSON_NullNamespace(t *testing.T) {
	str := `{"namespace": null, "type": "abc", "attr": "val"}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), "no required 'namespace' attribute")
	}
}

func TestLocationAttributes_UnmarshalJSON_NullEmptyAttr(t *testing.T) {
	str := `{"namespace": "ns","type": "abc", "attr1": null, "attr2": "val2", "attr3": ""}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, loc.Type, "abc")
	assert.Len(t, loc.Attributes, 3)
	assert.Nil(t, loc.Attributes["attr1"])
	assert.Equal(t, *loc.Attributes["attr2"], "val2")
	assert.Equal(t, *loc.Attributes["attr3"], "")
}

func TestLocationAttributes_UnmarshalJSON_NoType(t *testing.T) {
	str := `{"namespace": "ns","not_type": "abc", "attr1": "val1"}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), "no required 'type' attribute")
	}
}

func TestLocationAttributes_UnmarshalJSON_NullType(t *testing.T) {
	str := `{"namespace": "ns","type": null, "attr1": "val1"}`
	var loc LocationAttributes
	err := json.Unmarshal([]byte(str), &loc)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), "no required 'type' attribute")
	}
}

func TestLocationAttributes_MarshalJSON(t *testing.T) {
	a1 := "a"
	loc := LocationAttributes{
		Namespace: "ns",
		Type:      "t",
		Attributes: map[string]*string{
			"a1": &a1,
			"a2": nil,
		},
	}

	res, err := json.Marshal(&loc)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, string(res), `{"a1":"a","a2":null,"type":"t","namespace":"ns"}`)
}

package impl

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
)

const stateCfg2 = `
manifests:
- namespace: bla-bla
  states:
    key:
      attributes:
        - attr1
        - attr2
      shard:
        prefix: pref
        attributes:
          - attr1
`

const stateCfg3 = `
manifests:
- namespace: bla-bla
  states:
    key:
      attributes:
        - attr1
        - attr2
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
`

func getState(cfg string) entitysettings.StateSettings {
	s := settingsFromTmp(cfg)
	return s.State()
}

func TestStateValidateOk(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	err := getState(stateCfg3).Validate(&attrs)
	if err != nil {
		t.Fatal(err)
	}
}

func TestStateValidateUnknownType(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "not_key", map[string]string{"attr1": "val1", "attr2": "val2"})
	err := getState(stateCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "unknown state type '[bla-bla not_key]'")
}

func TestStateValidateUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes("not-bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	err := getState(stateCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "unknown state type '[not-bla-bla key]'")
}

func TestStateValidateNoAttr(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	err := getState(stateCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "no attribute attr3")
}

func TestStateValidateExtraAttr(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	err := getState(stateCfg2).Validate(&attrs)
	assertInvalidSettings(t, err, "extra attributes in data")
}

func TestStateShardKeyUnknownType(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "not_key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getState(stateCfg3).ShardKey(&attrs)
	assertInvalidSettings(t, err, "unknown state type '[bla-bla not_key]'")
}

func TestStateShardKeyUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes("not-bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getState(stateCfg3).ShardKey(&attrs)
	assertInvalidSettings(t, err, "unknown state type '[not-bla-bla key]'")
}

func TestStateShardKey(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  states:
    key:
      attributes:
        - attr1
        - attr2
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
          - attr3
`
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	key, err := getState(cfg).ShardKey(&attrs)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, key, HashShardKey("pref%val1%val3"))
}

func TestStateShardKeyFail(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  states:
    key:
      attributes:
        - attr1
      shard:
        prefix: pref
        attributes:
          - attr1
`
	attrs := entities.LocationAttributes{
		Namespace: "bla-bla",
		Type:      "key",
		Attributes: map[string]*string{
			"attr1": nil,
		},
	}
	_, err := getState(cfg).ShardKey(&attrs)
	assertInvalidSettings(t, err, "empty shard attribute attr1")
}

func TestStateLocationUnknownType(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "not_key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getState(stateCfg3).Location(&attrs)
	assertInvalidSettings(t, err, "unknown state type '[bla-bla not_key]'")
}

func TestStateLocationUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes("not-bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getState(stateCfg3).Location(&attrs)
	assertInvalidSettings(t, err, "unknown state type '[not-bla-bla key]'")
}

func TestStateLocation(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	res, err := getState(stateCfg2).Location(&attrs)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, res, entities.Location{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2"},
		ShardKey:   "8c61b0aa3ef948da98a9c01774532d53cded20df",
	})
}

func TestStateLocationFail(t *testing.T) {
	attr := "val1"
	attrs := entities.LocationAttributes{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: map[string]*string{"attr1": &attr, "attr2": nil},
	}
	_, err := getState(stateCfg2).Location(&attrs)
	assertInvalidSettings(t, err, "couldn't get attributes for state: attribute attr2 is null")
}

func TestStateAttrsFromLocation(t *testing.T) {
	loc := entities.Location{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2"},
	}
	res, err := getState(stateCfg2).AttributesFromLocation(&loc)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, res, entities.LocationAttributes{
		Namespace: "bla-bla",
		Type:      "key",
		Attributes: map[string]*string{
			"attr1": &loc.Attributes[0],
			"attr2": &loc.Attributes[1],
		},
	})
}

func TestStateAttrsFromLocationUnknownType(t *testing.T) {
	_, err := getState(stateCfg2).AttributesFromLocation(&entities.Location{
		Namespace:  "bla-bla",
		Type:       "not_key",
		Attributes: [5]string{"val1", "val2"},
	})
	assertInvalidSettings(t, err, "unknown state type '[bla-bla not_key]'")
}

func TestStateAttrsFromLocationUnknownNamespace(t *testing.T) {
	_, err := getState(stateCfg2).AttributesFromLocation(&entities.Location{
		Namespace:  "not-bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2"},
	})
	assertInvalidSettings(t, err, "unknown state type '[not-bla-bla key]'")
}

func TestStateAttrsFromLocationExtra(t *testing.T) {
	_, err := getState(stateCfg2).AttributesFromLocation(&entities.Location{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2", "val3"},
	})
	assertInvalidSettings(t, err, "couldn't get attributes for state: unknown attribute at position 2")
}

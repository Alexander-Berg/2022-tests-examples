package impl

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
)

const lockCfg2 = `
manifests:
- namespace: bla-bla
  locks:
    key:
      attributes:
        - attr1
        - attr2
      shard:
        prefix: pref
        attributes:
          - attr1
`

const lockCfg3 = `
manifests:
- namespace: bla-bla
  locks:
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

func getLock(cfg string) entitysettings.LockSettings {
	s := settingsFromTmp(cfg)
	return s.Lock()
}

func TestLockValidateOk(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	err := getLock(lockCfg3).Validate(&attrs)
	if err != nil {
		t.Fatal(err)
	}
}

func TestLockValidateUnknownType(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "not_key", map[string]string{"attr1": "val1", "attr2": "val2"})
	err := getLock(lockCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "unknown lock type '[bla-bla not_key]'")
}

func TestLockValidateUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes("not-bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	err := getLock(lockCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "unknown lock type '[not-bla-bla key]'")
}

func TestLockValidateNoAttr(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	err := getLock(lockCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "no attribute attr3")
}

func TestLockValidateExtraAttr(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	err := getLock(lockCfg2).Validate(&attrs)
	assertInvalidSettings(t, err, "extra attributes in data")
}

func TestLockShardKeyUnknownType(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "not_key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getLock(lockCfg3).ShardKey(&attrs)
	assertInvalidSettings(t, err, "unknown lock type '[bla-bla not_key]'")
}

func TestLockShardKeyUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes("not-bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getLock(lockCfg3).ShardKey(&attrs)
	assertInvalidSettings(t, err, "unknown lock type '[not-bla-bla key]'")
}

func TestLockShardKey(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  locks:
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
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	key, err := getLock(cfg).ShardKey(&attrs)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, key, HashShardKey("pref%val1%val3"))
}

func TestLockShardKeyFail(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  locks:
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
	_, err := getLock(cfg).ShardKey(&attrs)
	assertInvalidSettings(t, err, "empty shard attribute attr1")
}

func TestLockLocationUnknownType(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "not_key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getLock(lockCfg3).Location(&attrs)
	assertInvalidSettings(t, err, "unknown lock type '[bla-bla not_key]'")
}

func TestLockLocationUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes("not-bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	_, err := getLock(lockCfg3).Location(&attrs)
	assertInvalidSettings(t, err, "unknown lock type '[not-bla-bla key]'")
}

func TestLockLocation(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1", "attr2": "val2"})
	res, err := getLock(lockCfg2).Location(&attrs)
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

func TestLockLocationFail(t *testing.T) {
	attr := "val1"
	attrs := entities.LocationAttributes{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: map[string]*string{"attr1": &attr, "attr2": nil},
	}
	_, err := getLock(lockCfg2).Location(&attrs)
	assertInvalidSettings(t, err, "couldn't get attributes for lock: attribute attr2 is null")
}

func TestLockAttrsFromLocation(t *testing.T) {
	loc := entities.Location{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2"},
	}
	res, err := getLock(lockCfg2).AttributesFromLocation(&loc)
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

func TestLockAttrsFromLocationUnknownType(t *testing.T) {
	_, err := getLock(lockCfg2).AttributesFromLocation(&entities.Location{
		Namespace:  "bla-bla",
		Type:       "not_key",
		Attributes: [5]string{"val1", "val2"},
	})
	assertInvalidSettings(t, err, "unknown lock type '[bla-bla not_key]'")
}

func TestLockAttrsFromLocationUnknownNamespace(t *testing.T) {
	_, err := getLock(lockCfg2).AttributesFromLocation(&entities.Location{
		Namespace:  "not-bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2"},
	})
	assertInvalidSettings(t, err, "unknown lock type '[not-bla-bla key]'")
}

func TestLockAttrsFromLocationExtra(t *testing.T) {
	_, err := getLock(lockCfg2).AttributesFromLocation(&entities.Location{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: [5]string{"val1", "val2", "val3"},
	})
	assertInvalidSettings(t, err, "couldn't get attributes for lock: unknown attribute at position 2")
}

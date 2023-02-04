package impl

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
)

const accountCfg21 = `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
      - attr1
      - attr2
      add_attributes:
      - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`

const accountCfg1 = `
manifests:
- namespace: bla-bla
  accounts:
   key:
     attributes:
       - attr1
     shard:
        prefix: pref
        attributes:
          - attr1
     rollup_period:
       - "0 * * *"
`

const accountCfg3 = `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
        - attr2
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`

var vals = []string{"val1", "val2", "val3"}

func getAccount(cfg string) entitysettings.AccountSettings {
	s := settingsFromTmp(cfg)
	return s.Account()
}

func TestAccountValidateOk(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	err := getAccount(accountCfg3).Validate(&attrs)
	if err != nil {
		t.Fatal(err)
	}
}

func TestAccountValidateUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	err := getAccount(accountCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountValidateUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	err := getAccount(accountCfg3).Validate(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountValidateOkNoAdd(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	err := getAccount(accountCfg3).Validate(&attrs)
	if err != nil {
		t.Fatal(err)
	}
}

func TestAccountValidateOkAdd(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	err := getAccount(accountCfg21).Validate(&attrs)
	if err != nil {
		t.Fatalf("Error getting settings %s", err)
	}
}

func TestAccountValidateNoAttrNoAdd(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "v1", "attr2": "v2"})
	err := getAccount(accountCfg3).Validate(&attrs)

	assertInvalidSettings(t, err, "no attribute attr3")
}

func TestAccountValidateNoAttrAdd(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "v1", "attr2": "v2"})
	err := getAccount(accountCfg21).Validate(&attrs)

	assertInvalidSettings(t, err, "no additional attribute attr3")
}

func TestAccountValidateNoAttrAddMainMissing(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "v1", "attr3": "v2"})
	err := getAccount(accountCfg21).Validate(&attrs)

	assertInvalidSettings(t, err, "no attribute attr2")
}

func TestAccountValidateExtraAttrNoAdd(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
        - attr2
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	err := getAccount(cfg).Validate(&attrs)

	assertInvalidSettings(t, err, "extra 1 attributes in data: 'attr3'")
}

func TestAccountValidateExtraAttrAdd(t *testing.T) {
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{
		"attr1": "val1",
		"attr2": "val2",
		"attr3": "val3",
		"attr4": "val4",
		"attr5": "val5",
	})
	err := getAccount(accountCfg21).Validate(&attrs)

	assertInvalidSettings(t, err, "extra 2 attributes in data: 'attr4', 'attr5'")
}

func TestAccountShardKeyUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).ShardKey(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountShardKeyUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).ShardKey(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountShardKey(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
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
      rollup_period:
        - "0 * * *"
`
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	key, err := getAccount(cfg).ShardKey(&attrs)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, key, HashShardKey("pref%val1%val3"))
}

func TestAccountShardKeyNilKey(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
        - attr2
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
          - attr2
      rollup_period:
        - "0 * * *"
`
	attrsValues := []string{"val1", "val2"}
	attrs := entities.LocationAttributes{
		Namespace: "bla-bla",
		Type:      "key",
		Attributes: map[string]*string{
			"attr1": &attrsValues[0],
			"attr2": nil,
			"attr3": &attrsValues[1],
		},
	}

	_, err := getAccount(cfg).ShardKey(&attrs)
	assertInvalidSettings(t, err, "empty shard attribute attr2")
}

func TestAccountShardKeyAdd(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
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
      add_attributes:
        - attr4
      rollup_period:
        - "0 * * *"
`
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{
		"attr1": "val1",
		"attr2": "val2",
		"attr3": "val3",
		"attr4": "val4",
	})
	key, err := getAccount(cfg).ShardKey(&attrs)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, key, HashShardKey("pref%val1%val3"))
}

func TestAccountLocationUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).Location(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountLocationUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).Location(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountLocation(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	res, err := getAccount(accountCfg21).Location(&attrs)
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

func TestAccountLocationFail(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
        - attr2
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`
	attrs := entities.LocationAttributes{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: map[string]*string{"attr1": &vals[0], "attr2": nil},
	}
	_, err := getAccount(cfg).Location(&attrs)
	assertInvalidSettings(t, err, "couldn't get location attributes for account: attribute attr2 is null")
}

func TestAccountAccLocationUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).AccountLocation(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountAccLocationUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).AccountLocation(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountAccLocation(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	loc, err := getAccount(accountCfg21).AccountLocation(&attrs)
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, loc,
		entities.AccountLocation{
			Loc: entities.Location{
				Namespace:  "bla-bla",
				Type:       "key",
				Attributes: [5]string{"val1", "val2"},
				ShardKey:   "8c61b0aa3ef948da98a9c01774532d53cded20df",
			},
			AddAttributes: [5]string{"val3"},
		})
}

func TestAccountAccLocationFail(t *testing.T) {
	attrVals := []string{"val1", "val3"}
	attrs := entities.LocationAttributes{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: map[string]*string{"attr1": &attrVals[0], "attr2": nil, "attr3": &attrVals[1]},
	}
	_, err := getAccount(accountCfg21).AccountLocation(&attrs)
	assertInvalidSettings(t, err, "couldn't get location attributes for account: attribute attr2 is null")
}

func TestAccountAccLocationFailAdd(t *testing.T) {
	attrVals := []string{"val1", "val2"}
	attrs := entities.LocationAttributes{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: map[string]*string{"attr1": &attrVals[0], "attr2": &attrVals[1], "attr3": nil},
	}
	_, err := getAccount(accountCfg21).AccountLocation(&attrs)
	assertInvalidSettings(t, err, "couldn't get add location attributes for account: attribute attr3 is null")
}

func TestAccountAccLocationMaskUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).AccountLocationMask(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountAccLocationMaskUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).AccountLocationMask(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountAccLocationMask(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
        - attr2
      add_attributes:
        - attr3
        - attr4
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`

	attrVals := []string{"val1", "val4"}
	attrs := entities.LocationAttributes{
		Namespace:  "bla-bla",
		Type:       "key",
		Attributes: map[string]*string{"attr1": &attrVals[0], "attr2": nil, "attr3": nil, "attr4": &attrVals[1]},
	}
	loc, err := getAccount(cfg).AccountLocationMask(&attrs)
	if err != nil {
		t.Fatal(err)
	}

	locAttrValues := make([]string, 0)
	for _, v := range loc.Attributes {
		if v != nil {
			locAttrValues = append(locAttrValues, *v)
		}
	}
	locAddAttrValues := make([]string, 0)
	for _, v := range loc.AddAttributes {
		if v != nil {
			locAddAttrValues = append(locAddAttrValues, *v)
		}
	}

	assert.Equal(t, loc.Type, "key")
	assert.Nil(t, loc.Attributes[1])
	assert.Nil(t, loc.AddAttributes[0])
	assert.Equal(t, locAttrValues, []string{"val1", "", "", ""})
	assert.Equal(t, locAddAttrValues, []string{"val4", "", "", ""})
}

func TestAccountRollupPeriodUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).RollupPeriod(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountRollupPeriodUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).RollupPeriod(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountRollupPeriod(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
        - "*/15 */5 * *"
`
	attrs := getLocationAttributes("bla-bla", "key", map[string]string{"attr1": "val1"})
	period, err := getAccount(cfg).RollupPeriod(&attrs)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, period, []string{
		"0 * * *",
		"*/15 */5 * *",
	})
}

func TestAccountSubaccountMasksUnknownType(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "not_key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).SubaccountMasks(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountSubaccountMasksUnknownNamespace(t *testing.T) {
	attrs := getLocationAttributes(
		"not-bla-bla", "key",
		map[string]string{"attr1": "v1", "attr2": "v2", "attr3": "v3"})
	_, err := getAccount(accountCfg3).SubaccountMasks(&attrs)
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountSubaccountMasks(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
   key:
     attributes:
       - attr1
     add_attributes:
       - attr2
       - attr3
     sub_accounts:
       - []
       - - attr2
       - - attr3
       - - attr2
         - attr3
     shard:
       prefix: pref
       attributes:
         - attr1
     rollup_period:
       - "0 * * *"
`
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	masks, err := getAccount(cfg).SubaccountMasks(&attrs)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, masks, entities.SubaccountMasks{
		{},
		{true},
		{false, true},
		{true, true},
	})
}

func TestAccountSubaccountMasksNoConfig(t *testing.T) {
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	masks, err := getAccount(accountCfg21).SubaccountMasks(&attrs)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, masks, entities.SubaccountMasks{{}})
}

func TestAccountSubaccountMasksNoEmptySubaccount(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
   key:
     attributes:
       - attr1
     add_attributes:
       - attr2
       - attr3
     sub_accounts:
       - - attr2
         - attr3
     shard:
       prefix: pref
       attributes:
         - attr1
     rollup_period:
       - "0 * * *"
`
	attrs := getLocationAttributes(
		"bla-bla", "key",
		map[string]string{"attr1": "val1", "attr2": "val2", "attr3": "val3"})
	masks, err := getAccount(cfg).SubaccountMasks(&attrs)
	if err != nil {
		t.Fatal(err)
	}

	assert.Equal(t, masks, entities.SubaccountMasks{{true, true}})
}

func TestAccountAttrsFromLocation(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
      add_attributes:
        - attr2
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`
	attrVals := []string{"val1", "val2", "val3"}
	attrs := entities.LocationAttributes{
		Namespace: "bla-bla",
		Type:      "key",
		Attributes: map[string]*string{
			"attr1": &attrVals[0],
			"attr2": &attrVals[1],
			"attr3": &attrVals[2],
		},
	}
	resAttrs, err := getAccount(cfg).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "bla-bla",
			Type:       "key",
			Attributes: [5]string{"val1"},
		},
		AddAttributes: [5]string{"val2", "val3"},
	})
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, resAttrs, attrs)
}

func TestAccountAttrsFromLocationAddMask(t *testing.T) {
	cfg := `
manifests:
- namespace: bla-bla
  accounts:
    key:
      attributes:
        - attr1
      add_attributes:
        - attr2
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`
	attrVals := []string{"val1", "val2", "val3"}
	resAttrs, err := getAccount(cfg).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "bla-bla",
			Type:       "key",
			Attributes: [5]string{"val1"},
		},
		AddAttributes: [5]string{"val2", "val3"},
	})
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, resAttrs, entities.LocationAttributes{
		Namespace: "bla-bla",
		Type:      "key",
		Attributes: map[string]*string{
			"attr1": &attrVals[0],
			"attr2": &attrVals[1],
			"attr3": &attrVals[2],
		},
	})
}

func TestAccountAttrsFromLocationMainMask(t *testing.T) {
	attrVals := []string{"val1", "val2", "val3"}
	resAttrs, err := getAccount(accountCfg21).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "bla-bla",
			Type:       "key",
			Attributes: [5]string{"val1", "val2"},
		},
		AddAttributes: [5]string{"val3"},
	})
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, resAttrs, entities.LocationAttributes{
		Namespace: "bla-bla",
		Type:      "key",
		Attributes: map[string]*string{
			"attr1": &attrVals[0],
			"attr2": &attrVals[1],
			"attr3": &attrVals[2],
		},
	})
}

func TestAccountAttrsFromLocationUnknownType(t *testing.T) {
	_, err := getAccount(accountCfg1).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "bla-bla",
			Type:       "not_key",
			Attributes: [5]string{"val1"},
		},
	})
	assertInvalidSettings(t, err, "unknown account type '[bla-bla not_key]'")
}

func TestAccountAttrsFromLocationUnknownNamespace(t *testing.T) {
	_, err := getAccount(accountCfg1).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "not-bla-bla",
			Type:       "key",
			Attributes: [5]string{"val1"},
		},
	})
	assertInvalidSettings(t, err, "unknown account type '[not-bla-bla key]'")
}

func TestAccountAttrsFromLocationExtraMain(t *testing.T) {
	_, err := getAccount(accountCfg1).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "bla-bla",
			Type:       "key",
			Attributes: [5]string{"val1", "val2"},
		},
	})

	assertInvalidSettings(t, err, "failed to get attributes: unknown attribute at position 1")
}

func TestAccountAttrsFromLocationExtraAdd(t *testing.T) {
	_, err := getAccount(accountCfg1).AttributesFromLocation(&entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "bla-bla",
			Type:       "key",
			Attributes: [5]string{"val1"},
		},
		AddAttributes: [5]string{"val2"},
	})

	assertInvalidSettings(t, err, "failed to get add attributes: unknown attribute at position 0")
}

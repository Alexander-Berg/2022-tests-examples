package impl

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestDuplicateKey(t *testing.T) {
	data := `
manifests:
- namespace: test
  %s:
    key:
      attributes:
        - attr1
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
  %s:
    key:
      attributes:
        - attr4
      shard:
        prefix: pref
        attributes:
          - attr4
      rollup_period:
        - "0 * * *"
`
	sets := [][]string{
		{"accounts", "states"},
		{"accounts", "locks"},
		{"states", "locks"},
	}

	checkCase := func(firstType, secondType string) {
		settingsPath, err := mkTmpFile(fmt.Sprintf(data, firstType, secondType))
		defer os.Remove(settingsPath)
		if err != nil {
			t.Fatal(err)
		}
		_, err = NewSettings(context.Background(), settingsPath)
		if assert.Errorf(t, err, "%s-%s", firstType, secondType) {
			assert.Equalf(t, err.Error(), "duplicated key [test key]", "%s-%s", firstType, secondType)
		}
	}

	for _, set := range sets {
		checkCase(set[0], set[1])
	}
}

func TestDuplicateAttributeAndAddAttribute(t *testing.T) {
	cfg := `
manifests:
- namespace: test
  accounts:
    key:
      attributes:
        - attr1
      add_attributes:
        - attr1
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
`

	settingsPath, err := mkTmpFile(cfg)
	defer os.Remove(settingsPath)
	require.NoError(t, err)

	_, err = NewSettings(context.Background(), settingsPath)
	require.Error(t, err)
	assert.Equal(t, err.Error(), "attribute 'attr1' specified in attributes and add attributes")
}

func TestNoShard(t *testing.T) {
	cfg := `
manifests:
- namespace: test
  %s:
    key:
      attributes:
        - attr1
`

	checkType := func(cfgType string) {
		settingsPath, err := mkTmpFile(fmt.Sprintf(cfg, cfgType))
		defer os.Remove(settingsPath)
		if err != nil {
			t.Fatal(err)
		}

		_, err = NewSettings(context.Background(), settingsPath)
		if assert.Errorf(t, err, cfgType) {
			assert.Equalf(t, err.Error(), "empty shard attributes list", cfgType)
		}
	}

	for _, cfgType := range []string{"accounts", "states", "locks"} {
		checkType(cfgType)
	}
}

func TestInvalidShard(t *testing.T) {
	cfg := `
manifests:
- namespace: test
  %s:
    key:
      attributes:
        - attr1
      shard:
        prefix: pref
        attributes:
          - attr2
`

	checkType := func(cfgType string) {
		settingsPath, err := mkTmpFile(fmt.Sprintf(cfg, cfgType))
		defer os.Remove(settingsPath)
		if err != nil {
			t.Fatal(err)
		}

		_, err = NewSettings(context.Background(), settingsPath)
		if assert.Errorf(t, err, cfgType) {
			assert.Equalf(t, err.Error(), "no attribute 'attr2' for shard", cfgType)
		}
	}

	for _, cfgType := range []string{"accounts", "states", "locks"} {
		checkType(cfgType)
	}
}

func TestInvalidSubaccount(t *testing.T) {
	cfg := `
manifests:
- namespace: test
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
      sub_accounts:
        - - attr2
        - - attr3
        - - attr2
          - attr4
      rollup_period:
        - "0 * * *"
`

	settingsPath, err := mkTmpFile(cfg)
	defer os.Remove(settingsPath)
	if err != nil {
		t.Fatal(err)
	}

	_, err = NewSettings(context.Background(), settingsPath)
	if assert.Error(t, err) {
		assert.Equal(t, err.Error(), "no attribute 'attr4' for subaccount")
	}
}

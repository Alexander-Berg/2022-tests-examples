package impl

import (
	"context"
	"io/ioutil"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entitysettings"
	coreerrors "a.yandex-team.ru/billing/hot/accounts/pkg/core/errors"
	"a.yandex-team.ru/library/go/core/xerrors"
)

const rootCfg = `
manifests:
- namespace: ns1
  accounts:
    account_1:
      attributes:
        - attr1
      shard:
        prefix: pref
        attributes:
          - attr1
      rollup_period:
        - "0 * * *"
    account_2:
      attributes:
        - attr2
      shard:
        prefix: pref
        attributes:
          - attr2
      rollup_period:
        - "0 * * *"
  states:
    state_1:
      attributes:
        - attr3
      shard:
        prefix: pref
        attributes:
          - attr3
      rollup_period:
        - "0 * * *"
    state_2:
      attributes:
        - attr4
      shard:
        prefix: pref
        attributes:
          - attr4
      rollup_period:
        - "0 * * *"
  locks:
    lock_1:
      attributes:
        - attr5
      shard:
        prefix: pref
        attributes:
          - attr5
      rollup_period:
        - "0 * * *"
    lock_2:
      attributes:
        - attr6
      shard:
        prefix: pref
        attributes:
          - attr6
      rollup_period:
        - "0 * * *"
- namespace: ns2
  locks:
    lock_3:
      attributes:
        - attr7
      shard:
        prefix: pref
        attributes:
          - attr7
      rollup_period:
        - "0 * * *"
`

func getLocationAttributes(namespace, entityType string, attrs map[string]string) entities.LocationAttributes {
	res := entities.LocationAttributes{
		Namespace:  namespace,
		Type:       entityType,
		Attributes: make(map[string]*string),
	}
	for k, v := range attrs {
		val := v
		res.Attributes[k] = &val
	}
	return res
}

func assertInvalidSettings(t *testing.T, err error, msg string) {
	var codedErr coreerrors.CodedError
	assert.EqualError(t, err, msg)
	assert.True(t, xerrors.As(err, &codedErr))
	assert.Equal(t, codedErr.HTTPCode(), 400)
	assert.Equal(t, codedErr.CharCode(), "INVALID_SETTINGS")
}

func mkTmpFile(data string) (string, error) {
	tmpFile, err := ioutil.TempFile("", "settings*.yaml")
	if err != nil {
		return "", err
	}

	if _, err := tmpFile.WriteString(data); err != nil {
		return "", err
	}

	if err = tmpFile.Close(); err != nil {
		return "", err
	}
	return tmpFile.Name(), nil
}

func settingsFromTmp(rootCfg string) entitysettings.Settings {
	settingsPath, err := mkTmpFile(rootCfg)
	defer os.Remove(settingsPath)

	if err != nil {
		panic(err)
	}
	res, err := NewSettings(context.Background(), settingsPath)
	if err != nil {
		panic(err)
	}
	return res
}

func TestNewSettings(t *testing.T) {
	is := settingsFromTmp(rootCfg)

	s := is.(*settings)
	for _, k := range []settingKey{{"ns1", "account_1"}, {"ns1", "account_2"}} {
		if _, ok := s.config.Accounts[k]; !ok {
			t.Errorf("No account %s", k)
		}
	}
	for _, k := range []settingKey{{"ns1", "state_1"}, {"ns1", "state_2"}} {
		if _, ok := s.config.States[k]; !ok {
			t.Errorf("No state %s", k)
		}
	}
	for _, k := range []settingKey{{"ns1", "lock_1"}, {"ns1", "lock_2"}, {"ns2", "lock_3"}} {
		if _, ok := s.config.Locks[k]; !ok {
			t.Errorf("No lock %s", k)
		}
	}
}

func TestNewSettingsNoPath(t *testing.T) {
	is, err := NewSettings(context.Background(), "")
	s := is.(*settings)
	if err != nil {
		t.Fatal("Error making settings")
	}
	if len(s.config.Accounts) > 0 {
		t.Errorf("Has some accounts")
	}
	if len(s.config.States) > 0 {
		t.Errorf("Has some state")
	}
	if len(s.config.Locks) > 0 {
		t.Errorf("Has some locks")
	}
}

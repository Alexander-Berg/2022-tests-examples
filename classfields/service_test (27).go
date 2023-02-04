package setting

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
)

func TestSettings(t *testing.T) {

	var err error
	test.RunUp(t)
	defer test.Down(t)

	s := NewService(test_db.NewDb(t), test.NewLogger(t))
	setting1 := &Setting{
		Address:      "address1",
		Branch:       false,
		ServiceNames: []string{"service1, service2, service3"},
		States:       []state.DeploymentState{state.DeploymentState_SUCCESS, state.DeploymentState_REVERTED},
		DTypes:       []dtype.DeploymentType{dtype.DeploymentType_RUN, dtype.DeploymentType_REVERT},
	}
	setting1, err = s.NewSetting(setting1)
	test.Check(t, err)

	setting2 := &Setting{
		Address: "address2",
		Branch:  true,
	}
	setting2, err = s.NewSetting(setting2)
	test.Check(t, err)

	setting3 := &Setting{
		Address:      "address3",
		Branch:       true,
		ServiceNames: []string{"service4"},
		States:       []state.DeploymentState{state.DeploymentState_SUCCESS},
		DTypes:       []dtype.DeploymentType{dtype.DeploymentType_RUN},
	}
	setting3, err = s.NewSetting(setting3)
	test.Check(t, err)

	settings, err := s.Settings()
	test.Check(t, err)

	result1 := find(t, settings, setting1.ID())
	assert.Equal(t, 2, len(result1.DTypes))
	assert.Equal(t, 2, len(result1.States))
	assert.Equal(t, 3, len(result1.ServiceNames))
	assert.Equal(t, setting1.Address, result1.Address)
	assert.Equal(t, setting1.Branch, result1.Branch)

	result2 := find(t, settings, setting2.ID())
	assert.Equal(t, 0, len(result2.DTypes))
	assert.Equal(t, 0, len(result2.States))
	assert.Equal(t, 0, len(result2.ServiceNames))
	assert.Equal(t, setting2.Address, result2.Address)
	assert.Equal(t, setting2.Branch, result2.Branch)

	result3 := find(t, settings, setting3.ID())
	assert.Equal(t, 1, len(result3.DTypes))
	assert.Equal(t, 1, len(result3.States))
	assert.Equal(t, 1, len(result3.ServiceNames))
	assert.Equal(t, setting3.Address, result3.Address)
	assert.Equal(t, setting3.Branch, result3.Branch)
}

func find(t *testing.T, settings []*Setting, id int64) *Setting {

	for _, setting := range settings {
		if setting.ID() == id {
			return setting
		}
	}
	t.FailNow()
	return nil
}

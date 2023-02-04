package model

import (
	"testing"

	pbState "github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/stretchr/testify/assert"
)

func TestToProto(t *testing.T) {
	for _, st := range _StateValues {
		d := Deployment{State: st}
		result := d.GetStateProto()
		switch {
		case st == Undefined:
			assert.Equal(t, result.String(), pbState.DeploymentState_UNKNOWN.String(), st.String())
		default:
			assert.NotEqual(t, result.String(), pbState.DeploymentState_UNKNOWN.String(), st.String())
		}
	}
}

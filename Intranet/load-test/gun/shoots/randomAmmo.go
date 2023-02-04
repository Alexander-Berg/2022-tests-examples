package shoots

import "encoding/json"

type Ammo struct {
	Tag string `json:"tag"`
}

type RandomAmmo struct {
	Value interface{}
}

func (d *RandomAmmo) UnmarshalJSON(data []byte) error {
	var ammo Ammo
	if err := json.Unmarshal(data, &ammo); err != nil {
		return err
	}
	switch ammo.Tag {
	case GetIdentityTag:
		d.Value = new(GetIdentityAmmo)
	case CreateIdentityTag:
		d.Value = new(CreateIdentityAmmo)
	case AddToGroupTag:
		d.Value = new(AddToGroupAmmo)
	case DeleteIdentityTag:
		d.Value = new(DeleteIdentityAmmo)
	case ExistsInGroupTag:
		d.Value = new(ExistsInGroupAmmo)
	case ListIdentitiesTag:
		d.Value = new(ListIdentitiesAmmo)
	case ListIdentityGroupsTag:
		d.Value = new(ListIdentityGroupsAmmo)
	case MoveIdentityTag:
		d.Value = new(MoveIdentityAmmo)
	case RemoveFromGroupTag:
		d.Value = new(RemoveFromGroupAmmo)
	}
	return json.Unmarshal(data, d.Value)

}

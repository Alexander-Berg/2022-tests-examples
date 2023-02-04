package models

type Identity struct {
	ID       string `csv:"id" json:"id,omitempty"`
	ParentID string `csv:"parent_id" json:"parent_id,omitempty"`
	*ExternalIdentity
}

type CompositeID struct {
	ID               string            `json:"id,omitempty"`
	ExternalIdentity *ExternalIdentity `json:"external_identity,omitempty"`
}

type ExternalIdentity struct {
	ExternalID string `csv:"external_id" json:"external_id,omitempty"`
	TypeID     string `csv:"type_id" json:"type_id"`
}

type IdentityData struct {
	Name     string `json:"name,omitempty"`
	LastName string `json:"lastname,omitempty"`
	Phone    string `json:"phone,omitempty"`
	Email    string `json:"email,omitempty"`
	Slug     string `json:"slug,omitempty"`
}

"""create test table

Revision ID: f40d6ddffe0d
Revises: a57fbbc3c4e0
Create Date: 2021-09-20 11:10:22.511096

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = 'f40d6ddffe0d'
down_revision = 'a57fbbc3c4e0'
branch_labels = None
depends_on = None


def upgrade():
    op.create_table(
        'department_2021_09_21',
        sa.Column('id', sa.BigInteger, primary_key=True),
        sa.Column('source', sa.String),
        sa.Column('source_id', sa.BigInteger, unique=True),
        sa.Column('department_id', sa.BigInteger),
        sa.Column('native_lang', sa.String, nullable=True),
        sa.Column('intranet_status', sa.Integer, nullable=False),
        sa.Column('created_at', sa.DateTime, nullable=False),
        sa.Column('modified_at', sa.DateTime, nullable=False),
        sa.Column('position', sa.BigInteger, nullable=True),
        sa.Column('from_staff_id', sa.BigInteger, nullable=True),
        sa.Column('parent', sa.String, nullable=True),
        sa.Column('kind', sa.BigInteger, nullable=True),
        sa.Column('name', sa.String, nullable=True),
        sa.Column('name_en', sa.String, nullable=True),
        sa.Column('short_name', sa.String, nullable=True),
        sa.Column('short_name_en', sa.String, nullable=True),
        sa.Column('code', sa.String, nullable=True),
        sa.Column('url', sa.String, nullable=True),
        sa.Column('dynamic_url', sa.String, nullable=True),
        sa.Column('bg_color', sa.String, nullable=True),
        sa.Column('fg_color', sa.String, nullable=True),
        sa.Column('maillists', sa.String, nullable=True),
        sa.Column('clubs', sa.String, nullable=True),
        sa.Column('wiki_page', sa.String, nullable=True),
        sa.Column('jira_project', sa.String, nullable=True),
        sa.Column('description', sa.String, nullable=True),
        sa.Column('description_en', sa.String, nullable=True),
        sa.Column('is_expanded', sa.Boolean, nullable=True),
        sa.Column('is_hidden', sa.Boolean, nullable=True),
        sa.Column('is_cut_out', sa.Boolean, nullable=True),
        sa.Column('category', sa.String, nullable=True),
        sa.Column('instance_class', sa.String, nullable=True),
        sa.Column('oebs_creation_date', sa.DateTime, nullable=True),
    )


def downgrade():
    op.drop_table('department_2021_09_21')

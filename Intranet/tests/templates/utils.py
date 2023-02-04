# -*- coding: utf-8 -*-
from typing import Any, Dict, List

from django.conf import settings
from django.template.loader import render_to_string

from idm.tests.utils import create_user, random_slug
from idm.users.models import Group, User

LANG_CODES = [lang[0] for lang in settings.LANGUAGES]


def render_template(template_name: str, context: Dict[str, Any]) -> str:
    return render_to_string(template_name, context).strip()


def user_mention(user: User) -> str:
    return f'{user.get_full_name()} ({user.username})'


def group_mention(group: Group) -> str:
    return f'{group.get_localized_field("name")} ({group.external_id})'


def generate_named_user(**kwargs) -> User:
    return create_user(
        first_name=random_slug(),
        last_name=random_slug(),
        first_name_en=random_slug(),
        last_name_en=random_slug(),
        **kwargs
    )


def generate_approvers(count: int) -> List[User]:
    return [generate_named_user() for _ in range(count)]

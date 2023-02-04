from dataclasses import dataclass
from maps.doc.proto.testhelper.common import Image, set_image


@dataclass
class Author:
    name: str
    description: str = None
    favicon: Image = None
    uri: str = None


@dataclass
class Collection:
    id_: str
    title: str = None
    description: str = None
    image: Image = None
    rubric: str = None
    item_count: int = None
    author: Author = None


def collection_uri(id_):
    return f'ymapsbm1://collection?id={id_}'


def set_author(msg, author):
    msg.name = author.name
    if author.description:
        msg.description = author.description
    if author.favicon:
        set_image(msg.favicon, author.favicon)
    if author.uri:
        msg.uri = author.uri


def set_collection(msg, collection):
    msg.uri = collection_uri(collection.id_)
    if collection.title:
        msg.title = collection.title
    if collection.description:
        msg.description = collection.description
    if collection.image:
        set_image(msg.image, collection.image)
    if collection.item_count:
        msg.item_count = collection.item_count
    if collection.rubric:
        msg.rubric = collection.rubric
    if collection.author:
        set_author(msg.author, collection.author)
    msg.seoname = collection.id_


def add_collection(msg, collection):
    set_collection(msg.collection.add(), collection)

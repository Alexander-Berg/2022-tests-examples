/**
 * @jest-environment node
 */
import React from 'react';
import { shallow } from 'enzyme';

import { PostListingHelmet } from 'core/client/features/PostListing/PostListingHelmet/PostListingHelmet';
import { mockStore } from 'core/mocks/store.mock';
import { Container } from 'core/client/components/Container/Container';
import { Breadcrumbs } from 'core/client/components/Breadcrumbs/Breadcrumbs';

import { PostListingTitle } from '../PostListingTitle/PostListingTitle';
import { PostListingPosts } from '../PostListingPosts/PostListingPosts';
import { PostListingPagination } from '../PostListingPagination/PostListingPagination';
import { PostListingNavigationTags } from '../PostListingNavigationTags/PostListingNavigationTags';

import PostListingMain from './PostListingMain';

it('в разметке присутствует все обязательные элементы для страницы', () => {
    mockStore({});

    const wrapper = shallow(<PostListingMain/>);

    expect(wrapper.find(PostListingHelmet).exists()).toBe(true);
    expect(wrapper.find(Container).at(0).find(Breadcrumbs).exists()).toBe(true);
    expect(wrapper.find(PostListingNavigationTags).exists()).toBe(true);
    expect(wrapper.find(Container).at(1).find(PostListingTitle).exists()).toBe(true);
    expect(wrapper.find(PostListingPosts).exists()).toBe(true);
    expect(wrapper.find(PostListingPagination).exists()).toBe(true);
});

import { renderHook, act } from '@testing-library/react-hooks';

import useAccordion from './useAccordion';

it('addSection добавляет секцию', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section-1',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: () => {},
    };

    act(() => {
        result.current.addSection(section);
    });

    expect(result.current.sections).toEqual({
        [ section.id ]: section,
    });
});

it('removeSection удаляет секцию', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section-1',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: () => {},
    };

    act(() => {
        result.current.addSection(section);
    });

    expect(result.current.sections).toEqual({
        [ section.id ]: section,
    });

    act(() => {
        result.current.removeSection(section.id);
    });

    expect(result.current.sections).toEqual({});
});

it('expandSection раскрывает секцию', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section1',
        isCollapsable: true,
        isCollapsed: true,
        scrollTo: () => {},
    };

    act(() => {
        result.current.addSection(section);
    });

    act(() => {
        result.current.expandSection(section.id);
    });

    expect(result.current.sections.section1.isCollapsed).toEqual(false);
});

it('collapseSection скрывает секцию', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section1',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: () => {},
    };

    act(() => {
        result.current.addSection(section);
    });

    act(() => {
        result.current.collapseSection(section.id);
    });

    expect(result.current.sections.section1.isCollapsed).toEqual(true);
});

describe('getSection', () => {
    it('возвращает секцию, если она есть', () => {
        const {
            result,
        } = renderHook(() => useAccordion());

        const section = {
            id: 'section-1',
            isCollapsable: true,
            isCollapsed: false,
            scrollTo: () => {},
        };

        act(() => {
            result.current.addSection(section);
        });

        expect(result.current.getSection(section.id)).toEqual(section);
    });

    it('возвращает null, если секции нет', () => {
        const {
            result,
        } = renderHook(() => useAccordion());

        const section = {
            id: 'section-1',
            isCollapsable: true,
            isCollapsed: false,
            scrollTo: () => {},
        };

        act(() => {
            result.current.addSection(section);
        });

        expect(result.current.getSection('some-section')).toBeNull();
    });
});

it('getSectionsIds возвращает секции', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section-1',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: () => {},
    };

    const section2 = {
        id: 'section-2',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: () => {},
    };

    act(() => {
        result.current.addSection(section);
        result.current.addSection(section2);
    });

    expect(result.current.getSectionsIds()).toEqual([ section.id, section2.id ]);
});

it('isSectionCollapsable isCollapsable нужной секции', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section-1',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: () => {},
    };

    const section2 = {
        id: 'section-2',
        isCollapsable: false,
        isCollapsed: false,
        scrollTo: () => {},
    };

    act(() => {
        result.current.addSection(section);
        result.current.addSection(section2);
    });

    expect(result.current.isSectionCollapsable(section.id)).toEqual(true);
});

it('isSectionCollapsable вызывает scrollTo у нужной секции', () => {
    const {
        result,
    } = renderHook(() => useAccordion());

    const section = {
        id: 'section-1',
        isCollapsable: true,
        isCollapsed: false,
        scrollTo: jest.fn(),
    };

    const section2 = {
        id: 'section-2',
        isCollapsable: false,
        isCollapsed: false,
        scrollTo: jest.fn(),
    };

    act(() => {
        result.current.addSection(section);
        result.current.addSection(section2);
    });

    result.current.scrollToSection(section2.id);

    expect(section2.scrollTo).toHaveBeenCalledTimes(1);
    expect(section.scrollTo).not.toHaveBeenCalled();
});

export const isScrollable = (el: HTMLElement): boolean => {
  return el.scrollHeight !== el.clientHeight;
};

package com.vkyoungcn.smartdevices.yomemory.validatingEditor;

import java.util.Stack;

public class LimitedStack<T> extends Stack<T> {

  private int topLimitSize = 0;

  @Override
  public T push(T object) {
    if (topLimitSize > size()) {
      return super.push(object);
    }

    return object;
  }

  public int getTopLimitSize() {
    return topLimitSize;
  }

  public void setTopLimitSize(int topLimitSize) {
    this.topLimitSize = topLimitSize;
  }
}

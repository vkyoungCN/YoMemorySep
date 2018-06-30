package com.vkyoungcn.smartdevices.yomemory.validatingEditor;

import java.util.Stack;

/**
 * Thanks to origin author Adrián García Lomas
 */
public class FixedStack<T> extends Stack<T> {

  private int maxSize = 0;

  @Override
  public T push(T object) {
    if (maxSize > size()) {
      return super.push(object);
    }

    return object;
  }

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }
}

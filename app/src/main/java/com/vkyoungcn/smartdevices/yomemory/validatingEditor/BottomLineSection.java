package com.vkyoungcn.smartdevices.yomemory.validatingEditor;

/**
 * Thanks to origin author Adrián García Lomas
 */
@SuppressWarnings("all")
public class BottomLineSection {

  private float fromX;
  private float fromY;
  private float toX;
  private float toY;

  public BottomLineSection() {
  }

  public BottomLineSection(float fromX, float fromY, float toX, float toY) {
    this.fromX = fromX;
    this.fromY = fromY;
    this.toX = toX;
    this.toY = toY;
  }

  public void from(float x, float y) {
    this.fromX = x;
    this.fromY = y;
  }

  public void to(float x, float y) {
    this.toX = x;
    this.toY = y;
  }

  public float getFromX() {
    return fromX;
  }

  public void setFromX(float fromX) {
    this.fromX = fromX;
  }

  public float getFromY() {
    return fromY;
  }

  public void setFromY(float fromY) {
    this.fromY = fromY;
  }

  public float getToX() {
    return toX;
  }

  public void setToX(float toX) {
    this.toX = toX;
  }

  public float getToY() {
    return toY;
  }

  public void setToY(float toY) {
    this.toY = toY;
  }
}

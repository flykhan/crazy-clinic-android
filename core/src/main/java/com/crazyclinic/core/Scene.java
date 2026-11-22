package com.crazyclinic.core;

/** 一个可独立更新/渲染/接收触摸的界面。 */
public interface Scene {
    default void enter(Game game) {}

    void update(Game game, float dt);

    void render(Game game, Renderer r);

    default void touchDown(Game game, int pointerId, float x, float y) {}

    default void touchMove(Game game, int pointerId, float x, float y) {}

    default void touchUp(Game game, int pointerId, float x, float y) {}

    default void backPressed(Game game) {}

    default void pause(Game game) {}

    default void resume(Game game) {}
}

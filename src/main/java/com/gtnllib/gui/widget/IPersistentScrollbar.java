package com.gtnllib.gui.widget;

import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.ScrollData;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Drawing contract for {@link ScrollData} whose track remains visible when scrolling is inactive.
 * <p>
 * {@link GTNLListWidget} reserves the implementing scroll data's thickness on its configured edge and invokes
 * {@link #drawTrack(ScrollArea)} when MUI2 skips the inactive scroll bar. Active scroll bars continue to use
 * {@link ScrollData#drawScrollbar}.
 */
public interface IPersistentScrollbar {

    /**
     * Draws the track and handle relative to the scroll area's origin.
     *
     * @param area owning scroll area, including its current dimensions and scroll state
     */
    @SideOnly(Side.CLIENT)
    void drawTrack(ScrollArea area);
}

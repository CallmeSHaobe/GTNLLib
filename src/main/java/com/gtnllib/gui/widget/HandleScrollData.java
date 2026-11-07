package com.gtnllib.gui.widget;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.gtnllib.gui.theme.AE2Theme;
import com.gtnllib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side vertical scroll data with a fixed-size AE2 or vanilla handle.
 * <p>
 * {@link GTNLListWidget} reserves the track width and draws the disabled handle when the content fits. Other scroll
 * widgets must reserve {@link #getThickness()} pixels on the right and draw the inactive track themselves. The
 * available vertical track must be at least {@link AE2Theme#SCROLLER_HEIGHT} pixels tall.
 * <p>
 * Scrolling is smooth by default. {@link #rows(int)} enables immediate scrolling snapped to row boundaries. Scroll
 * shadows are never drawn.
 */
@SideOnly(Side.CLIENT)
public class HandleScrollData extends VerticalScrollData implements IPersistentScrollbar {

    private final @Nullable Variant variant;
    private final boolean small;
    private int rowHeight;

    /**
     * Creates scroll data with a large handle.
     *
     * @param variant AE2 palette, or {@code null} for the vanilla creative-inventory scroll bar
     */
    public HandleScrollData(@Nullable Variant variant) {
        this(variant, false);
    }

    /**
     * Creates scroll data with the requested handle style.
     *
     * @param variant AE2 palette, or {@code null} for a 14-pixel-wide vanilla track
     * @param small   whether to use the 7-pixel-wide AE2 handle instead of the 12-pixel-wide handle;
     *                ignored when {@code variant} is {@code null}
     */
    public HandleScrollData(@Nullable Variant variant, boolean small) {
        super(false, small && variant != null ? AE2Theme.SCROLLBAR_WIDTH : AE2Theme.scrollbarWidth(variant));
        this.variant = variant;
        this.small = small && variant != null;
    }

    /**
     * Configures row snapping and the wheel step.
     * <p>
     * Positive values disable animation and set the wheel step to one row. Both content endpoints remain reachable
     * when the scroll range is not divisible by the row height. Align content, viewport, padding and separators to
     * the row height when partial rows must not be visible.
     *
     * @param rowHeight row height in pixels; nonpositive values disable snapping without changing the wheel step
     * @return this scroll data
     */
    public HandleScrollData rows(int rowHeight) {
        this.rowHeight = Math.max(0, rowHeight);
        if (rowHeight > 0) setScrollSpeed(rowHeight);
        return this;
    }

    @Override
    public int getScrollBarLength(ScrollArea area) {
        return Math.min(AE2Theme.SCROLLER_HEIGHT, getFullVisibleSize(area));
    }

    /**
     * Calculates the handle offset along an active track.
     *
     * @param area            owning scroll area
     * @param scrollBarLength handle length in pixels
     * @param fullVisibleSize visible track length in pixels, smaller than the content length
     * @return handle offset from the track's start in pixels
     */
    @Override
    public int getScrollBarStart(ScrollArea area, int scrollBarLength, int fullVisibleSize) {
        return (int) ((long) (fullVisibleSize - scrollBarLength) * getScroll() / (getScrollSize() - fullVisibleSize));
    }

    @Override
    public boolean onMouseClicked(ScrollArea area, int mainAxisPos, int crossAxisPos, int button) {
        return super.onMouseClicked(area, mainAxisPos - trackTop(area), crossAxisPos, button);
    }

    /**
     * Converts a handle position to the progress consumed by {@link ScrollArea#drag(int, int)}.
     *
     * @param area         owning scroll area
     * @param mainAxisPos  mouse Y coordinate relative to the scroll area
     * @param crossAxisPos mouse X coordinate relative to the scroll area
     * @return progress adjusted for the content range used by MUI2 dragging
     */
    @Override
    public float getProgress(ScrollArea area, int mainAxisPos, int crossAxisPos) {
        int visible = getFullVisibleSize(area);
        int range = getScrollSize() - visible;
        if (range <= 0) return 0;
        int travel = visible - getScrollBarLength(area);
        if (travel <= 0) return getScroll() / (float) (range + getThickness());
        int position = mainAxisPos - trackTop(area) - clickOffset;
        if (position <= 0) return 0;
        if (position >= travel) return 1;
        // ScrollArea.drag includes the track thickness in its content range.
        return position / (float) travel * range / (range + getThickness());
    }

    @Override
    public boolean scrollTo(ScrollArea area, int x) {
        int maxScroll = Math.max(0, getScrollSize() - getFullVisibleSize(area));
        if (rowHeight > 0 && x > 0 && x < maxScroll) {
            x = (int) Math.min(Math.round(x / (double) rowHeight) * rowHeight, maxScroll);
        }
        return super.scrollTo(area, x);
    }

    @Override
    public boolean scrollBy(ScrollArea area, int x) {
        long target = (long) getScroll() + x;
        return scrollTo(area, (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, target)));
    }

    @Override
    public void animateTo(ScrollArea area, int x) {
        if (rowHeight > 0) scrollTo(area, x);
        else super.animateTo(area, x);
    }

    @Override
    public void drawScrollbar(ScrollArea area, ModularGuiContext context, WidgetTheme widgetTheme, IDrawable texture) {
        drawTrack(area);
    }

    @Override
    public void drawScrollShadow(ScrollArea area, ModularGuiContext context) {}

    /**
     * Draws the groove and enabled or disabled handle in scroll-area coordinates.
     * <p>
     * The groove extends one pixel above and below the available vertical track. An active horizontal scroll bar
     * is excluded from that track. This method does not reserve layout space or process input.
     *
     * @param area owning scroll area
     */
    @Override
    public void drawTrack(ScrollArea area) {
        boolean active = isScrollBarActive(area);
        int handleY = active ? getScrollBarStart(area, getScrollBarLength(area), false) : 0;
        int x = area.w() - getThickness();
        int top = trackTop(area);
        int height = getFullVisibleSize(area);
        // The groove border sits one pixel outside the scroll area so the handle can travel the full height.
        if (small) AE2Theme.drawSmallScrollbar(variant, x, top - 1, height + 2, handleY, active);
        else AE2Theme.drawScrollbar(variant, x, top - 1, height + 2, handleY, active);
    }

    private int trackTop(ScrollArea area) {
        ScrollData horizontal = getOtherScrollData(area);
        return horizontal != null && horizontal.isScrollBarActive(area) && horizontal.isOnAxisStart()
            ? horizontal.getThickness()
            : 0;
    }
}

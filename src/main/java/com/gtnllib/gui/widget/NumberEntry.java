package com.gtnllib.gui.widget;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.MathUtils;
import com.cleanroommc.modularui.utils.ParseResult;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnllib.gui.theme.AE2Theme;
import com.gtnllib.gui.theme.AE2Theme.Variant;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Client-side integer input with a text field, validation feedback and four pairs of step buttons.
 * <p>
 * Input may contain a signed {@code long} literal or a numeric expression with SI suffixes, such as {@code 2 * 64}
 * or {@code 1.5k}. Only integral results within the configured bounds are valid. Invalid text remains editable and
 * is accompanied by an error tooltip and icon. The text field accepts up to 64 characters.
 * <p>
 * Step sizes are 1, 10, 100 and 1000, or 1, 16, 32 and 64 while Shift or Ctrl is held. Steps are clamped to the
 * configured bounds. Increasing a value of one by a step larger than one selects that step value. Invalid input
 * is treated as zero when applying a step.
 * <p>
 * {@link #isInputValid()} describes the input. Inherited {@link #isValid()} describes the MUI2 widget lifecycle.
 * <p>
 * For example, {@code new NumberEntry(Variant.LIGHT).bounds(1, 1_000_000).value(64)} creates a bounded input with an
 * initial value of 64.
 */
@SideOnly(Side.CLIENT)
public class NumberEntry extends ParentWidget<NumberEntry> {

    /** Default widget dimensions in GUI pixels. */
    public static final int WIDTH = 138, HEIGHT = 62;
    private static final long[] STEPS = { 1, 10, 100, 1000 }, SHIFT_STEPS = { 1, 16, 32, 64 };
    private static final int[] BUTTON_X = { 0, 28, 62, 100 }, BUTTON_WIDTH = { 22, 28, 32, 38 };
    private static final int MINUS_Y = 42, FIELD_X = 28, FIELD_Y = 25, FIELD_WIDTH = 66, ICON_X = 104, ICON_Y = 27,
        MAX_LENGTH = 64, PLAIN_ERROR = 0xFFFF5555;
    private static final String LANG = "gtnllib.gui.number_entry.";
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final @Nullable Variant variant;
    private final Field field = new Field();
    private final Warning warning = new Warning();
    private final List<String> messages = new ObjectArrayList<>();
    private String text = "0";
    private long min, max = Long.MAX_VALUE, value;
    private boolean inputValid = true;
    private LongConsumer listener = ignored -> {};

    /**
     * Creates an entry with value zero and inclusive bounds from zero to {@link Long#MAX_VALUE}.
     *
     * @param variant palette for validation feedback, or {@code null} for the default error color and light icon;
     *                button and text-field styling follows the containing panel's theme
     */
    public NumberEntry(@Nullable Variant variant) {
        this.variant = variant;
        size(WIDTH, HEIGHT);
        for (int i = 0; i < STEPS.length; i++) {
            child(
                step(i, 1).pos(BUTTON_X[i], 0)
                    .size(BUTTON_WIDTH[i], AE2Theme.BUTTON_HEIGHT));
            child(
                step(i, -1).pos(BUTTON_X[i], MINUS_Y)
                    .size(BUTTON_WIDTH[i], AE2Theme.BUTTON_HEIGHT));
        }
        field.setMaxLength(MAX_LENGTH)
            .setTooltipOverride(true)
            .autoUpdateOnChange(true)
            .value(new StringValue.Dynamic(() -> text, next -> {
                text = next;
                validate(true);
            }))
            .tooltipAutoUpdate(true)
            .tooltipBuilder(tooltip -> messages.forEach(tooltip::addLine));
        child(
            field.pos(FIELD_X, FIELD_Y)
                .size(FIELD_WIDTH, AE2Theme.TEXT_FIELD_HEIGHT));
        child(
            warning.pos(ICON_X, ICON_Y)
                .size(AE2Theme.VALIDATION_ICON_SIZE, AE2Theme.VALIDATION_ICON_SIZE));
        validate(false);
    }

    /**
     * Sets inclusive bounds and revalidates the current text without changing it or notifying the listener.
     *
     * @param min minimum accepted value
     * @param max maximum accepted value; values below {@code min} are replaced with {@code min}
     * @return this widget
     */
    public NumberEntry bounds(long min, long max) {
        this.min = min;
        this.max = Math.max(min, max);
        validate(false);
        return this;
    }

    /**
     * Replaces the text with a decimal integer and revalidates it without notifying the listener.
     *
     * @param value value to display; values outside the bounds remain visible as invalid input
     * @return this widget
     */
    public NumberEntry value(long value) {
        setText(Long.toString(value));
        validate(false);
        return this;
    }

    /**
     * Sets the callback for valid values produced by user input.
     * <p>
     * The callback runs when the accepted value changes or invalid input becomes valid. Editing an expression
     * without changing its valid result, losing focus and stepping against a bound do not repeat the callback.
     * Programmatic calls to {@link #value(long)} and {@link #bounds(long, long)} do not invoke it.
     *
     * @param listener non-null callback receiving the accepted value on the client thread
     * @return this widget
     */
    public NumberEntry onChange(LongConsumer listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Returns the accepted value of the current text.
     *
     * @return the integer value, or an empty result while the input is invalid
     */
    public OptionalLong value() {
        return inputValid ? OptionalLong.of(value) : OptionalLong.empty();
    }

    /**
     * Reports whether the current text evaluates to an integer within the configured bounds.
     *
     * @return {@code true} if the input is valid
     */
    public boolean isInputValid() {
        return inputValid;
    }

    private ButtonWidget<?> step(int index, int sign) {
        ButtonWidget<?> button = new ButtonWidget<>();
        IDrawable background = AE2Theme.button(button, () -> true, () -> false);
        return button.background(background)
            .hoverBackground(background)
            .overlay(AE2Theme.label(button, () -> (sign > 0 ? "+" : "-") + steps()[index], () -> true, () -> false))
            .onMousePressed(mouse -> {
                if (mouse != 0) return false;
                add(sign * steps()[index]);
                return true;
            });
    }

    private static long[] steps() {
        return GuiScreen.isShiftKeyDown() || GuiScreen.isCtrlKeyDown() ? SHIFT_STEPS : STEPS;
    }

    private void add(long delta) {
        BigDecimal current = inputValid ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
        BigDecimal next = current.compareTo(BigDecimal.ONE) == 0 && delta > 1 ? BigDecimal.valueOf(delta)
            : current.add(BigDecimal.valueOf(delta));
        setText(
            next.max(BigDecimal.valueOf(min))
                .min(BigDecimal.valueOf(max))
                .toPlainString());
        validate(true);
    }

    private void setText(String next) {
        text = next;
        field.setText(next);
    }

    private void validate(boolean notify) {
        boolean wasValid = inputValid;
        long previous = value;
        messages.clear();
        BigDecimal number = parse(text);
        if (number == null) messages.add(tr("invalid"));
        else if (number.stripTrailingZeros()
            .scale() > 0) messages.add(tr("whole"));
        else if (number.compareTo(BigDecimal.valueOf(min)) < 0) messages.add(tr("min", min));
        else if (number.compareTo(BigDecimal.valueOf(max)) > 0) messages.add(tr("max", max));
        inputValid = messages.isEmpty();
        field.error(inputValid ? null : variant == null ? PLAIN_ERROR : variant.colors.error());
        warning.setEnabled(!inputValid);
        if (!inputValid) return;
        value = number.longValueExact();
        if (!INTEGER.matcher(text.strip())
            .matches()) messages.add("= " + value);
        if (notify && (!wasValid || previous != value)) listener.accept(value);
    }

    private static @Nullable BigDecimal parse(String text) {
        String trimmed = text.strip();
        if (trimmed.isEmpty()) return null;
        if (INTEGER.matcher(trimmed)
            .matches()) return new BigDecimal(trimmed);
        try {
            ParseResult result = MathUtils.parseExpression(trimmed, 0);
            if (result.isFailure() || !result.hasValue()
                || !result.getResult()
                    .isNumberValue())
                return null;
            return result.getResult()
                .getNumberValue();
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }

    private static String tr(String key, Object... args) {
        return StatCollector.translateToLocalFormatted(LANG + key, args);
    }

    private static class Field extends TextFieldWidget {

        @Override
        public void setText(String text) {
            super.setText(text);
            handler.clampCursors();
            getScrollData().scrollTo(getScrollArea(), 0);
        }

        private void error(@Nullable Integer color) {
            textColor = color;
        }
    }

    private class Warning extends Widget<Warning> {

        private Warning() {
            tooltipAutoUpdate(true).tooltipBuilder(tooltip -> messages.forEach(tooltip::addLine));
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            (variant == null ? Variant.LIGHT : variant).invalid
                .draw(0, 0, AE2Theme.VALIDATION_ICON_SIZE, AE2Theme.VALIDATION_ICON_SIZE);
        }
    }
}

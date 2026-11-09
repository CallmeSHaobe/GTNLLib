# AE2 Theme API

GTNLLib provides light and dark AE2 themes for ModularUI 2. Rendering helpers and themed widgets are client-side APIs. Their source Javadoc defines parameter units, return values, null handling and lifecycle requirements.

## API Reference

| Type | Responsibility |
| --- | --- |
| [AE2Theme](src/main/java/com/gtnllib/gui/theme/AE2Theme.java) | Theme registration, variant lookup, drawable factories and direct drawing helpers. Includes the `Variant`, `Colors`, `ButtonText` and `Icon` types. |
| [ScreenTheme](src/main/java/com/gtnllib/gui/theme/ScreenTheme.java) | Persistable selection of the default, light or dark panel theme. |
| [HandleScrollData](src/main/java/com/gtnllib/gui/widget/HandleScrollData.java) | Vertical scrolling with a fixed-size handle and optional row snapping. |
| [GTNLListWidget](src/main/java/com/gtnllib/gui/widget/GTNLListWidget.java) | List layout, initial scroll position and persistent scroll-bar integration. |
| [IPersistentScrollbar](src/main/java/com/gtnllib/gui/widget/IPersistentScrollbar.java) | Extension contract for scroll data that retains an inactive track. |
| [NumberEntry](src/main/java/com/gtnllib/gui/widget/NumberEntry.java) | Bounded integer input, expression parsing, step buttons and validation feedback. |

## Theme Selection

| Selection | Panel theme | Menu theme |
| --- | --- | --- |
| `ScreenTheme.DEFAULT` | Screen default (`themeId == null`) | `modularui.context_menu` |
| `ScreenTheme.AE2_LIGHT` | `gtnllib:ae2_light` | `gtnllib:ae2_light_menu` |
| `ScreenTheme.AE2_DARK` | `gtnllib:ae2_dark` | `gtnllib:ae2_dark_menu` |

GTNLLib registers these themes during client initialization. Apply the selected identifier through MUI2's screen theme registration. `AE2Theme.useMenuTheme` maps named child menu panels to the matching menu theme.

## Usage Examples

### Row-Based List

```java
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.gtnllib.gui.theme.AE2Theme.Variant;
import com.gtnllib.gui.widget.GTNLListWidget;
import com.gtnllib.gui.widget.HandleScrollData;

HandleScrollData scrolling = new HandleScrollData(Variant.LIGHT).rows(22);
ListWidget<IWidget, ?> list = new GTNLListWidget<>().scrollDirection(scrolling)
    .size(180, 110);
```

`GTNLListWidget` reserves the scroll-bar width and draws its disabled handle when the content fits. Row snapping includes both scroll endpoints. See `HandleScrollData.rows(int)` for layout alignment requirements. Dragging the handle is anchored to the offset at the press, so holding the handle still never moves long lists.

### Integer Input

```java
import java.util.OptionalLong;

import com.gtnllib.gui.theme.AE2Theme.Variant;
import com.gtnllib.gui.widget.NumberEntry;

NumberEntry amount = new NumberEntry(Variant.LIGHT).bounds(1, 1_000_000)
    .value(64);
OptionalLong requested = amount.value();
```

`value()` returns an empty result for invalid input. `isInputValid()` checks the input; inherited `isValid()` reports the MUI2 widget lifecycle. Callback semantics are defined by `NumberEntry.onChange(LongConsumer)`.

## Texture Attribution

Texture origins, transformations and licensing are specified in [AE2-gui-theme-NOTICE.txt](src/main/resources/META-INF/licenses/AE2-gui-theme-NOTICE.txt).

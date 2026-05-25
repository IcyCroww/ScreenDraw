# ScreenDraw

Client-side Fabric mod for drawing and annotating directly over the Minecraft screen.

Target: Minecraft `1.21.9`, Fabric Loader `0.17.2+`, Fabric API `0.134.1+1.21.9`.

## Controls

- `P` - open the drawing overlay.
- `LMB` - draw with the selected tool.
- Mouse wheel - change brush or eraser size.
- `Shift + mouse wheel` - change brush opacity.
- `Alt` - hold to open the color picker.
- `B` - brush, `E` - eraser, `X` - toggle brush/eraser.
- `S` - toggle smoothing.
- `C` - clear the canvas.
- `Ctrl+Z` - undo.
- `Ctrl+Y` or `Ctrl+Shift+Z` - redo.
- `Ctrl+S` - save the current drawing.
- `Ctrl+L` - load the saved drawing.
- `Ctrl+P` - export the drawing to PNG.
- `1-5` - load a color slot.
- `Shift+1-5` - save the current color and opacity to a slot.

Saved drawings are written to `run/screendraw/last-drawing.json`.
PNG exports are written to `run/screendraw/exports/`.

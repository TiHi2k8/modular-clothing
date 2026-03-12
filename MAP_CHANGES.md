# Map-Mod Änderungen - Isometrische Ansicht mit echten Texturen

## Was wurde geändert:

### ✅ Behobene Probleme:
1. **Zoom-Problem behoben**: Die Map verschiebt sich nicht mehr beim Zoomen
2. **Isometrische Ansicht**: -15° Winkel von Süden, als würde man von oben auf die Welt schauen
3. **Echte Block-Texturen**: Die Map zeigt jetzt die tatsächlichen Minecraft Block-Texturen an!

### 🎮 Neue Features:

#### Ansicht:
- **Isometrische Projektion** mit -15° Neigungswinkel von Süden
- Blick auf die Welt von oben (ähnlich wie aus großer Höhe)
- Höheninformation wird in der Y-Achse berücksichtigt
- **ECHTE MINECRAFT TEXTUREN** - Alle Blöcke werden mit ihren Original-Texturen angezeigt!

#### Steuerung:
- **Mausrad**: Zoom (0.5x - 8.0x)
- **Linke Maustaste + Ziehen**: Map verschieben
- **M oder ESC**: Map schließen

#### Darstellung:
- Verwendet den Minecraft Block-Textur-Atlas
- Zeigt die echte Textur der Oberseite jedes Blocks
- Alle Blöcke werden mit ihren Original-Texturen gerendert:
  - Gras mit Gras-Textur
  - Stein mit Stein-Textur
  - Wasser mit Wasser-Textur
  - Erze mit ihren echten Texturen
  - Und alle anderen Blöcke!
- Höhendarstellung durch Y-Koordinate in der isometrischen Projektion

### 🔧 Technische Details:

**Isometrische Projektion:**
```
isoX = relX * zoom
isoY = (-relZ * cos(-15°) + relY * sin(-15°)) * zoom
```

- **-15° Winkel** für bessere Sicht von oben
- X-Achse: Horizontal (Ost-West)
- Z-Achse und Y-Achse kombiniert für vertikale Position
- Von hinten nach vorne rendern für korrektes Layering

**Textur-Rendering:**
- Nutzt `TextureMap.LOCATION_BLOCKS_TEXTURE` (Minecraft Block-Atlas)
- Holt Texturen über `BlockRendererDispatcher` und `getBlockModelShapes()`
- UV-Koordinaten werden korrekt gemappt
- Fallback auf Erde-Textur bei Problemen

**Performance:**
- Render-Distanz passt sich automatisch an Zoom an
- Nur geladene Chunks werden angezeigt

### 📦 Build-Ausgabe:
Die Mod-Datei befindet sich in:
`build/libs/examplemod-1.0.jar`

Kopiere diese Datei in deinen Minecraft `mods/` Ordner.

### 🎨 Visuelle Verbesserungen:
- Statt einfarbige Blöcke siehst du jetzt die echten Minecraft-Texturen
- Die Map sieht aus wie eine Miniatur-Version der echten Welt
- Perfekt um die Umgebung zu erkunden!


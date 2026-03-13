Add-Type -AssemblyName System.Drawing

$width = 256
$height = 256
$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::Transparent)

# Draw GUI Background (176x166)
$bgBrush = [System.Drawing.Brushes]::LightGray
$g.FillRectangle($bgBrush, 0, 0, 176, 166)

# Draw Border for GUI
$borderPen = [System.Drawing.Pens]::DarkGray
$g.DrawRectangle($borderPen, 0, 0, 175, 165)

# Function to draw slot
function Draw-Slot($x, $y) {
    # Draw darker background for slot area
    $slotBrush = [System.Drawing.Brushes]::Gray
    $g.FillRectangle($slotBrush, $x-1, $y-1, 18, 18)

    # Draw highlight (bottom right)
    $highlightPen = [System.Drawing.Pens]::White
    $g.DrawLine($highlightPen, $x-1, $y+17, $x+17, $y+17)
    $g.DrawLine($highlightPen, $x+17, $y-1, $x+17, $y+17)

    # Draw shadow (top left)
    $shadowPen = [System.Drawing.Pens]::Black
    $g.DrawLine($shadowPen, $x-1, $y-1, $x+16, $y-1)
    $g.DrawLine($shadowPen, $x-1, $y-1, $x-1, $y+16)
}

# Draw Clothing Slots
Draw-Slot 120 8
Draw-Slot 120 26
Draw-Slot 138 26
Draw-Slot 102 26
Draw-Slot 138 44
Draw-Slot 102 44
Draw-Slot 138 62
Draw-Slot 102 62

# Draw Player Inventory
for ($i = 0; $i -lt 3; $i++) {
    for ($j = 0; $j -lt 9; $j++) {
        Draw-Slot (8 + $j * 18) (84 + $i * 18)
    }
}

# Draw Hotbar
for ($k = 0; $k -lt 9; $k++) {
    Draw-Slot (8 + $k * 18) 142
}

# Save
$path = "$PSScriptRoot/src/main/resources/assets/examplemod/textures/gui/clothing_gui.png"
# Create directory if not exists
$dir = [System.IO.Path]::GetDirectoryName($path)
if (!(Test-Path $dir)) {
    New-Item -ItemType Directory -Force -Path $dir
}
$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Host "Generated texture at $path"


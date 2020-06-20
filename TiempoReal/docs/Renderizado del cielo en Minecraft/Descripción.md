Esta documentación es una copia de la disponible en el proyecto TrueCraft: [https://github.com/ddevault/TrueCraft/wiki/Sky](https://github.com/ddevault/TrueCraft/wiki/Sky)

Analysis is based on the decompiled source of the vanilla b1.7.3 client.

# Concepts

## Celestial Angle

The celestial angle is defined by the following function, where X is a value between [0, 1].
```
Y = X + ( (1 - (cos(X * PI) + 1) / 2) - X ) / 3
```
Notch derives X from the current time-of-day (in ticks) divided by 24,000 (the number of ticks in a day-night cycle), minus 0.25.  If X is less than 0, he adds one in order to keep the domain between [0, 1] as required by the equation.  The reason Notch shifts the domain by 1/4 is not clear.

The following graph shows the result of Notch's celestial angle calculation function for inputs between [0, 24000] in increments of 240.

![](https://imgrush.com/i4h0uMLTbVgD.png)

## World Sky Color

The **World Sky Color** is the multiplicative product of the base sky color, a brightness multiplier, and other multipliers related to weather events.

#### Base Sky Color

The base sky color is derived from the biome and temperature of the voxel in which the player entity currently resides.  The input temperature is divided by 3 and the result clamped to [-1, 1].  Notch uses this constrained temperature to compute the color in the HSL coordinate representation where the hue, saturation, and lightness are defined as shown below.  The result is then converted to RGB.

```
let hue := 0.6222222 - constrainedTemperature * 0.05
let saturation := 0.5 + constrainedTemperature * 0.1
let lightness := 1.0
```

Only the Sky biome overrides this computation to instead return a constant (0.75R, 0.75G, 1.0B).

#### Brightness Multiplier

The brightness multiplier is derived from the current **celestial angle** using the following function with the result clamped to [0, 1]:

```
Y = cos(celestialAngle * 2PI) * 2 + 0.5
```

#### Weather

_TODO_

## World Fog Color

The **World Fog Color** is derived from the current **celestial angle**.  The exact calculation varies by dimension.

#### Overworld

The **celestial angle** is transformed using the following function with the result clamped to [0, 1]:

```
Y = cos(celestialAngle * 2PI) * 2 + 0.5
```

The red, green, and blue components are then derived as follows:

```
let red := 0.7529412 * Y * 0.94 + 0.06
let green := 0.8470588 * Y * 0.94 + 0.06
let blue := 1.0 * Y * 0.91 + 0.09
```

#### Nether

The Nether ignores the current **celestial angle** and uses a constant (0.2, 0.03, 0.03) for the **world fog color**.

# Rendering The Sky

Minecraft renders the sky using a solid colored ceiling plane, void (bottom) plane, and fog.  For the lowest visibility setting, only fog is used.  The sky is the first thing rendered in each frame.

![](https://sr.ht/TcJv.png)

### Clear The Frame Buffer

Minecraft uses the same color for both the fog and clear color.  This helps seamlessly blend the ceiling and void planes, as well as distant objects, with the atmosphere as shown in the above screenshot.  To avoid overloading terms, I will refer to this color as the **atmosphere color**.

The **atmosphere color** is derived from the **world sky color**, the **world fog color**, current weather conditions, brightness of the voxel where the player entity currently resides, and whether the player entity is inside of a cloud, within water, or within lava.

1. Start with the **world fog color**

2. Blend the **world sky color** (source) with the **atmosphere color** (destination) using the following equation for each color component:

  ```
  output = destination + (source - destination) * blendFactor
  ```

  Notch computes the blend factor based on the user's configured draw distance - a value between [0, 2], with 0 resulting in the furthest draw distance.  A 0 draw distance setting yields a blend factor 0.29.

  ```
  let blendFactor = 1.0 - pow(1.0 - (4 - drawDistance), 0.25)
  ```

3. If it is raining... _TODO_

4. If there is lightning... _TODO_ 

5. If the player entity is inside of a cloud... _TODO_

6. If the player entity is within water, set the **atmosphere color** to (0.02R, 0.02G, 0.2B), ignoring the existing value.  Then skip step seven.

7. If the player entity is within lava, set the **atmosphere color** to (0.6R, 0.1R, 0.0B), ignoring the existing value.

8. Determine the brightness of the voxel that the bottom of the player entity resides within.  Typically this voxel will contain an Air block but if it contains a stair block, tilled field block, or slab then use the brightness of the brightest neighboring block (in all 6 directions) instead.  Note that you will need to recurse on this step if none of the neighboring blocks are air blocks.

  To determine the brightness of a voxel take the maximum of the voxel's **Sky Light Value** minus the **Subtracted Sky Light**, and the voxel's **Block Light Value**.

  ```
  let brightness := MAX(voxel.skylight - subtractedSkylight, voxel. blockLight)
  ```

  The **Subtracted Sky Light** is derived from the current **celestial angle** using the following procedure:

  ```
  World::caclulateSubtractedSkyLight()
  {
      var subtractedSkyLight := cos(celestialAngle * 2PI) * 2.0 + 0.5
      subtractedSkyLight := CLAMP(subtractedSkylight, 0, 1)
  
      // Both getRainStrength() and getThunderStrength() return 0 for clear skies.
      subtractedSkyLight := subtractedSkyLight * (1.0 - World.getRainStrength() * 5) / 16.0
      subtractedSkyLight := subtractedSkyLight * (1.0 - World.getThunderStrength() * 5) / 16.0
 
      return (1.0 - subtractedSkyLight) * 11.0
  }
  ```

  Once the brightness for an appropriate voxel has been determined.  Use it to index into the appropriate lookup table (based on the current dimension) listed at the bottom of the Lighting article.

  _TODO - Finish this step_

Clear the color buffer and depth buffer.  Fill the color buffer with the **atmosphere color**.

![](https://sr.ht/FYu9.png)

### Orient The Camera

Aside for some minor effects, such as view bobbing, Notch avoids translating the camera until after the sky has been rendered.

### Configure The Fog

As mentioned previously, Minecraft uses fog to blend the ceiling plane, the void plane, and distant objects into a seamless sky.

Set the fog color to the **atmosphere color** computed previously.

If the player entity is inside of a cloud... _TODO_

Otherwise, if the player entity is within water:
* set the fog mode to exponential
* set the fog density to 0.1

Otherwise, if the player entity is within lava:
* set the fog mode to exponential
* set the fog density to 2.0

Otherwise:
* set the fog mode to linear
* set the fog start to 0.
* set the fog end to 8/10 of the distance to the view frustum's far plane.

### Draw The Ceiling Plane

Disable textures. Disable writes to the depth buffer. Enable fog.

Prepare a 128x128 plane tessellated from four equally sized quads.  The plane should have extents which are [-64, 64] along the X and Z axis (so the anchor point lies in the center).  Set the ambient color of the plane to the **world sky color**.  Draw this plane at (0, 16, 0).  This should result in the _ceiling plane_ being centered above the camera.  However, if you translated the camera prior to this step, you will also need to translate the _ceiling plane_.  

_Note: I specifically avoid mentioning units here because they don't matter.  The ceiling plane has no distance relationship with any other objects in the scene._

![](https://sr.ht/1laT.png)

### Draw The Sunrise or Sunset

_TODO_

### Draw The Celestial Bodies

Disable fog.  Disable the alpha test. Disable writes to the depth buffer.  Enable textures.  Enable blending.  Set the blend source factor to _source alpha_.  Set the blend destination factor to _one_.

#### Sun

Prepare a 60x60 plane.  The plane should have extents of [-30, 30] along the X and Z axis (so the anchor point lies in the center).  Set the ambient color of the plane to the (1R, 1G, 1B, 1 - rainFactor), where rainFactor is a value between [0, 1] (rainFactor is 0 when not raining).  Texture the plane with ```/terrain/sun.png``` from the current resource pack.  Draw this plane at (0, 100, 0) rotated along the x-axis by 360 multiplied by the **celestial angle**.  If you translated the camera prior to this step, you will also need to translate the _sun_.  

#### Moon

Prepare a 60x60 plane.  The plane should have extents of [-20, 20] along the X and Z axis (so the anchor point lies in the center).  Set the ambient color of the plane to the (1R, 1G, 1B, 1 - rainFactor), where rainFactor is a value between [0, 1] (rainFactor is 0 when not raining).  Texture the plane with ```/terrain/moon.png``` from the current resource pack.  Draw this plane at (0, -100, 0) rotated along the x-axis by 360 multiplied by the **celestial angle**.  If you translated the camera prior to this step, you will also need to translate the _moon_.  

#### Stars

_TODO_

### Draw The Void Plane

Disable textures. Disable writes to the depth buffer. Disable blending. Enable fog.

Prepare a 128x128 plane tessellated from four equally sized quads.  The plane should have extents which are [-64, 64] along the X and Z axis (so the anchor point lies in the center).  

If the player entity is in the Nether, set the ambient color of the plane to the **world sky color**.  If the player entity is in the Overworld, set the ambient color of the plane to the **world sky color** after transforming each component using the following set of functions:

```
red = worldSkyColorRed * 0.2 + 0.04
green = worldSkyColorGreen * 0.2 + 0.04
blue = worldSkyColorBlue * 0.6 + 0.1
```

Draw this plane at (0, -16, 0).  This should result in the _void plane_ being centered below the camera.  However, if you translated the camera prior to this step, you will also need to translate the _void plane_.  

![](https://sr.ht/Qa5J.png)

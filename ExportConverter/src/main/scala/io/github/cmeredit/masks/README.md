# Masks

The `masks` package provides utilities for working with spatial arrays of Boolean flags encoded as one-dimensional collections of numeric values. Let's break this down:

### "Utilities for working with spatial arrays of Boolean flags ..."
If you run the `export-to-nbt.lua` script in the `MapExporter` project, the generated file will contain some information about your Dwarf Fortress map as a whole (e.g., width of the map in tiles) as well as flags on a per-tile basis. For example, with default parameters, the script generates `magmaMask`, which records whether magma is present in each tile. `Mask` objects are wrappers that help you manipulate and combine masks.

#### Getting Flag Values

You can get the flag value of a tile with 0-indexed coordinates `(x, y, z)` by calling `val myFlagValue: Int = myMask.getBit(x, y, z)` or `myFlagValue: Boolean = myMask.getBoolean(x, y, z)`.

#### Combining Masks

Common logical operations have been provided:

```scala
import io.github.cmeredit.masks.Mask

val magmaMask: Mask = ???
val walkableTileMask: Mask = ???

val funPlacesToWalk: Mask = magmaMask and walkableTileMask
```
Others include `or`, `xor`, and `iff`. A unary `neg` is provided for logical negation. If you have a more complicated logical operation, it is better to use the `applyOp` function to combine masks in a single go rather than chaining together the simpler mask operations. For example, if you want to find the tiles at which `mask1` being set implies `mask2` is also set:
```scala
import io.github.cmeredit.masks.Mask

val mask1: Mask = ???
val mask2: Mask = ???

// Only zips and traverses mask1 and mask2 once
val implies: (Short, Short) => Short = (s1, s2) => (~s1 | s2).toShort
val implFromFasterMethod: Mask = mask1.applyOp(mask2, implies)

// Traverses mask1 to perform negation, then zips mask2 with the result, then traverses again
val implFromSlowerMethod: Mask = mask1.neg() or mask2
```

#### Shifting Masks

Functions are provided to shift a mask up/down by a single tile in any of the cardinal directions. This is useful when you want your mask to depend on the mask values at nearby tiles. For example, let's say you want to find walkable tiles that have a wall directly to the right of them. You could achieve that with:
```scala
import io.github.cmeredit.masks.Mask

val walkableTileMask: Mask = ???
val openTileMask: Mask = ???

// Mask is set if the tile is walkable, but has a non-open tile to the right
val myFancyMask: Mask = walkableTileMask and openTileMask.shiftedDownX(BoundaryOptions.AllFalse).neg()
```
The function `shiftedDownX` shifts all mask values one x-coordinate lower. The leftmost yz plane of mask values is dropped from the shifted version. The rightmost yz plane of the shifted mask is filled according to the supplied `BoundaryOptions`. The rightmost yz plane of `openTileMask.shiftedDownX(BoundaryOptions.AllFalse)` consists entirely of `False` flags. Similarly, `BoundaryOptions.AllTrue` would yield all `True` flags, and `BoundaryOptions.CopyBoundary` retains whatever data was already present. Here's a visual representation with a mask that has a single xy layer:
```
mask -> mask.shiftedDownX(BoundaryOptions.AllFalse)
0000    0000
0001 -> 0010
1110    1100
0111    1110

mask -> mask.shiftedDownX(BoundaryOptions.AllTrue)
0000    0001
0001 -> 0011
1110    1101
0111    1111

mask -> mask.shiftedDownX(BoundaryOptions.CopyOriginal)
0000    0000
0001 -> 0011
1110    1100
0111    1111
```
In the above code example, `openTileMask` was shifted down in the x direction using the `AllFalse` option. Conceptually, this means we want to treat tiles off the right-hand edge of the map as non-open. A walkable tile at the right edge of the map would therefore show up in the `myFancyMask` mask. If we had instead used the `AllTrue` option, tiles off the right edge of the map would instead be considered open, so walkable tiles on the right edge of the map would not show up in `myFancyMask`.

### "... encoded as one-dimensional collections of numeric values"
Masks wrap a `Vector[Short]`. At first glance, it's tempting to store a spatial array of Boolean flags in some type of structure like
```scala
type XRow = Vector[Boolean]
type XYLayer = Vector[XRow]
v_3d = Vector[XYLayer]
```
However, in practice, this leads to messier and slower code (or at least it did for me). Something a bit more memory friendly is to instead just store the flags in a flat container:
```scala
v_1d = Vector[Boolean]
```
The correspondence between these structures is 
```scala
// Map x and y dimensions
val (D_x, D_y): (Int, Int) = ???
// Indices in 3D array
val (x, y, z): (Int, Int, Int) = ???

assert(v_3d(z)(y)(x) == v_1d(z*D_x*D_y + y*D_x + x))
``` 
and 
```scala
// Map x and y dimensions
val (D_x, D_y): (Int, Int) = ???
// Index in 1d array
val k = ???

assert(v_1d(k) == v_3d(k / (D_x * D_y), (k / D_x) % D_y, k % D_x))
```
To see how much cleaner code can be, think about what it would take to find where two masks are both true under the two representations. With the 1D representations, it's as simple as:
```scala
val v_1d: Vector[Boolean] = ???
val w_1d: Vector[Boolean] = ???

v_3d.zip(w_3d).map({case (v_flag, w_flag) => v_flag && w_flag})
```
In comparison, with 3D representations, this becomes:
```scala
val v_3d: Vector[Vector[Vector[Boolean]]] = ???
val w_3d: Vector[Vector[Vector[Boolean]]] = ???

v_3d.zip(w_3d).map({case (v_xyLayer, w_xyLayer) => 
  v_xyLayer.zip(w_xyLayer).map({case (v_xRow, w_xRow) =>
    v_xRow.zip(w_xRow).map({case (v_flag, w_flag) => v_flag && w_flag})
  })
})
```
We can do even better than a `Vector[Boolean]`, though! In my implementation, each group of 16 flag values is stored as the bits in a single `Short`. The benefit of this is that bitwise operations such as `&` or `~` are (or might as well be) atomic, so we can operate on 16 flag values simultaneously using `Short`s, whereas we would only be able to operate on one flag at a time using regular Boolean values. The reason `Short` is used rather than a larger numeric type is that Dwarf Fortress maps are composed of 16x16 chunks of tiles. In order to support fast spatial slicing, it's important that each x-row of flags fits in a whole number of auxiliary types. The highest number of bits that satisfies this is 16, which is exactly the number of bits in a `Short`.


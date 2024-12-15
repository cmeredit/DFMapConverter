# Tags

The `tags` package contains NBT tag definitions. This includes the standard NBT content (name+payload) but also a little utility. To see more, check out the `Tag` trait for overall structure, or a particular `Tag` inheritor for concrete implementation details.

You can instantiate a `Tag` as you would a regular case class. For example, 

```scala
import io.github.cmeredit.nbt.tags.{ByteTag, CompoundTag}

val myCompoundTag = CompoundTag(
  "root",
  Vector(
    ByteTag("child 1", 32.toByte),
    CompoundTag(
      "child 2",
      Vector(
        ByteTag("grandchild 1", 0.toByte),
        ByteTag("grandchild 2", 0.toByte)
      )
    ),
    ByteTag("child 3", 64.toByte)
  )
)

myCompoundTag.printPretty()
```

defines a compound tag with nested children and prints it. The print output should look something like:

```
Compound Tag root
  |- Byte Tag child 1: 32
  |  
  |- Compound Tag child 2
  |    |- Byte Tag grandchild 1: 0
  |    |- Byte Tag grandchild 2: 0
  |  
  |- Byte Tag child 3: 64
```

This sort of instantiation is fine for situations where you know in advance what you want in your tags (e.g., testing with a small amount of curated data). However, this usually isn't the case. The `{...}.nbt.IO` object offers various utilities for reading/writing tags from/to compressed and uncompressed .nbt files.
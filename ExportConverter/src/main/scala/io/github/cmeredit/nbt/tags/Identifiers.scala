package io.github.cmeredit.nbt.tags

// Record magic values in one place.
// Magic values are inherently ugly. I prefer to just write some icky laborious code one time and forget about it.
object Identifiers {

  // Type names that I personally like
  object TypeNames {
    val byte:      String = "Byte"
    val short:     String = "Short"
    val int:       String = "Int"
    val long:      String = "Long"
    val float:     String = "Float"
    val double:    String = "Double"
    val byteArray: String = "Byte Array"
    val string:    String = "String"
    val list:      String = "List"
    val compound:  String = "Compound"
    val intArray:  String = "Int Array"
    val longArray: String = "Long Array"
  }

  // Tag IDs specified by the .nbt file format
  object TagIDs {
    val byte:      Int = 1
    val short:     Int = 2
    val int:       Int = 3
    val long:      Int = 4
    val float:     Int = 5
    val double:    Int = 6
    val byteArray: Int = 7
    val string:    Int = 8
    val list:      Int = 9
    val compound:  Int = 10
    val intArray:  Int = 11
    val longArray: Int = 12
  }

  object Conversions {

    val tagIDToTypeName: Map[Int, String] = Map(
      TagIDs.byte      -> TypeNames.byte,
      TagIDs.short     -> TypeNames.short,
      TagIDs.int       -> TypeNames.int,
      TagIDs.long      -> TypeNames.long,
      TagIDs.float     -> TypeNames.float,
      TagIDs.double    -> TypeNames.double,
      TagIDs.byteArray -> TypeNames.byteArray,
      TagIDs.string    -> TypeNames.string,
      TagIDs.list      -> TypeNames.list,
      TagIDs.compound  -> TypeNames.compound,
      TagIDs.intArray  -> TypeNames.intArray,
      TagIDs.longArray -> TypeNames.longArray,
    )

    val typeNameToTagID: Map[String, Int] = tagIDToTypeName.map({case (tagID, typename) => (typename, tagID)})

  }

}

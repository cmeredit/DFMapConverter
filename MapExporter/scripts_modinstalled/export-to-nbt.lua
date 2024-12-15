
--[====[
export-to-nbt
===========

Export the current map to an uncompressed .nbt file. The output file will be found in dfhack-config/mods/MapExporter/ in your DF install folder.

This script is meant to be used in conjunction with the ExportConverter project, found within https://github.com/cmeredit/DFMapConverter/tree/release. ExportConverter provides utilities for generating .obj meshes from a map exported using export-to-nbt.

Calling export-to-nbt with no flags will start the export process immediately with default settings. You can specify optional flags catered to your eventual use of the ExportConverter project, found by executing export-to-nbt --help.

Usage
-----

    export-to-nbt
    export-to-nbt --help
    export-to-nbt [optional flags]
]====]

-- Get the state path to know where to save the output file.
local scriptmanager = require('script-manager')
local statePath = scriptmanager.getModStatePath('MapExporter')

-- Get arguments.
local args = {...}

-- Define script options.
local validArgs = {
    includeMagma = {
        shortForm = "-m",
        longForm = "--include-magma",
        description = "Indicates that tiles containing magma should be treated as open. Use this if you want accessible magma oceans included in your fort-only meshes.",
        flag = false
    },
    onlyOpen = {
        shortForm = "-o",
        longForm = "--only-open-tiles",
        description = "Indicates that you only want to export information about which tiles are open/solid. Use this if you don't care about making a fort-only mesh.",
        flag = false
    },
    help = {
        shortForm = "-h",
        longForm = "--help",
        description = "Print this help message.",
        flag = false
    },
    invalidated = {
        shortForm = nil,
        longForm = nil,
        description = "An invalid option was entered. Please review the following help message and try again.",
        flag = false
    }
}

-- Parses options from args and detects invalid options
local function parseArguments()
    for _, currentArgument in pairs(args) do

        -- Until we find a valid matching argument, assume the argument is invalid
        local argIsInvalid = true

        -- Check the current argument against our list of valid args
        for validArgKey in pairs(validArgs) do

            -- If we find a match, set its flag and mark that the arg was not actually invalid
            if (currentArgument == validArgs[validArgKey].shortForm or currentArgument == validArgs[validArgKey].longForm) then
                validArgs[validArgKey].flag = true
                argIsInvalid = false
            end

        end

        -- If the argument was invalid, bail out early and just print an invalid args + help message
        if argIsInvalid then
            validArgs.invalidated.flag = true
            break
        end

    end
end


-- Displays a help message with script options.
local function displayHelpMessage()

    print("")
    print("Usage:")
    print("export-to-nbt [optional flags]")

    print("")
    print("Optional flags:")
    print("----------------------------------------------------------------")
    for validArgKey, validArg in pairs(validArgs) do
        if (validArgKey ~= "invalidated") then
            print("")
            print(validArg.longForm .. ", " .. validArg.shortForm)
            print(validArg.description)
        end
    end
    print("")
    print("----------------------------------------------------------------")

end


-- Returns the last 4 bytes of valueInt in order, again as ints
local function getBytes(valueInt)

    local remaining = valueInt

    local byte0 = remaining % 256
    remaining = remaining // 256

    local byte1 = remaining % 256
    remaining = remaining // 256

    local byte2 = remaining % 256
    remaining = remaining // 256

    local byte3 = remaining % 256
    remaining = remaining // 256

    return byte3, byte2, byte1, byte0

end


local function main()

    -- Parse arguments, then return early if the user entered an invalid flag or the help flag
    parseArguments()
    if validArgs.invalidated.flag then
        print(validArgs.invalidated.description)
        displayHelpMessage()
        return
    end
    if validArgs.help.flag then
        displayHelpMessage()
        return
    end

    -- Set up output data table.
    -- Assume every value in outputData is a table with a name (string), type (string), and value (any).
    -- This will be enough information to write the nbt file.
    local outputData = {}

    -- I don't really know how to deal with Lua's universal use of tables :(
    -- The reason I don't just use string keys is that I'm going to *prefer* if the nbt files are written in the same
    -- order that I wrote data to outputData. I don't think the pairs(*) order matches the write order when
    -- using string table keys.
    local nextOutputDataIndex = 0
    local registeredOutputNames = {}
    local function getOutputDataFromName(nameStr)

        for i=0, #outputData do

            if (outputData[i].name == nameStr) then
                return outputData[i]
            end

        end

    end

    local function setByte(nameStr, valueByte)

        --print("Setting byte with name", nameStr, "and value", valueByte)

        -- Check to see if this is actually a new value. In *my* code it always will be, lol
        -- If this is a value with a new name, make an entry at the next available index in outputData
        if not registeredOutputNames[nameStr] then

            -- Create new entry
            outputData[nextOutputDataIndex] = {
                name = nameStr,
                type = "Byte",
                value = valueByte
            }

            -- Register name
            registeredOutputNames[nameStr] = true

            -- Prepare next entry index
            nextOutputDataIndex = nextOutputDataIndex + 1

        -- Name is already registered, so go update the entry that already exists
        else
            for i=0,#outputData do
                if outputData[i].name == nameStr then
                    outputData[i].type = "Byte"
                    outputData[i].value = valueByte
                    break
                end

            end
        end

    end

    local function appendToByteArray(nameStr, valueByte)

        -- Similarly to setByte, check if this name has already been used and act accordingly.
        -- Also, if the name is registered, but to something other than a byte array, override that value.
        if (not registeredOutputNames[nameStr]) or (getOutputDataFromName(nameStr).type ~= "ByteArray") then

            -- Create new entry
            local newEntry = {
                name = nameStr,
                type = "ByteArray",
                value = {
                    writeIndex = 0,
                    data = {}
                }
            }
            newEntry.value.data[newEntry.value.writeIndex] = valueByte
            newEntry.value.writeIndex = newEntry.value.writeIndex + 1

            -- Set new entry in output data
            outputData[nextOutputDataIndex] = newEntry

            -- Register name
            registeredOutputNames[nameStr] = true

            -- Prepare next entry index
            nextOutputDataIndex = nextOutputDataIndex + 1

        else

            -- Name is already registered to a byte array. Append valueByte to that array and update its write index
            for i=0,#outputData do
                if outputData[i].name == nameStr then
                    outputData[i].value.data[outputData[i].value.writeIndex] = valueByte
                    outputData[i].value.writeIndex = outputData[i].value.writeIndex + 1
                    break
                end
            end

        end

    end



    -- Get and set map dimensions
    local xMax, yMax, zMax = dfhack.maps.getTileSize()
    setByte("x", xMax)
    setByte("y", yMax)
    setByte("z", zMax)


    -- Based on the user's flags, change which data we copy / how we copy it
    -- It's probably more intuitive to check the flags within the copying loop, but that's quite a bit slower
    -- This way, we only need to read our flags one time. That should help a tiny bit with speed.
    local byteManagers = {}
    local dataRetrievalFuncs = {}
    local typesOfDataToRetrieve = {}

    local function registerDataOutput(nameStr, dataRetrievalFunc)

        local function registerByteManager(nameStr)
            byteManagers[nameStr] = {
                numBitsWritten = 0,
                currentByte = 0
            }
        end
        local function registerDataRetrievalType(nameStr)
            typesOfDataToRetrieve[nameStr] = true
        end


        registerDataRetrievalType(nameStr)
        registerByteManager(nameStr)
        dataRetrievalFuncs[nameStr] = dataRetrievalFunc
    end

    -- Counterintuitively, if we want to include magma in the output meshes produced by ExportConverter, then were
    -- actually do NOT want to write any magma data. The reason is that ExportConverter, by default, uses
    -- the magma mask to exclude magma tiles. Without a magma mask, it is configured to assume there is no magma on the
    -- map, thereby excluding nothing.
    --
    -- In summary, if the includeMagma flag is set, that means we DO NOT need to write magma data.
    -- If the flag is not set, then magma needs to be taken into account in the mesh conversion, so we DO need to write magma data
    if not validArgs.includeMagma.flag then
        -- We need to write magma data so it can be excluded from the generated meshes

        local dataRetrievalFunc = function(x_coord, y_coord, z_coord)
            local designation, _ = dfhack.maps.getTileFlags(x_coord, y_coord, z_coord)
            -- designation[0]: liquid level.
            -- designation[21]: liquid type. Set to true if there is magma present (or in weird cases, seemingly randomly? e.g., I've seen a tile with a door no liquid have this set to "true")
            -- Magma is present in the specified tile iff the tile has liquid of type "true"
            return designation[21] and (designation[0] > 0)
        end

        registerDataOutput("magmaMask", dataRetrievalFunc)

    end

    -- In its default mode, this script assumes that we need to generate a fort-only mesh (as well as maybe an open-spaces mesh).
    -- The flood fill algorithm in ExportConverter needs to know which tiles are walkable and which are vertically passable (and needs to distinguish these).
    -- If the user is not interested in generating a fort-only mesh for whatever reason, then we don't need to record the walkable and v-passable
    -- info separately! The tiles considered "open" are exactly those that are walkable or vertically passable, so if the onlyOpen flag is set,
    -- then we can record the disjunction of these in a single byte array.
    if validArgs.onlyOpen.flag then
        -- Only need to record an "open" byte array

        local dataRetrievalFunc = function(x_coord, y_coord, z_coord)
            local tileType = dfhack.maps.getTileType(x_coord, y_coord, z_coord)
            local tileTypeAttributes = df.tiletype.attrs[tileType]
            local tileShapeAttributes = df.tiletype_shape.attrs[tileTypeAttributes.shape]
            return (tileShapeAttributes.walkable or tileShapeAttributes.passable_flow_down)
        end

        registerDataOutput("openTileMask", dataRetrievalFunc)

    else
        -- Need to record walkable and v-passable separately

        local walkableDataRetrievalFunc = function(x_coord, y_coord, z_coord)
            local tileType = dfhack.maps.getTileType(x_coord, y_coord, z_coord)
            local tileTypeAttributes = df.tiletype.attrs[tileType]
            local tileShapeAttributes = df.tiletype_shape.attrs[tileTypeAttributes.shape]
            return tileShapeAttributes.walkable
        end

        registerDataOutput("walkableMask", walkableDataRetrievalFunc)


        local pfdDataRetrievalFunc = function(x_coord, y_coord, z_coord)
            local tileType = dfhack.maps.getTileType(x_coord, y_coord, z_coord)
            local tileTypeAttributes = df.tiletype.attrs[tileType]
            local tileShapeAttributes = df.tiletype_shape.attrs[tileTypeAttributes.shape]
            return tileShapeAttributes.passable_flow_down
        end

        registerDataOutput("passableFlowDownMask", pfdDataRetrievalFunc)

    end




    -- Copy relevant game data into outputData
    -- Note that the number of blocks in a dwarf fortress map is always divisible by 8, so we don't need to
    -- worry about having lingering bits to write at the end of this loop. E.g., if a map somehow had 9 tiles,
    -- at the end of this loop we would have only written 9//8=1 byte (one missing bit!)
    for z = 0, zMax - 1 do
        for y = 0, yMax - 1 do
            for x = 0, xMax - 1 do
                for dataOutputName in pairs(typesOfDataToRetrieve) do

                    -- Get next bit and current byte so far
                    local nextBit = dataRetrievalFuncs[dataOutputName](x, y, z)
                    local currentByte = byteManagers[dataOutputName].currentByte
                    local numBitsWritten = byteManagers[dataOutputName].numBitsWritten

                    -- Always assume that the byte manager has left room for the next bit!
                    -- It's our responsibility to make sure of this.

                    -- Write the next byte and update bit count
                    currentByte = currentByte + (nextBit and 1 or 0)
                    numBitsWritten = numBitsWritten + 1

                    -- If the current byte has been filled, then write it to the appropriate output array,
                    -- then reset the byte manager.
                    --
                    -- If the current byte has not been filled, then bitshift the current byte to make room
                    -- for the next bit and then write back to the byte manager
                    if numBitsWritten == 8 then
                        -- Write to the appropriate output array
                        appendToByteArray(dataOutputName, currentByte)
                        -- Reset byte manager
                        byteManagers[dataOutputName].currentByte = 0
                        byteManagers[dataOutputName].numBitsWritten = 0
                    else
                        -- Bitshift currentByte to the left to make room for the next bit.
                        currentByte = currentByte * 2
                        byteManagers[dataOutputName].currentByte = currentByte
                        byteManagers[dataOutputName].numBitsWritten = numBitsWritten
                    end


                end
            end
        end
    end








    -- Open, write, and close output file ---------------------

    -- Set up output filename. Always include a datetime string. Include a nonempty options string if option flags were set.
    local time = os.date("*t")
    local datetimeStr = time.year .. "-" .. time.month .. "-" .. time.day .. " " .. time.hour .. ":" .. time.min .. ":" .. time.sec
    local optionsStr = ""
    if validArgs.includeMagma.flag then optionsStr = optionsStr .. " -m" end
    if validArgs.onlyOpen.flag then optionsStr = optionsStr .. " -o" end
    if optionsStr ~= "" then optionsStr = " - with options" .. optionsStr end

    -- Open
    local f = io.open(statePath .. "/df map export - " .. datetimeStr .. optionsStr .. ".nbt", "wb")

    local function writeByte(byteInt)
        f:write(string.char(byteInt))
    end


    -- Set up Compound tag
    -- 10 is the magic number for the Compound tag type ID
    writeByte(10)
    -- I'm going to just call the compound tag "root", which has four characters.
    -- The .nbt format expects 2 unsigned bytes to indicate the name length
    writeByte(0)
    writeByte(4)
    f:write("root")

    -- Write actual output data
    for i=0,#outputData do
        local v = outputData[i]

        -- Write Tag Type ID byte
        if v.type == "Byte" then

            -- 1 is the magic number for byte tags
            writeByte(1)

        elseif v.type == "ByteArray" then

            -- 7 is the magic number for byte array tags
            writeByte(7)

        end


        -- Write the name length bytes
        -- Ints are 64 bits / 8 bytes in Lua. The .nbt format expects 2 unsigned bytes to indicate the name length
        local _, _, highByte, lowByte = getBytes(string.len(v.name))
        writeByte(highByte)
        writeByte(lowByte)

        -- Write the name bytes
        f:write(v.name)

        -- Write the payload
        if v.type == "Byte" then

            -- Single byte in payload
            writeByte(v.value)

        elseif v.type == "ByteArray" then

            -- Payload consists of 4 bytes indicating the array length, then the bytes of the array.
            local byte3, byte2, byte1, byte0 = getBytes(v.value.writeIndex)
            writeByte(byte3)
            writeByte(byte2)
            writeByte(byte1)
            writeByte(byte0)
            for i=0,#v.value.data do
                writeByte(v.value.data[i])
            end

        end

    end

    -- Write the TAG_End tag ID
    writeByte(0)

    -- Close
    f:close()
end

main()
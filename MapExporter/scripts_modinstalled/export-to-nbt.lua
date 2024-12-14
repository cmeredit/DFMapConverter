
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



local function main()
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

end

main()
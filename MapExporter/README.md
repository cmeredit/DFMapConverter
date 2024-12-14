# MapExporter

This Lua project offers a script, `export-to-nbt.lua`, to dump a Dwarf Fortress map into an uncompressed .nbt file.

To learn more about standalone DFHack scripts, please see the [relevant section of the modding guide](https://docs.dfhack.org/en/stable/docs/guides/modding-guide.html#what-if-i-just-want-to-distribute-a-simple-standalone-script). 

To learn more about the DFHack Lua API, see the [reference manual](https://docs.dfhack.org/en/stable/docs/dev/Lua%20API.html) and the [DF-structures](https://github.com/DFHack/df-structures/tree/master) page and corresponding [Data Structure Definition Syntax](https://github.com/DFHack/df-structures/blob/master/SYNTAX.rst).

To learn more about [Lua](https://www.lua.org/), see the [reference manual](https://www.lua.org/manual/5.4/).

## Installation
1. Install [DFHack](https://docs.dfhack.org/en/stable/index.html). If you're using Steam, just install its [Steam version](https://store.steampowered.com/app/2346660/?snr=1_5_9__205). It will automatically launch once you run Dwarf Fortress.
2. Locate your Dwarf Fortress install folder. If using Steam:
   1. Right click the game in your Steam library.
   2. Click "Properties..."
   3. Click "Installed Files"
   4. Click "Browse"
   5. You're now in your DF install folder!
3. You now have two options to install the script. You do not need to do both.
   1. Option 1: Copy this entire folder (`MapExporter`) to `data/installed_mods/` in your DF install folder.
   2. Option 2: Open `dfhack-config/script-paths.txt` and add `+/[path to this project]/DFMapConverter/MapExporter/scripts_modinstalled` in a new line at the end of the file.

## Usage
1. Run Dwarf Fortress & DFHack, then open the DFHack console.
2. Open one of your saved games.
3. Choose a destination path and name for the exported file. Sorry for making you type a whole path, but I'm not sure how to use DFHack/Lua to get the path to the install folder :(
4. Execute `export-to-nbt [your full path] [your filename without file extension] [optional flags]` in the DFHack console. This will generate an uncompressed `.nbt` file with the information necessary for building nice `.obj` meshes from your map.
   1. By default, magma is excluded from generated fort meshes. This is because magma oceans often obscure interesting fort details if you have a deep magma smelter. If you want magma included in your fort meshes, set the `--include-magma` flag.
   2. By default, this script assumes you want to be able to generate a mesh of just your fort (rather than all open spaces in your map, such as caves). If you don't care about getting a mesh of just your fort, you can set the `--only-open-tiles` flag to get a smaller file.
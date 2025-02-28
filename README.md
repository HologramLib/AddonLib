# AddonLib

AddonLib is a library that helps you manage optional addons for your Minecraft plugin, ensuring compatibility and simplifying installation and updates.

## Why Use AddonLib?

- **Compatibility checks**: Ensures **addons** match your plugin's version.
- **Easy Updates**: Automatic updates for all addons.
- **Centralized Management**: Track addons via a simple **config** file.
- **User Friendly**: Users can install **addons** without manual downloads.

### Definitions

- **Main Plugin**: Your plugin (e.g., HologramLib) that others can extend with addons.
- **Addon**: A separate plugin that depends on your main plugin to work.
- **Registry**: A public list of all available addons (hosted online as a JSON file).
- **Config**: A file (`addons.json`) created by AddonLib to track installed addons and settings.

### Registry Structure
The **registry** is a JSON file hosted online. It contains:
- `baseURL`: The root URL where addons are stored (e.g., a GitHub organization).
- `addons`: A list of addons with descriptions and version compatibility.

Example **Registry** ([here is the entire HologramLib registry](https://raw.githubusercontent.com/HologramLib/Addons/main/registry.json)):
````json
{
   "baseURL":"https://github.com/HologramLib/",
   "addons":{
      "Commands":  { /*addon name (Same as the repository name)*/
         "description":"Commands for controlling holograms.",
         "versions":{
            "2.1.0" :"1.7.1", // addon v2.1.0 works with main plugin v1.7.1
            "2.0.0":"1.1.5",
            "1.9.0":"1.6.9",
            "1.8.0":"1.6.6"
         }
      }
   }
}
````

## Setup

> [!IMPORTANT]  
> Addon JARs must follow the naming convention `<addonName>-<version>.jar`  
> Example: CommandsAddon-1.0.0.jar

Shade AddonLib into your **main plugin**:

Example implementation
````java
AddonLib addonLib = new AddonLib(new Logger() {
  @Override
  public void log(LogLevel logLevel, String message) {
    // for example: Bukkit.getLogger().log(toJavaUtilLevel(logLevel), message);
  }
}, plugin.getDataFolder(), plugin.getDescription().getVersion())
   .setEnabledAddons(new String[]{"Commands"})
   .setRegistry("<your.url/registry.json>")
   .setBackupRegistry("<your-backup.url/registry.json>") /* Set this field! If the default registry fails, it will use the backup registry of HologramLib if this is not set. */
   .init();


/*
public static Level toJavaUtilLevel(Logger.LogLevel logLevel) {
     return switch (logLevel) {
        case INFO -> Level.INFO;
        case SUCCESS -> Level.FINE;
        case WARNING -> Level.WARNING;
        case ERROR -> Level.SEVERE;
     };
}
 */
````

Reloading the **config**:
`````java
/*If you set upgrade to true, all addons will be also updated to the latest compatible version*/
addonLib.reload(<upgrade>);
`````

AddonLib will generate a file called `addons.json` into your `plugins/<main plugin name>` folder
It will look like this:
````json
{
  "addons": {
    /*These addons are fetched from the registry*/
    "Commands": { 
      "enabled": true,
      "installedVersion": "2.1.0", /*This line will only be added when that addon is enabled and successfully downloaded*/
      "description": "Commands for controlling holograms."
    }
  },
  "settings": {
    "autoUpgrade": false /*If true, all addons will be updated to the latest compatible version on startup*/
  }
}
````


## Usage Examples

### List Installed Addons
Add a command to display **addon** statuses ingame:
```java
for (Map.Entry<String, AddonEntry> entry : addonLib.getConfig().getAddonEntries().entrySet()) {
  String status = entry.getValue().isEnabled() 
    ? ChatColor.GREEN + "Enabled" 
    : ChatColor.RED + "Disabled";
  
  sender.sendMessage(ChatColor.YELLOW + "- " + entry.getKey() + " v" + 
    entry.getValue().getInstalledVersion() + ": " + status);
  sender.sendMessage(ChatColor.GRAY + "  " + entry.getValue().getDescription());
}
```

## How AddonLib Manages Addons

### On Startup:
1. Creates `addons.json` **config** if missing.
2. Fetches the latest **registry** to check for new or updated addons.
3. Validates installed **addons**:
  - Removes addons incompatible with the current **main plugin** version.
  - Downloads missing **addons** or updates them to latest compatible version if `settings.autoUpgrade` in **config** is enabled.
4. Ensures only versions specified in the **config** are active by checking the plugins folder for .jars and downloads/removes addons based on the **config**.

### During Reload:
Using `addonLib.reload(<upgrade>);` from the AddonLib api:
- Refreshes the **registry** and updates **addons** based on the `upgrade` parameter:
  - **`upgrade=true`**: Updates all **addons** to their latest compatible versions.
  - **`upgrade=false`**: Adds new **addons** but keeps existing versions.

---

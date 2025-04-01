# OtterVaults

This plugin is a proof of concept example of playervaults for OtterSMP.<br>

# Commands
* `/vaults [player] [#]`
    * Aliases: /ottervaults, /playervaults, /pv
    * Opens the vault of the player's specified by the number.
    * If a player is not specified, the command sender will be used instead.
    * If no number is specified, the vaults of the player will be listed (or the command sender as mentioned above).


# Permissions

| Permission                    |                                                                               Description                                                                               |
|-------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| `ottervaults.use`             |                                                                        Grants access to /vaults.                                                                        |
| `ottervaults.admin`           |                                             Grants access to /vaults, opening others' vaults, and inherits bypassblacklist.                                             |
| `ottervaults.bypassblacklist` |                                                            Allows bypassing the item blacklist (if enabled).                                                            |
| `ottervaults.amount.<#>`      | Allows accessing all vaults from 1 to the number specified in the permission.<br/>Note that numbers higher than the `max_vault_count` in the config will not be checked |

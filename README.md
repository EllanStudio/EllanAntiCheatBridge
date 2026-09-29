# EllanAntiCheatBridge

EllanAntiCheatBridge is a cross-server anti-cheat alert bridge for Vulcan and
GrimAC on Paper networks.

It captures anti-cheat violations on backend servers, normalizes them into a
single protocol, sends them through a Velocity plugin message channel, and
broadcasts them to staff on every backend server.

## Features

- Vulcan `VulcanPostFlagEvent` and `VulcanPunishEvent` support.
- GrimAC `FlagEvent` support through the official event bus.
- Cross-server delivery through Velocity.
- Per-player `/ellanac alerts` toggle.
- Permission-based alert visibility.
- Configurable source filters, minimum VL, ignored checks and deduplication.
- Rate limiting and packet validation on the proxy.
- No bundled Vulcan or GrimAC binary dependencies.

## Modules

| Module | Output | Install on |
|---|---|---|
| `paper` | `EllanAntiCheatBridge-Paper-1.0.0.jar` | Spawn, Survival, Redstone, Test |
| `velocity` | `EllanAntiCheatBridge-Velocity-1.0.0.jar` | Velocity |

## Requirements

- Java 25
- Paper 26.2 or compatible fork
- Velocity 3.4 or newer
- Vulcan and/or GrimAC on the backend servers

## Installation

1. Put the Paper JAR into the `plugins` directory of every backend server that
   runs Vulcan or GrimAC.
2. Put the Velocity JAR into the Velocity `plugins` directory.
3. Start the proxy and backend servers once.
4. Configure `plugins/EllanAntiCheatBridge/config.yml` on each backend.
5. Configure `plugins/ellan-anticheat-bridge/config.properties` on Velocity.
6. Give staff `ellan.anticheat.notify`.

The default Paper configuration does not display alerts locally. The Velocity
plugin broadcasts the normalized alert back to every backend, including the
source server, so staff receive one unified alert regardless of where they are
playing.

If native Vulcan/Grim alerts are still visible to staff, either disable the
native alert permission on the matching groups or tolerate the duplicate local
alert.

## Commands

### Backend

```text
/ellanac alerts
/ellanac status
/ellanac reload
/ellanac test
```

### Velocity

```text
/ellanac status
/ellanac reload
/ellanac test
```

## Permissions

```text
ellan.anticheat.notify
ellan.anticheat.admin
```

## Paper Configuration

```yaml
server-name: auto
permission: ellan.anticheat.notify

# false when using the Velocity bridge, otherwise the source server can
# receive both its native alert and the bridged alert
display-locally: false
forward-to-proxy: true

filters:
  minimum-vl: 1.0
  ignored-checks: []

sources:
  vulcan: true
  vulcan-punishments: true
  grim: true

network:
  dedupe-millis: 350
```

## Building

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

Build outputs:

```text
paper/build/libs/EllanAntiCheatBridge-Paper-1.0.0.jar
velocity/build/libs/EllanAntiCheatBridge-Velocity-1.0.0.jar
```

## Compatibility Notes

Vulcan and GrimAC can both observe transaction packets. Vulcan's
`connection.other-anticheat-support` setting must be enabled when another
packet-based anti-cheat is installed.

The plugin uses reflection for Vulcan and GrimAC integration so the repository
does not redistribute proprietary Vulcan binaries. GrimAC integration follows
its public event bus API.

## License

MIT

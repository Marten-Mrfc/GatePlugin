# GatePlugin

GatePlugin is a Minecraft server plugin built using Kotlin and Paper API for Minecraft 1.21.1. This plugin provides gate management functionality for servers, with configurable break item restrictions.

## Features

- Gate management system
- Configurable item break restrictions
- Custom configuration options

## Requirements

- Java 21
- Paper 1.21.1
- KingdomCraft (optional dependency)
- WorldEdit (optional dependency)

## Installation

1. Download the latest release JAR file
2. Place the JAR in your server's plugins folder
3. Restart your server

## Configuration

The main configuration file is located at `plugins/gateplugin/config.yml`. Example configuration:

```yaml
breakItems:
  - "DIAMOND_SWORD:"
  - "PAPER:2"
```

## Building from Source

This project uses Gradle as the build system:

```bash
./gradlew build
```

To build and deploy to the test server:

```bash
./gradlew buildAndMove
```

## License

This project is licensed under a modified MIT license. See the [LICENSE.md](LICENSE) file for details.

© 2025 Marten_mrfcyt
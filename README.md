# MTR-34

## Setting up the development environment

It is recommended to use IntelliJ IDEA with the [manifold-ij](https://plugins.jetbrains.com/plugin/10057-manifold-ij) plugin.

Create a `build.properties` file and one of the following lines depending on your desired Minecraft version:

| Minecraft Version | Value                  |
|-------------------|------------------------|
| 1.17.1            | ```MC_VERSION=11701``` |
| 1.18.2            | ```MC_VERSION=11802``` |
| 1.19.2            | ```MC_VERSION=11902``` |
| 1.19.3            | ```MC_VERSION=11903``` |
| 1.19.4            | ```MC_VERSION=11904``` |
| 1.20.1            | ```MC_VERSION=12001``` |

Then run `./gradlew setupLibrary`.

## Building 
After setting up the environment, run `./gradlew build`.
# Redis Server Implementation

This project is a custom implementation of a Redis server in Java. It supports basic Redis commands and includes functionality for handling streams using the `XADD` command.

## Features

- **Basic Redis Commands**: Supports commands like `PING`, `ECHO`, `SET`, `GET`, `CONFIG`, `KEYS`, `INFO`, `REPLCONF`, `PSYNC`, `WAIT`, and `TYPE`.
- **Stream Support**: Implements the `XADD` command for adding entries to streams.
- **Thread Management**: Uses a thread pool for handling client connections.
- **Logging**: Utilizes the `Logger` framework for logging.
- **Singleton Pattern**: Ensures `RedisServer` is a singleton.
- **Error Handling**: Improved error handling and response.

## Getting Started

### Prerequisites

- Java 8 or higher
- Maven

### Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/suryasolurgm/redis-server.git
    cd redis-server
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```

### Running the Server

To start the Redis server, run the `Main` class:

```sh
java -cp target/redis-server-1.0-SNAPSHOT.jar Main

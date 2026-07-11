# Task Scheduler & IDE Automation Plugin

An advanced, production-grade IntelliJ Platform Plugin designed specifically for Android Studio. It allows developers to schedule and automate complex IDE actions, saved macros, terminal commands, and external scripts seamlessly based on delays or recurring intervals.

## Architecture

The plugin is architected using **SOLID** and **Clean Architecture** principles:

- **`com.scheduler.model`**: Fully decoupled business models and enums representing tasks, task statuses, execution history, and scheduling types.
- **`com.scheduler.scheduler`**: Task execution orchestrator using the strategy pattern. Supports running custom `TaskExecutor`s on background workers cleanly without blocking the main event-dispatch UI thread.
- **`com.scheduler.persistence`**: Utilizes IntelliJ Platform's `PersistentStateComponent` to write XML configurations of tasks, historical logs, and settings to the project root, persisting them across IDE launches and rebuilds safely.
- **`com.scheduler.ui`**: Responsive, native IntelliJ Swing tool window panels utilizing custom Action systems and standard components.

## Target Platform Requirements

- **IntelliJ Idea / Android Studio**: 2023.3+ (Iguana, Jellyfish, Koala, Ladybug)
- **JVM Runtime**: Java 17+
- **Kotlin Standard Library**: 1.9.0+

## Getting Started

### 1. Build from Source
To build the plugin and install it locally, run the Gradle wrapper in your terminal:
```bash
./gradlew buildPlugin
```
The generated plugin package will be available at:
```
build/distributions/task-scheduler-plugin-1.0.3.zip
```

### 2. Manual Installation
1. Open **Android Studio** (or **IntelliJ IDEA**).
2. Go to **Settings/Preferences** `Ctrl+Alt+S` (or `Cmd+,` on macOS).
3. Select **Plugins** from the left sidebar.
4. Click the gear icon (⚙️) on the top right, select **Install Plugin from Disk...**.
5. choose the compiled `task-scheduler-plugin-1.0.3.zip` file from your build output directory.
6. Restart Android Studio.

## Usage Guide
Once installed, a new Tool Window icon titled **Task Scheduler** (🟢) will appear on the **right-hand vertical sidebar** of Android Studio.

1. **Creating Tasks**: Click `New Task` (+) inside the toolbar. Name your task and choose the type (e.g., Terminal Command `./gradlew assembleDebug`).
2. **Scheduling**: Set your initial delay or check "Repeat continuously" and enter a sleep loop frequency in seconds.
3. **Execution Controls**: Highlight any task from the list and use the media-bar controls to **Start**, **Pause**, **Stop**, or trigger **Run Now** (forces execution ignoring timers).
4. **Inspecting Outputs**: Switch to the **History** tab to see real-time status output codes, logs, and process printouts.

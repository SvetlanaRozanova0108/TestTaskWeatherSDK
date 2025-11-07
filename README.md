# **Test Task Weather SDK**

## Overview

Weather SDK is a lightweight and high-performance Java library for interacting with the OpenWeather API. Designed with efficiency and scalability in mind, it eliminates Spring dependencies while leveraging WebClient for HTTP communication, Caffeine for caching.

## Features

High Performance: Uses WebClient for non-blocking HTTP requests and supports multi-threading.
Caching: Uses Caffeine for efficient caching of weather data.
Polling Mode: Automatically updates cached cities at a configurable interval.
On-Demand Mode: Fetches weather data only when requested.
Validation: DTO validation via Jakarta and Hibernate.
Logging: SLF4J integration for structured logging.
Custom Exceptions: Provides detailed exception handling for robust error management.

## Installation

To use Test Task Weather SDK, include the following dependency in your Maven project:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>weather-sdk</artifactId>
    <version>1.0.7</version>
</dependency>
```

## Getting Started

### Creating an Instance

Instances of `TestTaskWeatherSdk` must be created using `WeatherSdkFactory`:

```java
WeatherSdk sdk = WeatherSdkFactory.getInstance("1234567", ModeSDK.POLLING_MODE, 60);
```
- The first parameter is the **API key**.
- The second parameter is the **update mode** (`POLLING_MODE` or `ON_DEMAND_MODE`).
- The third parameter is the **polling interval (seconds)**, which must be `0` for `ON_DEMAND_MODE` mode or greater than `0` for `POLLING_MODE` mode.

### Removing an Instance

```java
WeatherSdkFactory.removeInstance("1234567");
```
This ensures that multiple instances with the same API key are not created.

### Fetching Weather

```java
String weatherJson = sdk.getWeather("Paris");
```
This method retrieves the latest weather data for a given city in JSON format.

### Updating Weather

```java
sdk.updateWeather("Paris");
```
Forces an update for the specified city.

### Managing Cache

```java
List<String> cachedCities = sdk.getCachedCities();
sdk.clearCache();
```
- `getCachedCities()` retrieves the list of currently cached cities.
- `clearCache()` removes all cached data.

### Controlling Polling Mode

```java
boolean pollingActive = sdk.isPollingEnabled();
sdk.stopPolling();
```
- `isPollingEnabled()` checks if polling is active.
- `stopPolling()` disables periodic updates.

### Shutting Down SDK

```java
sdk.shutdown();
```
Releases all resources and stops background operations.

## Exception Handling

The SDK provides custom exceptions for robust error handling:

- `ApiKeyBlockedException` – API key has been blocked by OpenWeather.
- `CityNotFoundException` – Requested city not found.
- `InvalidApiKeyException` – API key is invalid.
- `InvalidCityException` – Provided city name is invalid.
- `JsonParsingException` – JSON response could not be parsed.
- `NetworkException` – Network-related issues occurred.
- `SdkShutdownException` – SDK is used after being shut down.
- `UnexpectedApiException` – Unexpected error from OpenWeather API.
- `WeatherSdkException` – Generic exception for the Weather SDK.

## Architecture

The SDK is built with modular components:

- **`WeatherSdkImpl`** (Facade class) – Exposes all public methods.
- **`OpenWeatherApiClient`** – Handles API communication.
- **`WeatherCacheManager`** – Manages caching logic.
- **`WeatherUpdater`** – Handles polling-based updates.
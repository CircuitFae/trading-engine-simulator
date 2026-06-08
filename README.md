# Algorithmic Trading Engine Simulator

A production-style **Java stock exchange simulator** built from scratch using Object-Oriented Design, design patterns, and thread-safe concurrency. It models order books, limit/market order matching, trade execution, and real-time portfolio settlement — all in memory, with no external dependencies.

---

## Features

- **Multi-asset exchange** — independent order books per symbol (AAPL, TSLA, etc.)
- **Order types** — `LimitOrder` and `MarketOrder` (buy/sell)
- **Order book** — bid max-heap + ask min-heap with price-time priority
- **Matching strategies** — FIFO (default) and Pro-Rata (swappable at runtime)
- **Portfolio management** — cash balances and share holdings updated on every trade
- **Design patterns** — Strategy (matching) and Observer (portfolio updates)
- **Thread-safe** — `ReentrantLock`, `ConcurrentHashMap`, `AtomicLong`
- **Two interfaces** — console (`Main`) and GUI (`MainGUI`)

---

## Quick Start

### Prerequisites

- **JDK 17+** (Java 25 also works)
- Check installation:
  ```powershell
  java -version
  javac -version
  ```

### Run Console Simulation

```powershell
cd "D:\Final Year Projects\trading-engine-simulator"
.\run.bat
```

### Run GUI Simulation

```powershell
.\run_gui.bat
```

Click **RUN SIMULATION** in the window.

### Run from IDE

Open the project folder in Cursor / VS Code / IntelliJ, then run:

| Class | Description |
|-------|-------------|
| `com.tradingengine.Main` | Terminal output |
| `com.tradingengine.MainGUI` | Swing GUI window |

---

## Project Structure

```
trading-engine-simulator/
├── pom.xml                          # Maven build (Java 17)
├── README.md                        # This file
├── PROJECT_REPORT.md                # In-depth technical report
├── run.bat / run.ps1                # Console launcher
├── run_gui.bat / run_gui.ps1        # GUI launcher
└── src/main/java/com/tradingengine/
    ├── Main.java                    # Console entry point
    ├── MainGUI.java                 # GUI entry point
    ├── enums/
    │   └── OrderSide.java
    ├── domain/
    │   ├── Asset.java, Stock.java, Option.java
    │   ├── Order.java, LimitOrder.java, MarketOrder.java
    │   ├── Trade.java, Portfolio.java
    ├── orderbook/
    │   └── OrderBook.java
    ├── strategy/
    │   ├── MatchingStrategy.java
    │   ├── FIFOMatchingStrategy.java
    │   └── ProRataMatchingStrategy.java
    ├── observer/
    │   └── OrderBookObserver.java
    ├── portfolio/
    │   └── PortfolioManager.java
    └── engine/
        ├── TradingEngine.java
        └── OrderIdGenerator.java
```

---

## How It Works

```
Orders (Main / GUI / threads)
        │
        ▼
  TradingEngine
        │
        ├──► OrderBook (per symbol)
        │         │
        │         ├── bids (PriorityQueue — max-heap)
        │         ├── asks (PriorityQueue — min-heap)
        │         └── MatchingStrategy → Trades
        │
        └──► PortfolioManager (Observer)
                  └── updates cash & holdings
```

1. User submits a buy or sell order.
2. `TradingEngine` validates cash/shares via `PortfolioManager`.
3. `OrderBook` matches against the opposite side when prices cross.
4. Trades are created and portfolios are updated automatically.
5. Final balances are printed / shown in the GUI.

---

## Simulation Data

All data is **hardcoded for demonstration** — no database or live market API.

| Item | Value |
|------|-------|
| Assets | AAPL @ $150, TSLA @ $250 |
| Users | `user-1` ($100k), `user-2` ($100k + shares), `bot-1` ($50k) |
| Orders | Defined in `Main.java` / `MainGUI.java` |
| IDs | Auto-generated (`O-1`, `T-1`, …) |
| Timestamps | `LocalDateTime.now()` |

---

## Sample Output

```
=== Algorithmic Trading Engine Simulator ===

[ENGINE] Registered asset AAPL (Apple Inc.) @ $150.00
[ORDER] id=O-1 | user=user-1 | side=BUY | symbol=AAPL | qty=100 | limit=$151.00
[TRADE] id=T-1 | symbol=AAPL | qty=80 | price=$151.00 | buyer=user-1 | seller=user-2
[PORTFOLIO-UPDATE] trade=T-1 | buyer=user-1 debited $12080.00 + 80 AAPL | seller=user-2 credited ...
=== Simulation Complete ===
```

**GUI tags:** green = buy, red = sell, amber = trade, purple = portfolio, blue = engine.

---

## OOP & Design Patterns

| Concept | Where |
|---------|--------|
| Abstraction | `Asset`, `Order` |
| Inheritance | `Stock`, `LimitOrder`, `MarketOrder` |
| Encapsulation | `Portfolio` (locked cash/holdings) |
| Polymorphism | `MatchingStrategy`, `Order` subtypes |
| Strategy Pattern | `FIFOMatchingStrategy`, `ProRataMatchingStrategy` |
| Observer Pattern | `PortfolioManager.onTradeExecuted()` |

---

## Build with Maven (optional)

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.tradingengine.Main
mvn exec:java -Dexec.mainClass=com.tradingengine.MainGUI
```

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `javac` / `java` not found | Install JDK 17+ and add to PATH |
| `invalid flag: D:\Final` | Use `run.bat` (handles paths with spaces) |
| GUI button hard to see | Re-run `run_gui.bat` (button styling fixed) |
| `mvn` not recognized | Use `run.bat` — Maven is optional |


## Tech Stack

- Java 17+ (JDK only — no third-party libraries)
- Java Swing (GUI)
- `java.util.concurrent` (thread safety)
- Maven (optional build tool)


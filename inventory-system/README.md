# Clothing Store Inventory System

A standalone desktop inventory + order management app built with JavaFX and SQLite.
Opens straight to a dashboard — no login, no setup screen, no server to start.

## What's inside

- **Dashboard** — live totals (items, low stock, out of stock, inventory value) and a needs-attention table
- **Inventory** — add / edit / delete / restock items, category-organized
- **Checkout** — build a cart from in-stock items and confirm a sale

Every sale runs through a single database transaction that re-checks live
stock, deducts it, and logs an audit row — so an item can't be sold twice
even if two windows are open at once.

## Requirements to build

- JDK 17 or newer
- Maven 3.8+
- Internet access the first time you build (Maven downloads JavaFX + the SQLite driver)

## Run it during development

```bash
mvn clean javafx:run
```

This compiles and launches the app directly — use this while you're still
making changes.

## Build the final standalone app (what you hand to your client)

**Step 1 — build the runnable jar** (bundles the SQLite driver in):

```bash
mvn clean package
```

This produces `target/inventory-system-1.0.0.jar`.

**Step 2 — turn it into a native installer with `jpackage`** (ships with JDK 17+,
no extra install needed):

```bash
jpackage --input target/ \
  --name "Clothing Inventory" \
  --main-jar inventory-system-1.0.0.jar \
  --main-class com.clothingstore.inventory.App \
  --type exe \
  --win-menu \
  --win-shortcut
```

(On macOS use `--type dmg`, on Linux `--type deb` or `--type rpm`.)

This produces a real installer. Running it installs the app with its own
bundled Java runtime — your client never installs Java, never opens a
terminal, and never sees a console window. She just double-clicks the
icon it creates.

## Where the data lives

The app creates `inventory.db` (a single SQLite file) and an `images/`
folder in the same folder it's run from, the first time it launches.
Back up both and you've backed up the entire store's inventory, order
history, and item photos.

## Project structure

```
src/main/java/com/clothingstore/inventory/
├── App.java              # entry point, wires everything together
├── model/                # Item, Category, Order, OrderItem, enums, records
├── dao/                  # database access (JDBC + SQLite)
├── service/              # business rules (checkout transaction, dashboard stats, restock)
├── ui/                   # JavaFX screens (Dashboard, Inventory, Checkout, Orders, dialogs)
└── util/                 # ImageStorage -- copies picked photos into images/
src/main/resources/
└── schema.sql            # table definitions, run automatically on first launch
└── styles.css            # status badges, KPI cards, sidebar nav styling
```

## UI theme

The app uses [AtlantaFX](https://github.com/mkpaz/atlantafx) (`CupertinoLight`) for a
modern flat look, on top of which `styles.css` adds status pill badges and
KPI card styling. To switch themes, change the one line in `App.java`:

```java
Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
```

to `PrimerLight`, `PrimerDark`, `NordLight`, `NordDark`, `CupertinoDark`, or `Dracula`
(all in `atlantafx.base.theme`).

# Handoff Notes — Clothing Store Inventory System

Java 17 + JavaFX + SQLite desktop app. This document is for whoever picks up development next (human or AI) — it lists what's verified, what's been fixed/added across follow-up passes, and what's still open.

## What's verified

- All source files compile cleanly against real JavaFX 21 + SQLite JDBC jars.
- The app launches, connects to the database, creates its schema, and runs the image-path column migration successfully.
- The full class wiring (App → services → DAOs → UI) runs for several seconds under a virtual display with no exceptions, across every pass of changes below.
- Fixed early on: SQLite driver wasn't auto-registering because JavaFX's Application thread uses a different context classloader than the one that loaded the driver jar — fixed with an explicit `Class.forName("org.sqlite.JDBC")` in `DatabaseManager`.

## Pass 2 fixes (robustness)

1. **Form validation** — `ItemFormDialog` rejects blank SKU/name/category and negative or non-numeric price/quantity/reorder-threshold, showing all problems at once.
2. **UI error handling** — `InventoryView` and `CheckoutView` catch `DataAccessException` around add/edit/delete/restock/checkout and show a friendly `Alert` instead of an unhandled exception.
3. **Real restock flow with audit logging** — Restock is a quantity-input dialog (was a hardcoded +10), and it logs a `RESTOCK` row to `stock_transactions` via `InventoryService.restockItem()`. Previously restocks changed quantity but left no audit trail at all.
4. **Order history + cancellation** — `OrdersView` lists all orders, shows line items for the selected one, and can cancel a `COMPLETED` order, restocking every item and logging a `CANCELLATION` row per item in one transaction (`OrderService.cancelOrder`, `OrderDao.findLinesForOrder` / `updateStatus`).
5. **Category management** — `CategoryManagerDialog` (from Inventory's toolbar) adds/deletes categories instead of being stuck with the 4 seeded ones.
6. **Inventory search** — live filter by name/SKU (`FilteredList`) above the Inventory table.
7. **Clean shutdown** — `DatabaseManager.closeConnection()` runs on window close.

## Pass 3 addition (item photos)

8. **Image upload for items** — new `util/ImageStorage` copies a user-picked photo into a local `images/` folder (UUID filename, so no collisions) and returns a relative path stored on `Item.imagePath`. `ItemFormDialog` has a "Choose Image..." button with a live preview; the file is only copied on Save (cancelling the dialog leaves no orphan file). New `ui/ThumbnailCell` renders a 40x40 thumbnail, reused as a "Photo" column in both `InventoryView` and `CheckoutView`'s catalog table.
   - **Schema migration handled**: `DatabaseManager` runs a one-time `ALTER TABLE items ADD COLUMN image_path TEXT` on every startup, swallowing the "column already exists" error after the first run — so an existing `inventory.db` from before this feature upgrades automatically, no manual migration or data loss.

## Still not tested end-to-end

- Clicking through every dialog by hand on a real desktop (mouse/keyboard interaction, not just headless launch) — cancel-order confirmation, category deletion when items reference it, image picker file dialog, search edge cases
- `jpackage` build producing an actual `.exe` — the README documents the command, but no Windows environment was available to run it
- Behavior with a large catalog or many item photos (tested with a small/empty database only)

## Known gaps still open

1. Single shared `Connection` in `DatabaseManager`, no pooling — fine for one user on one machine, but will misbehave if the app is ever run twice against the same `inventory.db` at once, or extended to multiple users.
2. No backup/export option for `inventory.db` or the `images/` folder as a bundle.
3. Currency symbol (₱) is hardcoded in `DashboardView` and `CheckoutView` — not configurable.
4. Deleting an item that's referenced by an existing order will fail (foreign key constraint) — the error message tells the user why, but there's no "archive instead of delete" option, which is usually what you actually want for sold-out discontinued items.
5. Picked images are never resized/compressed before copying — a client selecting large phone-camera photos could bloat the `images/` folder over time. Worth downscaling on save if this becomes an issue.
6. No column sorting UI cues beyond JavaFX's default click-to-sort (works, but not obvious to a non-technical user).

## Suggested next steps

1. Click through every screen by hand on a real desktop to confirm the untested flows above.
2. Consider an "archive" `ItemStatus` path instead of hard delete, to avoid the foreign-key failure in gap #4.
3. Attempt the `jpackage` build on an actual Windows machine and confirm the client can double-click it with zero setup.
4. If image folder size becomes a concern, downscale/compress images in `ImageStorage.store()` before writing them.
5. If this ever needs multiple concurrent users, replace the single shared `Connection` with a small connection pool (e.g., HikariCP).

## Where things live (quick map)

```
model/    → Item, Category, Order, OrderItem, ItemStatus (enum), StockSummary/OrderLineRequest (records)
dao/      → JDBC access; ItemDao, OrderDao, CategoryDao, StockTransactionDao, DatabaseManager
service/  → InventoryService (CRUD + restock + dashboard stats), OrderService (checkout + cancellation)
ui/       → DashboardView, InventoryView, CheckoutView, OrdersView, ItemFormDialog, CategoryManagerDialog, CartLine, ThumbnailCell
util/     → ImageStorage (copies picked photos into images/)
App.java  → wires it all together, no login/setup screen by design
```

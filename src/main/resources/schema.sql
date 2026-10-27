CREATE TABLE IF NOT EXISTS categories (
    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS items (
    item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    sku TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category_id INTEGER REFERENCES categories(category_id),
    size TEXT,
    color TEXT,
    price REAL NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
<<<<<<< HEAD
    reorder_threshold INTEGER NOT NULL DEFAULT 5,
    status TEXT NOT NULL DEFAULT 'OUT_OF_STOCK',
    date_added TEXT NOT NULL,
    image_path TEXT,
    archived INTEGER NOT NULL DEFAULT 0
=======
    status TEXT NOT NULL DEFAULT 'OUT_OF_STOCK',
    date_added TEXT NOT NULL,
    image_path TEXT,
    removed INTEGER NOT NULL DEFAULT 0
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER REFERENCES items(item_id),
    change_amount INTEGER NOT NULL,
    reason TEXT NOT NULL,
    timestamp TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    order_id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_name TEXT,
    order_date TEXT NOT NULL,
    total_amount REAL NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER REFERENCES orders(order_id),
    item_id INTEGER REFERENCES items(item_id),
    quantity INTEGER NOT NULL,
    unit_price REAL NOT NULL
);

<<<<<<< HEAD
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    created_at TEXT NOT NULL
);

=======
>>>>>>> 1fad3874ce4da0e34b3cc67029bd87899e5d4823
INSERT OR IGNORE INTO categories (category_id, name) VALUES
    (1, 'Tops'), (2, 'Bottoms'), (3, 'Outerwear'), (4, 'Accessories');

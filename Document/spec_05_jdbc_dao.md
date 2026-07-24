# 課題仕様書 ⑤ コンソールアプリ：SQL Server LocalDB 接続（JDBC）+ DAO パターン

**フェーズ：** Phase 2-A　**期間目安：** 4 日　**担当：** 新人

---

## 目的

CSV で管理していたデータを SQL Server LocalDB（RDB）に移行する。
JDBC による DB アクセスと DAO パターンを習得する。

---

## 背景（業務イメージ）

CSV ファイルでの管理には限界があります。

- 複数人が同時に操作できない
- データが壊れやすい
- 検索・集計が難しい

そこでデータを SQL Server データベースで管理するように変更します。
CSV で管理していたロジックは DAO（Data Access Object）クラスに置き換えます。

> 今回は各自の PC 上で動く **SQL Server Express LocalDB** を使う（課題 ① でセットアップ済み）。チームで共有する DB サーバーではなく、個人のローカル環境にデータを持つ点に注意すること。

---

## 事前準備

- 課題 ① で LocalDB（`MSSQLLocalDB`）がセットアップ済みであることを確認する
- mssql-jdbc の Maven dependency を `pom.xml` に追加する

```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.6.1.jre11</version>
</dependency>
```

---

## テーブル設計・DDL

SSMS で `(localdb)\MSSQLLocalDB` に接続し、以下の SQL を実行すること。

```sql
-- データベース作成
CREATE DATABASE InventoryTraining;
GO

USE InventoryTraining;
GO

-- 部品マスタ
CREATE TABLE parts (
    part_code       VARCHAR(10)     NOT NULL,
    part_name       NVARCHAR(100)   NOT NULL,
    stock           INT             NOT NULL DEFAULT 0,
    warehouse_code  VARCHAR(10)     NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME        NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_parts PRIMARY KEY (part_code),
    CONSTRAINT chk_parts_stock CHECK (stock >= 0)
);

-- 入出庫履歴
CREATE TABLE stock_transactions (
    transaction_id  VARCHAR(10)     NOT NULL,
    part_code       VARCHAR(10)     NOT NULL,
    type            VARCHAR(3)      NOT NULL,  -- 'IN' or 'OUT'
    quantity        INT             NOT NULL,
    transaction_at  DATETIME        NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_stock_transactions PRIMARY KEY (transaction_id),
    CONSTRAINT fk_transactions_parts FOREIGN KEY (part_code) REFERENCES parts(part_code),
    CONSTRAINT chk_type CHECK (type IN ('IN', 'OUT')),
    CONSTRAINT chk_quantity CHECK (quantity > 0)
);
```

初期データも投入すること：
```sql
INSERT INTO parts (part_code, part_name, stock, warehouse_code) VALUES
('P001', N'ボルト M6',     100, 'WH01'),
('P002', N'ナット M6',      50, 'WH01'),
('P003', N'ワッシャー M6', 200, 'WH02');
```

---

## 変更するプロジェクト構成

```
training/
└── src/
    └── inventory/
        ├── model/
        │   ├── Part.java
        │   └── StockTransaction.java
        ├── dao/                          ← 新規追加（FileService の代替）
        │   ├── PartDao.java
        │   └── TransactionDao.java
        ├── util/
        │   └── DbConnection.java         ← 新規追加（DB 接続管理）
        ├── service/
        │   └── InventoryService.java     ← 変更（FileService → DAO を使うように）
        └── app/
            └── Main.java                 ← 変更なし（または微修正）
```

---

## 実装するクラス

### DbConnection クラス（`util/DbConnection.java`）

DB 接続を一元管理するクラス。

```java
// 接続文字列の例（LocalDB・Windows 認証のためユーザー名/パスワード不要）
String url = "jdbc:sqlserver://localhost;instanceName=MSSQLLocalDB;databaseName=InventoryTraining;"
            + "integratedSecurity=true;encrypt=false";
Connection conn = DriverManager.getConnection(url);
```

> **注意：** Windows 認証（`integratedSecurity=true`）を使う場合、`mssql-jdbc_auth-<version>-x64.dll` が必要になることがある。接続できない場合はエラーメッセージを担当者に共有すること。

実装すること：
- `static Connection getConnection()` — 接続を返す
- 接続文字列は定数またはプロパティファイルで管理する（コードに直書きしない）

---

### PartDao クラス（`dao/PartDao.java`）

部品マスタの DB アクセスを担当するクラス。

| メソッド | 説明 |
|---|---|
| `List<Part> findAll()` | 全部品を SELECT して返す |
| `Part findByCode(String partCode)` | 部品コードで検索して返す（なければ `null`） |
| `void insert(Part part)` | 部品を INSERT する |
| `void updateStock(String partCode, int newStock)` | 在庫数を UPDATE する |
| `boolean delete(String partCode)` | 部品を DELETE する |

**実装上の注意：**
- `PreparedStatement` を必ず使うこと（文字列連結で SQL を作らないこと）
- `Connection` / `PreparedStatement` / `ResultSet` は必ず `finally` または try-with-resources でクローズすること

---

### TransactionDao クラス（`dao/TransactionDao.java`）

入出庫履歴の DB アクセスを担当するクラス。

| メソッド | 説明 |
|---|---|
| `void insert(StockTransaction tx)` | 入出庫履歴を INSERT する |
| `List<StockTransaction> findAll()` | 全履歴を SELECT して返す |
| `List<StockTransaction> findByPartCode(String partCode)` | 部品コードで履歴を絞り込んで返す |

---

### InventoryService クラスの変更

- `FileService` への依存をすべて `PartDao` / `TransactionDao` に置き換える
- 入庫・出庫処理では「在庫更新 + 履歴 INSERT」を **同一トランザクション** で実行すること

```java
// トランザクション制御の例（InventoryService 内）
Connection conn = DbConnection.getConnection();
try {
    conn.setAutoCommit(false);  // トランザクション開始

    // 在庫数を更新
    partDao.updateStock(conn, partCode, newStock);

    // 入出庫履歴を INSERT
    transactionDao.insert(conn, tx);

    conn.commit();  // コミット
} catch (Exception e) {
    conn.rollback();  // ロールバック
    throw e;
} finally {
    conn.close();
}
```

---

## 業務ルール（変更なし）

| ルール | 内容 |
|---|---|
| 出庫は在庫以上の数量は不可 | Java 側で事前チェックし、不足時は例外をスロー |
| 数量は 1 以上 | 0 以下の入出庫数はエラー |
| 在庫更新と履歴 INSERT は同一トランザクション | どちらか失敗した場合は両方ロールバック |

---

## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | SSMS でテーブルが作成されていること |
| 2 | アプリから部品一覧が表示されること（SELECT）|
| 3 | 部品登録ができること（INSERT）|
| 4 | 入庫・出庫ができること（UPDATE + INSERT） |
| 5 | 在庫不足でエラーになること |
| 6 | 出庫失敗時に在庫数が変わっていないこと（ロールバック確認）|
| 7 | SSMS で `stock_transactions` テーブルに履歴が入っていること |

---

## 提出・確認方法

1. 上記の動作確認を一通りデモする
2. SSMS でテーブルの中身を SELECT して見せる
3. 担当者から「PreparedStatement を使う理由」「トランザクションとは何か」「rollback が必要な理由」を口頭で説明できること

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

## 事前準備：JDBC ドライバの追加

### なぜこの手順をやるか

Java からデータベースに接続するには **JDBC ドライバ**（外部ライブラリ）が必要。
今回はあえて JAR ファイルを手動でプロジェクトに追加する手順を踏む。

「外部ライブラリとは何か」「なぜビルドパスへの追加が必要か」を体感で理解することが目的。
（実務では Maven / Gradle で依存関係を管理するが、その仕組みを理解するための前提知識になる）

### 手順 1：JAR ファイルのダウンロード

1. [https://learn.microsoft.com/ja-jp/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server](https://learn.microsoft.com/ja-jp/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server) にアクセスする
2. 最新バージョンの ZIP ファイルをダウンロードする
3. ZIP を解凍し、以下のファイルを探す

```
解凍フォルダ/
└── enu/
    ├── mssql-jdbc-12.x.x.jre11.jar   ← Java 11 以上はこちらを使う
    └── mssql-jdbc-12.x.x.jre8.jar    ← Java 8 の場合はこちら
```

> JDK 17（Temurin）を使っているため `jre11` の JAR を選ぶこと。

### 手順 2：lib フォルダへの配置

1. Eclipse のプロジェクト直下に `lib` フォルダを作成する
   - プロジェクトを右クリック → 「新規」→「フォルダー」→ フォルダー名 `lib`
2. ダウンロードした `mssql-jdbc-12.x.x.jre11.jar` を `lib` フォルダにコピーする

```
05_project_inventory_system/
├── src/
├── lib/
│   └── mssql-jdbc-12.x.x.jre11.jar   ← ここに配置
└── ...
```

### 手順 3：ビルドパスへの追加

JAR をフォルダに置いただけでは Java から使えない。Eclipse のビルドパスに登録する必要がある。

1. プロジェクトを右クリック →「ビルド・パス」→「ビルド・パスの構成」をクリック
2. 「ライブラリー」タブを開く
3. 「JAR の追加」をクリック
4. `lib/mssql-jdbc-12.x.x.jre11.jar` を選択して「OK」
5. 「適用して閉じる」をクリック

**確認ポイント：**
プロジェクト直下に「参照ライブラリー」が表示され、その中に `mssql-jdbc-12.x.x.jre11.jar` が見えれば OK。

### 手順 4：動作確認

以下のコードを一時的に `main` メソッドに書いて実行し、エラーが出ないことを確認する。

```java
// ドライバが読み込めるかの確認（接続はまだしない）
Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
System.out.println("ドライバの読み込み成功");
```

> `ClassNotFoundException` が出た場合は、ビルドパスへの追加が正しくできていない。
> 手順 3 をやり直すこと。

---

> **【参考】Maven を使う場合**
>
> 実務プロジェクトでは JAR を手動管理するのではなく、Maven の `pom.xml` に依存関係を記述して自動的にダウンロード・管理する方法が一般的。
>
> ```xml
> <dependency>
>     <groupId>com.microsoft.sqlserver</groupId>
>     <artifactId>mssql-jdbc</artifactId>
>     <version>12.6.1.jre11</version>
> </dependency>
> ```
>
> 今回手動で行った「JAR のダウンロード → 配置 → ビルドパス登録」を、Maven が自動でやってくれるイメージ。課題⑥以降で Maven プロジェクトに移行した際に、その便利さを改めて実感できる。

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

## 実行ユーザーの作成

### なぜユーザーを作るか

実務ではアプリケーションごとに専用の DB ユーザーを用意し、
必要最低限の権限だけを付与するのが基本。
今回も実務に合わせ、アプリ専用ユーザーを作成して接続する。

### 手順 1：ログイン（サーバーレベル）の作成

SSMS で Windows 認証（`(localdb)\MSSQLLocalDB`）に接続した状態で以下を実行する。

```sql
USE master;
GO

-- SQL Server ログインを作成する（Windows 認証ユーザーとして登録）
CREATE LOGIN inventory_user
    WITH PASSWORD = 'P@ssw0rd123',
    DEFAULT_DATABASE = InventoryTraining,
    CHECK_EXPIRATION = OFF,
    CHECK_POLICY = OFF;
GO
```

### 手順 2：データベースユーザーの作成と権限付与

```sql
USE InventoryTraining;
GO

-- ログインをデータベースユーザーとして登録
CREATE USER inventory_user FOR LOGIN inventory_user;
GO

-- 必要最低限の権限を付与（SELECT / INSERT / UPDATE / DELETE のみ）
GRANT SELECT, INSERT, UPDATE, DELETE ON parts              TO inventory_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON stock_transactions TO inventory_user;
GO
```

> **ポイント：** `db_owner`（管理者権限）は付与しない。
> アプリが使う操作だけに絞るのがセキュリティの基本。

### 手順 3：権限の確認

作成したユーザーで以下を実行し、権限の範囲内で動くことを確認する。

```sql
-- inventory_user として実行（EXECUTE AS で権限を切り替えて確認）
EXECUTE AS USER = 'inventory_user';

-- OK：SELECT できる
SELECT * FROM parts;

-- NG になるはず：テーブル削除は権限外
DROP TABLE parts;   -- エラーになることを確認する

-- 元のユーザーに戻す
REVERT;
```

---

## 変更するプロジェクト構成

```
05_project_inventory_system/
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
// 接続文字列の例（SQL Server 認証）
String url  = "jdbc:sqlserver://localhost;instanceName=MSSQLLocalDB;"
            + "databaseName=InventoryTraining;encrypt=false";
String user = "inventory_user";
String pass = "P@ssw0rd123";
Connection conn = DriverManager.getConnection(url, user, pass);
```

> **注意：** ユーザー名・パスワードはコードに直書きしないこと。
> プロパティファイル（`db.properties` など）に切り出して読み込む形にすること。

```
# db.properties（プロジェクト直下に作成）
db.url=jdbc:sqlserver://localhost;instanceName=MSSQLLocalDB;databaseName=InventoryTraining;encrypt=false
db.user=inventory_user
db.password=P@ssw0rd123
```

```java
// プロパティファイルからの読み込み例
Properties props = new Properties();
props.load(new FileInputStream("db.properties"));
String url  = props.getProperty("db.url");
String user = props.getProperty("db.user");
String pass = props.getProperty("db.password");
Connection conn = DriverManager.getConnection(url, user, pass);
```

実装すること：
- `static Connection getConnection()` — 接続を返す
- 接続情報は `db.properties` から読み込む（コードに直書きしない）
- `db.properties` は `.gitignore` に追加してリポジトリに含めないこと

> **【参考】Windows 認証との違い：**
> `integratedSecurity=true` を指定すると OS のログインユーザーで接続する Windows 認証になる。
> 今回は実務に合わせて SQL Server 認証（ユーザー名・パスワード）を使う。

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

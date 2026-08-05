# 課題仕様書 ⑧ Web アプリ：Jakarta EE CRUD 完成

**フェーズ：** Phase 2-B　**期間目安：** 3〜4 日　**担当：** 新人

---

## 目的

課題 ⑦ で作った部品一覧・登録画面に、入出庫処理・履歴表示・バリデーション強化を追加し、
Web アプリとして一通り使えるレベルに完成させる。

---

## 画面イメージ

別ファイル `screen_mockups.html` を参照すること。

| 画面イメージ番号 | 対応する画面 |
|---|---|
| 画面 ④ | 入庫処理フォーム（部品プルダウン） |
| 画面 ⑤ | 出庫処理（在庫不足エラー） |
| 画面 ⑥ | 入出庫履歴一覧（部品コード絞り込みあり） |

---

## 追加する画面・機能

| 画面 | URL | 説明 |
|---|---|---|
| 入庫処理フォーム | `/stock/in` (GET) | 入庫フォームを表示 |
| 入庫処理 | `/stock/in` (POST) | 入庫を実行して在庫を増やす |
| 出庫処理フォーム | `/stock/out` (GET) | 出庫フォームを表示 |
| 出庫処理 | `/stock/out` (POST) | 出庫を実行して在庫を減らす |
| 入出庫履歴 | `/stock/history` (GET) | 入出庫履歴一覧を表示（絞り込み付き） |
| 部品削除 | `/parts/delete` (POST) | 部品を削除する |

---

## 追加するファイル

```
servlet/
├── StockInServlet.java      ← 入庫処理
├── StockOutServlet.java     ← 出庫処理
├── TransactionServlet.java  ← 履歴表示
└── PartDeleteServlet.java   ← 部品削除

jsp/
├── stock_in.jsp             ← 入庫フォーム
├── stock_out.jsp            ← 出庫フォーム
└── transaction_list.jsp     ← 入出庫履歴一覧
```

---

## 各機能の仕様

### ① 入庫処理

**入庫フォーム（GET）：**
- 部品コードと入庫数量を入力するフォームを表示する
- 部品コードはプルダウン（`<select>`）で選択できるようにする（`InventoryService.findAll()` で選択肢を生成）

**入庫処理（POST）：**
1. 部品コードと入庫数量を受け取る
2. バリデーション：数量は 1 以上の整数
3. `InventoryService.stockIn(partCode, quantity)` を呼び出す
4. 成功：部品一覧にリダイレクト（フラッシュメッセージで「入庫しました」を表示）
5. 失敗：エラーメッセージをフォームに表示する

---

### ② 出庫処理（`StockOutServlet.java` + `stock_out.jsp`）

**何をする画面か：**
部品の在庫を減らす。部品をプルダウンで選択し、出庫数量を入力して送信する。
在庫数を超える出庫はエラーとなり、フォームにエラーメッセージを表示する。
成功すると一覧にリダイレクトし、減った在庫数を確認できる。

**画面遷移：**
```
ナビメニュー「出庫」クリック
  └─→ 出庫フォーム（GET）
        └─→ 部品・数量を入力して送信（POST）
              ├─→ 成功：部品一覧にリダイレクト（「出庫しました」メッセージ）
              └─→ 失敗（在庫不足など）：フォームに戻りエラーメッセージを表示
```

**出庫フォーム（GET）：**
- 部品コードはプルダウン（`<select>`）で選択できるようにする（`InventoryService.findAll()` で選択肢を生成）
- 選択した部品の現在の在庫数をフォーム上に表示する（JavaScript または Servlet 側で対応）

**出庫処理（POST）：**
1. 部品コードと出庫数量を受け取る
2. バリデーション：数量は 1 以上の整数
3. `InventoryService.stockOut(partCode, quantity)` を呼び出す
4. 成功：部品一覧にリダイレクト（フラッシュメッセージで「出庫しました」を表示）
5. 失敗（在庫不足）：`StockShortageException` をキャッチしてフォームにエラーメッセージを表示する

**エラー表示例：**
```
エラー：在庫が不足しています。（要求数：200、現在庫：120）
```

---

### ③ 入出庫履歴一覧（`TransactionServlet.java` + `transaction_list.jsp`）

**何をする画面か：**
これまでの入庫・出庫の操作履歴を一覧で表示する。データの確認専用で、編集はできない。
部品コードで絞り込み検索もできる。

**画面遷移：**
```
ナビメニュー「入出庫履歴」クリック
  └─→ 履歴一覧（全件）表示
        └─→ 部品コードを入力して「検索」ボタン押下
              └─→ 同じ画面で絞り込み結果を表示（URL: /stock/history?partCode=P001）
```

**絞り込みの仕組み：**
- 検索フォームは GET で送信し、URL パラメータ `?partCode=P001` で絞り込み条件を渡す
- `partCode` が空の場合は全件表示、値がある場合は該当部品の履歴のみ表示する
- 絞り込み中は検索フォームに入力値を保持して表示すること（再入力不要にする）

**表示内容：**
```
===== 入出庫履歴 =====

部品コードで絞り込み: [P001____] [検索] [クリア]

3 件表示中（P001 で絞り込み中）

取引ID  | 部品コード | 種別 | 数量 | 日時
TX0001 | P001      | 入庫 | 50   | 2025-04-01 09:00:00
TX0002 | P001      | 出庫 | 30   | 2025-04-01 10:30:00
TX0005 | P001      | 入庫 | 200  | 2025-04-02 08:15:00
```

- 種別は `IN` → `入庫`、`OUT` → `出庫` と日本語で表示する
- 「クリア」リンクは絞り込みなしの URL（`/stock/history`）へのリンクにする
- 0 件の場合は「該当する履歴がありません。」と表示する

---

### ④ 部品削除

一覧画面に「削除」ボタンを追加する。

- 削除ボタンを押すと確認ダイアログを表示する（JavaScript の `confirm()`）
- 確認後 POST で `PartDeleteServlet` に送信する
- 削除成功後は一覧にリダイレクトする

> **注意：** 入出庫履歴が存在する部品を削除しようとした場合の挙動を検討し、適切にエラーを返すこと（外部キー制約のエラーをハンドリングする）。

---

## バリデーション強化

課題 ⑦ のバリデーションを共通化すること。

```java
// util/ValidationUtil.java（例）
public class ValidationUtil {

    public static boolean isRequired(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isPositiveInt(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
```

エラーメッセージは `Map<String, String>` で管理し、フィールドごとにメッセージを返すこと。

---

## フラッシュメッセージ（任意）

操作成功時に一覧画面で「〇〇しました」というメッセージを一度だけ表示する仕組みを実装する（任意）。

実装方針：
- POST 処理成功時にセッションにメッセージをセットする
- リダイレクト先の JSP でメッセージをセッションから取り出して表示し、セッションから削除する

---

## ビルド・デプロイ手順

### WAR ファイルのビルドと Tomcat へのデプロイ

Eclipse 上での実行確認が取れたら、WAR ファイルとしてビルドして Tomcat にデプロイする手順を体験する。

#### 手順 1：WAR ファイルのエクスポート

1. Eclipse でプロジェクトを右クリック →「エクスポート」→「WAR ファイル」を選択
2. 以下を設定してエクスポートする

| 項目 | 設定値 |
|---|---|
| Web プロジェクト | `05_project_inventory_system` |
| 出力先 | 任意のフォルダ（例：`C:\work\inventory.war`） |
| 最適化されたエクスポート | チェックあり |

#### 手順 2：Tomcat の webapps に配置

1. Tomcat を停止する（Eclipse の「サーバー」ビューから停止、または `shutdown.bat` を実行）
2. エクスポートした `inventory.war` を Tomcat の `webapps/` フォルダにコピーする

```
Tomcat インストールフォルダ/
└── webapps/
    └── inventory.war   ← ここにコピー
```

3. Tomcat を起動する（`startup.bat` を実行、または Eclipse のサーバービューから起動）
4. 起動後、`webapps/` に `inventory/` フォルダが自動展開されていることを確認する

```
webapps/
├── inventory.war
└── inventory/          ← 自動展開される
    ├── WEB-INF/
    └── jsp/
```

5. ブラウザで以下の URL にアクセスして動作確認する

```
http://localhost:8080/inventory/parts
```

> **ポイント：** Eclipse 上の「Tomcat で実行」は開発用の簡易実行。実務では WAR をビルドして Tomcat に配置するこの手順が基本になる。

---

### DB 接続プール（DBCP）の設定

#### なぜ接続プールが必要か

これまで DAO クラスで毎回 `DbConnection.getConnection()` を呼び出していたが、
Web アプリでは **1 リクエストごとに接続の作成・切断**が発生し、パフォーマンスが低下する。

**接続プール**は、あらかじめ一定数の接続を確保しておき、リクエストが来たら既存の接続を使い回す仕組み。
実務の Web アプリではほぼ必須の仕組みなので、設定方法を理解しておくこと。

```
【接続プールなし】                   【接続プールあり】
リクエスト → 接続作成 → 処理 → 切断   リクエスト → プールから取得 → 処理 → プールに返却
リクエスト → 接続作成 → 処理 → 切断   リクエスト → プールから取得 → 処理 → プールに返却
（毎回コストがかかる）               （接続の使い回しで高速）
```

#### Tomcat の JNDI データソースを使った設定

**① `context.xml` に接続プールの設定を追加する**

`WebContent/META-INF/context.xml` を作成し、以下を記述する。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Resource
        name="jdbc/InventoryDB"
        auth="Container"
        type="javax.sql.DataSource"
        driverClassName="com.microsoft.sqlserver.jdbc.SQLServerDriver"
        url="jdbc:sqlserver://(localdb)\\MSSQLLocalDB;databaseName=InventoryTraining;integratedSecurity=true;encrypt=false"
        maxTotal="10"
        maxIdle="5"
        maxWaitMillis="5000"
    />
</Context>
```

| 設定項目 | 説明 |
|---|---|
| `maxTotal` | プールが保持する最大接続数 |
| `maxIdle` | アイドル状態で保持する最大接続数 |
| `maxWaitMillis` | 接続が取得できない場合の最大待機時間（ms） |

**② `web.xml` にリソース参照を追加する**

```xml
<resource-ref>
    <description>DB Connection Pool</description>
    <res-ref-name>jdbc/InventoryDB</res-ref-name>
    <res-type>javax.sql.DataSource</res-type>
    <res-auth>Container</res-auth>
</resource-ref>
```

**③ `DbConnection.java` を接続プール経由に書き換える**

```java
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class DbConnection {

    public static Connection getConnection() throws Exception {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/InventoryDB");
        return ds.getConnection();
    }
}
```

> **ポイント：** `getConnection()` の呼び出し方は DAO 側で変わらない。
> 接続プールへの切り替えは `DbConnection` クラスだけ変更すればよい設計になっている。
> これが「DB アクセスを一箇所に集約する」メリットの一つ。

**④ mssql-jdbc の JAR を Tomcat の lib に追加する**

JNDI データソースは Tomcat が管理するため、JAR をプロジェクトの `lib/` ではなく
Tomcat の `lib/` フォルダにも配置する必要がある。

```
Tomcat インストールフォルダ/
└── lib/
    └── mssql-jdbc-12.x.x.jre11.jar   ← ここにもコピー
```

---

## 動作確認項目

| # | 確認内容 |
|---|----------|
| 1 | 入庫・出庫処理が正常に動作する |
| 2 | 在庫不足エラーが画面に表示される |
| 3 | 入出庫履歴が一覧で確認できる |
| 4 | 部品コードで履歴を絞り込みできる |
| 5 | 部品削除が動作する |
| 6 | 入出庫履歴のある部品を削除しようとするとエラーになる |

---

## 提出・確認方法

1. 全機能の動作デモをする
2. 担当者から「バリデーションを共通化した理由」「外部キー制約とはなにか」「セッションを使う場面はどこか」を口頭で説明できること

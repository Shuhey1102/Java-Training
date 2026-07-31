# 課題仕様書 ① 開発環境セットアップ

**フェーズ：** Phase 1　**期間目安：** 2〜3 日　**担当：** 新人

---

## 目的

Java 開発に必要なツールをすべてセットアップし、「Hello World」をビルド・実行できる状態を作る。
環境が整っていないと以降の課題がすべて止まるため、最優先で完了させること。

---

## 課題内容

以下の手順をすべて実施し、動作確認まで完了させること。

### 1. JDK インストール

**Eclipse Temurin 17 LTS** をインストールする。

- [https://adoptium.net/](https://adoptium.net/) にアクセスし、**Temurin 17 (LTS)** の Windows x64 インストーラー（`.msi`）をダウンロードして実行する
- インストーラーのオプションで「**Set JAVA_HOME variable**」「**Add to PATH**」にチェックが入っていることを確認してインストール（自動で環境変数が設定される）
- インストール後、コマンドプロンプトで以下を実行してバージョンが表示されることを確認する

```
java -version
javac -version
```

**確認ポイント：** `openjdk 17.x.x` および配布元として `Temurin` と表示されること

### 2. Eclipse（Pleiades）セットアップ

- Eclipse（Pleiades All in One）をダウンロードしてインストールする
- 日本語化されていることを確認する
- ワークスペースを任意のフォルダに設定する（例：`C:\workspace`）
- JDK 17 が Eclipse に認識されていることを確認する（ウィンドウ → 設定 → Java → インストール済み JRE）

### 3. VS Code セットアップ

- VS Code をインストールする
- 拡張機能「Extension Pack for Java」をインストールする
- VS Code からも `java -version` が実行できることを確認する

> VS Code はメインの IDE ではなく「調べもの・ちょっとしたファイル編集用」として使う。開発作業は基本的に Eclipse で行うこと。

### 4. Git セットアップ

- Git をインストールする
- 以下のコマンドで名前とメールアドレスを設定する

```
git config --global user.name "自分の名前"
git config --global user.email "自分のメールアドレス"
```

- Eclipse の Git パースペクティブが使えることを確認する

### 5. Maven 動作確認

- コマンドプロンプトで以下を実行し、バージョンが表示されることを確認する

```
mvn -version
```

### 6. SQL Server LocalDB セットアップ

### 6. SQL Server LocalDB セットアップ

LocalDB は Visual Studio Installer の個別コンポーネントからインストールする。

1. スタートメニューから「**Visual Studio Installer**」を起動する
2. インストール済み VS の「**変更**」をクリックする
3. 「**個別のコンポーネント**」タブを開く
4. 検索ボックスに `LocalDB` と入力し、**「SQL Server Express 2019 LocalDB」**（または最新版）にチェックを入れる
5. 「**変更**」ボタンをクリックしてインストール

#### インストール確認

コマンドプロンプトで以下を実行し、インスタンスが表示されることを確認する。

```
sqllocaldb info
```

表示例：
```
MSSQLLocalDB
```

インスタンスが停止している場合は起動する：

```
sqllocaldb start MSSQLLocalDB
```

#### SSMS から接続確認

- SSMS（SQL Server Management Studio）をインストールする（[ダウンロードページ](https://learn.microsoft.com/ja-jp/sql/ssms/download-sql-server-management-studio-ssms)）
- SSMS を起動し、サーバー名に以下を入力して「**Windows 認証**」で接続する

```
(localdb)\MSSQLLocalDB
```

- 接続後、以下の SQL を実行して結果が返ることを確認する

```sql
SELECT 1
```

> **ポイント：** LocalDB は Windows 認証で接続するため、ユーザー名・パスワードは不要。チームで共有する DB サーバーではなく、各自の PC 内でデータを管理する。

コマンドプロンプトで以下を実行し、インスタンスが表示されることを確認する。

```
sqllocaldb info
```

表示例：
```
MSSQLLocalDB
```

インスタンスが停止している場合は起動する：

```
sqllocaldb start MSSQLLocalDB
```

#### SSMS から接続確認

- SSMS（SQL Server Management Studio）をインストールする（[ダウンロードページ](https://learn.microsoft.com/ja-jp/sql/ssms/download-sql-server-management-studio-ssms)）
- SSMS を起動し、サーバー名に以下を入力して「**Windows 認証**」で接続する

```
(localdb)\MSSQLLocalDB
```

- 接続後、以下の SQL を実行して結果が返ることを確認する

```sql
SELECT 1
```

> **ポイント：** LocalDB は Windows 認証で接続するため、ユーザー名・パスワードは不要。チームで共有する DB サーバーではなく、各自の PC 内でデータを管理する。

### 7. Hello World の実行

Eclipse で以下の手順を実施すること。

1. 新規 Java プロジェクトを作成する（プロジェクト名：`training`）
2. `src` フォルダに `HelloWorld.java` を作成する
3. 以下のコードを入力する

```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```

4. 実行し、コンソールに `Hello World` が表示されることを確認する

---

## 提出物

以下をすべて担当者に確認してもらうこと。

| # | 確認内容 |
|---|----------|
| 1 | `java -version` の出力を画面キャプチャまたは口頭で報告 |
| 2 | `mvn -version` の出力を画面キャプチャまたは口頭で報告 |
| 3 | SQL Server に接続できた旨を報告 |
| 4 | Eclipse で Hello World が実行できることをデモ |

---

## 詰まったときは

- エラーメッセージは必ずコピーして担当者に共有すること
- ネットワーク・プロキシ関連のエラーが出た場合は自己解決しようとせず、すぐに担当者に相談すること
- 「何が起きているかわからない」状態が 30 分以上続いたら、その時点で担当者に声をかけること

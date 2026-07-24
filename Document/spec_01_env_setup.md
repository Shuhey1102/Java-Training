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

**OpenJDK 17 LTS** をインストールする。以下のいずれかから入手すること。

| 配布元 | URL | 備考 |
|---|---|---|
| Eclipse Temurin（推奨） | https://adoptium.net/ | 最も広く使われている OpenJDK ビルド |
| Microsoft Build of OpenJDK | https://aka.ms/download-jdk | Azure 環境との親和性が高い |

インストール後、以下を実施すること。

- 環境変数 `JAVA_HOME` を OpenJDK のインストールフォルダに設定する（例：`C:\Program Files\Eclipse Adoptium\jdk-17.x.x`）
- `PATH` に `%JAVA_HOME%\bin` を追加する
- コマンドプロンプトで以下を実行し、バージョンが表示されることを確認する

```
java -version
javac -version
```

**確認ポイント：** `openjdk 17.x.x` と表示されること（`java` ではなく `openjdk` であることを確認）

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

### 6. SQL Server Express LocalDB セットアップ

- SQL Server Express（LocalDB を含む）をインストールする（[Microsoft 公式サイト](https://www.microsoft.com/ja-jp/sql-server/sql-server-downloads) から「Express」をダウンロード → インストール種類で「カスタム」→「LocalDB」を選択）
- SSMS（SQL Server Management Studio）をインストールする
- コマンドプロンプトで LocalDB のインスタンス一覧を確認する

```
sqllocaldb info
```

- インスタンスが無い場合は作成する（例：`MSSQLLocalDB` という名前で自動作成されていることが多い）

```
sqllocaldb create MSSQLLocalDB
sqllocaldb start MSSQLLocalDB
```

- SSMS を起動し、サーバー名に以下を入力して接続する

```
(localdb)\MSSQLLocalDB
```

- 接続後、以下の SQL を実行して結果が返ることを確認する

```sql
SELECT 1
```

> **ポイント：** LocalDB は各自の PC 内で動くため、ホスト名やユーザー名・パスワードは不要（Windows 認証で接続する）。チームで共有する DB ではなく、各自のローカル環境にデータを持つ点を理解しておくこと。

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

以下をすべて担当者（Shuhey）に確認してもらうこと。

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

# 課題仕様書 ① 開発環境セットアップ

**フェーズ：** Phase 1　**期間目安：** 2〜3 日　**担当：** 新人

---

## 目的

Java 開発に必要なツールをすべてセットアップし、「Hello World」をビルド・実行できる状態を作る。
環境が整っていないと以降の課題がすべて止まるため、最優先で完了させること。

---

## 課題内容

以下の手順をすべて実施し、動作確認まで完了させること。

### 1. Eclipse（Pleiades All in One）セットアップ

Pleiades All in One の **Full Edition** には JDK・Tomcat が同梱されているため、
Java の個別インストールや環境変数の設定は不要。

1. [https://willbrains.jp/](https://willbrains.jp/) にアクセスし、最新の Eclipse をクリックする
2. **「Full Edition」** の Windows x64 版をダウンロードする
3. ダウンロードした ZIP ファイルを **`C:\`直下** に解凍する
   - 解凍先は必ず短いパス（`C:\pleiades` など）にすること（パスが長いと起動に失敗することがある）
   - 解凍ツールは Windows 標準機能または 7-Zip を使うこと（Lhaplus・Lhaca は不可）
4. `C:\pleiades\eclipse\eclipse.exe` を起動する
5. ワークスペースを任意のフォルダに設定する（例：`C:\workspace`）
6. メニューが日本語表示されていることを確認する

**JDK 確認：**
コマンドプロンプトで以下を実行し、バージョンが表示されることを確認する。

```
java -version
javac -version
```

> Full Edition に同梱の JDK が使われるため、`openjdk 17.x.x` または `21.x.x` と表示されれば OK。
> Eclipse の「ウィンドウ → 設定 → Java → インストール済み JRE」で認識されていることも確認すること。

---

### 2. VS Code セットアップ

- [https://code.visualstudio.com/](https://code.visualstudio.com/) から VS Code をインストールする
- 拡張機能「**Extension Pack for Java**」をインストールする

> VS Code はメインの IDE ではなく「調べもの・ちょっとしたファイル編集用」として使う。開発作業は基本的に Eclipse で行うこと。

---

### 3. Maven インストール

Pleiades に内蔵の Maven は Eclipse 内部からしか使えない。
コンソールから `mvn` コマンドを使うために別途インストールが必要。

1. [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) にアクセスする
2. **Binary zip archive**（例：`apache-maven-3.x.x-bin.zip`）をダウンロードする
3. ZIP を任意のフォルダに解凍する（例：`C:\maven\apache-maven-3.x.x`）
4. 環境変数を設定する
   - システム環境変数 `MAVEN_HOME` に解凍先のパスを設定する（例：`C:\maven\apache-maven-3.x.x`）
   - システム環境変数 `Path` に `%MAVEN_HOME%\bin` を追加する
5. コマンドプロンプトを再起動し、以下を実行してバージョンが表示されることを確認する

```
mvn -version
```

**確認ポイント：** `Apache Maven 3.x.x` と表示されること

---

### 4. Git セットアップ

- [https://git-scm.com/](https://git-scm.com/) から Git をインストールする
- インストール後、コマンドプロンプトで以下を実行して名前とメールアドレスを設定する

```
git config --global user.name "自分の名前"
git config --global user.email "自分のメールアドレス"
```

- Eclipse の Git パースペクティブが使えることを確認する（ウィンドウ → パースペクティブ → Git）

---

### 5. SQL Server LocalDB セットアップ

LocalDB は Visual Studio Installer の個別コンポーネントからインストールする。

1. スタートメニューから「**Visual Studio Installer**」を起動する
2. インストール済み VS の「**変更**」をクリックする
3. 「**個別のコンポーネント**」タブを開く
4. 検索ボックスに `LocalDB` と入力し、**「SQL Server Express 2019 LocalDB」**（または最新版）にチェックを入れる
5. 「**変更**」ボタンをクリックしてインストール

**インストール確認：**
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

---

### 6. SSMS セットアップ・接続確認

- [ダウンロードページ](https://learn.microsoft.com/ja-jp/sql/ssms/download-sql-server-management-studio-ssms) から SSMS（SQL Server Management Studio）をインストールする
- SSMS を起動し、以下の設定で接続する

| 項目 | 値 |
|---|---|
| サーバー名 | `(localdb)\MSSQLLocalDB` |
| 認証 | Windows 認証 |

- 接続後、以下の SQL を実行して結果が返ることを確認する

```sql
SELECT 1
```

> **ポイント：** LocalDB は Windows 認証で接続するため、ユーザー名・パスワードは不要。

---

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

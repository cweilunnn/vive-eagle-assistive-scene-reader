# VIVE Eagle Assistive Scene Reader

這是一個以「低視能者輔助」為核心的 Android Demo 專題。

專題目標是讓智慧眼鏡或手機相簿提供第一視角照片，交給 Gemini AI 判斷眼前環境，再用大字、詳細說明與語音提醒使用者附近是否有障礙或危險。

## 專題動機

對低視能者來說，人行道上的機車、施工圍欄、台階、坑洞或突然出現的障礙物，都可能造成外出時的不安與危險。

這個專題希望把 AI 智慧眼鏡變成一個隨身提醒工具：

1. 取得低視能者眼前的第一視角照片。
2. 用 AI 分析街景與障礙物。
3. 用簡潔大字顯示最重要的提醒。
4. 用小字顯示完整說明。
5. 用語音念出提醒，協助使用者做出安全判斷。

## 目前功能

- `一鍵分析最新相簿照片`
  - 適合搭配眼鏡官方 App 使用。
  - 先用眼鏡拍照並存進手機相簿，再回到本 App 一鍵讀取最新照片並送 Gemini 分析。

- `一鍵示範內建街景照片`
  - 使用專案內建的真實街景風格示範照片。
  - 適合課堂展示，不需要現場連接眼鏡。

- `手動選照片`
  - 從手機選擇任一照片進行 AI 分析。

- `重念提醒`
  - 重新播放最近一次 AI 產生的語音提醒。

## 技術重點

- Native Android app
- Java
- Gemini Developer API 影像分析
- Android MediaStore 讀取手機相簿最新照片
- Text-to-Speech 語音提醒
- 高對比、大字體 UI，方便低視能情境展示

## 安裝與執行

### 方式一：Android Studio

1. 安裝 Android Studio。
2. 開啟本專案資料夾。
3. 等待 Gradle Sync 完成。
4. 將 Android 手機接上 USB，開啟 USB debugging。
5. 選擇實體手機並執行 `app`。

### 方式二：Windows 一鍵安裝

專案內有提供：

```bat
install_and_open_app.bat
```

手機接上 USB 並允許 USB debugging 後，執行這個檔案會自動：

1. 編譯 debug APK。
2. 安裝到手機。
3. 打開 App。

## Gemini API Key

本專案不會把 API key 上傳到 GitHub。

請在本機新增或編輯 `local.properties`：

```properties
GEMINI_API_KEY=PASTE_YOUR_KEY_HERE
GEMINI_MODEL=gemini-2.5-flash
```

`local.properties` 已經被 `.gitignore` 排除，避免 API key 被公開。

更多設定方式請看 [docs/gemini-ai-setup.md](docs/gemini-ai-setup.md)。

## 專題限制

- 目前示範版的主要完成流程是「照片取得後進行 AI 分析」。
- 若眼鏡官方 App 可以把照片存到手機相簿，本 App 可直接讀取最新相簿照片分析。
- 若眼鏡照片只存在官方 App 私有資料夾，則需要官方 App 分享或匯出照片。
- AI 判斷可能出錯，本專題是輔助提醒，不應取代使用者本身判斷、白手杖、導盲犬或陪同者。

## 專案資料

- `app/`：Android App 原始碼
- `docs/`：設定與整合紀錄
- `gamma_assets/`：給 Gamma 生成簡報的 prompt
- `notebooklm_assets/`：NotebookLM 簡報素材與因果關係圖

## 安全與隱私提醒

智慧眼鏡與 AI 影像分析會接觸真實環境照片。使用時應避免上傳敏感資訊，也要注意是否拍攝到他人或私人場所。

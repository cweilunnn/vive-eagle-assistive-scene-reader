# Gemini AI Vision Setup

The app sends a selected, captured, or latest-gallery image to the Gemini Developer API and converts the response into a concise Traditional Chinese accessibility alert.

## 1. Create an API key

Open Google AI Studio:

https://aistudio.google.com/apikey

Create an API key for the Gemini Developer API. Free-tier availability and quotas are controlled by Google and may change.

## 2. Keep the key local

Open the project's `local.properties` file and add:

```properties
GEMINI_API_KEY=PASTE_YOUR_KEY_HERE
GEMINI_MODEL=gemini-2.5-flash
```

Do not put a real key in Java source, README examples, screenshots, or GitHub. `local.properties` is ignored by Git.

After editing the key, rebuild and reinstall the app.

## 3. Test with a real photo

1. Open the app on an Android phone.
2. Confirm the screen says `AI 模式：Gemini AI 真實影像分析`.
3. Use one of these buttons:
   - `一鍵分析最新相簿照片`
   - `一鍵示範內建街景照片`
   - `手動選照片`
4. Wait for the large concise result, smaller detail text, complete Gemini response, and spoken alert.

If the screen says the API key is missing, check `local.properties`, rebuild, and reinstall.

## Safety and key security

- AI output can be wrong. This is an experimental assistive prompt, not a safety guarantee.
- For a classroom demo, embedding a restricted API key in the APK is acceptable with caution. An APK can still be reverse-engineered.
- For a real product, call Gemini through your own backend. Never ship an unrestricted provider key inside the mobile app.
- Avoid uploading private or sensitive images. Public-space photography may involve bystanders and privacy concerns.

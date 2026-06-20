# Gemini AI Vision Setup

The app can send a selected or captured image to the Gemini Developer API and convert the response into a conservative accessibility alert.

## 1. Create a free API key

Open Google AI Studio:

https://aistudio.google.com/apikey

Create an API key for the Gemini Developer API. Free-tier availability and quotas are controlled by Google and may change.

## 2. Keep the key local

Open the project's `local.properties` file and add:

```properties
GEMINI_API_KEY=PASTE_YOUR_KEY_HERE
GEMINI_MODEL=gemini-2.5-flash
```

Do not put the key in Java source, README examples with a real value, or GitHub. `local.properties` is already ignored by Git.

After editing the key, run **Sync Project with Gradle Files**, then rebuild and reinstall the app.

## 3. Test with a real photo

1. Open the app on the ASUS phone.
2. Confirm the screen says `分析模式：Gemini AI 真實影像分析`.
3. Tap `選擇真實照片給 AI`.
4. Select a street or indoor scene photo from the phone.
5. Wait for the image, risk color, Traditional Chinese result, and spoken alert.

If the screen says `Mock 預設情境`, the API key was not included in the build. If the API call fails, the app explicitly labels the result as Mock fallback.

## Safety and key security

- AI output can be wrong. This is an experimental assistive prompt, not navigation or a safety guarantee.
- For a classroom demo, embedding a restricted API key in the APK is acceptable with caution. An APK can still be reverse-engineered.
- For a real product, call Gemini through your own backend. Never ship an unrestricted provider key inside the mobile app.
- Avoid uploading private or sensitive images. Public-space photography may involve bystanders and privacy concerns.

